package com.zorroa.archivist.repository;

import java.util.Set;

public interface FieldDao {

    boolean hideField(String name, boolean manual);

    boolean unhideField(String name);

    Set<String> getHiddenFields();
}
