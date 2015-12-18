package com.zorroa.archivist.processors;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 12/16/15.
 */
public class SimpleExcelIngestorTests extends ArchivistApplicationTests {


    @Test
    public void testProcessSingleFilter() throws Exception {

        SimpleExcelIngestor.Filter filter = new SimpleExcelIngestor.Filter();
        filter.setColumn("C");
        filter.setRelation(SimpleExcelIngestor.Relation.is);
        filter.setValue(7);

        SimpleExcelIngestor.Mapping mapping = new SimpleExcelIngestor.Mapping();
        mapping.setFilters(Lists.newArrayList(filter));
        mapping.addField("B", new SimpleExcelIngestor.Field("foo.B", SimpleExcelIngestor.Type.string));
        mapping.addField("C", new SimpleExcelIngestor.Field("foo.C", SimpleExcelIngestor.Type.integer));
        mapping.addField("D", new SimpleExcelIngestor.Field("foo.D", SimpleExcelIngestor.Type.string));
        mapping.addField("E", new SimpleExcelIngestor.Field("foo.E", SimpleExcelIngestor.Type.decimal));

        SimpleExcelIngestor ingestor = new SimpleExcelIngestor();
        ingestor.setArg("file", "src/test/resources/excelFile.xlsx");
        ingestor.setArg("mappings", Lists.newArrayList(mapping));

        AssetBuilder builder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");

        ingestor.init();
        ingestor.process(builder);

        assertEquals("G", builder.getAttr("foo", "B"));
        assertEquals(7, (int) builder.getAttr("foo", "C"));
        assertEquals("Seven", builder.getAttr("foo", "D"));
        assertEquals(2.3, builder.getAttr("foo", "E"), 0.1);
    }

    @Test
    public void testProcessSingleFilterExpression() throws Exception {

        SimpleExcelIngestor.Filter filter = new SimpleExcelIngestor.Filter();
        filter.setColumn("G");
        filter.setRelation(SimpleExcelIngestor.Relation.is);
        filter.setValue("${source.filename}");

        SimpleExcelIngestor.Mapping mapping = new SimpleExcelIngestor.Mapping();
        mapping.setFilters(Lists.newArrayList(filter));
        mapping.addField("B", new SimpleExcelIngestor.Field("foo.B", SimpleExcelIngestor.Type.string));
        mapping.addField("C", new SimpleExcelIngestor.Field("foo.C", SimpleExcelIngestor.Type.integer));
        mapping.addField("D", new SimpleExcelIngestor.Field("foo.D", SimpleExcelIngestor.Type.string));
        mapping.addField("E", new SimpleExcelIngestor.Field("foo.E", SimpleExcelIngestor.Type.decimal));

        SimpleExcelIngestor ingestor = new SimpleExcelIngestor();
        ingestor.setArg("file", "src/test/resources/excelFile.xlsx");
        ingestor.setArg("mappings", Lists.newArrayList(mapping));

        AssetBuilder builder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");

        ingestor.init();
        ingestor.process(builder);

        assertEquals("A", builder.getAttr("foo", "B"));
        assertEquals(1, (int) builder.getAttr("foo", "C"));
        assertEquals("One", builder.getAttr("foo", "D"));
        assertEquals(1.2, builder.getAttr("foo", "E"), 0.1);
    }
}
