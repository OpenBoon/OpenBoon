package com.zorroa.archivist.processors;

import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.service.SearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateAggregator extends IngestProcessor {

    private Folder dateFolder;

    @Autowired
    SearchService searchService;

    @Autowired
    FolderService folderService;

    @Override
    public void process(AssetBuilder asset) {
        // Create an aggregation over all of the years
        AssetAggregateBuilder yearAggBuilder = createYearAggBuilder();
        SearchResponse yearResponse = searchService.aggregate(yearAggBuilder);

        // Create the top level date folder, if needed
        Terms yearTerms = yearResponse.getAggregations().get("year");
        Collection<Terms.Bucket> yearBuckets = yearTerms.getBuckets();
        if (yearBuckets.size() > 0 && dateFolder == null) {
            try {
                dateFolder = folderService.get(0, "Date");
            } catch (EmptyResultDataAccessException e) {
                dateFolder = folderService.create(new FolderBuilder().setName("Date"));
            }
        }

        // Create each year folder and aggregate over each month in the year
        // FIXME: We should use a single multi-level agg for year/month
        for (Terms.Bucket yearBucket: yearBuckets) {
            int year = yearBucket.getKeyAsNumber().intValue();
            String yearName = Integer.toString(year);
            Folder yearFolder = null;
            try {
                yearFolder = folderService.get(dateFolder.getId(), yearName);
            } catch (EmptyResultDataAccessException e) {
                yearFolder = folderService.create(new FolderBuilder().setName(yearBucket.getKey())
                        .setParentId(dateFolder.getId()));
            }

            AssetAggregateBuilder monthAggBuilder = createMonthAggBuilder(yearFolder.getSearch());
            SearchResponse monthResponse = searchService.aggregate(monthAggBuilder);

            // Create each month folder for this year
            Terms monthTerms = monthResponse.getAggregations().get("month");
            Collection<Terms.Bucket> monthBuckets = monthTerms.getBuckets();
            for (Terms.Bucket monthBucket: monthBuckets) {
                try {
                    Date date = new SimpleDateFormat("MMMM").parse(monthBucket.getKey());
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int month = cal.get(Calendar.MONTH);
                    createMonthFolder(year, month, yearFolder);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private AssetAggregateBuilder createYearAggBuilder() {
        Map<String, Object> yearParams = new HashMap<>();
        yearParams.put("field", "source.date");
        yearParams.put("interval", "year");
        AssetFilter dateExistsFilter = new AssetFilter().setExistField("source.date");
        AssetSearch dateExistsSearch = new AssetSearch().setFilter(dateExistsFilter);
        AssetScript yearScript = new AssetScript().setScript("archivistDate").setParams(yearParams);
        return new AssetAggregateBuilder().setName("year").setScript(yearScript)
                .setSearch(dateExistsSearch).setSize(1000);
    }

    private AssetAggregateBuilder createMonthAggBuilder(AssetSearch yearSearch) {
        Map<String, Object> monthParams = new HashMap<>();
        monthParams.put("field", "source.date");
        monthParams.put("interval", "month");
        AssetScript monthScript = new AssetScript().setScript("archivistDate").setParams(monthParams);
        return new AssetAggregateBuilder().setName("month").setScript(monthScript)
                .setSearch(yearSearch).setSize(12);
    }

    public void createMonthFolder(int year, int month, Folder parentFolder) {
        String name = Integer.toString(year) + " " + new DateFormatSymbols().getMonths()[month];
        try {
            folderService.get(parentFolder.getId(), name);
        } catch (EmptyResultDataAccessException e) {
            String min = Integer.toString(year) + "-" + Integer.toString(month + 1) + "-01";
            String max = month == 11 ? Integer.toString(year+1) + "-01-01" :
                    Integer.toString(year) + "-" + Integer.toString(month + 2) + "-01";
            AssetFieldRange fieldRange = new AssetFieldRange().setField("source.date")
                    .setMin(min).setMax(max);
            AssetFilter monthFilter = new AssetFilter().setFieldRange(fieldRange);
            AssetSearch monthSearch = new AssetSearch().setFilter(monthFilter);
            FolderBuilder monthBuilder = new FolderBuilder().setName(name).setSearch(monthSearch)
                    .setParentId(parentFolder.getId());
            folderService.create(monthBuilder);
        }
    }

}
