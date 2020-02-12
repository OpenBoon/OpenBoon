package com.zorroa.zmlp.client.domain.asset;

import java.util.ArrayList;
import java.util.List;

public class BatchAssetSpec {

    private List<AssetSpec> batch = new ArrayList();

    public BatchAssetSpec addAsset(AssetSpec assetSpec){
        batch.add(assetSpec);
        return this;
    }

    public BatchAssetSpec addAsset(String assetSpecUrl){
        batch.add(new AssetSpec(assetSpecUrl));
        return this;
    }

    public BatchAssetSpec addAssets(List<AssetSpec> assetSpecs){
        this.batch.addAll(assetSpecs);
        return this;
    }

    public BatchAssetSpec clear(){
        this.batch.clear();
        return this;
    }

    public List<AssetSpec> getBatch() {
        return batch;
    }
}
