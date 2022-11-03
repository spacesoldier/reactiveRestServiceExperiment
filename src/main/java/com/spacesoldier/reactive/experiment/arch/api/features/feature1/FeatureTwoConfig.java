package com.spacesoldier.reactive.experiment.arch.api.features.feature1;

import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.EndpointAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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

        wiringAdapter.registerInitAction(
                "init feature two",
                () -> "[FEATURE 2]: Hey hey hey feature two init sequence message to anyone who interested"
        );

        wiringAdapter.registerFeature(
                FeatureTwoRequest.class,
                request -> secondFeatureService.performFeatureLogic((FeatureTwoRequest) request)
        );
    }

}
