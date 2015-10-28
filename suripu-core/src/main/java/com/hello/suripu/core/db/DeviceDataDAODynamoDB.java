package com.hello.suripu.core.db;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.util.Bucketing;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.db.util.DynamoDBItemAggregator;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jakepiccolo on 10/9/15.
 *
 * Schema:
 *  account_id(HK)  |  timestamp|external_device_id(RK)  |  ambient_temp  |  (rest of the values...)
 *
 * The hash key is the account_id, so it is required for getting device data and all data is attached to an account.
 *
 * The range key is the UTC timestamp, concatenated with the (external) device id. This is so that we can always query by time.
 * Why add on the external device ID? To ensure uniqueness of hash key + range key if you are paired to multiple Senses.
 * The device ID is added to the end of the range key so that using it in a range query is optional, i.e. it is possible to get
 * data for all devices for a given account and time range. Getting data for a specific device requires additional in-memory filtering.
 * Example range key: "2015-10-22 10:00|ABCDEF"
 *
 * We shard tables monthly based on the UTC timestamp, in order to better utilize our throughput on recent time series data.
 * See http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GuidelinesForTables.html#GuidelinesForTables.TimeSeriesDataAccessPatterns
 * and http://stackoverflow.com/a/30200359
 */
public class DeviceDataDAODynamoDB implements DeviceDataIngestDAO {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceDataDAODynamoDB.class);

    private final AmazonDynamoDB dynamoDBClient;
    private final String tablePrefix;

    public enum Attribute {
        ACCOUNT_ID ("aid", "N"),
        RANGE_KEY ("ts|dev", "S"),  // <utc_timestamp>|<external_device_id>
        AMBIENT_TEMP ("tmp", "N"),
        AMBIENT_LIGHT ("lite", "N"),
        AMBIENT_LIGHT_VARIANCE ("litevar", "N"),
        AMBIENT_HUMIDITY ("hum", "N"),
        AMBIENT_AIR_QUALITY_RAW ("aqr", "N"),
        AUDIO_PEAK_BACKGROUND_DB ("apbg", "N"),
        AUDIO_PEAK_DISTURBANCES_DB ("apd", "N"),
        AUDIO_NUM_DISTURBANCES ("and", "N"),
        OFFSET_MILLIS ("om", "N"),
        LOCAL_UTC_TIMESTAMP ("lutcts", "S"),
        WAVE_COUNT ("wc", "N"),
        HOLD_COUNT ("hc", "N");

        public final String name;
        public final String type;

        Attribute(String name, String type) {
            this.name = name;
            this.type = type;
        }

        /**
         * Useful instead of item.get(Attribute.<Attribute>.name) to avoid NullPointerException
         * @param item
         * @return
         */
        AttributeValue get(final Map<String, AttributeValue> item) {
            return item.get(this.name);
        }
    }

    private static final int MAX_PUT_ITEMS = 25;
    private static final int MAX_BATCH_WRITE_ATTEMPTS = 5;
    private static final int MAX_QUERY_ATTEMPTS = 5;

    // Store everything to the minute level
    private static final DateTimeFormatter DATE_TIME_READ_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:00Z");
    private static final String DATE_TIME_STRING_TEMPLATE = "yyyy-MM-dd HH:mm";
    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_STRING_TEMPLATE);

    public DeviceDataDAODynamoDB(final AmazonDynamoDB dynamoDBClient, final String tablePrefix) {
        this.dynamoDBClient = dynamoDBClient;
        this.tablePrefix = tablePrefix;
    }

    public String getTableName(final DateTime dateTime) {
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy_MM");
        return tablePrefix + "_" + dateTime.toString(formatter);
    }

    public CreateTableResult createTable(final String tableName) {
        final Attribute hashKeyAttribute = Attribute.ACCOUNT_ID;
        final Attribute rangeKeyAttribute = Attribute.RANGE_KEY;

        // attributes
        ArrayList<AttributeDefinition> attributes = Lists.newArrayList();
        attributes.add(new AttributeDefinition().withAttributeName(hashKeyAttribute.name).withAttributeType(hashKeyAttribute.type));
        attributes.add(new AttributeDefinition().withAttributeName(rangeKeyAttribute.name).withAttributeType(rangeKeyAttribute.type));

        // keys
        ArrayList<KeySchemaElement> keySchema = Lists.newArrayList();
        keySchema.add(new KeySchemaElement().withAttributeName(hashKeyAttribute.name).withKeyType(KeyType.HASH));
        keySchema.add(new KeySchemaElement().withAttributeName(rangeKeyAttribute.name).withKeyType(KeyType.RANGE));

        // throughput provision
        // TODO make this configurable
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributes)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return dynamoDBClient.createTable(request);
    }

    private AttributeValue dateTimeToAttributeValue(final DateTime dateTime) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER));
    }

    private AttributeValue getRangeKey(final DateTime dateTime, final String senseId) {
        return new AttributeValue(dateTime.toString(DATE_TIME_WRITE_FORMATTER) + "|" + senseId);
    }

    private HashMap<String, AttributeValue> deviceDataToAttributeMap(final DeviceData data) {
        final HashMap<String, AttributeValue> item = Maps.newHashMap();
        item.put(Attribute.ACCOUNT_ID.name, new AttributeValue().withN(String.valueOf(data.accountId)));
        item.put(Attribute.RANGE_KEY.name, getRangeKey(data.dateTimeUTC, data.externalDeviceId));
        item.put(Attribute.AMBIENT_TEMP.name, new AttributeValue().withN(String.valueOf(data.ambientTemperature)));
        item.put(Attribute.AMBIENT_LIGHT.name, new AttributeValue().withN(String.valueOf(data.ambientLight)));
        item.put(Attribute.AMBIENT_LIGHT_VARIANCE.name, new AttributeValue().withN(String.valueOf(data.ambientLightVariance)));
        item.put(Attribute.AMBIENT_HUMIDITY.name, new AttributeValue().withN(String.valueOf(data.ambientHumidity)));
        item.put(Attribute.AMBIENT_AIR_QUALITY_RAW.name, new AttributeValue().withN(String.valueOf(data.ambientAirQualityRaw)));
        item.put(Attribute.AUDIO_PEAK_BACKGROUND_DB.name, new AttributeValue().withN(String.valueOf(data.audioPeakBackgroundDB)));
        item.put(Attribute.AUDIO_PEAK_DISTURBANCES_DB.name, new AttributeValue().withN(String.valueOf(data.audioPeakDisturbancesDB)));
        item.put(Attribute.AUDIO_NUM_DISTURBANCES.name, new AttributeValue().withN(String.valueOf(data.audioNumDisturbances)));
        item.put(Attribute.WAVE_COUNT.name, new AttributeValue().withN(String.valueOf(data.waveCount)));
        item.put(Attribute.HOLD_COUNT.name, new AttributeValue().withN(String.valueOf(data.holdCount)));
        item.put(Attribute.LOCAL_UTC_TIMESTAMP.name, dateTimeToAttributeValue(data.dateTimeUTC.plusMillis(data.offsetMillis)));
        item.put(Attribute.OFFSET_MILLIS.name, new AttributeValue().withN(String.valueOf(data.offsetMillis)));
        return item;
    }

    private void backoff(int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("Throttled by DynamoDB, sleeping for {} ms.", sleepMillis);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while attempting exponential backoff.");
        }
    }

    /**
     * Convert a list of DeviceData to write request items, which can be used for batchInsert.
     * @param deviceDataList
     * @return Map of {tableName => writeRequests}
     */
    public Map<String, List<WriteRequest>> toWriteRequestItems(final List<DeviceData> deviceDataList) {
        // Create a map with hash+range as the key to deduplicate and avoid DynamoDB exceptions
        final Map<String, Map<String, WriteRequest>> writeRequestMap = Maps.newHashMap();
        for (final DeviceData data: deviceDataList) {
            final String tableName = getTableName(data.dateTimeUTC);
            final Map<String, AttributeValue> item = deviceDataToAttributeMap(data);
            final String hashAndRangeKey = item.get(Attribute.ACCOUNT_ID.name).getN() + item.get(Attribute.RANGE_KEY.name).getS();
            final WriteRequest request = new WriteRequest().withPutRequest(new PutRequest().withItem(item));
            if (writeRequestMap.containsKey(tableName)) {
                writeRequestMap.get(tableName).put(hashAndRangeKey, request);
            } else {
                final Map<String, WriteRequest> newMap = Maps.newHashMap();
                newMap.put(hashAndRangeKey, request);
                writeRequestMap.put(tableName, newMap);
            }
        }

        final Map<String, List<WriteRequest>> requestItems = Maps.newHashMapWithExpectedSize(writeRequestMap.size());
        for (final Map.Entry<String, Map<String, WriteRequest>> entry : writeRequestMap.entrySet()) {
            requestItems.put(entry.getKey(), Lists.newArrayList(entry.getValue().values()));
        }

        return requestItems;
    }

    /**
     * Batch insert write requests.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * Utilizes exponential backoff in case of throttling.
     *
     * @param requestItems - map of {tableName => writeRequests}
     * @return the remaining unprocessed items. The return value of this method can be used to call this method again
     * to process remaining items.
     */
    public Map<String, List<WriteRequest>> batchInsert(final Map<String, List<WriteRequest>> requestItems) {
        int numAttempts = 0;
        Map<String, List<WriteRequest>> remainingItems = requestItems;

        do {
            if (numAttempts > 0) {
                // Being throttled! Back off, buddy.
                backoff(numAttempts);
            }

            numAttempts++;
            final BatchWriteItemRequest batchWriteItemRequest = new BatchWriteItemRequest().withRequestItems(remainingItems);
            final BatchWriteItemResult result = this.dynamoDBClient.batchWriteItem(batchWriteItemRequest);
            // check for unprocessed items
            remainingItems = result.getUnprocessedItems();
        } while (!remainingItems.isEmpty() && (numAttempts < MAX_BATCH_WRITE_ATTEMPTS));

        return remainingItems;
    }

    private int countWriteRequestItems(final Map<String, List<WriteRequest>> requestItems) {
        int total = 0;
        for (final List<WriteRequest> writeRequests : requestItems.values()) {
            total += writeRequests.size();
        }
        return total;
    }

    /**
     * Batch insert list of DeviceData objects.
     * Subject to DynamoDB's maximum BatchWriteItem size.
     * @param deviceDataList
     * @return The number of successfully inserted elements.
     */
    public int batchInsert(final List<DeviceData> deviceDataList) {

        final Map<String, List<WriteRequest>> requestItems = toWriteRequestItems(deviceDataList);
        final Map<String, List<WriteRequest>> remainingItems = batchInsert(requestItems);

        final int totalItemsToInsert = countWriteRequestItems(requestItems);

        if (!remainingItems.isEmpty()) {
            final int remainingItemsCount = countWriteRequestItems(remainingItems);
            LOGGER.warn("Exceeded {} attempts to batch write to Dynamo. {} items left over.",
                    MAX_BATCH_WRITE_ATTEMPTS, remainingItemsCount);
            return totalItemsToInsert - remainingItemsCount;
        }

        return totalItemsToInsert;
    }

    /**
     * Partitions and inserts list of DeviceData objects.
     * @param deviceDataList
     * @return The number of items that were successfully inserted
     */
    public int batchInsertAll(final List<DeviceData> deviceDataList) {
        final List<List<DeviceData>> deviceDataLists = Lists.partition(deviceDataList, MAX_PUT_ITEMS);
        int successfulInsertions = 0;

        // Insert each chunk
        for (final List<DeviceData> deviceDataListToWrite: deviceDataLists) {
            try {
                successfulInsertions += batchInsert(deviceDataListToWrite);
            } catch (AmazonClientException e) {
                LOGGER.error("Got exception while attempting to batchInsert to DynamoDB: {}", e);
            }

        }

        return successfulInsertions;
    }

    @Override
    public Class name() {
        return  DeviceDataDAODynamoDB.class;
    }

    private DateTime getFloorOfDateTime(final DateTime dateTime, final Integer toMinutes) {
        return new DateTime(dateTime, DateTimeZone.UTC).withMinuteOfHour(dateTime.getMinuteOfHour() - (dateTime.getMinuteOfHour() % toMinutes));
    }

    private DateTime timestampFromDDBItem(final Map<String, AttributeValue> item) {
        final String dateString = Attribute.RANGE_KEY.get(item).getS().substring(0, DATE_TIME_STRING_TEMPLATE.length());
        return DateTime.parse(dateString + ":00Z", DATE_TIME_READ_FORMATTER);
    }

    private String externalDeviceIdFromDDBItem(final Map<String, AttributeValue> item) {
        return item.get(Attribute.RANGE_KEY.name).getS().substring(DATE_TIME_STRING_TEMPLATE.length() + 1);
    }

    private DeviceData aggregateDynamoDBItemsToDeviceData(final List<Map<String, AttributeValue>> items, final DeviceData template) {
        final DynamoDBItemAggregator aggregator = new DynamoDBItemAggregator(items);
        return new DeviceData.Builder()
                .withAccountId(template.accountId)
                .withExternalDeviceId(template.externalDeviceId)
                .withDateTimeUTC(template.dateTimeUTC)
                .withOffsetMillis(template.offsetMillis)
                .withAmbientTemperature((int) aggregator.min(Attribute.AMBIENT_TEMP.name))
                .calibrateAmbientLight((int) aggregator.roundedMean(Attribute.AMBIENT_LIGHT.name))
                .withAmbientLightVariance((int) aggregator.roundedMean(Attribute.AMBIENT_LIGHT_VARIANCE.name))
                .withAmbientHumidity((int) aggregator.roundedMean(Attribute.AMBIENT_HUMIDITY.name))
                .withWaveCount((int) aggregator.sum(Attribute.WAVE_COUNT.name))
                .withHoldCount((int) aggregator.sum(Attribute.HOLD_COUNT.name))
                .withAudioNumDisturbances((int) aggregator.max(Attribute.AUDIO_NUM_DISTURBANCES.name))
                .withAudioPeakBackgroundDB((int) aggregator.max(Attribute.AUDIO_PEAK_BACKGROUND_DB.name))
                .withAudioPeakDisturbancesDB((int) aggregator.max(Attribute.AUDIO_PEAK_DISTURBANCES_DB.name))
                .withAmbientAirQualityRaw((int) aggregator.roundedMean(Attribute.AMBIENT_AIR_QUALITY_RAW.name))
                .build();
    }

    private List<DeviceData> aggregateDynamoDBItemsToDeviceData(final List<Map<String, AttributeValue>> items, final Integer slotDuration) {
        final List<DeviceData> resultList = Lists.newLinkedList();
        LinkedList<Map<String, AttributeValue>> currentWorkingList = Lists.newLinkedList();
        final DeviceData.Builder templateBuilder = new DeviceData.Builder();
        for (final Map<String, AttributeValue> item: items) {
            final DateTime itemDateTime = timestampFromDDBItem(item);
            if (currentWorkingList.isEmpty()) {
                // First iteration
                currentWorkingList.add(item);
            } else if (timestampFromDDBItem(currentWorkingList.getLast()).isAfter(itemDateTime)) {
                // Unsorted list
                throw new IllegalArgumentException("Input DeviceDatas must be sorted.");
            } else if (getFloorOfDateTime(timestampFromDDBItem(currentWorkingList.getLast()), slotDuration)
                    .plusMinutes(slotDuration)
                    .isAfter(itemDateTime)) {
                // Within the window of our current working set.
                currentWorkingList.add(item);
            } else {
                // Outside the window, aggregate working set to single value.
                resultList.add(aggregateDynamoDBItemsToDeviceData(currentWorkingList, templateBuilder.build()));

                // Create new working sets
                currentWorkingList = Lists.newLinkedList();
                currentWorkingList.add(item);
            }
            templateBuilder
                    .withDateTimeUTC(getFloorOfDateTime(itemDateTime, slotDuration))
                    .withAccountId(Long.valueOf(item.get(Attribute.ACCOUNT_ID.name).getN()))
                    .withExternalDeviceId(externalDeviceIdFromDDBItem(item))
                    .withOffsetMillis(Integer.valueOf(item.get(Attribute.OFFSET_MILLIS.name).getN()));
        }
        if (!currentWorkingList.isEmpty()) {
            resultList.add(aggregateDynamoDBItemsToDeviceData(currentWorkingList, templateBuilder.build()));
        }
        return resultList;
    }

    private List<Map<String, AttributeValue>> query(final String tableName,
                                                    final Map<String, Condition> queryConditions,
                                                    final Collection<String> targetAttributes)
    {
        final List<Map<String, AttributeValue>> results = Lists.newArrayList();

        Map<String, AttributeValue> lastEvaluatedKey = null;
        int numAttempts = 0;
        boolean keepTrying = true;

        do {
            numAttempts++;
            final QueryRequest queryRequest = new QueryRequest()
                    .withTableName(tableName)
                    .withKeyConditions(queryConditions)
                    .withAttributesToGet(targetAttributes)
                    .withExclusiveStartKey(lastEvaluatedKey);

            final QueryResult queryResult;
            try {
                queryResult = this.dynamoDBClient.query(queryRequest);
            } catch (ProvisionedThroughputExceededException e) {
                backoff(numAttempts);
                continue;
            }
            final List<Map<String, AttributeValue>> items = queryResult.getItems();

            if (queryResult.getItems() != null) {
                for (final Map<String, AttributeValue> item : items) {
                    if (!item.keySet().containsAll(targetAttributes)) {
                        LOGGER.warn("Missing field in item {}", item);
                        continue;
                    }
                    results.add(item);
                }
            }

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();
            keepTrying = (lastEvaluatedKey != null);

        } while (keepTrying && (numAttempts < MAX_QUERY_ATTEMPTS));

        // TODO should actually probably throw an error or return a flag here if your query could not complete
        if (lastEvaluatedKey != null) {
            LOGGER.warn("Exceeded {} attempts while querying. Stopping with last evaluated key: {}",
                    MAX_QUERY_ATTEMPTS, lastEvaluatedKey);
        }

        return results;
    }

    public ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long accountId,
            final String externalDeviceId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration,
            final Collection<Attribute> targetAttributes)
    {

        final Condition selectByAccountId  = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN(String.valueOf(accountId)));

        final Condition selectByTimestamp = new Condition()
                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                .withAttributeValueList(
                        getRangeKey(start, externalDeviceId),
                        getRangeKey(end, externalDeviceId));

        final Map<String, Condition> queryConditions = Maps.newHashMap();
        queryConditions.put(Attribute.ACCOUNT_ID.name, selectByAccountId);
        queryConditions.put(Attribute.RANGE_KEY.name, selectByTimestamp);

        final List<String> targetAttributeNames = Lists.newArrayList();
        for (final Attribute attribute : targetAttributes) {
            targetAttributeNames.add(attribute.name);
        }

        final List<Map<String, AttributeValue>> results = Lists.newArrayList();
        for (final DateTime dateTime: DateTimeUtil.dateTimesForStartOfMonthBetweenDates(start, end)) {
            final String tableName = getTableName(dateTime);
            results.addAll(query(tableName, queryConditions, targetAttributeNames));
        }

        final List<Map<String, AttributeValue>> filteredResults = Lists.newLinkedList();
        for (final Map<String, AttributeValue> item : results) {
            if (externalDeviceIdFromDDBItem(item).equals(externalDeviceId)) {
                filteredResults.add(item);
            }
        }

        return ImmutableList.copyOf(aggregateDynamoDBItemsToDeviceData(filteredResults, slotDuration));
    }


    public ImmutableList<DeviceData> getBetweenByAbsoluteTimeAggregateBySlotDuration(
            final Long accountId,
            final String externalDeviceId,
            final DateTime start,
            final DateTime end,
            final Integer slotDuration)
    {
        return getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, start, end, slotDuration, Arrays.asList(Attribute.values()));
    }

    private final static ImmutableSet<Attribute> BASE_ATTRIBUTES = new ImmutableSet.Builder<Attribute>()
            .add(Attribute.ACCOUNT_ID)
            .add(Attribute.OFFSET_MILLIS)
            .add(Attribute.LOCAL_UTC_TIMESTAMP)
            .add(Attribute.RANGE_KEY)
            .build();

    private final static Map<String, Set<Attribute>> SENSOR_NAME_TO_ATTRIBUTES = new ImmutableMap.Builder<String, Set<Attribute>>()
            .put("humidity", ImmutableSet.of(Attribute.AMBIENT_TEMP, Attribute.AMBIENT_HUMIDITY))
            .put("temperature", ImmutableSet.of(Attribute.AMBIENT_TEMP))
            .put("particulates", ImmutableSet.of(Attribute.AMBIENT_AIR_QUALITY_RAW))
            .put("light", ImmutableSet.of(Attribute.AMBIENT_LIGHT))
            .put("sound", ImmutableSet.of(Attribute.AUDIO_PEAK_BACKGROUND_DB, Attribute.AUDIO_PEAK_DISTURBANCES_DB))
            .build();

    private static Set<Attribute> sensorNameToAttributeNames(final String sensorName) {
        final Set<Attribute> sensorAttributes = SENSOR_NAME_TO_ATTRIBUTES.get(sensorName);
        if (sensorAttributes == null) {
            throw new IllegalArgumentException("Unknown sensor name: '" + sensorName + "'");
        }
        return new ImmutableSet.Builder<Attribute>().addAll(BASE_ATTRIBUTES).addAll(sensorAttributes).build();
    }

    @Timed
    public List<Sample> generateTimeSeriesByUTCTime(
            final Long queryStartTimestampInUTC,
            final Long queryEndTimestampInUTC,
            final Long accountId,
            final String externalDeviceId,
            final int slotDurationInMinutes,
            final String sensor,
            final Integer missingDataDefaultValue,
            final Optional<Device.Color> color,
            final Optional<Calibration> calibrationOptional) throws IllegalArgumentException {

        final DateTime queryEndTime = new DateTime(queryEndTimestampInUTC, DateTimeZone.UTC);
        final DateTime queryStartTime = new DateTime(queryStartTimestampInUTC, DateTimeZone.UTC);

        LOGGER.trace("Client utcTimeStamp : {} ({})", queryEndTimestampInUTC, new DateTime(queryEndTimestampInUTC));
        LOGGER.trace("QueryEndTime: {} ({})", queryEndTime, queryEndTime.getMillis());
        LOGGER.trace("QueryStartTime: {} ({})", queryStartTime, queryStartTime.getMillis());

        LOGGER.debug("Calling getBetweenByAbsoluteTimeAggregateBySlotDuration with arguments: ({}, {}, {}, {}, {}, {})", accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
        final List<DeviceData> rows = getBetweenByAbsoluteTimeAggregateBySlotDuration(accountId, externalDeviceId, queryStartTime, queryEndTime, slotDurationInMinutes, sensorNameToAttributeNames(sensor));
        LOGGER.debug("Retrieved {} rows from database", rows.size());

        if(rows.size() == 0) {
            return new ArrayList<>();
        }

        // create buckets with keys in UTC-Time
        final int currentOffsetMillis = rows.get(0).offsetMillis;
        final DateTime now = queryEndTime.withSecondOfMinute(0).withMillisOfSecond(0);
        final int remainder = now.getMinuteOfHour() % slotDurationInMinutes;
        // if 4:36 -> bucket = 4:35

        final DateTime nowRounded = now.minusMinutes(remainder);
        LOGGER.trace("Current Offset Milis = {}", currentOffsetMillis);
        LOGGER.trace("Remainder = {}", remainder);
        LOGGER.trace("Now (rounded) = {} ({})", nowRounded, nowRounded.getMillis());


        final long absoluteIntervalMS = queryEndTimestampInUTC - queryStartTimestampInUTC;
        final int numberOfBuckets= (int) ((absoluteIntervalMS / DateTimeConstants.MILLIS_PER_MINUTE) / slotDurationInMinutes + 1);

        final Map<Long, Sample> map = Bucketing.generateEmptyMap(numberOfBuckets, nowRounded, slotDurationInMinutes, missingDataDefaultValue);

        LOGGER.trace("Map size = {}", map.size());

        final Optional<Map<Long, Sample>> optionalPopulatedMap = Bucketing.populateMap(rows, sensor, color, calibrationOptional);

        if(!optionalPopulatedMap.isPresent()) {
            LOGGER.debug("Map not populated, returning empty list of samples.");
            return Collections.EMPTY_LIST;
        }

        // Override map with values from DB
        final Map<Long, Sample> merged = Bucketing.mergeResults(map, optionalPopulatedMap.get());

        LOGGER.trace("New map size = {}", merged.size());

        final List<Sample> sortedList = Bucketing.sortResults(merged, currentOffsetMillis);
        return sortedList;

    }
}
