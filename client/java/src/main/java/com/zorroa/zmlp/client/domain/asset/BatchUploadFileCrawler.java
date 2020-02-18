package com.zorroa.zmlp.client.domain.asset;

import com.zorroa.zmlp.client.domain.ZmlpClientException;

import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BatchUploadFileCrawler {

    private String filePath = "";

    private Integer maxDepth;

    private Long maximumFileSize = 104857600L; //100 MB

    private Long maximumBatchSize = 262144000L; //250 MB

    private List<String> fileTypes = new ArrayList();

    private List<String> mimeTypes = new ArrayList();

    private Predicate<Path> fileTypePredicate = path -> {
        String[] split = path.toString().split("\\.");
        String fileType = Stream.of(split).reduce((first, last) -> last).get();
        return fileTypes.contains(fileType);
    };

    private Predicate<Path> fileMimeTypePredicate = path -> mimeTypes.contains(getMimeTypeFromFile(path));


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

    public List<Path> filter() throws IOException, ZmlpClientException {
        Stream<Path> walk = getWalk();

        List<Predicate> filters = new ArrayList();

        if (!fileTypes.isEmpty()) filters.add(fileTypePredicate);
        if (!mimeTypes.isEmpty()) filters.add(fileMimeTypePredicate);

        Predicate<Path> joinedFilter = filters.stream().reduce(w -> true, Predicate::and);

        List<Path> collect = walk.filter(joinedFilter).collect(Collectors.toList());

        checkFiles(collect);

        return collect;
    }


    public List<AssetSpec> asAssetSpecList() throws IOException {
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

    private Stream<Path> getWalk() throws IOException {
        if (maxDepth != null)
            return Files.walk(Paths.get(this.filePath), this.maxDepth);
        else
            return Files.walk(Paths.get(this.filePath));
    }


    private String getMimeTypeFromFile(Path file) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(file.getFileName().toString());
    }

}
