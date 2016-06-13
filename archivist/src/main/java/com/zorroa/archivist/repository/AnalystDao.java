package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.domain.AnalystState;

import java.util.List;

/**
 * Created by chambers on 2/10/16.
 */
public interface AnalystDao {

    Analyst create(AnalystPing ping);

    boolean update(AnalystPing ping);

    Analyst get(int id);

    Analyst get(String url);

    int count();

    List<Analyst> getAll();

    List<Analyst> getAll(AnalystState state);

    boolean setState(String host, AnalystState newState, AnalystState oldState);

    boolean setState(String host, AnalystState newState);

    List<Analyst> getActive();
}
