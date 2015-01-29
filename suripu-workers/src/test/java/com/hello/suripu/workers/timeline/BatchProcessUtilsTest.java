package com.hello.suripu.workers.timeline;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 1/29/15.
 */
public class BatchProcessUtilsTest {


    private DeviceDAO deviceDAO = mock(DeviceDAO.class);
    private MergedUserInfoDynamoDB mergedUserInfoDynamoDB = mock(MergedUserInfoDynamoDB.class);

    @Test
    public void testGroupAccountAndProcessDateLocalUTC(){
        final HashMap<String, Set<DateTime>> groupedPillIds = new HashMap<>();
        final DateTime targetDate1 = new DateTime(2015, 1, 20, 7, 10, DateTimeZone.UTC);
        final DateTime targetDate2 = new DateTime(2015, 1, 20, 20, 0, DateTimeZone.UTC);
        final HashSet<DateTime> targetDatesUTC = new HashSet<>();
        targetDatesUTC.add(targetDate1);
        targetDatesUTC.add(targetDate2);

        final String pillId1 = "Pang's 911";
        final String sensId = "Sense";
        groupedPillIds.put(pillId1, targetDatesUTC);

        final long accountId = 1L;
        final List<DeviceAccountPair> deviceAccountPairsForPill = new ArrayList<>();
        deviceAccountPairsForPill.add(new DeviceAccountPair(accountId, 2L, pillId1));

        final List<DeviceAccountPair> deviceAccountPairsForSense = new ArrayList<>();
        deviceAccountPairsForSense.add(new DeviceAccountPair(accountId, 3L, sensId));

        when(deviceDAO.getLinkedAccountFromPillId(pillId1)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForPill));
        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForSense));

        when(mergedUserInfoDynamoDB.getTimezone(sensId, accountId)).thenReturn(Optional.of(DateTimeZone.UTC));

        final Map<Long, Set<DateTime>> groupedtargetDateLocalUTC = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(groupedPillIds,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        final Set<DateTime> expected = new HashSet<>();
        expected.add(new DateTime(2015, 1, 19, 0, 0, DateTimeZone.UTC));
        expected.add(new DateTime(2015, 1, 20, 0, 0, DateTimeZone.UTC));

        assertThat(groupedtargetDateLocalUTC.size(), is(1));
        assertThat(groupedtargetDateLocalUTC.get(accountId).containsAll(expected), is(true));

    }


    @Test
    public void testGroupAccountAndProcessDateLocalUTCSameDay(){
        final HashMap<String, Set<DateTime>> groupedPillIds = new HashMap<>();
        final DateTime targetDate1 = new DateTime(2015, 1, 20, 7, 10, DateTimeZone.UTC);
        final DateTime targetDate2 = new DateTime(2015, 1, 20, 9, 0, DateTimeZone.UTC);
        final HashSet<DateTime> targetDatesUTC = new HashSet<>();
        targetDatesUTC.add(targetDate1);
        targetDatesUTC.add(targetDate2);

        final String pillId1 = "Pang's 911";
        final String sensId = "Sense";
        groupedPillIds.put(pillId1, targetDatesUTC);

        final long accountId = 1L;
        final List<DeviceAccountPair> deviceAccountPairsForPill = new ArrayList<>();
        deviceAccountPairsForPill.add(new DeviceAccountPair(accountId, 2L, pillId1));

        final List<DeviceAccountPair> deviceAccountPairsForSense = new ArrayList<>();
        deviceAccountPairsForSense.add(new DeviceAccountPair(accountId, 3L, sensId));

        when(deviceDAO.getLinkedAccountFromPillId(pillId1)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForPill));
        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(ImmutableList.copyOf(deviceAccountPairsForSense));

        when(mergedUserInfoDynamoDB.getTimezone(sensId, accountId)).thenReturn(Optional.of(DateTimeZone.UTC));

        final Map<Long, Set<DateTime>> groupedtargetDateLocalUTC = BatchProcessUtils.groupAccountAndProcessDateLocalUTC(groupedPillIds,
                this.deviceDAO,
                this.mergedUserInfoDynamoDB);

        final Set<DateTime> expected = new HashSet<>();
        expected.add(new DateTime(2015, 1, 19, 0, 0, DateTimeZone.UTC));

        assertThat(groupedtargetDateLocalUTC.size(), is(1));
        assertThat(groupedtargetDateLocalUTC.get(accountId).containsAll(expected), is(true));

    }
}