package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.client;

import org.springframework.util.StringUtils;

import java.util.Collection;

public enum CollectionFormat {
    CSV(","), TSV("\t"), SSV(" "), PIPES("|"), MULTI(null);

    private final String separator;
    private CollectionFormat(String separator) {
        this.separator = separator;
    }

    public String collectionToString(Collection<?> collection) {
        return StringUtils.collectionToDelimitedString(collection, separator);
    }
}