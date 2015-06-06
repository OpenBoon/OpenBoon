package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;

public interface UserDao {

    User get(String id);

    User create(UserBuilder builder);

    String getPassword(String id);

}
