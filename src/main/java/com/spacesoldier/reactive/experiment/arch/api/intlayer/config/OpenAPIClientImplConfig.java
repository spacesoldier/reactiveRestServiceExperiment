package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.ApiClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpenAPIClientImplConfig {

    @Value("${external-resources.google.base-url}")
    private String externalResourceBasePath;

    @Bean
    public ApiClientImpl prepareOpenApiClient(){
        return ApiClientImpl.builder()
                                .basePath(externalResourceBasePath)
                            .build();
    }
}
