package com.hello.suripu.core.db.colors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Device;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

public abstract class SenseColorDAOSQLImpl implements SenseColorDAO {

    @SingleValueResult
    @RegisterMapper(DeviceColorMapper.class)
    @SqlQuery("SELECT * FROM sense_colors WHERE sense_id = :sense_id LIMIT 1")
    public abstract Optional<Device.Color> getColorForSense(@Bind("sense_id") final String senseId);

    @SqlUpdate("INSERT INTO sense_colors (sense_id, color) VALUES(:sense_id, :color);")
    public abstract int saveColorForSense(@Bind("sense_id") final String senseId, @Bind("color") final String color);

    @SqlUpdate("UPDATE sense_colors SET color = :color WHERE sense_id = :sense_id;")
    public abstract int update(@Bind("sense_id") final String senseId, @Bind("color") final String color);

    @SqlQuery("SELECT device_id\n" +
            "FROM   account_device_map\n" +
            "LEFT OUTER JOIN sense_colors\n" +
            "  ON (account_device_map.device_id = sense_colors.sense_id)\n" +
            "  WHERE sense_colors.color IS NULL;")
    public abstract ImmutableList<String> missing();
}
