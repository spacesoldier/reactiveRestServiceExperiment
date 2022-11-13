package com.spacesoldier.reactive.experiment.arch.api.intlayer.startup;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.IntlayerObjectRouter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.kafka.ReactorKafkaAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.ApiClientAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.adapter.ExternalResourceCallDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

public class AppReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();

        Map<String, ExternalResourceCallDefinition> externalResourceCallDefs = context.getBeansOfType(ExternalResourceCallDefinition.class);

        if (!externalResourceCallDefs.isEmpty()) {

            ApiClientAdapter adapter = context.getBean(ApiClientAdapter.class);

            externalResourceCallDefs.forEach(
                    (key, value) -> adapter.registerResourceClient(value)
            );
        }

        ReactorKafkaAdapter kafkaAdapter = context.getBean(ReactorKafkaAdapter.class);

        if (kafkaAdapter != null) {
            // collect the configuration items
            // prepare inbound and outbound channels
            kafkaAdapter.prepareForOperations();
        }

        IntlayerObjectRouter objectRouter = context.getBean(IntlayerObjectRouter.class);

        objectRouter.start();

        if (kafkaAdapter != null) {
            // when object router is operational
            // start listening to topics and produce messages
            kafkaAdapter.startOperations();
        }
    }
}
