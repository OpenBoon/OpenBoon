package com.zorroa.analyst.repository;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chambers on 2/9/16.
 */
@Repository
public class AssetDaoImpl implements AssetDao {

    private final NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    @Autowired
    Client client;

    @Value("${analyst.index.alias}")
    private String alias;

    @Override
    public AnalyzeResult bulkUpsert(List<AssetBuilder> builders) {
        AnalyzeResult result = new AnalyzeResult();
        List<AssetBuilder> retries = Lists.newArrayList();

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (AssetBuilder builder : builders) {
            String id = uuidGenerator.generate(builder.getAbsolutePath()).toString();
            bulkRequest.add(prepareUpsert(builder, id));
        }

        BulkResponse bulk = bulkRequest.get();

        int index = 0;
        for (BulkItemResponse response : bulk) {
            UpdateResponse update = response.getResponse();
            if (response.isFailed()) {
                String message = response.getFailure().getMessage();
                AssetBuilder asset = builders.get(index);
                if (removeBrokenField(asset, message)) {
                    result.errorsRecoverable++;
                    retries.add(builders.get(index));
                } else {
                    result.errors.add(new StringBuilder(1024).append(
                            message).append(",").append(asset.getAbsolutePath()).toString());
                    result.errorsNotRecoverable++;
                }
            } else if (update.isCreated()) {
                result.created++;
            } else {
                result.updated++;
            }
            index++;
        }

        /*
         * TODO: limit number of retries to reasonable number.
         */
        if (!retries.isEmpty()) {
            result.retries++;
            result.add(bulkUpsert(retries));
        }
        return result;
    }

    private static final Pattern[] RECOVERABLE_BULK_ERRORS = new Pattern[] {
            Pattern.compile("^MapperParsingException\\[failed to parse \\[(.*?)\\]\\];"),
            Pattern.compile("\"term in field=\"(.*?)\"\"")
    };

    private boolean removeBrokenField(AssetBuilder asset, String error) {
        for (Pattern pattern: RECOVERABLE_BULK_ERRORS) {
            Matcher matcher = pattern.matcher(error);
            if (matcher.find()) {
                return asset.removeAttr(matcher.group(1));
            }
        }
        return false;
    }

    private UpdateRequestBuilder prepareUpsert(AssetBuilder builder, String id) {
        /**
         * Close the AssetBuilder which has an open file handle to the asset itself.
         */
        builder.close();
        builder.buildKeywords();

        byte[] doc = Json.serialize(builder.getDocument());
        return client.prepareUpdate(alias, "asset", id)
                .setDoc(doc)
                .setId(id)
                .setUpsert(doc);
    }
}
