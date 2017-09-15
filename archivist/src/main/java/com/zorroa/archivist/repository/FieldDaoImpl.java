package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class FieldDaoImpl extends AbstractDao implements FieldDao {

    @Override
    public boolean hideField(String name, boolean manual) {
        if (isDbVendor("postgresql")) {
            return jdbc.update(
                    "INSERT INTO field_hide (pk_field, bool_manual) VALUES (?, ?) ON CONFLICT(pk_field) DO NOTHING",
                    name, manual) == 1;
        }
        else {
            return jdbc.update("MERGE INTO field_hide (pk_field, bool_manual) KEY(pk_field) VALUES (?, ?)", name, manual) == 1;
        }
    }

    @Override
    public boolean unhideField(String name) {
        return jdbc.update("DELETE from field_hide WHERE pk_field=?", name) == 1;
    }

    @Override
    public Set<String> getHiddenFields() {
        return ImmutableSet.copyOf(jdbc.queryForList("SELECT pk_field from field_hide", String.class));
    }
}
