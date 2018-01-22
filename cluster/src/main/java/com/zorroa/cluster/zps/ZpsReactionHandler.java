package com.zorroa.cluster.zps;

import com.zorroa.sdk.processor.Reaction;
import com.zorroa.sdk.processor.SharedData;

/**
 * Created by chambers on 3/24/17.
 */
public interface ZpsReactionHandler {

    void handle(ZpsTask task, SharedData shared, Reaction reaction);

}
