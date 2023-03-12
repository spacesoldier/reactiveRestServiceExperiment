package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.FirstFeatureService;
import com.spacesoldier.reactive.experiment.arch.api.features.feature1.SecondFeatureService;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.api.AppInitActionDefinition;
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
    public AppInitActionDefinition featureThreeInit(){
        return AppInitActionDefinition.builder()
                .initActionName(
                        ThirdFeatureService.FEATURE_THREE_READY
                )
                .initAction(
                        () -> "[FEATURE 3]: Feature three ready for rock!!!"
                )
                .dependsOn(
                        new HashSet<>(){
                            {
                                add(FirstFeatureService.FEATURE_ONE_READY);
                                add(SecondFeatureService.FEATURE_TWO_SRV_READY);
                            }
                        }
                )
                .build();
    }
    @Bean
    public void configFeatureThree(){
        endpointAdapter.registerRequestBuilder(
                ThirdFeatureServiceRequest.class,
                ThirdFeatureService.transformRequest()
        );

        wiringAdapter.registerFeature(
                ThirdFeatureServiceRequest.class,
                request -> thirdFeatureService.performFeatureLogic((ThirdFeatureServiceRequest) request),
                 new HashSet<>(){{
                     add(FirstFeatureService.FEATURE_ONE_READY);
                     add(SecondFeatureService.FEATURE_TWO_SRV_READY);
                 }}
        );
    }
}
