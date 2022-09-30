package com.spacesoldier.reactive.experiment.arch.api.features.feature1.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data @Builder
public class FeatureTwoResponse {
    @Nullable @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;
    @Nullable @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String payload;
}
