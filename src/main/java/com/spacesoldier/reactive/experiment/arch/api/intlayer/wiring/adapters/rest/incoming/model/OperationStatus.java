package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.model;

public enum OperationStatus {
    OK("OK"),
    FAIL("FAIL"),
    PROBLEMS("PROBLEMS");

    public final String status;

    private OperationStatus(String status){
        this.status = status;
    }
}
