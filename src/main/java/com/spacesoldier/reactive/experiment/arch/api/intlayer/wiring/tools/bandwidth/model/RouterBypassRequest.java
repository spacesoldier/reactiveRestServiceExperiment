package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriorityComparator;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.function.Function;

@Data
@Builder
public class RouterBypassRequest implements Comparable<RouterBypassRequest> {
    private String requestId;
    private String correlId;
    private RequestPriority priority;
    Function delayedCall;
    private Object payload;
    private OffsetDateTime bypassStart;
    private OffsetDateTime bypassEnd;

    private final RequestPriorityComparator rqComparator = new RequestPriorityComparator();
    @Override
    public int compareTo(@NotNull RouterBypassRequest that) {
        int result = 0;
        if (this.getPriority() != null && that.getPriority() != null){
            result = rqComparator.compare( this.getPriority(), that.getPriority() );
        }
        return result;
    }
}
