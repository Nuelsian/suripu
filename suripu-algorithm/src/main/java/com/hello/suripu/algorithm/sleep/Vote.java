package com.hello.suripu.algorithm.sleep;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.DataUtils;
import com.hello.suripu.algorithm.utils.MotionFeatures;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import com.sun.tools.javac.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pangwu on 3/19/15.
 */
public class Vote {
    private final static Logger LOGGER = LoggerFactory.getLogger(Vote.class);
    private final MotionScoreAlgorithm motionScoreAlgorithmDefault;

    private final MotionCluster motionCluster;
    private final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures;
    private final double rawAmpMean;
    private final double rawKickOffMean;

    private final SleepPeriod sleepPeriod;

    private final boolean insertEmpty = true;
    private final boolean removeNoise = false;
    private final boolean capScoreOutOfPeriod = false;
    private final boolean defaultOverride = false;
    private final boolean newSearch = true;
    private final boolean newSearchWakeUp = true;

    public Vote(final List<AmplitudeData> rawData,
                final List<AmplitudeData> kickOffCounts,
                final List<AmplitudeData> rawSound,
                final List<DateTime> lightOutTimes,
                final Optional<DateTime> firstWaveTimeOptional){
        if(firstWaveTimeOptional.isPresent()) {
            LOGGER.debug("--------> Wave {}, millis {}", firstWaveTimeOptional.get(), firstWaveTimeOptional.get().getMillis());
        }

        this.motionScoreAlgorithmDefault = MotionScoreAlgorithm.createDefault(rawData,
                lightOutTimes,
                firstWaveTimeOptional);

        final List<AmplitudeData> noDuplicates = DataUtils.makePositive(DataUtils.dedupe(rawData));
        this.rawAmpMean = NumericalUtils.mean(noDuplicates);
        final List<AmplitudeData> noDuplicateKickOffCounts = DataUtils.dedupe(kickOffCounts);
        this.rawKickOffMean = NumericalUtils.mean(noDuplicateKickOffCounts);

        List<AmplitudeData> dataWithGapFilled = DataUtils.fillMissingValues(noDuplicates, DateTimeConstants.MILLIS_PER_MINUTE);
        List<AmplitudeData> alignedKickOffs = DataUtils.fillMissingValues(noDuplicateKickOffCounts, DateTimeConstants.MILLIS_PER_MINUTE);
        if(insertEmpty) {
            final int insertLengthMin = 20;
            dataWithGapFilled = DataUtils.insertEmptyData(dataWithGapFilled, insertLengthMin, 1);
            alignedKickOffs = DataUtils.insertEmptyData(alignedKickOffs, insertLengthMin, 1);
        }

        this.motionCluster = MotionCluster.create(dataWithGapFilled, rawAmpMean, alignedKickOffs, rawKickOffMean, removeNoise);
        final List<Segment> motionSegments = MotionCluster.toSegments(this.motionCluster.getCopyOfClusters());
        final List<Segment> lightSegments = timeDeltaSegments(lightOutTimes,
                20 * DateTimeConstants.MILLIS_PER_MINUTE,
                rawData.get(0).offsetMillis);
        final List<Segment> waveSegments = firstWaveTimeOptional.isPresent() ?
                timeDeltaSegments(Lists.newArrayList(firstWaveTimeOptional.get()),
                5 * DateTimeConstants.MILLIS_PER_MINUTE,
                rawData.get(0).offsetMillis) : Collections.EMPTY_LIST;
        final Optional<Segment> inBedSegment = SleepPeriod.getSleepPeriod(dataWithGapFilled, motionSegments);

        this.sleepPeriod = SleepPeriod.createFromSegment(inBedSegment.get());
        this.sleepPeriod.addVotingSegments(motionSegments);
        this.sleepPeriod.addVotingSegments(SoundCluster.getClusters(rawSound));
        this.sleepPeriod.addVotingSegments(lightSegments);
        this.sleepPeriod.addVotingSegments(waveSegments);

        LOGGER.debug("data start from {} to {}", new DateTime(this.sleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(this.sleepPeriod.getOffsetMillis())),
                new DateTime(this.sleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(this.sleepPeriod.getOffsetMillis())));

        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> motionFeatures = MotionFeatures.generateTimestampAlignedFeatures(dataWithGapFilled,
                MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES,
                MotionFeatures.WAKEUP_FEATURE_AGGREGATE_WINDOW_IN_MINUTES,
                false);
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures = MotionFeatures.aggregateData(motionFeatures, MotionFeatures.MOTION_AGGREGATE_WINDOW_IN_MINUTES);
        LOGGER.debug("smoothed data size {}", aggregatedFeatures.get(MotionFeatures.FeatureType.MAX_AMPLITUDE).size());
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> filtered = removeNoise ?
                MotionCluster.petFiltering(motionCluster.getCopyOfClusters(),
                aggregatedFeatures,
                motionCluster.getDensityThreshold(), rawAmpMean) :
                aggregatedFeatures;
        LOGGER.debug("sleep period {}, {}",
                new DateTime(sleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())),
                new DateTime(sleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())));

        if(this.capScoreOutOfPeriod) {
            final Map<MotionFeatures.FeatureType, List<AmplitudeData>> capFeatures = capFeaturesBySleepPeriod(filtered, sleepPeriod);
            this.aggregatedFeatures = capFeatures;
        }else{
            this.aggregatedFeatures = filtered;
        }
    }




    public final List<Segment> getAwakes(final long beginMillis, final long endMillis, final boolean debug){
        final List<Segment> allAwakesPeriods = this.sleepPeriod.getAwakePeriods(debug);
        final List<Segment> awakesInTheRange = new ArrayList<>();

        for(final Segment segment:allAwakesPeriods){
            if(beginMillis <= segment.getStartTimestamp() && endMillis >= segment.getEndTimestamp()){
                awakesInTheRange.add(segment);
            }
        }
        return awakesInTheRange;
    }

    @Deprecated
    private List<AmplitudeData> preprocessNoiseFilter(final List<AmplitudeData> rawData, final List<AmplitudeData> kickOffCounts){
        final double amplitudeMean = NumericalUtils.mean(rawData);
        final double kickOffMean = NumericalUtils.mean(kickOffCounts);

        final List<AmplitudeData> filtered = new ArrayList<>();
        for(int i = 0; i < rawData.size(); i++){
            final AmplitudeData amplitude = rawData.get(i);
            final AmplitudeData kickOff = kickOffCounts.get(i);

            if(amplitude.amplitude < amplitudeMean && kickOff.amplitude <= kickOffMean){
                filtered.add(new AmplitudeData(amplitude.timestamp, 0d, amplitude.offsetMillis));
                continue;
            }
            filtered.add(new AmplitudeData(amplitude.timestamp, amplitude.amplitude, amplitude.offsetMillis));
        }

        return filtered;
    }

    private Map<MotionFeatures.FeatureType, List<AmplitudeData>> capFeaturesBySleepPeriod(final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                                                          final Segment sleepPeriod){
        final Set<MotionFeatures.FeatureType> featureTypes = features.keySet();
        final Map<MotionFeatures.FeatureType, List<AmplitudeData>> result = new HashMap<>();
        final int preserveEdgeMillis = 15 * DateTimeConstants.MILLIS_PER_MINUTE;

        for(final MotionFeatures.FeatureType featureType:featureTypes){
            result.put(featureType, new ArrayList<AmplitudeData>());

            if(featureType != MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE &&
                    featureType != MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE){
                result.put(featureType, Lists.newArrayList(features.get(featureType)));
            }

            final List<AmplitudeData> originalFeature = features.get(featureType);

            for(int i = 0; i < originalFeature.size(); i++){
                final AmplitudeData item = originalFeature.get(i);
                final long timestamp = item.timestamp;
                if(timestamp >= sleepPeriod.getStartTimestamp() - preserveEdgeMillis &&
                        timestamp <= sleepPeriod.getEndTimestamp() + preserveEdgeMillis){
                    result.get(featureType).add(item);
                    continue;
                }

                result.get(featureType).add(new AmplitudeData(timestamp, 0d, item.offsetMillis));
            }
        }
        return result;
    }

    private List<Segment> timeDeltaSegments(final List<DateTime> dateTimes, final int deltaMillis, final int offsetMillis){
        final List<Segment> segments = new ArrayList<>();
        for(final DateTime dateTime:dateTimes){
            segments.add(new Segment(dateTime.getMillis() - deltaMillis, dateTime.getMillis() + deltaMillis, offsetMillis));
        }
        return segments;
    }

    public SleepEvents<Segment> getResult(final boolean debug){
        if(debug){
            LOGGER.debug("+++++++++++++ amp mean {}, kickoff mean {}", this.rawAmpMean, this.rawKickOffMean);
            MotionCluster.printClusters(this.motionCluster.getCopyOfClusters());
        }
        final SleepEvents<Segment> defaultEvents = this.motionScoreAlgorithmDefault.getSleepEvents(debug);

        final SleepEvents<Segment> events = aggregate(defaultEvents);
        if(debug){
            LOGGER.debug("IN_BED: {}",
                    new DateTime(events.goToBed.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.goToBed.getOffsetMillis())));
            LOGGER.debug("SLEEP: {}",
                    new DateTime(events.fallAsleep.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.fallAsleep.getOffsetMillis())));
            LOGGER.debug("WAKE_UP: {}",
                    new DateTime(events.wakeUp.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.wakeUp.getOffsetMillis())));
            LOGGER.debug("OUT_BED: {}",
                    new DateTime(events.outOfBed.getStartTimestamp(),
                            DateTimeZone.forOffsetMillis(events.outOfBed.getOffsetMillis())));
        }

        return events;
    }

    private static boolean isEmptyBounds(final Pair<Integer, Integer> bounds){
        if(bounds.fst > -1 && bounds.snd > -1){
            return false;
        }
        return true;
    }

    private SleepEvents<Segment> aggregate(final SleepEvents<Segment> defaultEvents){

        //final long wakeUpSearchTime = Math.min(defaultEvents.wakeUp.getStartTimestamp(), sleepEvents.fallAsleep.getStartTimestamp());

        final List<ClusterAmplitudeData> clusterCopy = this.motionCluster.getCopyOfClusters();

        Segment inBed = defaultEvents.goToBed;
        Segment sleep = defaultEvents.fallAsleep;

        final Pair<Long, Long> sleepTimesMillis = safeGuardPickSleep(this.sleepPeriod,
                clusterCopy,
                this.getAggregatedFeatures(),
                defaultEvents.fallAsleep.getStartTimestamp());
        long sleepTimeMillis = sleepTimesMillis.snd;
        inBed = new Segment(sleepTimesMillis.fst, sleepTimesMillis.fst + DateTimeConstants.MILLIS_PER_MINUTE, sleep.getOffsetMillis());
        sleep = new Segment(sleepTimeMillis,
                sleepTimeMillis + DateTimeConstants.MILLIS_PER_MINUTE,
                defaultEvents.fallAsleep.getOffsetMillis());
        if(inBed.getStartTimestamp() > sleep.getStartTimestamp()){
            inBed = new Segment(sleep.getStartTimestamp() - 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.getStartTimestamp() - 9 * DateTimeConstants.MILLIS_PER_MINUTE,
                    sleep.getOffsetMillis());
        }


        Segment wakeUp = defaultEvents.wakeUp;
        Segment outBed = defaultEvents.outOfBed;

        final Pair<Long, Long> wakeUpTimesMillis = safeGuardPickWakeUp(clusterCopy,
                sleepPeriod,
                getAggregatedFeatures(),
                defaultEvents.wakeUp.getStartTimestamp());
        wakeUp = new Segment(wakeUpTimesMillis.fst, wakeUpTimesMillis.fst + DateTimeConstants.MILLIS_PER_MINUTE, wakeUp.getOffsetMillis());
        outBed = new Segment(wakeUpTimesMillis.snd, wakeUpTimesMillis.snd + DateTimeConstants.MILLIS_PER_MINUTE, inBed.getOffsetMillis());


        return SleepEvents.create(inBed, sleep, wakeUp, outBed);
    }

    private static long pickMaxScoreWakeUp(final SleepPeriod sleepPeriod,
                                           final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                           final long originalWakeUpMillis){

        final Optional<AmplitudeData> maxScoreItem = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                originalWakeUpMillis,
                sleepPeriod.getEndTimestamp());
        if(!maxScoreItem.isPresent()){
            return originalWakeUpMillis;
        }

        return maxScoreItem.get().timestamp;
    }

    protected static long pickWakeUp(final List<ClusterAmplitudeData> clusters,
                                final List<ClusterAmplitudeData> wakeUpMotionCluster,
                                final SleepPeriod sleepPeriod,
                                final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                final long wakeUpMillisSleepPeriod){
        if(wakeUpMotionCluster.size() == 0){
            return wakeUpMillisSleepPeriod;
        }

        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, wakeUpMillisSleepPeriod);

        if(isEmptyBounds(originalBounds)){
            return pickMaxScoreWakeUp(sleepPeriod, features, wakeUpMillisSleepPeriod);
        }

        if(clusters.get(originalBounds.fst).timestamp == wakeUpMotionCluster.get(0).timestamp &&
                clusters.get(originalBounds.snd).timestamp == wakeUpMotionCluster.get(wakeUpMotionCluster.size() - 1).timestamp){
            return wakeUpMillisSleepPeriod;
        }

        return pickMaxScoreWakeUp(sleepPeriod, features, wakeUpMillisSleepPeriod);

    }





    private static ClusterAmplitudeData getItem(final List<ClusterAmplitudeData> clusters, final int i){
        return clusters.get(i);
    }

    protected static Pair<Integer, Integer> pickSleepClusterIndexV1(final List<ClusterAmplitudeData> clusters,
                                                                    final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures,
                                                                    final long originalSleepMillis) {
        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, originalSleepMillis);
        final List<AmplitudeData> wakeFeature = aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE);
        double maxWakeScore = 0d;
        long maxWakeMillis = 0;
        final List<AmplitudeData> sleepFeature = aggregatedFeatures.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE);
        double maxSleepScore = 0d;
        long maxSleepMillis = 0;
        final long endTimestamp = wakeFeature.get(wakeFeature.size() - 1).timestamp;
        long searchEndMillis = endTimestamp;
        for(int i = 0; i < wakeFeature.size(); i++) {
            final long timestamp = wakeFeature.get(i).timestamp;
            final double combinedForward = wakeFeature.get(i).amplitude;
            final double combinedBackWard = sleepFeature.get(i).amplitude;
            if(combinedForward > 0 || combinedBackWard > 0){
                searchEndMillis = timestamp + 3 * DateTimeConstants.MILLIS_PER_HOUR;
                LOGGER.debug("******* search end time {}",
                        new DateTime(searchEndMillis, DateTimeZone.forOffsetMillis(wakeFeature.get(i).offsetMillis)));
                break;
            }
        }

        final Optional<AmplitudeData> maxSleepScoreOptional = getMaxScore(aggregatedFeatures,
                MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                sleepFeature.get(0).timestamp,
                searchEndMillis);
        final Optional<AmplitudeData> maxWakeScoreOptional = getMaxScore(aggregatedFeatures,
                MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                sleepFeature.get(0).timestamp,
                searchEndMillis);
        maxSleepMillis = maxSleepScoreOptional.get().timestamp;
        maxWakeMillis = maxWakeScoreOptional.get().timestamp;

        LOGGER.debug("max sleep score {}, wake score {}",
                new DateTime(maxSleepMillis, DateTimeZone.forOffsetMillis(wakeFeature.get(0).offsetMillis)),
                new DateTime(maxWakeMillis, DateTimeZone.forOffsetMillis(wakeFeature.get(0).offsetMillis)));
        final Pair<Integer, Integer> wakeBounds = MotionCluster.getClusterByTime(clusters, maxWakeMillis);
        final Pair<Integer, Integer> sleepBounds = MotionCluster.getClusterByTime(clusters, maxSleepMillis);
        if (wakeBounds.fst == sleepBounds.fst && wakeBounds.fst == originalBounds.fst) {
            if(!isEmptyBounds(originalBounds)) {
                LOGGER.debug("All agree! sleep cluster start {} end {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return new Pair<>(originalBounds.fst + 1, originalBounds.snd);
        }
        if (wakeBounds.fst == sleepBounds.fst && wakeBounds.fst != originalBounds.fst) {
            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(wakeBounds)) {
                LOGGER.debug("Two agree, false detection impacted by other source (time/light). " +
                                "sleep cluster start {} end {}, corrected {}, {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, wakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, wakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.snd).offsetMillis)));
            }
            return wakeBounds;
        }
        if (wakeBounds.fst != sleepBounds.fst) {
            // Need to further investigate this case
            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(wakeBounds) && !isEmptyBounds(sleepBounds)) {
                LOGGER.debug("None agree, use wake, wake {} - {}, sleep {} - {}, detect {} - {}",
                        new DateTime(getItem(clusters, wakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, wakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, wakeBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, sleepBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, sleepBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, sleepBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, sleepBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return wakeBounds;
        }
        if(isEmptyBounds(originalBounds)){
            return originalBounds;
        }
        return new Pair<>(originalBounds.fst + 1, originalBounds.snd);
    }

    protected static Pair<Integer, Integer> pickSleepClusterIndex(final List<ClusterAmplitudeData> clusters,
                                                           final Map<MotionFeatures.FeatureType, List<AmplitudeData>> aggregatedFeatures,
                                                           final Segment sleepPeriod,
                                                           final long originalSleepMillis){
        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, originalSleepMillis);
        final long endSearchTimeMillis = sleepPeriod.getStartTimestamp() + 3 * DateTimeConstants.MILLIS_PER_HOUR;
        final Optional<AmplitudeData> maxWakeScoreItem = getMaxScore(aggregatedFeatures,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                sleepPeriod.getStartTimestamp(),
                endSearchTimeMillis);
        final Optional<AmplitudeData> maxSleepScoreItem = getMaxScore(aggregatedFeatures,
                MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                sleepPeriod.getStartTimestamp(),
                endSearchTimeMillis);

        if(!maxSleepScoreItem.isPresent() && !maxWakeScoreItem.isPresent()){
            LOGGER.debug("No score peak found.");
            return new Pair<>(-1, -1);
        }

        final Pair<Integer, Integer> maxWakeBounds = MotionCluster.getClusterByTime(clusters, maxWakeScoreItem.get().timestamp);
        final Pair<Integer, Integer> maxSleepBounds = MotionCluster.getClusterByTime(clusters, maxSleepScoreItem.get().timestamp);

        if (maxWakeBounds.fst == maxSleepBounds.fst && maxWakeBounds.fst == originalBounds.fst) {
            if(!isEmptyBounds(originalBounds)) {
                LOGGER.debug("All agree! sleep cluster start {} end {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return new Pair<>(originalBounds.fst, originalBounds.snd);
        }

        if (maxWakeBounds.fst == maxSleepBounds.fst && maxWakeBounds.fst != originalBounds.fst) {

            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(maxWakeBounds)) {
                LOGGER.debug("Two agree, false detection impacted by other source (time/light). " +
                                "sleep cluster start {} end {}, corrected {}, {}",
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, maxWakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, maxWakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.snd).offsetMillis)));
            }
            return maxWakeBounds;
        }

        if (maxWakeBounds.fst != maxSleepBounds.fst) {
            // Need to further investigate this case
            if(!isEmptyBounds(originalBounds) && !isEmptyBounds(maxWakeBounds) && !isEmptyBounds(maxSleepBounds)) {
                LOGGER.debug("None agree, use wake, wake {} - {}, sleep {} - {}, detect {} - {}",
                        new DateTime(getItem(clusters, maxWakeBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, maxWakeBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxWakeBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, maxSleepBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxSleepBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, maxSleepBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, maxSleepBounds.snd).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.fst).offsetMillis)),
                        new DateTime(getItem(clusters, originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(getItem(clusters, originalBounds.snd).offsetMillis)));
            }
            return maxWakeBounds;
        }

        if(isEmptyBounds(originalBounds)){
            return originalBounds;
        }

        return new Pair<>(originalBounds.fst, originalBounds.snd);
    }


    protected static Pair<Integer, Integer> pickWakeUpClusterIndex(final List<ClusterAmplitudeData> clusters,
                                                                   final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                                   final Segment sleepPeriod,
                                                                   final long originalWakeUpMillis){
        final Pair<Integer, Integer> originalBounds = MotionCluster.getClusterByTime(clusters, originalWakeUpMillis);
        LOGGER.debug("original wake up {}", new DateTime(originalWakeUpMillis,
                DateTimeZone.forOffsetMillis(clusters.get(0).offsetMillis)));
        if(isEmptyBounds(originalBounds)){
            return new Pair<>(-1, -1);
        }

        final List<Segment> clustersInSleepPeriod = new ArrayList<>();
        final List<Segment> allClusters = MotionCluster.toSegments(clusters);
        for(final Segment cluster:allClusters){
            if(cluster.getStartTimestamp() >= sleepPeriod.getStartTimestamp() && cluster.getEndTimestamp() <= sleepPeriod.getEndTimestamp()){
                clustersInSleepPeriod.add(cluster);
            }
        }

        if(clustersInSleepPeriod.size() == 0){
            return new Pair<>(-1, -1);
        }
        final Segment lastClusterInSleepPeriod = clustersInSleepPeriod.get(clustersInSleepPeriod.size() - 1);
        if(lastClusterInSleepPeriod.getStartTimestamp() == clusters.get(originalBounds.fst).timestamp &&
                lastClusterInSleepPeriod.getEndTimestamp() == clusters.get(originalBounds.snd).timestamp){
            LOGGER.debug("Wake up all agree! wake up cluster {} - {}",
                    new DateTime(lastClusterInSleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())),
                    new DateTime(lastClusterInSleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())));
            return originalBounds;
        }

        if(lastClusterInSleepPeriod.getStartTimestamp() - clusters.get(originalBounds.snd).timestamp < DateTimeConstants.MILLIS_PER_HOUR &&
                lastClusterInSleepPeriod.getDuration() < 20 * DateTimeConstants.MILLIS_PER_MINUTE){
            return originalBounds;
        }

        LOGGER.debug("Detected cluster too far form end of sleep, detected cluster {} - {}, last cluster {} - {}",
                new DateTime(clusters.get(originalBounds.fst).timestamp, DateTimeZone.forOffsetMillis(clusters.get(originalBounds.fst).offsetMillis)),
                new DateTime(clusters.get(originalBounds.snd).timestamp, DateTimeZone.forOffsetMillis(clusters.get(originalBounds.snd).offsetMillis)),
                new DateTime(lastClusterInSleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())),
                new DateTime(lastClusterInSleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(lastClusterInSleepPeriod.getOffsetMillis())));

        final long startSearchMillis = clusters.get(originalBounds.snd).timestamp + 5 * DateTimeConstants.MILLIS_PER_MINUTE;
        final long endSearchMillis = sleepPeriod.getEndTimestamp();
        final Optional<AmplitudeData> maxScoreItem = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                startSearchMillis,
                endSearchMillis);
        if(!maxScoreItem.isPresent()){
            LOGGER.debug("Cannot find max score in {} - {}",
                    new DateTime(startSearchMillis, DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())),
                    new DateTime(endSearchMillis, DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())));
            return new Pair<>(-1, -1);
        }
        final Pair<Integer, Integer> newBounds = MotionCluster.getClusterByTime(clusters, maxScoreItem.get().timestamp);
        if(isEmptyBounds(newBounds)){
            LOGGER.debug("Max score time {} doesn't has a cluster.",
                    new DateTime(maxScoreItem.get().timestamp, DateTimeZone.forOffsetMillis(maxScoreItem.get().offsetMillis)));
            final Pair<Integer, Integer> lastBounds = MotionCluster.getLast(clusters);
            return lastBounds;
        }
        return newBounds;

    }

    private static Pair<Long, Long> predictionBoundsMillis(final long wakeUpMillisPredicted, final Optional<Segment> predictionSegment){
        if(!predictionSegment.isPresent()){
            return new Pair<>(wakeUpMillisPredicted, wakeUpMillisPredicted + 10 * DateTimeConstants.MILLIS_PER_MINUTE);
        }
        return new Pair<>(wakeUpMillisPredicted, predictionSegment.get().getEndTimestamp());
    }

    protected static Pair<Long, Long> safeGuardPickWakeUp(final List<ClusterAmplitudeData> clusters,
                                                          final SleepPeriod sleepPeriod,
                                                          final Map<MotionFeatures.FeatureType, List<AmplitudeData>> featuresNotCapped,
                                                          final long wakeUpMillisPredicted){

        final List<Segment> clusterSegments = MotionCluster.toSegments(clusters);

        final Segment lastSegment = clusterSegments.get(clusterSegments.size() - 1);
        Segment lastSegmentInSleepPeriod = lastSegment;

        for(final Segment segment:clusterSegments){
            if(segment.getStartTimestamp() <= sleepPeriod.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE) {
                lastSegmentInSleepPeriod = segment;
            }
        }

        Optional<Segment> predictionSegment = Optional.absent();
        for(final Segment segment:clusterSegments){
            if(segment.getStartTimestamp() - 10 * DateTimeConstants.MILLIS_PER_MINUTE <= wakeUpMillisPredicted &&
                    segment.getEndTimestamp() + 10 * DateTimeConstants.MILLIS_PER_MINUTE >= wakeUpMillisPredicted){
                predictionSegment = Optional.of(segment);
                break;
            }
        }

        // predict in last segment of sleep period.
        if(wakeUpMillisPredicted >= lastSegmentInSleepPeriod.getStartTimestamp() &&
                wakeUpMillisPredicted <= lastSegmentInSleepPeriod.getEndTimestamp() + 15 * DateTimeConstants.MILLIS_PER_MINUTE){
            LOGGER.debug("HAPPY USER, wake up cluster is last cluster.");
            return new Pair<>(wakeUpMillisPredicted, lastSegmentInSleepPeriod.getEndTimestamp());
        }

        if(wakeUpMillisPredicted < lastSegmentInSleepPeriod.getStartTimestamp()) {
            // predict << last segment of sleep period
            if (lastSegmentInSleepPeriod.getStartTimestamp() - wakeUpMillisPredicted > 40 * DateTimeConstants.MILLIS_PER_MINUTE) {
                LOGGER.debug("Predicted too far way from end, predicted {}",
                        new DateTime(wakeUpMillisPredicted, DateTimeZone.forOffsetMillis(lastSegment.getOffsetMillis())));
                final Optional<AmplitudeData> maxWakeUpScoreOptional = getMaxScore(featuresNotCapped,
                        MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                        lastSegmentInSleepPeriod.getEndTimestamp() - 120 * DateTimeConstants.MILLIS_PER_MINUTE,
                        lastSegmentInSleepPeriod.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

                final Optional<AmplitudeData> maxSleepScoreOptional = getMaxScore(featuresNotCapped,
                        MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                        wakeUpMillisPredicted,
                        lastSegmentInSleepPeriod.getEndTimestamp());

                if(maxSleepScoreOptional.isPresent()){
                    if(wakeUpMillisPredicted <= maxSleepScoreOptional.get().timestamp &&
                            maxSleepScoreOptional.get().timestamp < lastSegmentInSleepPeriod.getStartTimestamp()){
                        LOGGER.debug("Max drop between prediction and last segment detected, prediction is likely right.");
                        return predictionBoundsMillis(wakeUpMillisPredicted, predictionSegment);
                    }
                }
                if (!maxWakeUpScoreOptional.isPresent()) {
                    return new Pair<>(lastSegmentInSleepPeriod.getStartTimestamp(), lastSegmentInSleepPeriod.getEndTimestamp());
                }

                final Pair<Integer, Integer> maxScoreCluster = MotionCluster.getClusterByTime(clusters, maxWakeUpScoreOptional.get().timestamp);
                if(isEmptyBounds(maxScoreCluster)) {
                    return new Pair<>(maxWakeUpScoreOptional.get().timestamp, lastSegmentInSleepPeriod.getEndTimestamp());
                }
                return new Pair<>(maxWakeUpScoreOptional.get().timestamp, clusters.get(maxScoreCluster.snd).timestamp);
            }else {

                LOGGER.debug("OK USER: Predict not too far from end");
                final Optional<AmplitudeData> maxWakeUpScoreOptional = getMaxScore(featuresNotCapped,
                        MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                        wakeUpMillisPredicted - 20 * DateTimeConstants.MILLIS_PER_MINUTE,
                        lastSegmentInSleepPeriod.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

                if (!maxWakeUpScoreOptional.isPresent()) {
                    return new Pair<>(wakeUpMillisPredicted, wakeUpMillisPredicted + 10 * DateTimeConstants.MILLIS_PER_MINUTE);
                }

                final Pair<Integer, Integer> maxScoreCluster = MotionCluster.getClusterByTime(clusters, maxWakeUpScoreOptional.get().timestamp);
                if(isEmptyBounds(maxScoreCluster)) {
                    return predictionBoundsMillis(wakeUpMillisPredicted, predictionSegment);
                }
                return new Pair<>(maxWakeUpScoreOptional.get().timestamp, clusters.get(maxScoreCluster.snd).timestamp);
            }
        }

        // predict > last segment in sleep period
        if(lastSegment.getStartTimestamp() == lastSegmentInSleepPeriod.getStartTimestamp() &&
                lastSegment.getEndTimestamp() == lastSegmentInSleepPeriod.getEndTimestamp()){

            // No motion cluster after end of sleep period.
            LOGGER.debug("-------------* No maid found, last motion cluster {} - {}",
                    new DateTime(lastSegment.getStartTimestamp(), DateTimeZone.forOffsetMillis(lastSegment.getOffsetMillis())),
                    new DateTime(lastSegment.getEndTimestamp(), DateTimeZone.forOffsetMillis(lastSegment.getOffsetMillis())));

            LOGGER.debug("Sleep period detection wrong.");
            final List<AmplitudeData> amps = featuresNotCapped.get(MotionFeatures.FeatureType.MAX_AMPLITUDE);
            final long lastMotionMillis = amps.get(amps.size() - 1).timestamp;
            if(lastMotionMillis - wakeUpMillisPredicted > DateTimeConstants.MILLIS_PER_HOUR){
                final Optional<AmplitudeData> maxWakeUpScoreOptional = getMaxScore(featuresNotCapped,
                        MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                        lastMotionMillis - 60 * DateTimeConstants.MILLIS_PER_HOUR,
                        lastMotionMillis);
                if(maxWakeUpScoreOptional.isPresent()){
                    return new Pair<>(maxWakeUpScoreOptional.get().timestamp, lastMotionMillis);
                }
                return new Pair<>(lastMotionMillis - 10 * DateTimeConstants.MILLIS_PER_MINUTE, lastMotionMillis);
            }
            return new Pair<>(wakeUpMillisPredicted, lastMotionMillis);

        }else {

            // last segment in sleep period < last segment && predict > last segment in sleep period
            LOGGER.debug("-------------* Maid found, last motion cluster in sleep period {} - {}",
                    new DateTime(lastSegmentInSleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(lastSegmentInSleepPeriod.getOffsetMillis())),
                    new DateTime(lastSegmentInSleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(lastSegmentInSleepPeriod.getOffsetMillis())));

            if(wakeUpMillisPredicted < lastSegment.getStartTimestamp() && wakeUpMillisPredicted > sleepPeriod.getEndTimestamp()){
                LOGGER.debug("Sleep period {} - {} wrong, might due to data quality issue, keep predicted result",
                        new DateTime(sleepPeriod.getStartTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())),
                        new DateTime(sleepPeriod.getEndTimestamp(), DateTimeZone.forOffsetMillis(sleepPeriod.getOffsetMillis())));
                return predictionBoundsMillis(wakeUpMillisPredicted, predictionSegment);
            }


            final Optional<AmplitudeData> maxWakeUpScoreOptional = getMaxScore(featuresNotCapped,
                    MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                    lastSegmentInSleepPeriod.getEndTimestamp() - 60 * DateTimeConstants.MILLIS_PER_MINUTE,
                    lastSegmentInSleepPeriod.getEndTimestamp() );
            if(!maxWakeUpScoreOptional.isPresent()){
                return new Pair<>(lastSegmentInSleepPeriod.getStartTimestamp(), lastSegmentInSleepPeriod.getEndTimestamp());
            }

            final Pair<Integer, Integer> maxScoreCluster = MotionCluster.getClusterByTime(clusters, maxWakeUpScoreOptional.get().timestamp);
            if(isEmptyBounds(maxScoreCluster)) {
                return new Pair<>(maxWakeUpScoreOptional.get().timestamp, lastSegmentInSleepPeriod.getEndTimestamp());
            }
            return new Pair<>(maxWakeUpScoreOptional.get().timestamp, clusters.get(maxScoreCluster.snd).timestamp);

        }

        //return new Pair<>(wakeUpMillisPredicted, lastSegmentInSleepPeriod.getEndTimestamp());

    }

    protected static Pair<Long, Long> pickSleepV1(final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                             final long originalSleepMillis){

        final List<AmplitudeData> wakeFeature = features.get(MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE);
        final List<AmplitudeData> sleepFeature = features.get(MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE);
        final long endTimestamp = wakeFeature.get(wakeFeature.size() - 1).timestamp;
        long searchEndMillis = endTimestamp;

        for(int i = 0; i < wakeFeature.size(); i++) {
            final long timestamp = wakeFeature.get(i).timestamp;
            final double combinedForward = wakeFeature.get(i).amplitude;
            final double combinedBackWard = sleepFeature.get(i).amplitude;
            if ((combinedForward > 0 || combinedBackWard > 0) && searchEndMillis == endTimestamp) {
                searchEndMillis = timestamp + 3 * DateTimeConstants.MILLIS_PER_HOUR;
                break;
            }
        }

        long inBedMillis = originalSleepMillis - 10 * DateTimeConstants.MILLIS_PER_MINUTE;
        final Optional<AmplitudeData> firstMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                wakeFeature.get(0).timestamp,
                searchEndMillis);
        if(firstMaxScoreItemOptional.isPresent()){
            inBedMillis = firstMaxScoreItemOptional.get().timestamp;
        }

        final Optional<AmplitudeData> predictedMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                originalSleepMillis - 20 * DateTimeConstants.MILLIS_PER_MINUTE,
                originalSleepMillis + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

        LOGGER.debug("first {}, max {}, predicted {}",
                new DateTime(firstMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(predictedMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(predictedMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(originalSleepMillis, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)));

        if(firstMaxScoreItemOptional.get().timestamp < originalSleepMillis - 2 * DateTimeConstants.MILLIS_PER_HOUR){
            if(firstMaxScoreItemOptional.get().amplitude * 5 < predictedMaxScoreItemOptional.get().amplitude){
                return new Pair<>(inBedMillis, originalSleepMillis);
            }
            final Optional<AmplitudeData> maxDrop = getMaxScore(features,
                    MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                    sleepFeature.get(0).timestamp,
                    searchEndMillis);
            if(!maxDrop.isPresent()){
                return new Pair<>(inBedMillis, inBedMillis + 10 * DateTimeConstants.MILLIS_PER_MINUTE);
            }
            return new Pair<>(inBedMillis, maxDrop.get().timestamp);
        }
        return new Pair<>(inBedMillis, originalSleepMillis);
    }


    protected static Pair<Long, Long> safeGuardPickSleep(final SleepPeriod sleepPeriod,
                                                         final List<ClusterAmplitudeData> clusters,
                                                         final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                         final long originalSleepMillis){


        final List<Segment> clusterSegments = MotionCluster.toSegments(clusters);
        Segment firstCluster = clusterSegments.get(0);
        for(final Segment cluster:clusterSegments){
            if(cluster.getEndTimestamp() >= sleepPeriod.getStartTimestamp()){
                firstCluster = cluster;
                LOGGER.debug("First motion cluster in sleep period {} - {}",
                        new DateTime(firstCluster.getStartTimestamp(), DateTimeZone.forOffsetMillis(firstCluster.getOffsetMillis())),
                        new DateTime(firstCluster.getEndTimestamp(), DateTimeZone.forOffsetMillis(firstCluster.getOffsetMillis())));
                break;
            }
        }

        final Optional<AmplitudeData> firstMaxScoreItemOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                firstCluster.getStartTimestamp(),
                firstCluster.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

        if(firstCluster.getStartTimestamp() <= originalSleepMillis &&
                firstCluster.getEndTimestamp() + 15 * DateTimeConstants.MILLIS_PER_MINUTE >= originalSleepMillis){
            LOGGER.debug("HAPPY USER: Predicted sleep in first cluster. predicted sleep {}",
                    new DateTime(originalSleepMillis, DateTimeZone.forOffsetMillis(firstCluster.getOffsetMillis())));

            return new Pair<>(firstCluster.getStartTimestamp(), originalSleepMillis);
        }

        final Optional<AmplitudeData> predictedMaxScoreOptional = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_BACKWARD_AVERAGE_AMPLITUDE,
                originalSleepMillis - 20 * DateTimeConstants.MILLIS_PER_MINUTE,
                originalSleepMillis + 20 * DateTimeConstants.MILLIS_PER_MINUTE);

        if(predictedMaxScoreOptional.isPresent() && firstMaxScoreItemOptional.isPresent()){
            if(predictedMaxScoreOptional.get().amplitude / 5d > firstMaxScoreItemOptional.get().amplitude){
                for(final Segment cluster:clusterSegments){
                    if(cluster.getStartTimestamp() <= predictedMaxScoreOptional.get().timestamp &&
                            cluster.getEndTimestamp() >= predictedMaxScoreOptional.get().timestamp){
                        LOGGER.debug("NOISY SLEEP PERIOD: predicted sleep in cluster {} - {}",
                                new DateTime(cluster.getStartTimestamp(), DateTimeZone.forOffsetMillis(cluster.getOffsetMillis())),
                                new DateTime(cluster.getEndTimestamp(), DateTimeZone.forOffsetMillis(cluster.getOffsetMillis())));

                        return new Pair<>(cluster.getStartTimestamp(), originalSleepMillis);
                    }
                }
            }
        }

        LOGGER.debug("COMPLETELY OFF: Move prediction to first cluster {} - {}",
                new DateTime(firstCluster.getStartTimestamp(), DateTimeZone.forOffsetMillis(firstCluster.getOffsetMillis())),
                new DateTime(firstCluster.getEndTimestamp(), DateTimeZone.forOffsetMillis(firstCluster.getOffsetMillis())));
        final long inBedMillis = firstCluster.getStartTimestamp();

        LOGGER.debug("first {}, predicted {}",
                new DateTime(firstMaxScoreItemOptional.get().timestamp, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)),
                new DateTime(originalSleepMillis, DateTimeZone.forOffsetMillis(firstMaxScoreItemOptional.get().offsetMillis)));

        final Optional<AmplitudeData> maxDrop = getMaxScore(features,
                MotionFeatures.FeatureType.DENSITY_DROP_BACKTRACK_MAX_AMPLITUDE,
                firstCluster.getStartTimestamp() - 10 * DateTimeConstants.MILLIS_PER_MINUTE,
                firstCluster.getEndTimestamp() + 20 * DateTimeConstants.MILLIS_PER_MINUTE);
        if(!maxDrop.isPresent()){
            return new Pair<>(inBedMillis, inBedMillis + 10 * DateTimeConstants.MILLIS_PER_MINUTE);
        }

        return new Pair<>(inBedMillis, maxDrop.get().timestamp);
    }

    public Map<MotionFeatures.FeatureType, List<AmplitudeData>> getAggregatedFeatures(){
        return ImmutableMap.copyOf(this.aggregatedFeatures);
    }

    public static Optional<AmplitudeData> getMaxScore(final Map<MotionFeatures.FeatureType, List<AmplitudeData>> features,
                                                        final MotionFeatures.FeatureType featureType,
                                                      final long startSearchMillis, final long endSearchMillis){
        final List<AmplitudeData> feature = features.get(featureType);
        if(feature == null || feature.size() == 0){
            return Optional.absent();
        }

        Optional<AmplitudeData> maxScore = Optional.absent();
        for(final AmplitudeData datum:feature){
            if(datum.timestamp >= startSearchMillis && datum.timestamp <= endSearchMillis){
                if(!maxScore.isPresent()){
                    maxScore = Optional.of(datum);
                    continue;
                }

                if(maxScore.get().amplitude <= datum.amplitude){
                    maxScore = Optional.of(datum);
                }
            }
        }

        return maxScore;
    }

}