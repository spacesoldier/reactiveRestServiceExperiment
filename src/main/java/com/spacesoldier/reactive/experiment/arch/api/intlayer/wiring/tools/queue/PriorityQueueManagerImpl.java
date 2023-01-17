package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PriorityQueueManagerImpl implements QueueManager{

    Map<String,PriorityQueueImpl> priorityQueues = new ConcurrentHashMap<>();

    @Builder
    private PriorityQueueManagerImpl(){

    }

    @Override
    public RequestsQueue queueOnDemand(String queueName) {
        RequestsQueue output = null;

        if (!priorityQueues.containsKey(queueName)){
            priorityQueues.put(
                                queueName,
                                PriorityQueueImpl.builder()
                                                    .queueName(queueName)
                                                .build()
                            );
            log.info("[QUEUE MANAGER]: new priority queue "+queueName);
        }

        output = priorityQueues.get(queueName);

        return output;
    }
}
