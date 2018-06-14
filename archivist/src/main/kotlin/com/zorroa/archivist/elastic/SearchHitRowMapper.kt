package com.zorroa.archivist.elastic


/**
 * An interface used by ElasticTemplate for mapping documents from an Elastic search result
 * on a per-document basis.  Implementations of this interface perform the actual work of
 * mapping each document to an object.
 *
 * @author chambers
 *
 * @param <T>
</T> */
interface SearchHitRowMapper<T> {

    @Throws(Exception::class)
    fun mapRow(hit: SingleHit): T

}
