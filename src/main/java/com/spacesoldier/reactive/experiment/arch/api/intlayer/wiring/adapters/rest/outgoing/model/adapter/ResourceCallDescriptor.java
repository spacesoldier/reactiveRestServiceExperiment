package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter;

import lombok.Builder;
import org.springframework.http.HttpMethod;

import java.util.Objects;

public class ResourceCallDescriptor {
    private String path;
    private HttpMethod method;

    @Builder
    private ResourceCallDescriptor(String path, HttpMethod method){
        this.path = path;
        this.method = method;
    }

    @Override
    public boolean equals(Object o){
        boolean result = false;

        // self check
        if (this == o){
            return true;
        }

        // null check
        if (o == null){
            return false;
        }

        // type check and cast
        if (getClass() != o.getClass()){
            return false;
        }

        ResourceCallDescriptor that = (ResourceCallDescriptor) o;

        result = Objects.equals(this.path, that.path)
                    &&
                 Objects.equals(this.method, that.method);

        return result;
    }
}
