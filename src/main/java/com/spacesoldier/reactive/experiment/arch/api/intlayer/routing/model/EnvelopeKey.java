package com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

//@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        EnvelopeKey.JSON_PROPERTY_RQID,
        EnvelopeKey.JSON_PROPERTY_CORRELID,
        EnvelopeKey.JSON_PROPERTY_PRIORITY
})
public class EnvelopeKey {
    public static final String JSON_PROPERTY_RQID="rqId";
    private String rqId;
    public static final String JSON_PROPERTY_CORRELID="correlId";
    private String correlId;
    public static final String JSON_PROPERTY_PRIORITY="priority";
    private RequestPriority priority;

    @JsonProperty(JSON_PROPERTY_RQID)
    public String getRqId() {
        return rqId;
    }

    @JsonProperty(JSON_PROPERTY_RQID)
    public void setRqId(String rqId) {
        this.rqId = rqId;
    }

    @JsonProperty(JSON_PROPERTY_CORRELID)
    public String getCorrelId() {
        return correlId;
    }

    @JsonProperty(JSON_PROPERTY_CORRELID)
    public void setCorrelId(String correlId) {
        this.correlId = correlId;
    }

    @JsonProperty(JSON_PROPERTY_PRIORITY)
    public RequestPriority getPriority() {
        return priority;
    }

    @JsonProperty(JSON_PROPERTY_PRIORITY)
    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }
}
