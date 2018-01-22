package com.zorroa.cluster.zps;

import com.zorroa.sdk.domain.ZorroaSdkException;

/**
 * Created by chambers on 7/3/16.
 */
public class ZpsException extends ZorroaSdkException {

    public ZpsException() {
        super();
    }

    public ZpsException(String message) {
        super(message);
    }

    public ZpsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZpsException(Throwable cause) {
        super(cause);
    }

}
