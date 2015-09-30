package com.zorroa.archivist;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractSearchScript;

import java.util.*;

public class ArchivistDateScript extends AbstractSearchScript {

    private String fieldParam;
    private String intervalParam;
    private List<String> termsParam;
    private int calendarField;

    public ArchivistDateScript(@Nullable Map<String, Object> params) {
        fieldParam = (String)params.get("field");
        termsParam = (List<String>)params.get("terms");
        intervalParam = (String)params.get("interval");
        if (intervalParam.equals("year")) {
            calendarField = Calendar.YEAR;
        } else if (intervalParam.equals("month")) {
            calendarField = Calendar.MONTH;
        } else if (intervalParam.equals("day")) {
            calendarField = Calendar.DAY_OF_WEEK;
        }
    }

    @Override
    public Object run() {
        Object field = doc().get(fieldParam);
        Date date = null;
        if (field.getClass() == ScriptDocValues.Longs.class) {
            ScriptDocValues.Longs longs = (ScriptDocValues.Longs)field;
            date = new Date(longs.get(0));
        } else if (field.getClass() == Date.class) {
            date = (Date) field;
        } else {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Object value = calendar.getDisplayName(calendarField, Calendar.LONG, Locale.ENGLISH);
        if (value == null) {
            value = Integer.toString(calendar.get(calendarField));
        }
        if (termsParam == null) {
            return value;
        }
        for (String term : termsParam) {
            if (term.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
