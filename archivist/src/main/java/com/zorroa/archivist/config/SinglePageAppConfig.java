package com.zorroa.archivist.config;

import com.google.common.collect.ImmutableSet;
import com.zorroa.common.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 11/1/16.
 */
@Configuration
public class SinglePageAppConfig extends WebMvcConfigurerAdapter {

    @Autowired
    ApplicationProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path curator = properties.getPath("archivist.path.curator");
        registry.addResourceHandler("/**")
                .addResourceLocations(curator.toUri().toString())
                .resourceChain(false)
                .addResolver(new PushStateResourceResolver());
        super.addResourceHandlers(registry);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        // put in front of the thymeleaf resolver
        registry.setOrder(-1);
    }

    private class PushStateResourceResolver implements ResourceResolver {

        /**
         * The react index.html file
         */
        private final Resource index;

        /**
         * All the file types served by react...not sure we need this.
         */
        private final Set<String> handledExtensions = ImmutableSet.of("html", "js", "json", "csv", "css", "png", "svg", "eot", "ttf", "woff", "woff2", "appcache", "jpg", "jpeg", "gif", "ico","map");

        /**
         *These are basically endpoints on the server we can't use in react.
         */
        private final Set<String> ignoredPaths = ImmutableSet.of("api", "gui", "health", "login", "logout");

        public PushStateResourceResolver() {
            index = new FileSystemResource(properties.getString("archivist.path.home") + "/web/curator/index.html");
        }

        @Override
        public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
            return resolve(requestPath, locations);
        }

        @Override
        public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
            Resource resolvedResource = resolve(resourcePath, locations);
            if (resolvedResource == null) {
                return null;
            }
            try {
                return resolvedResource.getURL().toString();
            } catch (IOException e) {
                return resolvedResource.getFilename();
            }
        }

        private Resource resolve(String requestPath, List<? extends Resource> locations) {
            if (isIgnored(requestPath)) {
                return null;
            }
            if (isHandled(requestPath)) {
                return locations.stream()
                        .map(loc -> createRelative(loc, requestPath))
                        .filter(resource -> resource != null && resource.exists())
                        .findFirst()
                        .orElse(null);
            }
            return index;
        }

        private Resource createRelative(Resource resource, String relativePath) {
            try {
                return resource.createRelative(relativePath);
            } catch (IOException e) {
                return null;
            }
        }

        private boolean isIgnored(String path) {
            return ignoredPaths.contains(path);
        }

        private boolean isHandled(String path) {
            String extension = StringUtils.getFilenameExtension(path);
            return handledExtensions.contains(extension);
        }
    }
}
