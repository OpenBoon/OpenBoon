package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.*;

import java.util.List;

public interface BlobDao {

    Blob create(String app, String feature, String name, Object data);

    boolean update(BlobId bid, Object data);

    boolean delete(BlobId blob);

    Blob get(int id);

    Blob get(String app, String feature, String name);

    BlobId getId(String app, String feature, String name, Access forAccess);

    List<Blob> getAll(String app, String feature);

    Acl getPermissions(BlobId blob);

    Acl setPermissions(BlobId blob, SetPermissions req);
}
