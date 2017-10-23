package com.zorroa.archivist.domain;

import com.google.common.collect.Lists;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.domain.Tuple;

import java.util.List;

/**
 * Created by chambers on 7/12/16.
 */
public class JobFilter {

    private JobState state;

    private PipelineType type;

    private Integer userId;

    public JobState getState() {
        return state;
    }

    public JobFilter setState(JobState state) {
        this.state = state;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public JobFilter setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public Integer getUserId() {
        return userId;
    }

    public JobFilter setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public Tuple<String, List<Object>> getQuery(String base, Pager page) {
        List<String> where = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (state != null) {
            where.add("int_state=?");
            values.add(state.ordinal());
        }

        if (type != null) {
            where.add("int_type=?");
            values.add(type.ordinal());
        }

        if (userId != null) {
            where.add("int_user_created=?");
            values.add(userId);
        }

        if (page != null) {
            values.add(page.getSize());
            values.add(page.getFrom());
        }

        StringBuilder sb = new StringBuilder(base.length() + 256);
        sb.append(base);
        if (!where.isEmpty()) {
            if (base.contains(" WHERE" )) {
                sb.append(" AND ");
            }
            else {
                sb.append( "WHERE ");
            }
            sb.append(String.join(" AND ", where));
        }

        if (page != null) {
            sb.append("ORDER BY pk_job DESC ");
            sb.append("LIMIT ? OFFSET ?");
        }

        return new Tuple(sb.toString(), values);
    }
}
