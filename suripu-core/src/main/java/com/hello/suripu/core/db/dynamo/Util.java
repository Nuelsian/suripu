package com.hello.suripu.core.db.dynamo;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.db.responses.SingleItemDynamoDBResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by jakepiccolo on 3/9/16.
 */
public class Util {
    private final static Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private static void backoff(final String tableName, int numberOfAttempts) {
        try {
            long sleepMillis = (long) Math.pow(2, numberOfAttempts) * 50;
            LOGGER.warn("reason=dynamodb-throttling sleep-millis={} table={}", sleepMillis, tableName);
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOGGER.error("error=interrupted-exception table={} exception={}", tableName, e);
        }
    }

    public static SingleItemDynamoDBResponse getWithBackoff(final AmazonDynamoDB dynamoDB, final String tableName, final Map<String, AttributeValue> key) {
        GetItemResult result = null;
        int numAttempts = 0;

        do {
            numAttempts++;
            try {
                result = dynamoDB.getItem(tableName, key);
            } catch (ResourceNotFoundException rnfe) {
                return new SingleItemDynamoDBResponse(Optional.<Map<String,AttributeValue>>absent(), Response.Status.FAILURE, Optional.of(rnfe));
            } catch (ProvisionedThroughputExceededException ptee) {
                if (numAttempts < 5) {
                    backoff(tableName, numAttempts);
                } else {
                    LOGGER.error("error=ProvisionedThroughputExceededException tries={} status=aborted table={}",
                            numAttempts, tableName);
                    return new SingleItemDynamoDBResponse(Optional.<Map<String,AttributeValue>>absent(), Response.Status.FAILURE, Optional.of(ptee));
                }
            }
        } while (result == null);

        if (result.getItem() == null) {
            return new SingleItemDynamoDBResponse(Optional.<Map<String,AttributeValue>>absent(), Response.Status.FAILURE, Optional.<Exception>absent());
        }
        return new SingleItemDynamoDBResponse(Optional.of(result.getItem()), Response.Status.SUCCESS, Optional.<Exception>absent());
    }


    // region Table creation
    private static CreateTableResult createTable(final AmazonDynamoDB dynamoDB, final String tableName,
                                                 final List<AttributeDefinition> attributeDefinitions,
                                                 final List<KeySchemaElement> keySchema,
                                                 final Long readCapacityUnits, final Long writeCapacityUnits)
    {
        // throughput provision
        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(readCapacityUnits)
                .withWriteCapacityUnits(writeCapacityUnits);

        final CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(provisionedThroughput);

        return dynamoDB.createTable(request);
    }

    /**
     * Create a table with the given hash key.
     */
    public static CreateTableResult createTable(final AmazonDynamoDB dynamoDB, final String tableName, final Attribute hashKey,
                                                final Long readCapacityUnits, final Long writeCapacityUnits)
    {
        List<AttributeDefinition> attributes = ImmutableList.of(
                new AttributeDefinition().withAttributeName(hashKey.shortName()).withAttributeType(hashKey.type())
        );

        // Keys
        List<KeySchemaElement> keySchema = ImmutableList.of(
                new KeySchemaElement().withAttributeName(hashKey.shortName()).withKeyType(KeyType.HASH)
        );

        return createTable(dynamoDB, tableName, attributes, keySchema, readCapacityUnits, writeCapacityUnits);
    }

    /**
     * Create a table with the given hash and range keys.
     */
    public static CreateTableResult createTable(final AmazonDynamoDB dynamoDB, final String tableName,
                                                final Attribute hashKey, final Attribute rangeKey,
                                                final Long readCapacityUnits, final Long writeCapacityUnits)
    {
        List<AttributeDefinition> attributes = ImmutableList.of(
                new AttributeDefinition().withAttributeName(hashKey.shortName()).withAttributeType(hashKey.type()),
                new AttributeDefinition().withAttributeName(rangeKey.shortName()).withAttributeType(rangeKey.type())
        );

        // Keys
        List<KeySchemaElement> keySchema = ImmutableList.of(
                new KeySchemaElement().withAttributeName(hashKey.shortName()).withKeyType(KeyType.HASH),
                new KeySchemaElement().withAttributeName(rangeKey.shortName()).withKeyType(KeyType.RANGE)
        );

        return createTable(dynamoDB, tableName, attributes, keySchema, readCapacityUnits, writeCapacityUnits);
    }
    // endregion Table creation
}
