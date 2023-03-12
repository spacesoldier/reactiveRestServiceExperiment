package com.spacesoldier.reactive.experiment.arch.api.features.feature0;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.api.AppInitActionDefinition;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.EndpointAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class FeatureOneConfig {

    @Autowired
    EndpointAdapter endpointAdapter;

    @Autowired
    WiringAdapter wiringAdapter;

    @Autowired
    FirstFeatureService firstFeatureService;

    @Bean
    public AppInitActionDefinition featureOneInit(){
        return AppInitActionDefinition.builder()
                    .initActionName(
                            FirstFeatureService.FEATURE_ONE_READY
                    )
                    .initAction(
                            () -> "[FEATURE 1]: First feature service at your command, sir!"
                    )
                .build();
    }

    @Bean
    public void configFeatureOne(){
        endpointAdapter.registerRequestBuilder(
                FeatureOneRequest.class,
                request -> FeatureOneRequest.builder().build()
        );

        wiringAdapter.registerFeature(
                FeatureOneRequest.class,
                request -> firstFeatureService.performFeatureLogic((FeatureOneRequest) request)
        );
    }
}
