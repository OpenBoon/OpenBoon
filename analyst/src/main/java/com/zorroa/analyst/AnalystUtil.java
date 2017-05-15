package com.zorroa.analyst;

import java.net.URI;

/**
 * Created by chambers on 5/10/17.
 */
public class AnalystUtil {

    public static final String convertUriToClusterAddr(String uri) {

        /**
         * Backwards compatible with archivist 0.34
         */
        if (uri.startsWith("http")) {
            URI u = URI.create(uri);
            int port = u.getPort();
            if (port == 8066) {
                port = port-1;
            }
            return u.getHost().concat(":").concat(String.valueOf(port));
        }
        else {
            return uri;
        }
    }

}
