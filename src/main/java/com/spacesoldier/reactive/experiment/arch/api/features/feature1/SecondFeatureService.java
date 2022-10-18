package com.spacesoldier.reactive.experiment.arch.api.features.feature1;

import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoResponse;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.model.RestRequestEnvelope;

import java.util.function.Function;

public interface SecondFeatureService {

    static Function<RestRequestEnvelope, Object> transformRequest(){
        return envelope -> {

            String reqBody = null;

            if (envelope.getPayload().getClass() == String.class){
                reqBody = (String) envelope.getPayload();
            } else {
                reqBody = envelope.getPayload().toString();
            }

            return FeatureTwoRequest.builder()
                        .request(reqBody)
                    .build();
        };
    }
    FeatureTwoResponse performFeatureLogic(FeatureTwoRequest req);
}
