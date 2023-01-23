package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model;

import lombok.Builder;
import lombok.Data;

import java.util.function.Function;

@Data @Builder
public class LimiterPassRequest {
    private String coinId;
    private Object payload;
    private Function transmissionFn;
}
