package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.FirstFeatureService;
import com.spacesoldier.reactive.experiment.arch.api.features.feature1.SecondFeatureService;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.EndpointAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashSet;

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

        wiringAdapter.registerInitAction(
                ThirdFeatureService.FEATURE_THREE_READY,
                () -> "[FEATURE 3]: Feature three ready for rock!!!",
                new HashSet<>(){
                    {
                        add(FirstFeatureService.FEATURE_ONE_READY);
                        add(SecondFeatureService.FEATURE_TWO_SRV_READY);
                    }
                }
        );

        wiringAdapter.registerFeature(
                ThirdFeatureServiceRequest.class,
                request -> thirdFeatureService.performFeatureLogic((ThirdFeatureServiceRequest) request)
        );
    }
}
