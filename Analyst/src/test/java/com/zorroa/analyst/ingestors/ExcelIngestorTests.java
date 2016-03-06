package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by wex on 3/3/16.
 */
public class ExcelIngestorTests extends AbstractTest {

    @Test
    public void testContainsProcess() {
        testTwoMappings(ExcelIngestor.MatchFunction.containsField, "Arlo 1");
    }

    @Test
    public void testFuzzyProcess() {
        testTwoMappings(ExcelIngestor.MatchFunction.fuzzyField, "4rlo i");
    }

    void testTwoMappings(ExcelIngestor.MatchFunction matchFunction, String fieldValue) {
        ExcelIngestor excelIngestor = new ExcelIngestor();
        excelIngestor.fileName = "Data_Listing-Caswell.xlsx";
        ExcelIngestor.RowMapping rowMapping1 = new ExcelIngestor.RowMapping();
        rowMapping1.assetField = "petrol.wellName";
        rowMapping1.sheetName = "WCR - Data Listing";
        rowMapping1.matchFunction = matchFunction;
        rowMapping1.matchFilters = Arrays.asList(ExcelIngestor.MatchFilter.toLower);
        rowMapping1.titleRow = 5;
        rowMapping1.matchColumn = "WELL NAME";
        rowMapping1.outputColumns = Arrays.asList("GA_BASIN", "GA_SUB_BASIN", "OPERATOR", "SPUD_DATE", "RIG_RELEASE_DATE");
        rowMapping1.keywordColumns = rowMapping1.outputColumns;
        ExcelIngestor.RowMapping rowMapping2 = new ExcelIngestor.RowMapping();
        rowMapping2.assetField = "petrol.wellName";
        rowMapping2.sheetName = "Core & Cuttings at GA";
        rowMapping2.matchFunction = matchFunction;
        rowMapping2.matchFilters = Arrays.asList(ExcelIngestor.MatchFilter.toLower);
        rowMapping2.titleRow = 4;
        rowMapping2.outputSchema = "Cuttings";
        rowMapping2.matchColumn = "WELLNAME";
        rowMapping2.outputColumns = ImmutableList.of("OPERATOR", "TESTTYPE");
        rowMapping2.outputColumns = Arrays.asList("OPERATOR", "TESTTYPE");
        rowMapping2.keywordColumns = rowMapping1.outputColumns;
        excelIngestor.rowMappings = Arrays.asList(rowMapping1, rowMapping2);
        initIngestProcessor(excelIngestor);
        File file = getResourceFile("/images/toucan.jpg");
        AssetBuilder asset = new AssetBuilder(file.getAbsolutePath());
        asset.getSource().setType("image/" + asset.getExtension());
        asset.setAttr("petrol", "wellName", fieldValue);
        excelIngestor.process(asset);
        assertEquals("Apache Northwest Pty Ltd", asset.getAttr("Excel", "OPERATOR"));
        assertEquals("Apache Energy Limited", asset.getAttr("Cuttings", "OPERATOR"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 9, 11);
        assertTrue(DateUtils.isSameDay(asset.getAttr("Excel", "SPUD_DATE"), calendar.getTime()));
        calendar.set(2009, 10, 11);
        assertTrue(DateUtils.isSameDay(asset.getAttr("Excel", "RIG_RELEASE_DATE"), calendar.getTime()));
    }

}
