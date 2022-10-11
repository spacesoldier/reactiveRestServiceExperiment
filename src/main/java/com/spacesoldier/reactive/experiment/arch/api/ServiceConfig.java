package com.spacesoldier.reactive.experiment.arch.api;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneResponse;
import com.spacesoldier.reactive.experiment.arch.api.features.feature0.spec.FeatureOneAPISpec;
import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceResponse;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.EndpointAdapter;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Component
public class ServiceConfig {
    @Autowired
    EndpointAdapter endpointAdapter;

    @Bean
    @FeatureOneAPISpec
    RouterFunction<ServerResponse> initFeatureOneAPI(){
        return route(
                GET("/api/feature0"),
                req -> endpointAdapter.forwardRequestToLogic(
                        FeatureOneRequest.class,
                        req,
                        FeatureOneResponse.class
                )
        );
    }

    @Bean
    RouterFunction<ServerResponse> initFeatureTwoAPI(){
        return route(
                                POST("/api/feature1"),
                                req ->  endpointAdapter.forwardRequestToLogic(
                                                                                String.class,
                                                                                FeatureTwoRequest.class,
                                                                                req,
                                                                                FeatureOneResponse.class
                                                                              )
        );
    }

    @Bean
    RouterFunction<ServerResponse> initFeatureThreeAPI(){
        return route(
                                GET("/api/feature2/{shnooops}"),
                                req -> endpointAdapter.forwardRequestToLogic(
                                                                                ThirdFeatureServiceRequest.class,
                                                                                req,
                                                                                ThirdFeatureServiceResponse.class
                                                                            )
        );
    }

//    @Bean
//    RouterFunction<ServerResponse> configApi(){
//        return route(
//                                GET("/api/feature0"),
//                                req -> endpointAdapter.forwardRequestToLogic(
//                                                                                FeatureOneRequest.class,
//                                                                                req,
//                                                                                FeatureOneResponse.class
//                                                                            )
//                )
//                .and(
//                        route(
//                                POST("/api/feature1"),
//                                req ->  endpointAdapter.forwardRequestToLogic(
//                                                                                String.class,
//                                                                                FeatureTwoRequest.class,
//                                                                                req,
//                                                                                FeatureOneResponse.class
//                                                                              )
//                        )
//                ).
//                and(
//                        route(
//                                GET("/api/feature2/{shnooops}"),
//                                req -> endpointAdapter.forwardRequestToLogic(
//                                                                                ThirdFeatureServiceRequest.class,
//                                                                                req,
//                                                                                ThirdFeatureServiceResponse.class
//                                                                            )
//                        )
//                );
//    }
}
