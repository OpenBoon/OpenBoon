package com.zorroa.zmlp.client.domain.asset;

import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.app.AssetApp;
import com.zorroa.zmlp.client.domain.ZmlpClientException;

import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Batch of File Path and specification to be uploaded
 */
public class BatchUploadFileCrawler {

    /**
     * File Path
     */
    private String filePath = "";

    /**
     * Maximum depth to search files
     */
    private Integer maxDepth;

    /**
     * Maximum allowed File Size. 100MB is the default value
     */
    private Long maximumFileSize = 104857600L;

    /**
     * Maximum allowed Batch Size. 250MB is the default value
     */
    private Long maximumBatchSize = 262144000L; //250 MB

    /**
     * Files Types to be searched. e.g.: png, jpg, mov, json...
     */
    private List<String> fileTypes = new ArrayList();

    /**
     * Files Mime Types to be searched. e.g:
     */
    private List<String> mimeTypes = new ArrayList();

    private Predicate<Path> fileTypePredicate = path -> {
        String[] split = path.toString().split("\\.");
        String fileType = Stream.of(split).reduce((first, last) -> last).get();
        boolean contains = fileTypes.contains(fileType);
        return contains;
    };

    private Predicate<Path> fileMimeTypePredicate = path -> {
        boolean contains = mimeTypes.contains(getMimeTypeFromFile(path));
        return contains;
    };


    public BatchUploadFileCrawler(String filePath) {
        loadInstanceVars(filePath);
    }

    public BatchUploadFileCrawler(String filePath, Integer maxDepth) {
        this.maxDepth = maxDepth;
        loadInstanceVars(filePath);
    }

    private void loadInstanceVars(String path) {
        this.filePath = Optional.ofNullable(path).orElse("");

        Optional.ofNullable(System.getenv("ZMLP_MAXIMUM_FILE_SIZE"))
                .ifPresent(value -> this.maximumFileSize = Long.parseLong(value));
        Optional.ofNullable(System.getenv("ZMLP_MAXIMUM_BATCH_SIZE"))
                .ifPresent(value -> this.maximumBatchSize = Long.parseLong(value));
    }

    public BatchUploadFileCrawler addFileType(String fileType) {
        this.fileTypes.add(fileType);
        return this;
    }

    public BatchUploadFileCrawler addAllFileTypes(List<String> fileTypesList) {
        this.fileTypes.addAll(fileTypesList);
        return this;
    }

    public BatchUploadFileCrawler addAllFileTypes(String... fileTypesList) {
        this.fileTypes.addAll(Arrays.asList(fileTypesList));
        return this;
    }

    public BatchUploadFileCrawler addMimeType(String mimeType) {
        this.mimeTypes.add(mimeType);
        return this;
    }

    public BatchUploadFileCrawler addMimeTypeList(String... mimeTypeList) {
        this.mimeTypes.addAll(Arrays.asList(mimeTypeList));
        return this;
    }

    public BatchUploadFileCrawler addMimeTypeList(List<String> mimeTypeList) {
        this.mimeTypes.addAll(mimeTypeList);
        return this;
    }

    public List<Path> filter() throws ZmlpClientException {
        Stream<Path> walk = getWalk();

        List<Predicate> filters = new ArrayList();

        if (!fileTypes.isEmpty()) filters.add(fileTypePredicate);
        if (!mimeTypes.isEmpty()) filters.add(fileMimeTypePredicate);

        Predicate<Path> joinedFilter = filters.stream().reduce(w -> false, Predicate::or);

        List<Path> collect = walk.filter(joinedFilter).collect(Collectors.toList());

        checkFiles(collect);

        return collect;
    }

    public List<AssetSpec> asAssetSpecList() throws ZmlpClientException {
        return this.filter()
                .stream()
                .map(path -> new AssetSpec(path.toString())).collect(Collectors.toList());
    }

    private void checkFiles(List<Path> collect) throws ZmlpClientException {

        // Filter files with allowed length
        collect = collect.stream().filter(path -> path.toFile().length() < this.maximumFileSize).collect(Collectors.toList());

        //Compute the batch size
        Long batchSize = collect.stream().map(p -> p.toFile().length()).reduce((i1, i2) -> i1 + i2).orElse(0L);

        if (batchSize > this.maximumBatchSize)
            throw new ZmlpClientException("Maximum Batch Size Exceeded");

    }

    public List<BatchCreateAssetsResponse> upload(AssetApp app, AssetUploadCallback callback) {

        List<Path> filter = this.filter();
        List<BatchCreateAssetsResponse> result = new ArrayList();

        filter.stream().forEach(
                batch -> {
                    String uri = batch.toString();
                    BatchCreateAssetsResponse batchCreateAssetResponse = app.uploadFiles(new AssetSpec(uri));
                    result.add(batchCreateAssetResponse);

                    Optional.ofNullable(callback)
                            .ifPresent(c ->
                            c.run(
                                    new AssetUploadStatus()
                                            .setStatus(result)
                                            .setBatchNumber(result.size())
                                            .setFileCount(filter.size()))
                    );
                }
        );

        return result;
    }

    public List<BatchCreateAssetsResponse> upload(AssetApp app) {
        return this.upload(app, null);
    }


        private Stream<Path> getWalk() throws ZmlpClientException {
        try {
            if (maxDepth != null)
                return Files.walk(Paths.get(this.filePath), this.maxDepth);
            else
                return Files.walk(Paths.get(this.filePath));
        } catch (IOException ex) {
            throw new ZmlpClientException("Error on file crawling", ex);
        }
    }

    private String getMimeTypeFromFile(Path file) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(file.getFileName().toString());
    }

}


