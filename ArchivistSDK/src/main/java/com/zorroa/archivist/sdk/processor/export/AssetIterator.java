package com.zorroa.archivist.sdk.processor.export;

import com.zorroa.archivist.sdk.domain.Asset;

/**
 * A AssetIterator provides an Iterable<Asset> to an Export graph.  The iterator should allow for
 * arbitrarily large exports by paging assets into memory as iteration occurs.
 */
public interface AssetIterator {

    /**
     * Return an Iterable which provides Assets
     *
     * @return
     */
    Iterable<Asset> getIterator();

}
