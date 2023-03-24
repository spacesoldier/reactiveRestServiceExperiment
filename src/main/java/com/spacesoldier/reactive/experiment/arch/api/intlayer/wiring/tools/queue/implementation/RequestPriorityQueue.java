package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.implementation;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.RequestsQueue;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class RequestPriorityQueue implements RequestsQueue {

    private final Map<RequestPriority, Queue<RouterBypassRequest>> queues = new HashMap<>(){
        {
            put(    RequestPriority.BACKGROUND, new ConcurrentLinkedQueue<>());   // for low-priority items
            put(
                    RequestPriority.REGULAR_LEVEL,                                // variable priority items
                    new PriorityQueue<>()
            );
            put(    RequestPriority.USER_LEVEL, new ConcurrentLinkedQueue<>());   // for highest priority items
        }
    };

    private final String queueName;

    @Builder
    private RequestPriorityQueue(
            String queueName,
            Consumer<RouterBypassRequest> onItemPut,
            Consumer<RouterBypassRequest> onItemGet

    ){
        this.queueName = queueName;
        this.onItemPut = onItemPut;
        this.onItemGet = onItemGet;
    }

    String errorMsgTemplate = "[QUEUE ERROR]: null element provided to queue %s";

    Consumer<RouterBypassRequest> onItemPut;
    Consumer<RouterBypassRequest> onItemGet;

    @Override
    public Consumer<RouterBypassRequest> putItem() {
        return request -> {
            if (request != null){
                RequestPriority priority = request.getPriority();
                Queue queueToPut = queues.get(priority);

                if (queueToPut != null){
                    queueToPut.add(request);
                } else {
                    log.info("[QUEUE WARN]: putting an item to unknown queue: "+priority.str());
                }

                if (onItemPut != null){
                    onItemPut.accept(request);
                }
            } else {
                log.info(
                        String.format(
                                errorMsgTemplate,
                                queueName
                        )
                );
            }
        };
    }

    @Override
    public Supplier<RouterBypassRequest> getItem() {

        return () -> {
            RouterBypassRequest queuedItem = null;

            // try to get a high priority queued item
            queuedItem = queues.get(RequestPriority.USER_LEVEL).poll();

            // if there are no any high priority items
            if (queuedItem == null){
                // try to get regular priority item
                queuedItem = queues.get(RequestPriority.REGULAR_LEVEL).poll();
            }

            // and the last but not least - try to get the low priority item
            if (queuedItem == null){
                queuedItem = queues.get(RequestPriority.BACKGROUND).poll();
            }

            if (queuedItem != null && onItemGet != null){
                onItemGet.accept(queuedItem);
            }

            return queuedItem;
        };

    }

    @Override
    public Supplier<Integer> queueSize() {

        return () -> {
            Optional<Integer> queueLength = queues.values().stream().map(Collection::size).reduce(Integer::sum);

            return queueLength.orElse(0);
        };
    }

    @Override
    public void subscribeOnItemPut(Consumer<RouterBypassRequest> onItemPut) {
        this.onItemPut = onItemPut;
    }

    @Override
    public void subscribeOnItemGet(Consumer<RouterBypassRequest> onItemGet) {
        this.onItemGet = onItemGet;
    }
}
