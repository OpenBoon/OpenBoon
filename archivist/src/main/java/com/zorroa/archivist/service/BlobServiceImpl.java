package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.BlobDao;
import com.zorroa.archivist.repository.PermissionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BlobServiceImpl implements BlobService {

    @Autowired
    BlobDao blobDao;

    @Autowired
    PermissionDao permissionDao;

    @Override
    public Blob set(String app, String feature, String name, Object blob) {
        try {
            BlobId id = blobDao.getId(app, feature, name, Access.Write);
            blobDao.update(id, blob);
            return blobDao.get(app, feature, name);
        } catch (EmptyResultDataAccessException e) {
            return blobDao.create(app, feature, name, blob);
        }
    }

    @Override
    public Blob get(String app, String feature, String name) {
        return blobDao.get(app, feature, name);
    }

    @Override
    public BlobId getId(String app, String feature, String name, Access forAccess) {
        return blobDao.getId(app, feature, name, forAccess);
    }

    @Override
    public Acl getPermissions(BlobId blob) {
        return blobDao.getPermissions(blob);
    }

    @Override
    public Acl setPermissions(BlobId blob, SetPermissions perms) {
        permissionDao.resolveAcl(perms.getAcl(), false);
        return blobDao.setPermissions(blob, perms);
    }

    @Override
    public List<Blob> getAll(String app, String feature) {
        return blobDao.getAll(app, feature);
    }
}
