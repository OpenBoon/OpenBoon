package com.zorroa.archivist.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.sdk.domain.Pager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class DaoFilter {

    protected static final Logger logger = LoggerFactory.getLogger(DaoFilter.class);

    @JsonIgnore
    private boolean built = false;

    @JsonIgnore
    private String whereClause;

    @JsonIgnore
    protected List<String> where = Lists.newArrayList();

    @JsonIgnore
    protected List<Object> values = Lists.newArrayList();

    protected Map<String, String> sort = Maps.newHashMap();

    public DaoFilter() { }

    public abstract void build();

    @JsonIgnore
    public abstract Map<String,String> getSortMap();

    public void addToWhere(String col) {
        this.where.add(col);
    }

    public void addToValues(Object ... val) {
        for (Object o: val) {
            values.add(o);
        }
    }

    public String getQuery(String base, Pager page) {
        __build();
        StringBuilder sb = new StringBuilder(1024);
        sb.append(base);
        sb.append(" ");
        if (JdbcUtils.isValid(whereClause)) {
            if (!base.contains("WHERE")) {
                sb.append(" WHERE ");
            }
            sb.append(whereClause);
        }

        if (getSortMap() != null && !sort.isEmpty()) {
            StringBuilder order = new StringBuilder(64);
            for (Map.Entry<String, String> sort: sort.entrySet()) {
                String col = getSortMap().get(sort.getKey());
                if (col != null) {
                    order.append(col + " " + (sort.getValue().startsWith("a") ? "asc " : "desc "));
                    order.append(",");
                }
            }
            if (order.length() > 0) {
                order.deleteCharAt(order.length()-1);
                sb.append(" ORDER BY ");
                sb.append(order);
            }
        }

        if (page != null) {
            sb.append(" LIMIT ? OFFSET ?");
        }

        return sb.toString();
    }

    public String getCountQuery(String base) {
        __build();

        StringBuilder sb = new StringBuilder(1024);
        sb.append("SELECT COUNT(1) FROM ");
        sb.append(base.substring(base.indexOf("FROM") + 5));
        if (JdbcUtils.isValid(whereClause)) {
            if (!base.contains("WHERE")) {
                sb.append(" WHERE ");
            }
            sb.append(whereClause);
        }
        return sb.toString();
    }

    public Object[] getValues() {
        __build();
        return values.toArray();
    }

    public Object[] getValues(Pager page) {
        __build();
        Object[] result = Arrays.copyOf(values.toArray(), values.size()+2);
        result[values.size()]= page.getSize();
        result[values.size()+1]= page.getFrom();
        return result;
    }

    private void __build() {
        if (!built) {
            built = true;
            build();

            if (JdbcUtils.isValid(where)) {
                whereClause = String.join(" AND ", where);
            }
            where.clear();
        }
    }

    public Map<String, String> getSort() {
        return sort;
    }

    public DaoFilter setSort(Map<String, String> sort) {
        this.sort = sort;
        return this;
    }

    @JsonIgnore
    public DaoFilter forceSort(Map<String, String> sort) {
        this.sort = sort;
        return this;
    }
}
