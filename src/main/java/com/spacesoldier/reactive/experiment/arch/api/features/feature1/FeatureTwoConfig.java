package com.spacesoldier.reactive.experiment.arch.api.features.feature1;

import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.EndpointAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class FeatureTwoConfig {

    @Autowired
    EndpointAdapter endpointAdapter;

    @Autowired
    WiringAdapter wiringAdapter;

    @Autowired
    SecondFeatureService secondFeatureService;

    @Bean
    public void configureFeatureTwo(){

        endpointAdapter.registerRequestBuilder(
                FeatureTwoRequest.class,
                SecondFeatureService.transformRequest()
        );

        wiringAdapter.registerFeature(
                FeatureTwoRequest.class,
                request -> secondFeatureService.performFeatureLogic((FeatureTwoRequest) request)
        );
    }

}
