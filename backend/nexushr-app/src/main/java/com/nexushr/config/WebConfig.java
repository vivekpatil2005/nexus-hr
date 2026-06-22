package com.nexushr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward 1-level paths (e.g., /employees, /dashboard)
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
        // Forward 2-level paths (e.g., /employees/123)
        registry.addViewController("/{path1:[^\\.]*}/{path2:[^\\.]*}")
                .setViewName("forward:/index.html");
        // Forward 3-level paths (e.g., /employees/123/reports)
        registry.addViewController("/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}
