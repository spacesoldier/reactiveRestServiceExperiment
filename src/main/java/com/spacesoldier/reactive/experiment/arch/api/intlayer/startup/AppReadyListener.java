package com.spacesoldier.reactive.experiment.arch.api.intlayer.startup;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.IntlayerObjectRouter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public class AppReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();

        IntlayerObjectRouter objectRouter = context.getBean(IntlayerObjectRouter.class);

        objectRouter.start();
    }

}
