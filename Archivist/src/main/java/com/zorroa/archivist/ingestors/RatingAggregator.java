package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * Added the ratings folders via SQL schema update.
 */
@Deprecated
public class RatingAggregator extends IngestProcessor {

    Folder ratingFolder;

    @Autowired
    FolderService folderService;

    @Override
    public void process(AssetBuilder asset) {
        if (ratingFolder != null) {
            return;
        }
        try {
            ratingFolder = folderService.get(0, "★ Rating");
        } catch (EmptyResultDataAccessException e) {
            AssetFilter ratingFilter = new AssetFilter().setExistField("Xmp.Rating");
            FolderBuilder ratingBuilder = new FolderBuilder().setName("★ Rating")
                    .setSearch(new AssetSearch().setFilter(ratingFilter));
            ratingFolder = folderService.create(ratingBuilder);
            for (int i = 1; i <= 5; ++i) {
                AssetFieldTerms terms = new AssetFieldTerms().setField("Xmp.Rating")
                        .setTerm(Integer.toString(i));
                AssetFilter starFilter = new AssetFilter().setFieldTerm(terms);
                String starTitle = new String(new char[i]).replace("\0", "★");
                FolderBuilder starBuilder = new FolderBuilder().setName(starTitle)
                        .setSearch(new AssetSearch().setFilter(starFilter))
                        .setParentId(ratingFolder.getId());
                folderService.create(starBuilder);
            }
        }
    }
}
