package com.zorroa.archivist.domain

import java.util.*

class FileUploadSpec(
        val name : String? = null,
        val processors: List<ProcessorRef>?=null
)