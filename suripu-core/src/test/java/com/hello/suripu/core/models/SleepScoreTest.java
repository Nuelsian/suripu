package com.hello.suripu.core.models;

import com.hello.suripu.core.util.SleepScoreUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by jimmy on 7/21/15.
 */
public class SleepScoreTest {

    @Test
    public void testDefaultWeighting() {
        final SleepScore.Weighting weighting = new SleepScore.Weighting();
        final MotionScore fakeMotion = new MotionScore(0, 0, 0.0f, 0, 90);
        final Integer fakeDuration = 80;
        final Integer fakeEnvironment = 100;

        final SleepScore.Builder builder = new SleepScore.Builder();
        final SleepScore sleepScore = builder.withEnvironmentalScore(fakeEnvironment)
                .withSleepDurationScore(fakeDuration)
                .withMotionScore(fakeMotion)
                .build();

        Integer calculatedWeight = Math.round((weighting.motion * fakeMotion.score)
                + (weighting.duration * fakeDuration)
                + (weighting.environmental * fakeEnvironment));

        assertThat(sleepScore, notNullValue());
        assertThat(sleepScore.value, is(calculatedWeight));
        assertThat(sleepScore.value, greaterThanOrEqualTo(0));
        assertThat(sleepScore.value, lessThanOrEqualTo(100));
        assertThat(sleepScore.motionScore, is(fakeMotion));
        assertThat(sleepScore.sleepDurationScore, is(fakeDuration));
        assertThat(sleepScore.environmentalScore, is(fakeEnvironment));
    }

    @Test
    public void testDurationWeighting() {
        final SleepScore.Weighting weighting = new SleepScore.DurationHeavyWeighting();
        final MotionScore fakeMotion = new MotionScore(0, 0, 0.0f, 0, 90);
        final Integer fakeDuration = 80;
        final Integer fakeEnvironment = 100;

        final SleepScore.Builder builder = new SleepScore.Builder();
        final SleepScore sleepScore = builder.withEnvironmentalScore(fakeEnvironment)
                .withSleepDurationScore(fakeDuration)
                .withMotionScore(fakeMotion)
                .withWeighting(weighting)
                .build();

        Integer calculatedWeight = Math.round((weighting.motion * fakeMotion.score)
                + (weighting.duration * fakeDuration)
                + (weighting.environmental * fakeEnvironment));

        assertThat(sleepScore, notNullValue());
        assertThat(sleepScore.value, is(calculatedWeight));
        assertThat(sleepScore.value, greaterThanOrEqualTo(0));
        assertThat(sleepScore.value, lessThanOrEqualTo(100));
        assertThat(sleepScore.motionScore, is(fakeMotion));
        assertThat(sleepScore.sleepDurationScore, is(fakeDuration));
        assertThat(sleepScore.environmentalScore, is(fakeEnvironment));
    }

    @Test
    public void testDurationWeightingV2() {
        final SleepScore.Weighting weighting = new SleepScore.DurationHeavyWeightingV2();
        final MotionScore fakeMotion = new MotionScore(0, 0, 0.0f, 0, 90);
        final Integer fakeDuration = 80;
        final Integer fakeEnvironment = 100;

        final SleepScore.Builder builder = new SleepScore.Builder();
        final SleepScore sleepScore = builder.withEnvironmentalScore(fakeEnvironment)
                .withSleepDurationScore(fakeDuration)
                .withMotionScore(fakeMotion)
                .withWeighting(weighting)
                .build();

        Integer calculatedWeight = Math.round((weighting.motion * fakeMotion.score)
                + (weighting.duration * fakeDuration)
                + (weighting.environmental * fakeEnvironment));

        assertThat(sleepScore, notNullValue());
        assertThat(sleepScore.value, is(calculatedWeight));
        assertThat(sleepScore.value, greaterThanOrEqualTo(0));
        assertThat(sleepScore.value, lessThanOrEqualTo(100));
        assertThat(sleepScore.motionScore, is(fakeMotion));
        assertThat(sleepScore.sleepDurationScore, is(fakeDuration));
        assertThat(sleepScore.environmentalScore, is(fakeEnvironment));
    }

    @Test
    public void testAwakeTimesPenalty() {
        final SleepScore.Weighting weighting = new SleepScore.DurationHeavyWeightingV2();
        final MotionScore fakeMotion = new MotionScore(0, 0, 0.0f, 0, 90);
        final Integer fakeDuration = 80;
        final Integer fakeEnvironment = 100;

        final Integer timesAwake = 3;
        final Integer penalty = SleepScoreUtils.calculateTimesAwakePenaltyScore(timesAwake);

        assertThat(penalty, is(timesAwake * SleepScoreUtils.AWAKE_PENALTY_SCORE));

        final SleepScore.Builder builder = new SleepScore.Builder();
        final SleepScore sleepScoreWithoutPenalty = builder.withEnvironmentalScore(fakeEnvironment)
                .withSleepDurationScore(fakeDuration)
                .withMotionScore(fakeMotion)
                .withWeighting(weighting)
                .build();

        Integer calculatedWeight = Math.round((weighting.motion * fakeMotion.score)
                + (weighting.duration * fakeDuration)
                + (weighting.environmental * fakeEnvironment));

        assertThat(sleepScoreWithoutPenalty, notNullValue());
        assertThat(sleepScoreWithoutPenalty.value, is(calculatedWeight));

        final SleepScore sleepScoreWithPenalty = new SleepScore.Builder()
                .withEnvironmentalScore(fakeEnvironment)
                .withSleepDurationScore(fakeDuration)
                .withMotionScore(fakeMotion)
                .withWeighting(weighting)
                .withTimesAwakePenaltyScore(penalty)
                .build();

        assertThat(sleepScoreWithPenalty, notNullValue());
        assertThat(sleepScoreWithPenalty.value, is(calculatedWeight + penalty));
        assertThat(sleepScoreWithPenalty.timesAwakePenaltyScore, is(penalty));
    }
    @Test
    public void testBuilderTransition() {
        final SleepScore.Weighting weightingOld = new SleepScore.DurationHeavyWeightingV2();
        final MotionScore fakeMotionOld = new MotionScore(0, 0, 0.0f, 0, 90);
        final Integer fakeDurationOld = 80;
        final Integer fakeEnvironmentOld = 100;
        final Integer fakeTimesAwakePenaltyOld = - 10;

        final SleepScore.Weighting weightingNew = new SleepScore.DurationHeavyWeightingV2();
        final MotionScore fakeMotionNew = new MotionScore(0, 0, 0.0f, 0, 0);
        final Integer fakeDurationNew = 85;
        final Integer fakeEnvironmentNew = 100;
        final float transitionWeight = 0.1332f;

        final SleepScore.BuilderTransition builderTransition = new SleepScore.BuilderTransition();
        final SleepScore sleepScore = builderTransition
                .withEnvironmentalScoreOld(fakeEnvironmentOld)
                .withSleepDurationScoreOld(fakeDurationOld)
                .withMotionScoreOld(fakeMotionOld)
                .withTimesAwakePenaltyScoreOld(fakeTimesAwakePenaltyOld)
                .withWeightingOld(weightingOld)
                .withEnvironmentalScoreNew(fakeEnvironmentNew)
                .withSleepDurationScoreNew(fakeDurationNew)
                .withMotionScoreNew(fakeMotionNew)
                .withTimesAwakePenaltyScoreNew(0)
                .withWeightingNew(weightingNew)
                .withTransitionWeight(transitionWeight)
                .build();

        Integer calculatedWeight = Math.round(((weightingOld.motion * fakeMotionOld.score)
                + (weightingOld.duration * fakeDurationOld)
                + (weightingOld.environmental * fakeEnvironmentOld)
                + fakeTimesAwakePenaltyOld)*(1 - transitionWeight)
                + ((weightingNew.motion * fakeMotionNew.score)
                + (weightingNew.duration * fakeDurationNew)
                + (weightingNew.environmental * fakeEnvironmentNew )) * transitionWeight);

        assertThat(sleepScore, notNullValue());
        assertThat(sleepScore.value, is(calculatedWeight));
        assertThat(sleepScore.value, greaterThanOrEqualTo(0));
        assertThat(sleepScore.value, lessThanOrEqualTo(100));
        assertThat(sleepScore.motionScore, is(fakeMotionNew));
        assertThat(sleepScore.sleepDurationScore, is(fakeDurationNew));
        assertThat(sleepScore.environmentalScore, is(fakeEnvironmentNew));
    }

}