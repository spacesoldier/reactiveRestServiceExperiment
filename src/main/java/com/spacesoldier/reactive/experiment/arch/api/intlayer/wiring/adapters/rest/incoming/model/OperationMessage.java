package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class OperationMessage {
    private MessageSeverity severity;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String text;
}
