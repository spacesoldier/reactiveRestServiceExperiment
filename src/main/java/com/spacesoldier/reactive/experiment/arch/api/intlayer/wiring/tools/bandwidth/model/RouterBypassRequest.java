package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RouterBypassRequest {
    private String requestId;
    private String correlId;
    private RequestPriority priority;
    private Object payload;
    private OffsetDateTime bypassStart;
    private OffsetDateTime bypassEnd;
}
