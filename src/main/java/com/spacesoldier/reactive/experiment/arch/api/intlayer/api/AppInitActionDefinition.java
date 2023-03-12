package com.spacesoldier.reactive.experiment.arch.api.intlayer.api;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.function.Supplier;

@Data @Builder
public class AppInitActionDefinition {
    private String initActionName;
    private Supplier initAction;
    private Class actionCompleteSignalType;
    private Set<String> dependsOn;
}
