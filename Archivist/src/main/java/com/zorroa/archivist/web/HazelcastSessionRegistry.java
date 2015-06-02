package com.zorroa.archivist.web;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PostConstruct;

import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableList;
import com.hazelcast.core.HazelcastInstance;

/**
 * Copied from the standard SessionRegistryImpl, except using Hazelcast clustering which
 * allows authenticated HTTP sessions to span Archivist nodes.
 *
 * https://github.com/spring-projects/spring-security
 *
 * @author chambers
 *
 */
public class HazelcastSessionRegistry implements SessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastSessionRegistry.class);

    @Autowired
    HazelcastInstance hazelcastInstance;

    private Map<Object, Set<String>> principals;
    private Map<String, SessionInformation> sessionIds;

    @PostConstruct
    public void init() {
        principals = hazelcastInstance.getMap("principals");
        sessionIds = hazelcastInstance.getMap("sessionIds");
    }

    @Override
    public List<Object> getAllPrincipals() {
        return ImmutableList.copyOf(principals.keySet());
    }

    @Override
    public List<SessionInformation> getAllSessions(Object principal,
            boolean includeExpiredSessions) {

        final Set<String> sessionsUsedByPrincipal = principals.get(principal);

        if (sessionsUsedByPrincipal == null) {
            return ImmutableList.of();
        }

        List<SessionInformation> list = Lists.newArrayListWithExpectedSize(
                sessionsUsedByPrincipal.size());

        for (String sessionId : sessionsUsedByPrincipal) {
            SessionInformation sessionInformation = getSessionInformation(sessionId);

            if (sessionInformation == null) {
                continue;
            }

            if (includeExpiredSessions || !sessionInformation.isExpired()) {
                list.add(sessionInformation);
            }
        }

        return list;
    }

    @Override
    public SessionInformation getSessionInformation(String sessionId) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");
        return sessionIds.get(sessionId);
    }

    @Override
    public void refreshLastRequest(String sessionId) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");

        SessionInformation info = getSessionInformation(sessionId);

        if (info != null) {
            info.refreshLastRequest();
        }
    }

    @Override
    public void registerNewSession(String sessionId, Object principal) {
        Assert.hasText(sessionId, "SessionId required as per interface contract");
        Assert.notNull(principal, "Principal required as per interface contract");

        if (logger.isDebugEnabled()) {
            logger.debug("Registering session " + sessionId + ", for principal "
                    + principal);
        }

        if (getSessionInformation(sessionId) != null) {
            removeSessionInformation(sessionId);
        }

        sessionIds.put(sessionId,
                new SessionInformation(principal, sessionId, new Date()));

        Set<String> sessionsUsedByPrincipal = principals.get(principal);

        if (sessionsUsedByPrincipal == null) {
            sessionsUsedByPrincipal = new CopyOnWriteArraySet<String>();
            Set<String> prevSessionsUsedByPrincipal = principals.putIfAbsent(principal,
                    sessionsUsedByPrincipal);
            if (prevSessionsUsedByPrincipal != null) {
                sessionsUsedByPrincipal = prevSessionsUsedByPrincipal;
            }
        }

        sessionsUsedByPrincipal.add(sessionId);

        if (logger.isTraceEnabled()) {
            logger.trace("Sessions used by '" + principal + "' : "
                    + sessionsUsedByPrincipal);
        }
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

        sessionIds.remove(sessionId);

        Set<String> sessionsUsedByPrincipal = principals.get(info.getPrincipal());

        if (sessionsUsedByPrincipal == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Removing session " + sessionId
                    + " from principal's set of registered sessions");
        }

        sessionsUsedByPrincipal.remove(sessionId);

        if (sessionsUsedByPrincipal.isEmpty()) {
            // No need to keep object in principals Map anymore
            if (logger.isDebugEnabled()) {
                logger.debug("Removing principal " + info.getPrincipal()
                        + " from registry");
            }
            principals.remove(info.getPrincipal());
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Sessions used by '" + info.getPrincipal() + "' : "
                    + sessionsUsedByPrincipal);
        }

    }

    public void onApplicationEvent(SessionDestroyedEvent event) {
        String sessionId = event.getId();
        removeSessionInformation(sessionId);
    }
}
