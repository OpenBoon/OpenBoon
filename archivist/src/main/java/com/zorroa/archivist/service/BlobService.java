package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;

import java.util.List;

public interface BlobService {

    Blob set(String app, String feature, String name, Object blob);

    Blob get(String app, String feature, String name);

    BlobId getId(String app, String feature, String name, Access forAccess);

    Acl getPermissions(BlobId blob);

    Acl setPermissions(BlobId blob, SetPermissions perms);

    List<Blob> getAll(String app, String feature);
}
