package com.zorroa.archivist.config

import java.net.URI

object SupportedTypes {

    val SUPPORTED_URI_SCHEMES = setOf("gs", "http", "https", "asset")

    fun isSupportedUri(uri: String) : Boolean {
        return URI.create(uri).scheme in SUPPORTED_URI_SCHEMES
    }
}