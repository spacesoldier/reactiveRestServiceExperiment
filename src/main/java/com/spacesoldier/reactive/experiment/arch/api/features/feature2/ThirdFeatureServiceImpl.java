package com.spacesoldier.reactive.experiment.arch.api.features.feature2;

import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceRequest;
import com.spacesoldier.reactive.experiment.arch.api.features.feature2.model.ThirdFeatureServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
@Slf4j
public class ThirdFeatureServiceImpl implements ThirdFeatureService{

    @Override
    public ThirdFeatureServiceResponse performFeatureLogic(ThirdFeatureServiceRequest request) {
        log.info("[FEATURE 3]: doing something");

        Flux<String> testFlux = Flux.fromIterable(
                new ArrayList<String>(){{
                    add("beep");
                    add("boop");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    add("weeeeep");
                }}
        ).doOnComplete(
                () -> {
                    log.info("OOOOOH");
                }
        );

        testFlux = testFlux.doOnComplete(
                                            () -> {
                                                    log.info("WEEEEEEEEEEE");
                                                    }
                                        );

        testFlux = testFlux.doFinally(
                signalType -> {
                    log.info("FINALLY DO EHEHEHEHEHE");
                }
        );

        testFlux.subscribe(
                msg -> log.info(msg)
        );

        Mono<String> testMono = Mono.just("1111111111111111111");

        testMono.doFinally(
                signalType -> {
                    log.info("EHEHEHEHEHE");
                }
        );

        testMono.subscribe(
                test -> log.info(test)
        );

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return ThirdFeatureServiceResponse.builder()
                                                .shnooops(request.getPathVar())
                                            .build();
    }
}
