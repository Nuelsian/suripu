package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.TrendsDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateScore;
import com.hello.suripu.core.models.Insights.AvailableGraph;
import com.hello.suripu.core.models.Insights.DowSample;
import com.hello.suripu.core.models.Insights.GraphSample;
import com.hello.suripu.core.models.Insights.TrendGraph;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 12/15/14.
 */
public class TrendGraphProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendGraphProcessor.class);
    private static int TRENDS_AVAILABLE_AFTER_DAYS = 10; // no trends before collecting 10 days of data
    private static long DAY_IN_MILLIS = 86400000;

    private final TrendsDAO trendsDAO;
    private final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB;
    private final TrackerMotionDAO trackerMotionDAO;

    public TrendGraphProcessor(TrendsDAO trendsDAO, AggregateSleepScoreDAODynamoDB scoreDAODynamoDB, TrackerMotionDAO trackerMotionDAO) {
        this.trendsDAO = trendsDAO;
        this.scoreDAODynamoDB = scoreDAODynamoDB;
        this.trackerMotionDAO = trackerMotionDAO;
    }

    @Timed
    public List<AvailableGraph> getGraphList(final Account account) {
        final List<AvailableGraph> graphlist = new ArrayList<>();
        final boolean eligible = checkEligibility(account.created);
        if (eligible) {
            graphlist.add(new AvailableGraph(TrendGraph.DataType.SLEEP_DURATION.getValue(), TrendGraph.TimePeriodType.DAY_OF_WEEK.getValue()));

            for (TrendGraph.TimePeriodType timePeriodType : TrendGraph.TimePeriodType.values()) {
                graphlist.add(new AvailableGraph(TrendGraph.DataType.SLEEP_SCORE.getValue(), timePeriodType.getValue()));
            }
        }
        return graphlist;
    }

    @Timed
    public List<TrendGraph> getAllGraphs(final Account account) {
        final List<TrendGraph> graphs = new ArrayList<>();
        final boolean eligible = checkEligibility(account.created);
        if (eligible) {
            final long accountId = account.id.get();
            graphs.add(getTrendGraph(accountId, TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.HISTOGRAM, TrendGraph.TimePeriodType.DAY_OF_WEEK));
            graphs.add(getTrendGraph(accountId, TrendGraph.DataType.SLEEP_DURATION, TrendGraph.GraphType.HISTOGRAM, TrendGraph.TimePeriodType.DAY_OF_WEEK));
            graphs.add(getTrendGraph(accountId, TrendGraph.DataType.SLEEP_SCORE, TrendGraph.GraphType.TIME_SERIES_LINE, TrendGraph.TimePeriodType.OVER_TIME_ALL));
        }
        return graphs;
    }

    @Timed
    public TrendGraph getTrendGraph(final long accountId, final TrendGraph.DataType dataType,
                                           final TrendGraph.GraphType graphType,
                                           final TrendGraph.TimePeriodType timePeriodType) {
        final List<GraphSample> dataPoints = new ArrayList<>();

        if (graphType == TrendGraph.GraphType.HISTOGRAM) {
            dataPoints.addAll(getTrendsDowData(dataType, accountId));

        } else if (graphType == TrendGraph.GraphType.TIME_SERIES_LINE && timePeriodType != TrendGraph.TimePeriodType.DAY_OF_WEEK) {
            if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
                dataPoints.addAll(getScoreOverTimeData(accountId, timePeriodType));
            }
        }
        return new TrendGraph(dataType, graphType, timePeriodType, dataPoints);
    }


    private List<GraphSample> getTrendsDowData(final TrendGraph.DataType dataType, final long accountId) {
        // histogram data
        ImmutableList<DowSample> dowSamples = ImmutableList.copyOf(new ArrayList<DowSample>());
        if (dataType == TrendGraph.DataType.SLEEP_SCORE) {
            dowSamples = this.trendsDAO.getSleepScoreDow(accountId);
        } else if (dataType == TrendGraph.DataType.SLEEP_DURATION) {
            dowSamples = this.trendsDAO.getSleepDurationDow(accountId);
        }

        if (dowSamples.size() > 0) {
            final Map<Integer, Float> samplesMap = new HashMap<>();
            for (final DowSample sample : dowSamples) {
                samplesMap.put(sample.dayOfWeek, sample.value);
            }

            final List<GraphSample> sampleData = new ArrayList<>();
            for (int dow = 1; dow <= 7; dow++) {
                final String xLabel = TrendGraph.DayOfWeekLabel.fromInt(dow);
                if (samplesMap.containsKey(dow)) {
                    final float value = samplesMap.get(dow);
                    final TrendGraph.DataLabel label = TrendGraph.getDataLabel(dataType, value);
                    sampleData.add(new GraphSample(xLabel, value, label));
                    continue;
                }
                sampleData.add(new GraphSample(xLabel, 0.0f, TrendGraph.DataLabel.OK));
            }
            return sampleData;
        }

        return Collections.emptyList();
    }

    private List<GraphSample> getScoreOverTimeData(final long accountId, TrendGraph.TimePeriodType timePeriodType) {
        // over time graph is only available for sleep-score at this time
        final int numDays = TrendGraph.getTimePeriodDays(timePeriodType);
        final DateTime endDateTime = DateTime.now().withTimeAtStartOfDay();
        final DateTime startDateTime = endDateTime.minusDays(numDays);

        // get timezone offsets for the required dates
        final Map<DateTime, Integer> userOffsetMillis = this.getUserTimeZoneOffsets(accountId, startDateTime, endDateTime);

        // get daily scores
        final ImmutableList<AggregateScore> scores = this.scoreDAODynamoDB.getBatchScores(accountId,
                DateTimeUtil.dateToYmdString(startDateTime),
                DateTimeUtil.dateToYmdString(endDateTime), numDays);

        // aggregate
        final List<GraphSample> sampleData = new ArrayList<>();
        for (final AggregateScore score : scores) {
            final float value = (float) score.score;
            final TrendGraph.DataLabel label = TrendGraph.getDataLabel(TrendGraph.DataType.SLEEP_SCORE, value);
            final DateTime date = getDateTimeFromString(score.date);
            int offsetMillis = 0;
            if (userOffsetMillis.containsKey(date)) {
                offsetMillis = userOffsetMillis.get(date);
            }
            sampleData.add(new GraphSample(date.getMillis(), value, offsetMillis, label));
        }
        return sampleData;
    }

    private Map<DateTime, Integer> getUserTimeZoneOffsets(final long accountId, final DateTime startDate, final DateTime endDate) {
        final long daysDiff = (endDate.getMillis() - startDate.getMillis()) / DAY_IN_MILLIS;
        final List<DateTime> dates = new ArrayList<>();
        for (int i = 0; i < (int) daysDiff; i++) {
            dates.add(startDate.plusDays(i));
        }
        return this.trackerMotionDAO.getOffsetMillisForDates(accountId, dates);
    }

    private static boolean checkEligibility(final DateTime accountCreated) {
        if (accountCreated.plusDays(TRENDS_AVAILABLE_AFTER_DAYS).isBeforeNow()) {
            return true;
        }
        return false;
    }

    private static DateTime getDateTimeFromString(final String dateString) {
        return  DateTime.parse(dateString, DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATE_FORMAT))
                .withZone(DateTimeZone.UTC).withTimeAtStartOfDay();
    }
}
