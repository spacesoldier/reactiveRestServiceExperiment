package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.EndpointAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class FeatureThreeConfig {
    @Autowired
    EndpointAdapter endpointAdapter;

    @Autowired
    WiringAdapter wiringAdapter;

    @Autowired
    ThirdFeatureService thirdFeatureService;

    @Bean
    public void configFeatureThree(){
        endpointAdapter.registerRequestBuilder(
                ThirdFeatureServiceRequest.class,
                ThirdFeatureService.transformRequest()
        );

        wiringAdapter.registerFeature(
                ThirdFeatureServiceRequest.class,
                request -> thirdFeatureService.performFeatureLogic((ThirdFeatureServiceRequest) request)
        );
    }
}
