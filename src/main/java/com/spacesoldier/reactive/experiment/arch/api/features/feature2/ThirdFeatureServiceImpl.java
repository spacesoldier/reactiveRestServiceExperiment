package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceResponse;
import org.springframework.stereotype.Service;

@Service
public class ThirdFeatureServiceImpl implements ThirdFeatureService{

    @Override
    public ThirdFeatureServiceResponse performFeatureLogic(ThirdFeatureServiceRequest request) {
        return ThirdFeatureServiceResponse.builder()
                                                .shnooops(request.getPathVar())
                                            .build();
    }
}
