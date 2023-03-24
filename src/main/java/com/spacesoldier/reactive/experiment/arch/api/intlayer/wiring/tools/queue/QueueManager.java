package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.queue;

public interface QueueManager {
    RequestsQueue queueOnDemand(String queueName, QueueTactics tactics);
}
