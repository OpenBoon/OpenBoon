package com.zorroa.archivist.sdk.schema;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.Ingest;

import java.util.List;

/**
 * The ingest schema contains all the information related to the ingest that brought in the asset.
 */
public class ImportSchema extends SetSchema<ImportSchema.IngestProperties> {

    public IngestProperties addIngest(Ingest ingest) {
        IngestProperties result = new IngestProperties(ingest.getId());
        add(result);
        return result;
    }

    public IngestProperties addIngest(AnalyzeRequest req) {
        IngestProperties result = new IngestProperties(req.getIngestId());
        add(result);
        return result;
    }

    public static class IngestProperties {
        private int id;
        private long timestamp;
        private List<String> ingestProcessors;

        public IngestProperties() { }

        public IngestProperties(int id) {
            this.id = id;
            this.timestamp  =System.currentTimeMillis();
            this.ingestProcessors = Lists.newArrayList();
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public long getTimestamp() {
            return timestamp;
        }


        public IngestProperties setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public List<String> getIngestProcessors() {
            return ingestProcessors;
        }

        public IngestProperties setIngestProcessors(List<String> ingestProcessors) {
            this.ingestProcessors = ingestProcessors;
            return this;
        }

        public void addToIngestProcessors(String className) {
            ingestProcessors.add(className);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IngestProperties)) return false;
            IngestProperties that = (IngestProperties) o;
            return getId() == that.getId();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getId());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("timestamp", timestamp)
                    .add("ingestProcessors", ingestProcessors)
                    .toString();
        }
    }
}
