package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.List;

@Data @Builder
public class RestResponseEnvelope {

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OperationStatus status;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<OperationMessage> messages;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;
}
