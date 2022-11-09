package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ThirdFeatureServiceImpl implements ThirdFeatureService{

    @Override
    public ThirdFeatureServiceResponse performFeatureLogic(ThirdFeatureServiceRequest request) {
        log.info("[FEATURE 3]: doing something");
        return ThirdFeatureServiceResponse.builder()
                                                .shnooops(request.getPathVar())
                                            .build();
    }
}
