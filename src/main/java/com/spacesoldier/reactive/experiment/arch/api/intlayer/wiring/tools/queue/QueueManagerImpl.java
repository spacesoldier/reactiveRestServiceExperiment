package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.implementation.RequestPriorityQueue;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.implementation.RoundRobinPriorityQueue;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
@Slf4j
public class QueueManagerImpl implements QueueManager{
    Map<String, RequestsQueue> requestQueues = new ConcurrentHashMap<>();

    private Function<String,RequestsQueue> obtainPriorityOnlyQueue(){
        return queueName -> {
            RequestsQueue output = null;
            if (!requestQueues.containsKey(queueName)){
                requestQueues.put(
                        queueName,
                        RequestPriorityQueue.builder()
                                                .queueName(queueName)
                                            .build()
                );
                log.info("[QUEUE MANAGER]: new priority queue "+queueName);
            }

            output = requestQueues.get(queueName);

            return output;
        };
    }

    private Function<String, RequestsQueue> obtainRoundRobinPriorityQueue(){
        return queueName -> {
            RequestsQueue output = null;

            if (!requestQueues.containsKey(queueName)){
                requestQueues.put(
                        queueName,
                        RoundRobinPriorityQueue.builder()
                                                    .queueName(queueName)
                                                    .requestQueueSource(
                                                            qName-> queueOnDemand(qName,QueueTactics.PRIORITY_ONLY)
                                                    )
                                                .build()
                );
                output = requestQueues.get(queueName);
            } else {
                RequestsQueue existingQueue = requestQueues.get(queueName);
                if (existingQueue instanceof RoundRobinPriorityQueue rrQueue){
                    output = rrQueue;
                } else {
                    String logErrorTemplate = "[QUEUE MANAGER]: ERROR cannot resolve round robin queue %s due to existing one having different tactics type";
                    log.info(
                            String.format(
                                    logErrorTemplate,
                                    queueName
                            )
                    );
                }
            }

            return output;
        };
    }

    private final Map<QueueTactics, Function<String, RequestsQueue>> obtainQueueByName = new HashMap<>(){{
        put(QueueTactics.PRIORITY_ONLY, obtainPriorityOnlyQueue());
        put(QueueTactics.ROUND_ROBIN, obtainRoundRobinPriorityQueue());
    }};



    @Builder
    private QueueManagerImpl(){

    }

    @Override
    public RequestsQueue queueOnDemand(
            String queueName,
            QueueTactics tactics
    ) {
        RequestsQueue output = null;

        if (obtainQueueByName.containsKey(tactics)){
            output = obtainQueueByName.get(tactics).apply(queueName);
        }

        return output;
    }
}
