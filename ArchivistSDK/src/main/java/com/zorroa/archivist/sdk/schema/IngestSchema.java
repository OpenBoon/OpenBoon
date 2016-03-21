package com.zorroa.archivist.sdk.schema;

import com.google.common.base.Objects;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.Ingest;

/**
 * The ingest schema contains all the information related to the ingest that brought in the asset.
 */
public class IngestSchema extends SetSchema<IngestSchema.IngestProperties> {

    public void addIngest(Ingest ingest) {
        add(new IngestProperties(ingest));
    }

    public void addIngest(AnalyzeRequest req) {
        add(new IngestProperties(req));
    }

    public static class IngestProperties {
        private int id;
        private int pipeline;
        private long timestamp;

        public IngestProperties(AnalyzeRequest req) {
            this.id = req.getIngestId();
            this.pipeline = req.getIngestPipelineId();
            this.timestamp  =System.currentTimeMillis();
        }

        public IngestProperties(Ingest ingest) {
            this.id = ingest.getId();
            this.pipeline = ingest.getPipelineId();
            this.timestamp = System.currentTimeMillis();
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getPipeline() {
            return pipeline;
        }

        public void setPipeline(int pipeline) {
            this.pipeline = pipeline;
        }

        public long getTimestamp() {
            return timestamp;
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
    }
}
