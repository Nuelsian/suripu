package com.hello.suripu.core.models.device.v2;


import com.amazonaws.AmazonServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.api.output.OutputProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.PillHeartBeatDAO;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.WifiInfoDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.PairingInfo;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.util.PillColorUtil;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class DeviceProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceProcessor.class);

    private final DeviceDAO deviceDAO;
    private final DeviceDataDAO deviceDataDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final SensorsViewsDynamoDB sensorsViewsDynamoDB;
    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final TrackerMotionDAO trackerMotionDAO;
    private final WifiInfoDAO wifiInfoDAO;
    private final SenseColorDAO senseColorDAO;


    private DeviceProcessor(final DeviceDAO deviceDAO, final DeviceDataDAO deviceDataDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final SensorsViewsDynamoDB sensorsViewsDynamoDB, final PillHeartBeatDAO pillHeartBeatDAO, final TrackerMotionDAO trackerMotionDAO, final WifiInfoDAO wifiInfoDAO, final SenseColorDAO senseColorDAO) {
        this.deviceDAO = deviceDAO;
        this.deviceDataDAO = deviceDataDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.sensorsViewsDynamoDB = sensorsViewsDynamoDB;
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.trackerMotionDAO = trackerMotionDAO;
        this.wifiInfoDAO = wifiInfoDAO;
        this.senseColorDAO = senseColorDAO;
    }

    /**
     * Retrieve pairing info which contains sense ID and number of senses paired
     *
     * @param accountId internal account ID
     * @return optional pairing info
     */
    public Optional<PairingInfo> getPairingInfo(final Long accountId) {
        final Optional<DeviceAccountPair> senseAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if(!senseAccountPairOptional.isPresent()) {
            LOGGER.warn("No sense paired for account = {}", accountId);
            return Optional.absent();
        }
        final DeviceAccountPair senseAccountPair = senseAccountPairOptional.get();
        final ImmutableList<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(senseAccountPair.externalDeviceId);
        return Optional.of(PairingInfo.create(senseAccountPair.externalDeviceId, pairs.size()));
    }


    /**
     * Unpair a pill from an account
     *
     * @param accountId internal account ID
     * @param pillId external pill ID
     */
    public void unregisterPill(final Long accountId, final String pillId) {
        final Integer numRows = deviceDAO.deletePillPairing(pillId, accountId);
        if(numRows == 0) {
            LOGGER.warn("Did not find active pill to unregister");
        }

        final List<DeviceAccountPair> sensePairedWithAccount = this.deviceDAO.getSensesForAccountId(accountId);
        if(sensePairedWithAccount.isEmpty()){
            LOGGER.error("No sense paired with account {}", accountId);
            return;
        }

        final String senseId = sensePairedWithAccount.get(0).externalDeviceId;

        try {
            this.mergedUserInfoDynamoDB.deletePillColor(senseId, accountId, pillId);
        }catch (Exception ex){
            LOGGER.error("Delete pill {}'s color from user info table for sense {} and account {} failed: {}",
                    pillId,
                    senseId,
                    accountId,
                    ex.getMessage());
        }
    }


    /**
     * Unpair a sense from an account
     *
     * @param accountId internal account ID
     * @param senseId external sense ID
     */
    public void unregisterSense(final Long accountId, final String senseId) {
        final Integer numRows = deviceDAO.deleteSensePairing(senseId, accountId);
        final Optional<UserInfo> alarmInfoOptional = this.mergedUserInfoDynamoDB.unlinkAccountToDevice(accountId, senseId);

        if(numRows == 0) {
            LOGGER.warn("Did not find active sense to unregister");
        }

        if(!alarmInfoOptional.isPresent()){
            LOGGER.warn("Cannot find device {} account {} pair in merge info table.", senseId, accountId);
        }
    }


    /**
     * Factory reset which means unpair all pills from input account, and unpair all account associated with input sense
     *
     * @param accountId internal account ID
     * @param senseId external sense ID
     */
    public void factoryReset(final Long accountId, final String senseId) {
        final List<UserInfo> pairedUsers = mergedUserInfoDynamoDB.getInfo(senseId);
        try {
            deviceDAO.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new Transaction<Void, DeviceDAO>() {
                @Override
                public Void inTransaction(final DeviceDAO transactional, final TransactionStatus status) throws Exception {
                    final Integer pillDeleted = transactional.deletePillPairingByAccount(accountId);
                    LOGGER.info("Factory reset - delete {} Pills linked to account {}", pillDeleted, accountId);

                    final Integer accountUnlinked = transactional.unlinkAllAccountsPairedToSense(senseId);
                    LOGGER.info("Factory reset - delete {} accounts linked to Sense {}", accountUnlinked, senseId);

                    for (final UserInfo userInfo : pairedUsers) {
                        try {
                            mergedUserInfoDynamoDB.unlinkAccountToDevice(userInfo.accountId, userInfo.deviceId);
                        } catch (AmazonServiceException awsEx) {
                            LOGGER.error("Failed to unlink account {} from Sense {} in merge user info. error {}",
                                    userInfo.accountId,
                                    userInfo.deviceId,
                                    awsEx.getErrorMessage());
                        }
                    }
                    return null;
                }
            });
        }catch (UnableToExecuteStatementException sqlExp){
            LOGGER.error("Failed to factory reset Sense {}, error {}", senseId, sqlExp.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Update / insert wifi info for a sense
     *
     * @param accountId internal account ID
     * @param wifiInfo container of wifi specs
     */
    public Boolean upsertWifiInfo(final Long accountId, final WifiInfo wifiInfo) throws InvalidWifiInfoException {
        final Optional<DeviceAccountPair> deviceAccountPairOptional = deviceDAO.getMostRecentSensePairByAccountId(accountId);

        if (deviceAccountPairOptional.isPresent()) {
            final String mostRecentlyPairedSenseId = deviceAccountPairOptional.get().externalDeviceId;
            if (mostRecentlyPairedSenseId.equals(wifiInfo.senseId)) {
                return wifiInfoDAO.put(wifiInfo);
            }
            LOGGER.debug("Account {} attempted to update wifi info for sense {} but the most recently paired sense is {}", accountId, wifiInfo.senseId, mostRecentlyPairedSenseId);
            throw new InvalidWifiInfoException(String.format("Mismatch account id from token and input object"));
        }
        throw new InvalidWifiInfoException(String.format("No associated account found for sense %s", wifiInfo.senseId));
    }


    /**
     * Get all senses and pills for an account
     *
     * @param deviceQueryInfo all the info needed to do queries
     * @return a Devices object which contains list of all associated senses and pills
     */
    public Devices getAllDevices(final DeviceQueryInfo deviceQueryInfo) {
        final List<DeviceAccountPair> senseAccountPairs = deviceDAO.getSensesForAccountId(deviceQueryInfo.accountId);
        final List<DeviceAccountPair> pillAccountPairs = deviceDAO.getPillsForAccountId(deviceQueryInfo.accountId);
        final Map<String, Optional<WifiInfo>> wifiInfoMap = retrieveWifiInfoMap(senseAccountPairs);
        final Optional<Pill.Color> pillColorOptional = retrievePillColor(deviceQueryInfo.accountId, senseAccountPairs);

        final List<Sense> senses = getSenses(deviceQueryInfo, senseAccountPairs, wifiInfoMap);
        final List<Pill> pills = getPills(pillAccountPairs, pillColorOptional);

        return new Devices(senses, pills);
    }

    private List<Sense> getSenses(final DeviceQueryInfo deviceQueryInfo, final List<DeviceAccountPair> senseAccountPairs, final Map<String, Optional<WifiInfo>> wifiInfoMap) {
        final List<Sense> senses = Lists.newArrayList();

        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            final Optional<DeviceStatus> senseStatusOptional = retrieveSenseStatus(senseAccountPair, deviceQueryInfo.isLastSeenDBEnabled, deviceQueryInfo.isSensorsDBUnavailable);
            final Optional<WifiInfo> wifiInfoOptional = wifiInfoMap.get(senseAccountPair.externalDeviceId);
            final Optional<Sense.Color> senseColorOptional = senseColorDAO.get(senseAccountPair.externalDeviceId);
            senses.add(Sense.create(senseAccountPair, senseStatusOptional, senseColorOptional, wifiInfoOptional));
        }
        return senses;
    }

    private List<Pill> getPills(final List<DeviceAccountPair> pillAccountPairs, final Optional<Pill.Color> pillColorOptional) {
        final List<Pill> pills = Lists.newArrayList();
        for (final DeviceAccountPair pillAccountPair : pillAccountPairs) {
            final Optional<DeviceStatus> pillStatusOptional = retrievePillStatus(pillAccountPair);
            pills.add(Pill.create(pillAccountPair, pillStatusOptional, pillColorOptional));
        }
        return pills;
    }


    @VisibleForTesting
    public Optional<DeviceStatus> retrieveSenseStatus(final DeviceAccountPair senseAccountPair, final Boolean isLastSeenDBEnabled, final Boolean isSensorsDBUnavailable) {
        // First attempt: get it from last seen record in dynamo db
        if (isLastSeenDBEnabled) {
            return sensorsViewsDynamoDB.senseStatus(senseAccountPair.externalDeviceId, senseAccountPair.accountId, senseAccountPair.internalDeviceId);
        }

        if (isSensorsDBUnavailable) {
            return Optional.absent();
        }
        // Second attempt: get it from sensor db with assumption that such sense has been active since an hour ago
        Optional<DeviceStatus> senseStatusOptional = deviceDataDAO.senseStatusLastHour(senseAccountPair.internalDeviceId);

        // Third attempt: get if from sensor db with assumption that such sense has been active since a week ago
        if (!senseStatusOptional.isPresent()) {
            senseStatusOptional = deviceDataDAO.senseStatusLastWeek(senseAccountPair.internalDeviceId);
        }
        return senseStatusOptional;
    }


    @VisibleForTesting
    public Optional<DeviceStatus> retrievePillStatus(final DeviceAccountPair pillAccountPair) {
        // First attempt: get it from heartbeat
        Optional<DeviceStatus> pillStatusOptional = this.pillHeartBeatDAO.getPillStatus(pillAccountPair.internalDeviceId);

        // Second attempt: get it from tracker motion
        if (!pillStatusOptional.isPresent()) {
            pillStatusOptional = this.trackerMotionDAO.pillStatus(pillAccountPair.internalDeviceId, pillAccountPair.accountId);
        }
        return pillStatusOptional;
    }


    @VisibleForTesting
    public Optional<Pill.Color> retrievePillColor(final Long accountId, final List<DeviceAccountPair> senseAccountPairs) {
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            if (!accountId.equals(senseAccountPair.accountId)) {
                continue;
            }
            final Optional<UserInfo> userInfoOptional = mergedUserInfoDynamoDB.getInfo(senseAccountPair.externalDeviceId, senseAccountPair.accountId);
            if (!userInfoOptional.isPresent()) {
                return Optional.absent();
            }
            final Optional<OutputProtos.SyncResponse.PillSettings> pillSettingsOptional =  userInfoOptional.get().pillColor;
            if (!pillSettingsOptional.isPresent()){
                return Optional.absent();
            }
            return Optional.of(PillColorUtil.displayPillColor(pillSettingsOptional.get().getPillColor()));
        }
        return Optional.absent();
    }


    @VisibleForTesting
    public Map<String, Optional<WifiInfo>> retrieveWifiInfoMap(final List<DeviceAccountPair> senseAccountPairs) {
        final List<String> senseIds = Lists.newArrayList();
        for (final DeviceAccountPair senseAccountPair : senseAccountPairs) {
            senseIds.add(senseAccountPair.externalDeviceId);
        }
        return wifiInfoDAO.getBatchStrict(senseIds);
    }

    public static class Builder {
        private DeviceDAO deviceDAO;
        private DeviceDataDAO deviceDataDAO;
        private MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
        private SensorsViewsDynamoDB sensorsViewsDynamoDB;
        private PillHeartBeatDAO pillHeartBeatDAO;
        private TrackerMotionDAO trackerMotionDAO;
        private WifiInfoDAO wifiInfoDAO;
        private SenseColorDAO senseColorDAO;

        public Builder withDeviceDAO(final DeviceDAO deviceDAO) {
            this.deviceDAO = deviceDAO;
            return this;
        }

        public Builder withDeviceDataDAO(final DeviceDataDAO deviceDataDAO) {
            this.deviceDataDAO = deviceDataDAO;
            return this;
        }

        public Builder withMergedUserInfoDynamoDB(final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
            this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
            return this;
        }

        public Builder withSensorsViewDynamoDB(final SensorsViewsDynamoDB sensorsViewDynamoDB) {
            this.sensorsViewsDynamoDB = sensorsViewDynamoDB;
            return this;
        }

        public Builder withPillHeartbeatDAO(final PillHeartBeatDAO pillHeartbeatDAO) {
            this.pillHeartBeatDAO = pillHeartbeatDAO;
            return this;
        }

        public Builder withTrackerMotionDAO(final TrackerMotionDAO trackerMotionDAO) {
            this.trackerMotionDAO = trackerMotionDAO;
            return this;
        }

        public Builder withWifiInfoDAO(final WifiInfoDAO wifiInfoDAO) {
            this.wifiInfoDAO = wifiInfoDAO;
            return this;
        }

        public Builder withSenseColorDAO(final SenseColorDAO senseColorDAO) {
            this.senseColorDAO = senseColorDAO;
            return this;
        }

        public DeviceProcessor build() {
            return new DeviceProcessor(deviceDAO, deviceDataDAO, mergedUserInfoDynamoDB, sensorsViewsDynamoDB, pillHeartBeatDAO, trackerMotionDAO, wifiInfoDAO, senseColorDAO);
        }
    }

    public static class InvalidWifiInfoException extends Exception {
        public InvalidWifiInfoException(final String message) {
            super(message);
        }
    }

}