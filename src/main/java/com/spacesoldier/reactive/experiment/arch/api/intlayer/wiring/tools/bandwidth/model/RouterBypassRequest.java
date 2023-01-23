package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.function.Function;

@Data
@Builder
public class RouterBypassRequest {
    private String requestId;
    private String correlId;
    private RequestPriority priority;
    Function delayedCall;
    private Object payload;
    private OffsetDateTime bypassStart;
    private OffsetDateTime bypassEnd;
}
