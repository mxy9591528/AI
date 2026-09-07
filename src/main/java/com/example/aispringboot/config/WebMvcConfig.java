package com.example.aispringboot.config;

import com.example.aispringboot.service.system.FileService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig
implements WebMvcConfigurer {
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = FileService.UPLOAD_ROOT.resolve("").toUri().toString();
        registry.addResourceHandler("/upload/**").addResourceLocations(location);
    }
}

