package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceResponse;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.model.RestRequestEnvelope;

import java.util.Map;
import java.util.function.Function;

public interface ThirdFeatureService {
    static String FEATURE_THREE_READY = "prepare feature three";
    static Function<RestRequestEnvelope, Object> transformRequest(){
        return envelope -> {

            String reqBody = null;

            // here will be taking a path parameter
            Map<String,String> pathVars = envelope.getPathVariables();

            return ThirdFeatureServiceRequest.builder()
                                                .pathVar(pathVars.get("shnooops"))
                                            .build();
        };
    }

    ThirdFeatureServiceResponse performFeatureLogic(ThirdFeatureServiceRequest request);

}
