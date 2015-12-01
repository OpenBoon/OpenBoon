package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Created by chambers on 9/5/15.
 */
@Repository
public class IngestScheduleDaoImpl extends AbstractDao implements IngestScheduleDao {

    private static final String INSERT =
            "INSERT INTO " +
                "schedule " +
            "(" +
                "str_name,"+
                "user_created,"+
                "time_created,"+
                "user_modified, "+
                "time_modified, "+
                "clock_run_at_time, " +
                "time_next,"+
                "csv_days " +
            ") "+
            "VALUES (?,?,?,?,?,?,?,?)";

    private static final String UPDATE_INGESTS =
            "INSERT INTO " +
                    "map_schedule_to_ingest " +
                    "(pk_schedule, pk_ingest) VALUES (?,?)";


    @Override
    public IngestSchedule create(IngestScheduleBuilder builder) {
        long time = System.currentTimeMillis();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_schedule"});
            ps.setString(1, builder.getName());
            ps.setInt(2, SecurityUtils.getUser().getId());
            ps.setLong(3, time);
            ps.setInt(4, SecurityUtils.getUser().getId());
            ps.setLong(5, time);

            /*
             * DB wants time formatted particular way.
             */
            String runAtTime = builder.getRunAtTime();
            if (StringUtils.countMatches(runAtTime, ':') == 1) {
                runAtTime = runAtTime + ":00";
            }

            ps.setString(6, runAtTime);
            ps.setLong(7, IngestSchedule.determineNextRunTime(
                    builder.getDays(), builder.getRunAtTimeLocalTime()));

            String days = StringUtils.join(
                    builder.getDays().stream().map(p->p.ordinal()).toArray(), ",");
            ps.setString(8, days);

            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();

        if (builder.getIngestIds() != null) {
            for (Long ingestId : builder.getIngestIds()) {
                jdbc.update(UPDATE_INGESTS, id, ingestId);
            }
        }

        return get(id);
    }

    private final RowMapper<IngestSchedule> MAPPER = (rs, row) -> {
        IngestSchedule result = new IngestSchedule();
        result.setId(rs.getInt("pk_schedule"));
        result.setName(rs.getString("str_name"));
        result.setRunAtTime(rs.getString("clock_run_at_time"));
        result.setIngestIds(jdbc.queryForList("SELECT pk_ingest FROM map_schedule_to_ingest WHERE pk_schedule=?",
                Long.class, rs.getInt("pk_schedule")));
        List<DayOfWeek> days = Lists.newArrayList();
        for (String v : rs.getString("csv_days").split(",")) {
            days.add(DayOfWeek.values()[Integer.valueOf(v).intValue()]);
        }
        result.setDays(days);
        return result;
    };

    @Override
    public IngestSchedule get(int id) {
        return jdbc.queryForObject("SELECT * FROM schedule WHERE pk_schedule=?", MAPPER, id);
    }

    @Override
    public List<IngestSchedule> getAll() {
        return jdbc.query("SELECT * FROM schedule", MAPPER);
    }


    @Override
    public List<IngestSchedule> getAllReady() {
        return jdbc.query("SELECT * FROM schedule WHERE bool_enabled=? AND time_next < ?",
                MAPPER, true, System.currentTimeMillis());
    }

    @Override
    public void started(IngestSchedule schedule) {
        jdbc.update("UPDATE schedule SET time_executed=?, time_next=? WHERE pk_schedule=?",
                System.currentTimeMillis(), IngestSchedule.determineNextRunTime(schedule), schedule.getId());
    }

    @Override
    public void mapScheduleToIngests(IngestSchedule schedule, List<Long> ingests) {
        jdbc.update("DELETE FROM map_schedule_to_ingest WHERE pk_schedule=?", schedule.getId());

        if (ingests == null || ingests.isEmpty()) {
            return;
        }

        jdbc.batchUpdate(UPDATE_INGESTS, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, schedule.getId());
                ps.setLong(2, ingests.get(i));
            }
            @Override
            public int getBatchSize() {
                return ingests.size();
            }
        });
    }

    private static final String UPDATE =
            "UPDATE " +
                "schedule " +
            "SET " +
                "str_name=?,"+
                "user_modified=?,"+
                "time_modified=?,"+
                "clock_run_at_time=?,"+
                "time_next=?,"+
                "csv_days=? " +
            "WHERE " +
                "pk_schedule=?";

    @Override
    public boolean update(IngestSchedule schedule) {

        String days = "";
        if (schedule.getDays() != null) {
            days = StringUtils.join(
                    schedule.getDays().stream().map(p->p.ordinal()).toArray(), ",");
        }

        mapScheduleToIngests(schedule, schedule.getIngestIds());

        return jdbc.update(UPDATE,
                schedule.getName(),
                SecurityUtils.getUser().getId(),
                System.currentTimeMillis(),
                schedule.getRunAtTime(),
                IngestSchedule.determineNextRunTime(
                        schedule.getDays(), LocalTime.parse(schedule.getRunAtTime())),
                days,
                schedule.getId()) == 1;
    }
}
