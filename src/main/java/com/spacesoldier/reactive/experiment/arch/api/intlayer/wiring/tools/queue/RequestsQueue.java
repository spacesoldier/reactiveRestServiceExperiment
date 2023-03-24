package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue;


import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface RequestsQueue {
    Consumer<RouterBypassRequest> putItem();
    Supplier<RouterBypassRequest> getItem();

    Supplier<Integer> queueSize();

    void subscribeOnItemPut(Consumer<RouterBypassRequest> onItemPut);
    void subscribeOnItemGet(Consumer<RouterBypassRequest> onItemGet);
}
