package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RequestPriority;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class PriorityQueueImpl implements RequestsQueue {

    private Map<RequestPriority, Queue> queues = new HashMap<>(){
        {
            put(    RequestPriority.BACKGROUND, new ConcurrentLinkedQueue());   // for low-priority items
            put(
                    RequestPriority.REGULAR_LEVEL,                              // variable priority items
                    new PriorityQueue<Comparable>(
                            (o1, o2) -> o1.compareTo(o2)
                    )
            );
            put(    RequestPriority.USER_LEVEL, new ConcurrentLinkedQueue());   // for highest priority items
        }
    };

    private String queueName;

    @Builder
    private PriorityQueueImpl(
            String queueName,
            Consumer onItemPut,
            Consumer onItemGet

    ){
        this.queueName = queueName;
        this.onItemPut = onItemPut;
        this.onItemGet = onItemGet;
    }

    String errorMsgTemplate = "[QUEUE ERROR]: null element provided to queue %s with %s priority";

    Consumer onItemPut;
    Consumer onItemGet;

    @Override
    public Consumer putItem(RequestPriority priority) {
        return item -> {
            if (item != null){
                Queue queueToPut = queues.get(priority);

                if (queueToPut != null){
                    queueToPut.add(item);
                } else {
                    log.info("[QUEUE WARN]: putting an item to unknown queue: "+priority.str());
                }

                if (onItemPut != null){
                    onItemPut.accept(item);
                }
            } else {
                log.info(
                        String.format(
                                errorMsgTemplate,
                                queueName,
                                priority.description()
                        )
                );
            }
        };
    }

    @Override
    public Supplier getItem() {

        return () -> {
            Object queuedItem = null;

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
    public void subscribeOnItemPut(Consumer onItemPut) {
        this.onItemPut = onItemPut;
    }

    @Override
    public void subscribeOnItemGet(Consumer onItemGet) {
        this.onItemGet = onItemGet;
    }
}
