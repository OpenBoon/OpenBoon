package com.zorroa.security;

import java.util.List;

/**
 * Created by chambers on 7/13/17.
 */
public interface UserDetailsPlugin {

    List<String> getGroups(String username);

    String getGroupType();

    String getEmailDomain();

}
