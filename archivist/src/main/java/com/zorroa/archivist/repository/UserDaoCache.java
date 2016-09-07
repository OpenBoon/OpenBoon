package com.zorroa.archivist.repository;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zorroa.archivist.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 9/7/16.
 */
@Component
public class UserDaoCache {

    @Autowired
    UserDao userDao;

    private final LoadingCache<Integer, User> cachedUserName = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .initialCapacity(32)
            .maximumSize(1024)
            .build(new CacheLoader<Integer, User>() {
                @Override
                public User load(Integer key) throws Exception {
                    return userDao.get(key);
                }
            });

    public User getUser(int id) {
        try {
            return cachedUserName.get(id);
        } catch (Exception e) {
            return new User().setUsername("unknown").setId(0);
        }
    }
}
