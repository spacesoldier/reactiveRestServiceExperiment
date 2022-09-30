package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.model;

public enum MessageSeverity {

    CRITICAL("CRITICAL"),
    ERROR("ERROR"),
    WARNING("WARNING");

    public final String severity;
    private MessageSeverity(String severity) {
        this.severity = severity;
    }
}
