package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.EnvelopeKey;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;

public interface RoutingHelper {
    String defineRequestPriority(String requestId, RequestPriority priority);
    Boolean requestIsPrioritised(String requestId);
    RequestPriority parsePriorityForRequestID(String requestId);
    String removePriorityFromRequestID(String requestId);
    String encodeCorrelId(String requestStr, String correlId);
    String encodeRequestKeyParams(String requestId, String correlId, RequestPriority priority);
    EnvelopeKey decodeRequestParams(String envelopeKeyStr);
}
