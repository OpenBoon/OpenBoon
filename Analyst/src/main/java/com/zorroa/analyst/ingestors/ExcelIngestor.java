package com.zorroa.analyst.ingestors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.Argument;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.common.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A simple excel ingestor based on Apache POI.  This particular processor handles .xlsx files.
 **
 */
public class ExcelIngestor extends IngestProcessor {

    private Logger logger = LoggerFactory.getLogger(ExcelIngestor.class);

    @Argument
    public String fileName = null;

    @Argument
    public List<RowMapping> rowMappings = null;

    /**
     * We match a field in the asset to a row in the spreadsheet using a RowMapping.
     * Mappings are applied in order, so you can do an initial match and then use
     * the resulting data from the first match in the second mapping so you can
     * chain together data from separate sheets in the file.
     *
     * Before mapping, string data in the asset field and spreadsheet cells can
     * be filtered using a few methods:
     *
     *   strings: toLower, spaceToUnderscore, dashToUnderscore, ...
     *
     * The match function can be set to:
     *
     *   strings: exact, containsField, fuzzyField
     *   geopoint: nearest (assumes matchColumn is latitude and matchColumn+1 is longitude)
     *
     * After we find the matching row, the columns of that row are added to the
     * asset schemaName using the field names specified by the title row of the column.
     * Output and keyword columns can be specified either using the column titles
     * or the letter name of the column. We first compare titles, and then letters.
     *
     * Example JSON argument string:

        {
            "fileName" : "foo.xlsx",
            "rowMappings" : [ {
                "assetField" : "hampton.wellName",
                "matchFunction" : "contains"
                "matchFilters" : [ "toLower", "spaceToUnderscore", "dashToUnderscore" ],
                "sheetName" : "sheet1",
                "titleRow" : 2
                "matchColumn" : "C",
                "outputSchema" : "hampton",
                "outputColumns" : [ "B", "C", "D", "E", "G", "K" ],
                "keywordColumns" : [ "D", "E", "G", "K" ],
            } ]
        }
    */

    public enum MatchFunction {
        exactString, containsField, fuzzyField, nearestLatLong, nearestGDALatLong, nearestGDAZone,
    }

    public enum MatchFilter {
        toLower, spaceToUnderscore, dashToUnderscore, removeNonAlphaNum, removeNum, removeWhitespace
    }

    public static class RowMapping {
        public String assetField;                  // required
        public MatchFunction matchFunction = MatchFunction.containsField;
        public List<MatchFilter> matchFilters;     // null allowed
        public String sheetName;                   // null == first sheet
        public int titleRow = -1;
        public String matchColumn;                 // null == first column
        public String outputSchema = "Excel";
        public List<String> outputColumns;         // null == all
        public List<String> keywordColumns;        // null == all
    }

    private XSSFWorkbook workbook;
    private List<RowMapper> rowMappers;

    @Override
    public void init() {
        super.init();
        String path = applicationProperties.getString("analyst.path.models") + "/excel/" + fileName;
        try {
            FileInputStream excelFile = new FileInputStream(path);
            workbook = new XSSFWorkbook(excelFile);

            // Convert the mappings into mappers using the factory and initialize
            RowMapperFactory rowMapperFactory = new RowMapperFactory();
            rowMappers = Lists.newArrayListWithCapacity(rowMappings.size());
            for (RowMapping rowMapping : rowMappings) {
                RowMapper rowMapper = rowMapperFactory.getRowMapper(rowMapping.matchFunction);
                if (rowMapper != null) {
                    rowMapper.init(rowMapping, workbook);
                    rowMappers.add(rowMapper);
                }
            }
        } catch (FileNotFoundException e) {
            logger.warn("Failed to find Excel file {}", path, e);
            throw new IngestException(e.getMessage(), e);
        } catch (IOException e) {
            logger.warn("Failed to read Excel workbook {}", fileName, e);
            throw new IngestException(e.getMessage(), e);
        }
    }

    /**
     * Returns true if we've already created all schema for this mapping.
     *
     * @param asset Current asset to check
     * @return true if all schema have been mapped for this asset
     */
    private boolean isMapped(AssetBuilder asset) {
        for (RowMapping rowMapping : rowMappings) {
            if (!asset.contains(rowMapping.outputSchema)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void process(AssetBuilder asset) {
        if (workbook == null) {
            return;
        }

        if (isMapped(asset) && !asset.isChanged()) {
            logger.debug("Excel rowMappings already exist for {}", asset);
            return;
        }

        for (RowMapper rowMapper : rowMappers) {
            rowMapper.process(asset);
        }
    }

    /**
     * RowMappers implement a specific MatchFunction, performing that match
     * on the current asset, outputting the named data into the asset.
     */
    private abstract static class RowMapper {
        protected XSSFSheet sheet;
        private Map<String, Integer> titleToColumnIndexMap;
        RowMapping rowMapping;

        void init(RowMapping rowMapping, XSSFWorkbook workbook) {
            this.rowMapping = rowMapping;
            sheet = rowMapping.sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(rowMapping.sheetName);
            titleToColumnIndexMap = getTitleToColumnIndexMap(sheet, titleRow());
        }

        protected int titleRow() {
            return rowMapping.titleRow < 1 ? 0 : rowMapping.titleRow - 1;
        }

        abstract void process(AssetBuilder asset);

        protected String filter(String str, List<MatchFilter> filters) {
            for (MatchFilter filter: filters) {
                switch (filter) {
                    case toLower:
                        str = str.toLowerCase();
                        break;
                    case spaceToUnderscore:
                        str = str.replace(' ', '_');
                        break;
                    case dashToUnderscore:
                        str = str.replace('-', '_');
                        break;
                    case removeNonAlphaNum:
                        str = str.replaceAll("[^A-Za-z0-9]", "");
                        break;
                    case removeNum:
                        str = str.replaceAll("[0-9]", "");
                        break;
                    case removeWhitespace:
                        str = str.replaceAll("\\s+","");
                        break;
                }
            }
            return str;
        }

        /**
         * Return the cell for the specified column title or name string.
         *
         * @param row Row containing cell
         * @param name Name of column, e.g. "A", "B", ... or column title
         * @return Cell in row at column name
         */
        protected Cell getCell(Row row, String name) {
            int index = titleToColumnIndexMap.containsKey(name) ? titleToColumnIndexMap.get(name) : CellReference.convertColStringToIndex(name);
            return row.getCell(index);
        }

        private Map<String, Integer> getTitleToColumnIndexMap(XSSFSheet sheet, int titleRow) {
            Row row = sheet.getRow(titleRow);
            DataFormatter dataFormatter = new DataFormatter();
            Map<String, Integer> titleToColumnIndexMap = Maps.newHashMap();
            for (int c = row.getFirstCellNum(); c <= row.getLastCellNum(); ++c) {
                Cell cell = row.getCell(c);
                if (cell != null) {
                    titleToColumnIndexMap.put(dataFormatter.formatCellValue(cell), cell.getColumnIndex());
                }
            }
            return titleToColumnIndexMap;
        }
    }

    public static class ContainsRowMapper extends RowMapper {
        List<String> filteredCells;

        @Override
        public void init(RowMapping rowMapping, XSSFWorkbook workbook) {
            super.init(rowMapping, workbook);
            DataFormatter dataFormatter = new DataFormatter();
            filteredCells = Lists.newArrayListWithCapacity(sheet.getLastRowNum() - sheet.getFirstRowNum());
            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); ++r) {
                if (r <= titleRow()) {
                    continue;
                }
                Row row = sheet.getRow(r);
                Cell cell = getCell(row, rowMapping.matchColumn);
                String filteredValue = filter(dataFormatter.formatCellValue(cell), rowMapping.matchFilters);
                filteredCells.add(filteredValue);
            }
        }

        @Override
        public void process(AssetBuilder asset) {
            String field = filter(asset.getAttr(rowMapping.assetField), rowMapping.matchFilters);
            for (int r = 0; r < filteredCells.size(); ++r) {
                if (field.contains(filteredCells.get(r))) {
                    addAttributesAndKeywords(r, asset);
                }
            }
        }

        protected void addAttributesAndKeywords(int r, AssetBuilder asset) {
            Row row = sheet.getRow(r + titleRow() + 1);
            Calendar calendar = Calendar.getInstance();
            calendar.set(1940, 1, 1);
            final Date firstValidDate = calendar.getTime();
            final Date currentDate = new Date();
            DataFormatter dataFormatter = new DataFormatter();
            for (String column: rowMapping.outputColumns) {
                Cell cell = getCell(row, column);
                switch (cell.getCellType()) {
                    case Cell.CELL_TYPE_STRING:
                        asset.setAttr(rowMapping.outputSchema, column, cell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        Object value = null;
                        Date date = cell.getDateCellValue();
                        if (date.before(currentDate) && date.after(firstValidDate)) {
                            value = date;
                        } else {
                            value = cell.getNumericCellValue();
                        }
                        asset.setAttr(rowMapping.outputSchema, column, value);
                        break;
                }
            }
            for (String column: rowMapping.keywordColumns) {
                Cell cell = getCell(row, column);
                asset.addKeywords(1, true /*suggest*/, dataFormatter.formatCellValue(cell));
            }
        }
    }

    public static class FuzzyRowMapper extends  ContainsRowMapper {
        @Override
        public void process(AssetBuilder asset) {
            String field = filter(asset.getAttr(rowMapping.assetField), rowMapping.matchFilters);
            int minD = 4;
            int bestRow = -1;
            for (int r = 0; r < filteredCells.size(); ++r) {
                int d = StringUtils.getLevenshteinDistance(field, filteredCells.get(r));
                if (d < minD) {
                    minD = d;
                    bestRow = r;
                    if (d == 0) {
                        break;
                    }
                }
            }
            if (bestRow >= 0) {
                addAttributesAndKeywords(bestRow, asset);
            }
        }
    }

    private String getStringValue(Cell cell) {
        DataFormatter dataFormatter = new DataFormatter();
        return dataFormatter.formatCellValue(cell);
    }

    public static class RowMapperFactory {
        public RowMapper getRowMapper(MatchFunction matchFunction) {
            switch (matchFunction) {
                case exactString:
                    break;
                case containsField:
                    return new ContainsRowMapper();
                case fuzzyField:
                    return new FuzzyRowMapper();
                case nearestLatLong:
                    break;
                case nearestGDALatLong:
                    break;
                case nearestGDAZone:
                    break;
            }
            return null;
        }
    }

}
