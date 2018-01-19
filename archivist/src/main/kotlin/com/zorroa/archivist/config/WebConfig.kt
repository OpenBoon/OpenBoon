package com.zorroa.archivist.config

import com.google.common.collect.ImmutableSet
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.sdk.util.FileUtils
import org.h2.server.web.WebServlet
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.util.StringUtils
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import org.springframework.web.servlet.resource.ResourceResolver
import org.springframework.web.servlet.resource.ResourceResolverChain
import java.io.IOException
import javax.servlet.http.HttpServletRequest

@Configuration
class SinglePageAppConfig : WebMvcConfigurerAdapter() {

    @Autowired
    internal lateinit var properties: ApplicationProperties

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val curator = properties.getPath("archivist.path.curator")
        registry.addResourceHandler("/**")
                .addResourceLocations(curator.toUri().toString())
                .resourceChain(false)
                .addResolver(PushStateResourceResolver())
        super.addResourceHandlers(registry)
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/").setViewName("forward:/index.html")
        // put in front of the thymeleaf resolver
        registry.setOrder(-1)
    }

    private inner class PushStateResourceResolver : ResourceResolver {

        /**
         * The react index.html file
         */
        private val index: Resource

        /**
         * All the file types served by react...not sure we need this.
         */
        private val handledExtensions = setOf("html", "js", "json", "csv", "css", "png", "svg", "eot", "ttf", "woff", "woff2", "appcache", "jpg", "jpeg", "gif", "ico", "map")

        /**
         * These are basically endpoints on the server we can't use in react.
         */
        private val ignoredPaths = setOf("api", "admin", "health", "login", "logout", "docs")

        init {
            index = FileSystemResource(properties.getString("archivist.path.home") + "/web/curator/index.html")
        }

        override fun resolveResource(request: HttpServletRequest, requestPath: String, locations: List<Resource>, chain: ResourceResolverChain): Resource? {
            return resolve(requestPath, locations)
        }

        override fun resolveUrlPath(resourcePath: String, locations: List<Resource>, chain: ResourceResolverChain): String? {
            val resolvedResource = resolve(resourcePath, locations) ?: return null
            return try {
                resolvedResource.url.toString()
            } catch (e: IOException) {
                resolvedResource.filename
            }
        }

        private fun resolve(requestPath: String, locations: List<Resource>): Resource? {
            if (isIgnored(requestPath)) {
                return null
            }
            return if (isHandled(requestPath)) {
                locations.stream()
                        .map<Resource> { loc -> createRelative(loc, requestPath) }
                        .filter { resource -> resource != null && resource.exists() }
                        .findFirst()
                        .orElse(null)
            } else index
        }

        private fun createRelative(resource: Resource, relativePath: String): Resource? {
            return try {
                resource.createRelative(relativePath)
            } catch (e: IOException) {
                null
            }

        }

        private fun isIgnored(path: String): Boolean {
            return ignoredPaths.contains(path)
        }

        private fun isHandled(path: String): Boolean {
            val extension = StringUtils.getFilenameExtension(path)
            return handledExtensions.contains(extension)
        }
    }


    companion object {

        private val logger = LoggerFactory.getLogger(StaticResourceConfiguration::class.java)
    }

}

@Configuration
class StaticResourceConfiguration : WebMvcConfigurerAdapter() {

    @Autowired
    internal lateinit var properties: ApplicationProperties

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val path = java.io.File(FileUtils.normalize(
                properties.getString("archivist.path.docs") + "/"))
        registry.addResourceHandler("/docs/**").addResourceLocations(
                path.toURI().toString() + "/").setCachePeriod(0)

        /**
         * TODO: for the admin pages, going to move this stuff so we can expose 1 path.
         */
        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/public/assets/")
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/public/css/")
        registry.addResourceHandler("/admin/es/**").addResourceLocations("classpath:/public/es/")
    }
}
