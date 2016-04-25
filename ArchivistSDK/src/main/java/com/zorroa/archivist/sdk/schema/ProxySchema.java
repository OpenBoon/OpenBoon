package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.archivist.sdk.domain.Proxy;

/**
 * The ProxySchema is a list of available proxy objects.
 */
public class ProxySchema extends ListSchema<Proxy> {

    /**
     * Return the largest proxy.
     *
     * @return
     */
    @JsonIgnore
    public Proxy getLargest()  {
        return stream()
                .sorted((o1, o2) -> Integer.compare(o2.getWidth(), o1.getWidth()))
                .findFirst().orElse(null);
    }

    /**
     * Return the smallest proxy.
     *
     * @return
     */
    @JsonIgnore
    public Proxy getSmallest()  {
        return stream()
                .sorted((o1, o2) -> Integer.compare(o1.getWidth(), o2.getWidth()))
                .findFirst().orElse(null);
    }

    /**
     * Return the first proxy greater than or equal to the minimum dimension.  If there is problem loading
     * the proxy IOException is thrown.
     *
     * @param minDim
     * @return
     */
    @JsonIgnore
    public Proxy atLeastThisSize(int minDim)  {
        return stream()
                .filter(p->p.getWidth() >= minDim || p.getHeight() >= minDim)
                .sorted((o1, o2) -> Integer.compare(o1.getWidth(), o2.getWidth()))
                .findFirst().orElse(null);
    }

    /**
     * Return the first proxy less than or equal to the minimum dimension.  If there is problem loading
     * the proxy IOException is thrown.
     *
     * @param minDim
     * @return
     */
    @JsonIgnore
    public Proxy thisSizeOrBelow(int minDim)  {
        return stream()
                .filter(p->p.getWidth() <=minDim || p.getHeight() <= minDim)
                .sorted((o1, o2) -> Integer.compare(o2.getWidth(), o1.getWidth()))
                .findFirst().orElse(null);
    }
}
