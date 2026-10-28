package com.bnp.kingmainandhiskingdom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the bundled React app from classpath:/static and falls back to index.html for
 * client-side routes (e.g. /persons), so a browser refresh on a React route keeps working.
 * Your own controllers (/api/**) always win over this handler; unknown /api/** and /actuator/** URLs give a 404.
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (!resourcePath.isEmpty() && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        boolean looksLikeFile = resourcePath.substring(resourcePath.lastIndexOf('/') + 1).contains(".");
                        // server URLs are never client-side routes: an unknown /api/... or /actuator/... is a real 404,
                        // not a (200) copy of index.html
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/") || looksLikeFile) {
                            return null;
                        }
                        Resource index = new ClassPathResource("static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
