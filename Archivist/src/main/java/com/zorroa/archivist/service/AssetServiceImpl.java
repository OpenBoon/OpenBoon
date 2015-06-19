package com.zorroa.archivist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;
import com.zorroa.archivist.repository.AssetDao;

/**
 *
 * @author chambers
 *
 */
@Component
public class AssetServiceImpl implements AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetServiceImpl.class);

    @Autowired
    AssetDao assetDao;

    @Override
    public Asset createAsset(AssetBuilder builder) {
        return assetDao.create(builder);
    }

    @Override
    public boolean fastCreateAsset(AssetBuilder builder) {
        try {
            assetDao.fastCreate(builder);
            return true;
        } catch (org.elasticsearch.index.engine.DocumentAlreadyExistsException ignore) {
            // thrown if the doc exists.
        }
        catch (Exception e) {
            logger.warn("failed to ingest asset: {}", builder, e);
        }
        return false;
    }

    @Override
    public boolean assetExistsByPath(String path) {
        return assetDao.existsByPath(path);
    }
}
