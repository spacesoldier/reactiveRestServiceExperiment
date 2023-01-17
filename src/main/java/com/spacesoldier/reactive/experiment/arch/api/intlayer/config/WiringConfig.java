package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.IntlayerObjectRouter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.providers.MonoChannelProvider;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class WiringConfig {

    @Autowired
    MonoChannelProvider monoChannelProvider;

    @Autowired
    IntlayerObjectRouter intlayerObjectRouter;

    @Bean
    public WiringAdapter initWiringAdapter(){
        WiringAdapter wiring = WiringAdapter.builder()
                    .incomingMsgSink        (   intlayerObjectRouter.singleRequestsInput()                      )
                    .monoProvider           (
                                                requestId -> monoChannelProvider.newWire(requestId)
                                                                                .getMonoToSubscribe()
                                            )
                    .routableFunctionSink   (
                                                (typeToRoute,routableFn) ->
                                                        intlayerObjectRouter.addRoutableFunction(
                                                                                                    typeToRoute,
                                                                                                    routableFn
                                                                                                )
                                            )
                .build();

        intlayerObjectRouter.setOnRouterReadyAction(wiring.invokeInitActions());

        return wiring;
    }
}
