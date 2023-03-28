package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.implementation;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.RequestsQueue;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class RoundRobinPriorityQueue implements RequestsQueue {
    private String queueNamePrefix = "roundRobin";

    private final Map<Class, RequestsQueue> queuesToWrite = new ConcurrentHashMap<>();
    private final Deque<RequestsQueue> queuesToRead = new ConcurrentLinkedDeque<>();

    private Function<String, RequestsQueue> requestQueueSource;

    @Builder
    public RoundRobinPriorityQueue(
            String queueName,
            Function<String, RequestsQueue> requestQueueSource
    ){
        this.queueNamePrefix = queueName;
        this.requestQueueSource = requestQueueSource;
    }

    @Override
    public Consumer<RouterBypassRequest> putItem() {
        return request -> {
            Class payloadType = request.getPayload().getClass();

            if (!queuesToWrite.containsKey(payloadType)){
                RequestsQueue newQueueToWrite = null;

                String newQueueName = queueNamePrefix+"-"+payloadType.getSimpleName();

                if (requestQueueSource != null){
                    newQueueToWrite = requestQueueSource.apply(newQueueName);
                } else {
                    // queue manager was not set, let's improvise
                    newQueueToWrite = RequestPriorityQueue.builder()
                                                                .queueName(newQueueName)
                                                            .build();
                }

                if (newQueueToWrite != null){
                    queuesToWrite.put(payloadType,newQueueToWrite);
                    newQueueToWrite.putItem().accept(request);
                }
            } else {
                RequestsQueue queueToWrite = queuesToWrite.get(payloadType);
                queueToWrite.putItem().accept(request);
            }

        };
    }

    @Override
    public Supplier<RouterBypassRequest> getItem() {
        return () -> {
            RouterBypassRequest output = null;
            RequestsQueue itemSource = queuesToRead.poll();

            if (itemSource != null){
                output = itemSource.getItem().get();
                queuesToRead.addLast(itemSource);
            }
            return output;
        };
    }

    @Override
    public Supplier<Integer> queueSize() {

        return () -> queuesToWrite.values()
                                    .stream()
                                    .map(queue -> queue.queueSize().get())
                                    .reduce(Integer::sum)
                                    .orElse(0);
    }

    Consumer<RouterBypassRequest> onItemPut;
    Consumer<RouterBypassRequest> onItemGet;

    @Override
    public void subscribeOnItemPut(Consumer<RouterBypassRequest> onItemPut) {
        this.onItemPut = onItemPut;
    }

    @Override
    public void subscribeOnItemGet(Consumer<RouterBypassRequest> onItemGet) {
        this.onItemGet = onItemGet;
    }
}
