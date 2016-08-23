package com.zorroa.archivist.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.sdk.domain.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Copied from the standard SessionRegistryImpl except with JDBC support.  Eventually
 * we'll remove the in-memory session storage.
 *
 * https://github.com/spring-projects/spring-security
 *
 * @author chambers
 *
 */
public class JdbcSessionRegistry implements SessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(JdbcSessionRegistry.class);

    @Autowired
    SessionDao sessionDao;

    @Autowired
    UserDao userDao;

    private final LoadingCache<String, SessionInformation> sessionCache = CacheBuilder.newBuilder()
            .maximumSize(250)
            .concurrencyLevel(4)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build(new CacheLoader<String, SessionInformation>() {
                public SessionInformation load(String key) throws Exception {
                    Session s = sessionDao.get(key);
                    SessionInformation info;
                    if (s == null) {
                        info = new SessionInformation("anonymous", key,
                                new Date(s.getRefreshTime()));
                    }
                    else {
                        info = new SessionInformation(
                                userDao.get(s.getUserId()), key, new Date(s.getRefreshTime()));
                    }
                    return info;
                }
            });

    /**
     * Get list of unique users with a session. I don't know why this is needed.
     * @return
     */
    @Override
    public List<Object> getAllPrincipals() {
        return ImmutableList.copyOf(userDao.getAllWithSession());
    }

    /**
     * Get all sessions for a given user.  I don't know why this is needed.
     *
     * @param principal
     * @param includeExpiredSessions
     * @return
     */
    @Override
    public List<SessionInformation> getAllSessions(Object principal,
                                                   boolean includeExpiredSessions) {
        User user = getUser(principal);
        List<Session> sessions = sessionDao.getAll(user);
        List<SessionInformation> result = Lists.newArrayListWithCapacity(sessions.size());

        for (Session s: sessions) {
            result.add(new SessionInformation(
                    user, s.getCookieId(), new Date(s.getRefreshTime())));
        }
        return result;
    }

    @Override
    public SessionInformation getSessionInformation(String sessionId) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");

        try {
            return sessionCache.get(sessionId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void refreshLastRequest(String sessionId) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");

        SessionInformation info = getSessionInformation(sessionId);

        if (info != null) {
            info.refreshLastRequest();
            sessionDao.refreshLastRequestTime(sessionId);
        }
    }

    @Override
    public void registerNewSession(String sessionId, Object principal) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");
        Assert.notNull(principal, "Principal required as per interface contract");

        logger.info("Registering session " + sessionId + ", for principal "
                + principal);
        if (logger.isDebugEnabled()) {
            logger.debug("Registering session " + sessionId + ", for principal "
                    + principal);
        }

        principal = getUser(principal);

        Session session = sessionDao.create((User)principal, sessionId);
        logger.info("caching session id: {}", sessionId);
        sessionCache.put(sessionId,
                new SessionInformation(principal, sessionId, new Date()));
    }

    @Override
    public void removeSessionInformation(String sessionId) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");
        SessionInformation info = getSessionInformation(sessionId);

        if (info == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.debug("Removing session " + sessionId
                    + " from set of registered sessions");
        }

        sessionDao.delete(sessionId);
        sessionCache.invalidate(sessionId);
    }

    public void onApplicationEvent(SessionDestroyedEvent event) {
        String sessionId = event.getId();
        removeSessionInformation(sessionId);
    }

    public User getUser(Object principal) {
        if (!(principal instanceof User)) {
            try {
                return userDao.get(principal.toString());
            } catch (Exception e) {
                throw new BadCredentialsException("Invalid username or password");
            }
        }
        else {
            return (User) principal;
        }
    }
}
