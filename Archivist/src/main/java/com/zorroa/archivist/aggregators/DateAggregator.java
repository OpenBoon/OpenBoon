package com.zorroa.archivist.aggregators;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.Argument;
import com.zorroa.archivist.service.FolderService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.archivist.service.UserService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateAggregator extends Aggregator {

    @Autowired
    SearchService searchService;

    @Autowired
    FolderService folderService;

    @Autowired
    UserService userService;

    @Argument
    private String dateFolderName = "Date";

    @Argument
    private String dateField = "source.date";

    private Folder dateFolder;
    private Acl acl;

    public void setDateFolderName(String dateFolderName) {
        this.dateFolderName = dateFolderName;
    }

    public void setDateField(String dateField) {
        this.dateField = dateField;
    }

    @Override
    public void init(Ingest ingest) {
        acl = new Acl()
                .addEntry(userService.getPermission("internal::server"), Access.Write, Access.Read)
                .addEntry(userService.getPermission("group::user"), Access.Read);
    }

    @Override
    public void aggregate() {

        // Create an aggregation over all of the years
        AssetAggregateBuilder yearAggBuilder = createYearAggBuilder();
        SearchResponse yearResponse = searchService.aggregate(yearAggBuilder);

        Terms yearTerms = yearResponse.getAggregations().get("year");
        Collection<Terms.Bucket> yearBuckets = yearTerms.getBuckets();

        if (yearBuckets.size() > 0 && dateFolder == null) {
            // Default "Date" folder should exist, but create any other top-level folder
            try {
                dateFolder = folderService.get(0, dateFolderName);
            } catch (EmptyResultDataAccessException e) {
                dateFolder = folderService.create(new FolderBuilder().setAcl(acl).setName(dateFolderName));
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
                yearFolder = folderService.create(new FolderBuilder()
                        .setName(yearName)
                        .setParentId(dateFolder.getId())
                        .setAcl(acl));
            }

            AssetSearch yearSearch = new AssetSearch().setFilter(new AssetFilter()
                    .setScript(yearScript(yearName)));
            AssetAggregateBuilder monthAggBuilder = createMonthAggBuilder(yearSearch);
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

    private AssetScript yearScript(String year) {
        Map<String, Object> yearParams = new HashMap<>();
        yearParams.put("field", dateField);
        yearParams.put("interval", "year");
        if (year != null) {
            ArrayList<String> yearTerms = Lists.newArrayList();
            yearTerms.add(year);
            yearParams.put("terms", yearTerms);
        }
        return new AssetScript().setScript("archivistDate").setParams(yearParams);
    }

    private AssetAggregateBuilder createYearAggBuilder() {
        AssetFilter dateExistsFilter = new AssetFilter().setExistField(dateField);
        AssetSearch dateExistsSearch = new AssetSearch().setFilter(dateExistsFilter);
        return new AssetAggregateBuilder().setName("year").setScript(yearScript(null))
                .setSearch(dateExistsSearch).setSize(0);
    }

    private AssetAggregateBuilder createMonthAggBuilder(AssetSearch yearSearch) {
        Map<String, Object> monthParams = new HashMap<>();
        monthParams.put("field", dateField);
        monthParams.put("interval", "month");
        AssetScript monthScript = new AssetScript().setScript("archivistDate").setParams(monthParams);
        return new AssetAggregateBuilder().setName("month").setScript(monthScript)
                .setSearch(yearSearch).setSize(12);
    }

    public void createMonthFolder(int year, int month, Folder parentFolder) {
        String name = new DateFormatSymbols().getMonths()[month];
        try {
            folderService.get(parentFolder.getId(), name);
        } catch (EmptyResultDataAccessException e) {
            String min = Integer.toString(year) + "-" + Integer.toString(month + 1) + "-01";
            String max = month == 11 ? Integer.toString(year+1) + "-01-01" :
                    Integer.toString(year) + "-" + Integer.toString(month + 2) + "-01";
            AssetFieldRange fieldRange = new AssetFieldRange().setField(dateField)
                    .setMin(min).setMax(max);
            AssetFilter monthFilter = new AssetFilter().setFieldRange(fieldRange);
            AssetSearch monthSearch = new AssetSearch().setFilter(monthFilter);
            FolderBuilder monthBuilder = new FolderBuilder()
                    .setName(name)
                    .setSearch(monthSearch)
                    .setParentId(parentFolder.getId())
                    .setAcl(acl);
            folderService.create(monthBuilder);
        }
    }
}
