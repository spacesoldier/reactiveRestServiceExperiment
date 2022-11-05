package com.spacesoldier.reactive.experiment.arch.api.features.feature0;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FeatureOneSrvImpl implements FirstFeatureService {
    @Override
    public FeatureOneResponse performFeatureLogic(FeatureOneRequest request) {

        log.info("[FEATURE 1] request received");

        return FeatureOneResponse.builder()
                .payload("beeep beeeep beeeeeeeeeeeeeeep wtf")
                .build();
    }
}
