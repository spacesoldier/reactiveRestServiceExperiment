package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.model;

import org.springframework.lang.Nullable;

import java.util.List;

public class RestResponseSchema<T> {
    @Nullable
    public OperationStatus status;

    @Nullable
    public List<OperationMessage> messages;

    @Nullable
    public T data;
}
