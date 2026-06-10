package com.nekocafe.config;

import com.nekocafe.cat.service.CatPhotoStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CatPhotoStorageService catPhotoStorageService;

    public WebMvcConfig(CatPhotoStorageService catPhotoStorageService) {
        this.catPhotoStorageService = catPhotoStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(catPhotoStorageService.urlPattern())
            .addResourceLocations(catPhotoStorageService.uploadDir().toUri().toString());
    }
}
