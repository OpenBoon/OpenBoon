package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.search.AssetSearch;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LogSpec defines log entry properties.
 */
public class LogSpec {

    private final long timestamp = System.currentTimeMillis();

    /**
     * The message, if any.
     */
    private String message;

    /**
     * Store both the name and the ID
     *
     * "user": {
     *     "username": "foo",
     *     "id": 1
     * }
     */
    private Map<String, Object> user;

    /**
     * Store the target object this way to maintain the typed Id.
     *
     * "target": {
     *      "export": {
     *          "id": 1
     *      }
     * }
     *
     */
    private Map<String, Object[]> target;

    /**
     * The type of log, action, event, etc.
     */
    private String type;

    private String action;

    private Set<String> query;

    private Map<String,Object> attrs;

    public LogSpec() {

    }

    public static final LogSpec build(LogAction action, AssetSearch search) {
        return new LogSpec()
                .setUser(SecurityUtils.getUserOrNull())
                .setAction(action.toString().toLowerCase())
                .setSearch(search);
    }

    public static final LogSpec build(String action, Loggable target) {
        return new LogSpec()
                .setUser(SecurityUtils.getUserOrNull())
                .setAction(action.toString().toLowerCase())
                .setTarget(target);
    }

    public static final LogSpec build(LogAction action, Loggable target) {
        return new LogSpec()
                .setUser(SecurityUtils.getUserOrNull())
                .setAction(action.toString().toLowerCase())
                .setTarget(target);
    }
    public static final LogSpec build(LogAction action, String type, Object ... id) {
        return new LogSpec()
                .setUser(SecurityUtils.getUserOrNull())
                .setAction(action.toString().toLowerCase())
                .setTarget(ImmutableMap.of(type, new Object[] {id}));
    }

    public LogSpec setUser(UserBase user) {
        if (user != null) {
            this.user = ImmutableMap.of(
                    "username", user.getUsername(),
                    "id", user.getId());
        }
        return this;
    }

    public LogSpec setTarget(Loggable target) {
        this.target = ImmutableMap.of(
                target.getTargetType(), new Object[] {target.getTargetId() });
        return this;
    }

    public String getAction() {
        return action;
    }

    public LogSpec setAction(String action) {
        this.action = action;
        return this;
    }

    public LogSpec setAction(LogAction action) {
        this.action = action.toString().toLowerCase();
        return this;
    }

    public Set<String> getQuery() {
        return query;
    }

    public LogSpec setQuery(Set<String> query) {
        this.query = query;
        return this;
    }

    public LogSpec setSearch(AssetSearch search) {
        this.query = Sets.newHashSet();
        if (search.isQuerySet()) {
            this.query.add(search.getQuery());
        }
        if (search.getFilter() != null) {
            if (search.getFilter().getTerms() != null) {
                for (Map.Entry<String, List<Object>> entry : search.getFilter().getTerms().entrySet()) {
                    for (Object value: entry.getValue()) {
                        this.query.add(value.toString());
                    }
                }
            }
        }
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public LogSpec setMessage(String message) {
        this.message = message;
        return this;
    }

    public Map<String, Object> getUser() {
        return user;
    }

    public LogSpec setUser(Map<String, Object> user) {
        this.user = user;
        return this;
    }

    public Map<String, Object[]> getTarget() {
        return target;
    }

    public LogSpec setTarget(Map<String, Object[]> target) {
        this.target = target;
        return this;
    }

    public String getType() {
        return type;
    }

    public LogSpec setType(String type) {
        this.type = type;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public LogSpec setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    public LogSpec putToAttrs(String key, Object value) {
        if (this.attrs == null) {
            this.attrs = Maps.newHashMap();
        }
        this.attrs.put(key, value);
        return this;
    }
}
