package com.zorroa.archivist.repository.filters;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.common.domain.Paging;

import java.util.Arrays;
import java.util.List;

public abstract class Filter {

    private boolean built = false;
    private String orderBy;
    private String whereClause;

    protected List<String> where = Lists.newArrayList();
    protected List<Object> values = Lists.newArrayList();

    public Filter() { }

    public abstract void build();

    public String getQuery(String base, Paging page) {
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

        if (orderBy!=null) {
            sb.append(" ORDER BY ");
            sb.append(orderBy);
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

    public Object[] getValues(Paging page) {
        __build();
        Object[] result = Arrays.copyOf(values.toArray(), values.size()+2);
        result[values.size()]= page.getSize();
        result[values.size()+1]= page.getFrom();
        return result;
    }

    public void __build() {
        if (!built) {
            build();
            built = true;

            if (JdbcUtils.isValid(where)) {
                whereClause = String.join(" AND ", where);
            }
            where.clear();
        }
    }

    public String getOrderBy() {
        return orderBy;
    }

    public Filter setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }
}
