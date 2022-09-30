package com.spacesoldier.reactive.experiment.arch.api.features.feature1;

import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature1.model.FeatureTwoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FeatureTwoSrvImpl implements SecondFeatureService{

    @Override
    public FeatureTwoResponse performFeatureLogic(FeatureTwoRequest req) {

        log.info("[FEATURE 1] request received");

        if (req.getRequest().equalsIgnoreCase("aaa")){

        } else {

        }

        return FeatureTwoResponse.builder()
                .payload("weeeeeeeeeep")
                .build();
    }


}
