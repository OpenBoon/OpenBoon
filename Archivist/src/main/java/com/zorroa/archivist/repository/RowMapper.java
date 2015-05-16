package com.zorroa.archivist.repository;


/**
 * An interface used by ElasticTemplate for mapping documents from an Elastic search result
 * on a per-document basis.  Implementations of this interface perform the actual work of
 * mapping each document to an object.
 *
 * @author chambers
 *
 * @param <T>
 */
public interface RowMapper<T> {

    public T mapRow(String id, long version, byte[] source);

}
