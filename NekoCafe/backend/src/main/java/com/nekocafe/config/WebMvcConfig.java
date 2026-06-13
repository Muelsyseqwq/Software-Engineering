package com.nekocafe.config;

import com.nekocafe.cat.service.CatPhotoStorageService;
import com.nekocafe.store.service.StorePhotoStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CatPhotoStorageService catPhotoStorageService;
    private final StorePhotoStorageService storePhotoStorageService;

    public WebMvcConfig(CatPhotoStorageService catPhotoStorageService, StorePhotoStorageService storePhotoStorageService) {
        this.catPhotoStorageService = catPhotoStorageService;
        this.storePhotoStorageService = storePhotoStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(catPhotoStorageService.urlPattern())
            .addResourceLocations(catPhotoStorageService.uploadDir().toUri().toString());
        registry.addResourceHandler(storePhotoStorageService.urlPattern())
            .addResourceLocations(storePhotoStorageService.uploadDir().toUri().toString());
    }
}
