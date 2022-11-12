package com.spacesoldier.reactive.experiment.arch.api.features.feature0;

import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature0.model.FeatureOneResponse;

public interface FirstFeatureService {
    static String FEATURE_ONE_READY = "prepare feature one";
    FeatureOneResponse performFeatureLogic(FeatureOneRequest request);
}
