package com.zorroa.archivist.domain

import com.zorroa.zmlp.util.Json
import org.apache.commons.codec.digest.DigestUtils


class ProcessorMetric (
    val processor : String,
    val module: String,
    val checksum: Long,
    val error: String?
)

class Metrics (
    var signature: String?,
    var pipeline: List<ProcessorMetric>?
)