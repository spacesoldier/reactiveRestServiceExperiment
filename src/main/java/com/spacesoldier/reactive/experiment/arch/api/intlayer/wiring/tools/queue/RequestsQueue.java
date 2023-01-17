package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue;


import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface RequestsQueue {
    Consumer putItem(
            RequestPriority priority
    );
    Supplier getItem();

    Supplier<Integer> queueSize();

    void subscribeOnItemPut(Consumer onItemPut);
    void subscribeOnItemGet(Consumer onItemGet);
}
