package boonai.archivist.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.CompositePropertySource
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.PropertySource
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.FileReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays
import java.util.Properties

class ApplicationPropertiesException : RuntimeException {

    constructor() : super() {}

    constructor(msg: String) : super(msg) {}

    constructor(msg: String, t: Throwable) : super(msg, t) {}

    constructor(t: Throwable) : super(t) {}
}

interface ApplicationProperties {
    fun <T> get(key: String, type: Class<T>): T
    fun getString(key: String): String
    fun getString(key: String, def: String): String
    fun split(key: String, delimiter: String): Iterable<String>
    fun getPath(key: String): Path
    fun getPath(key: String, def: Path): Path
    fun getInt(key: String): Int
    fun getDouble(key: String): Double
    fun getBoolean(key: String): Boolean
    fun getInt(key: String, def: Int): Int
    fun getDouble(key: String, def: Double): Double
    fun getBoolean(key: String, def: Boolean): Boolean
    fun getMap(prefix: String): Map<String, Any>
    fun max(key: String, value: Int): Int
    fun max(key: String, value: Double): Double
    fun min(key: String, value: Int): Int
    fun min(key: String, value: Double): Double
    fun getProperties(prefix: String): Properties
    fun getProperties(prefix: String, includePrefix: Boolean): Properties
    fun getList(key: String): List<String>

    /**
     * Parses the value of a give attribute into a Map<String,String>. The format
     * is k=v,k=v,k=v
     */
    fun parseToMap(key: String): Map<String, String>
}

@Component
class SpringApplicationProperties : ApplicationProperties {

    @Autowired
    private lateinit var env: ConfigurableEnvironment

    override fun <T> get(key: String, type: Class<T>): T {
        return env.getProperty(key, type)
    }

    override fun getString(key: String): String {
        val result = env.getProperty(key)
            ?: throw ApplicationPropertiesException("Configuration key not found: '$key'")
        return result.trim { it <= ' ' }
    }

    override fun getString(key: String, def: String): String {
        val result = env.getProperty(key) ?: return def
        return result.trim { it <= ' ' }
    }

    override fun getList(key: String): List<String> {
        val value = getString(key)
        return if (value.startsWith("file:")) {
            try {
                val result = mutableListOf<String>()
                val path = value.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                BufferedReader(FileReader(path)).use { br ->
                    for (line in br.lines()) {
                        line.trim().takeIf {
                            it.isNotEmpty() && !it.startsWith("#")
                        }?.apply { result.add(this) }
                    }
                }
                result
            } catch (e: Exception) {
                throw ApplicationPropertiesException(
                    "Invalid file for '" + key + "', " + e.message, e
                )
            }
        } else {
            value.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    override fun split(key: String, delimiter: String): Iterable<String> {
        return key.split(delimiter).map { it.trim() }.filter { it.isEmpty() }
    }

    override fun getPath(key: String): Path {
        val result = env.getProperty(key)
            ?: throw ApplicationPropertiesException("Configuration key not found: '$key'")
        return Paths.get(result.trim()).toAbsolutePath().normalize()
    }

    override fun getPath(key: String, path: Path): Path {
        val result = env.getProperty(key) ?: return path
        return Paths.get(result.trim()).toAbsolutePath().normalize()
    }

    override fun getInt(key: String): Int {
        try {
            return Integer.valueOf(getString(key))
        } catch (t: Throwable) {
            throw ApplicationPropertiesException(t)
        }
    }

    override fun getDouble(key: String): Double {
        try {
            return java.lang.Double.valueOf(getString(key))
        } catch (t: Throwable) {
            throw ApplicationPropertiesException(t)
        }
    }

    override fun getBoolean(key: String): Boolean {
        try {
            return java.lang.Boolean.valueOf(getString(key))
        } catch (t: Throwable) {
            throw ApplicationPropertiesException(t)
        }
    }

    override fun getInt(key: String, def: Int): Int {
        return try {
            Integer.valueOf(getString(key))
        } catch (t: Throwable) {
            def
        }
    }

    override fun getDouble(key: String, def: Double): Double {
        return try {
            java.lang.Double.valueOf(getString(key))
        } catch (t: Throwable) {
            def
        }
    }

    override fun getBoolean(key: String, def: Boolean): Boolean {
        return try {
            java.lang.Boolean.valueOf(getString(key))
        } catch (t: Throwable) {
            def
        }
    }

    override fun getMap(prefix: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        for (propertySource in env.propertySources) {
            walkPropertySource(result, prefix, propertySource)
        }
        return result
    }

    override fun parseToMap(key: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val str = getString(key, "")
        for (item in str.split(',')) {
            if ("=" in item) {
                val (k, v) = item.split('=', limit = 2)
                result[k.trim()] = v.trim()
            }
        }
        return result
    }

    override fun getProperties(prefix: String): Properties {
        return getProperties(prefix, true)
    }

    override fun getProperties(prefix: String, includePrefix: Boolean): Properties {
        val result = Properties()
        val map = mutableMapOf<String, Any>()

        for (propertySource in env.propertySources) {
            walkPropertySource(map, prefix, propertySource)
        }

        if (includePrefix) {
            map.forEach({ k, v -> result.put(k, v.toString()) })
        } else {
            map.forEach({ k, v -> result.put(k.substring(prefix.length), v.toString()) })
        }
        return result
    }

    override fun max(key: String, value: Int): Int {
        return try {
            Math.max(Integer.valueOf(getString(key)), value)
        } catch (t: Throwable) {
            value
        }
    }

    override fun max(key: String, value: Double): Double {
        return try {
            Math.max(java.lang.Double.valueOf(getString(key)), value)
        } catch (t: Throwable) {
            value
        }
    }

    override fun min(key: String, value: Int): Int {
        return try {
            Math.min(Integer.valueOf(getString(key)), value)
        } catch (t: Throwable) {
            value
        }
    }

    override fun min(key: String, value: Double): Double {
        return try {
            Math.min(java.lang.Double.valueOf(getString(key)), value)
        } catch (t: Throwable) {
            value
        }
    }

    private fun walkPropertySource(result: MutableMap<String, Any>, prefix: String, propSource: PropertySource<*>) {

        if (propSource is CompositePropertySource) {
            propSource.propertySources.forEach { ps -> walkPropertySource(result, prefix, propSource) }
            return
        }

        if (propSource is EnumerablePropertySource<*>) {
            Arrays.asList(*propSource.propertyNames).stream()
                .filter { key -> key.startsWith(prefix) }.forEach { key -> result[key] = env.getProperty(key) }
        }
    }
}
