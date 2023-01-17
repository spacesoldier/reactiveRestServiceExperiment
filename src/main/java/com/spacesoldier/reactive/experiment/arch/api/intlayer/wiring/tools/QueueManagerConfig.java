package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.PriorityQueueManagerImpl;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue.QueueManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class QueueManagerConfig {

    @Bean
    public QueueManager initPriorityQueueManager(){
        return PriorityQueueManagerImpl.builder()
                                    .build();
    }
}
