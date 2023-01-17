package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.channels;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.EnumMap;
import java.util.function.Consumer;

import static reactor.core.publisher.Sinks.EmitResult.*;

public class FluxChannel {
    @Getter
    private String adapterName;

    private final Logger logger = LoggerFactory.getLogger("FLUX ADAPTER");

    private Sinks.Many loopbackSink;

    @Getter @Setter
    private Disposable channelTask;

    @Getter
    private Flux streamToSubscribe;

    private String adapterStatusMsgTemplate = "[%s ADAPTER]: %s , %s";

    private Consumer logErrorStatus(String status){

        return
                input -> logger.info(
                        String.format(
                                adapterStatusMsgTemplate,
                                adapterName.toUpperCase(),
                                "ERROR",
                                status)
                );
    }

    // firstly it's only a monitoring, later there could be some handlers implementing additional logic
    private EnumMap<Sinks.EmitResult, Consumer> emitResultHandlers = new EnumMap<>(Sinks.EmitResult.class){
        {
            put(FAIL_TERMINATED,logErrorStatus("FAIL_TERMINATED"));
            put(FAIL_OVERFLOW, logErrorStatus("FAIL_OVERFLOW"));
            put(FAIL_CANCELLED, logErrorStatus("FAIL_CANCELLED"));
            put(FAIL_NON_SERIALIZED, msg -> {
                // when multiple threads try to write simultaneously, fail fast and retry
                // which is an optimistic scenarion in comparison with using a synchronized lock
                loopbackSink.emitNext(msg, (type, result) -> {
                    logErrorStatus("AGAIN FAIL_NON_SERIALIZED");
                    return true;
                });
                logErrorStatus("RETRY FAIL_NON_SERIALIZED");
            });
            put(FAIL_ZERO_SUBSCRIBER,logErrorStatus("FAIL_ZERO_SUBSCRIBER"));
        }
    };

    public FluxChannel(String adapterName){
        this.loopbackSink = Sinks.many().multicast().onBackpressureBuffer(15000);
        this.streamToSubscribe = this.loopbackSink.asFlux()
//                .limitRate(10)
//                .limitRate(50)
                .publishOn(
                        Schedulers.newBoundedElastic(
                                16,
                                15000,
                                adapterName
                        )
                ); // let it be a cold source, or use .share(); // if we want a hot source
        this.adapterName = adapterName;
    }

    private synchronized Sinks.EmitResult publishMessage(Object message){
        return loopbackSink.tryEmitNext(message);
    }
    public Consumer getStreamInput(){
        String errMsgTemplate = "[%s STREAM ERROR]: %s";
        return message -> {
            if (message != null){
                //Sinks.EmitResult sinkResult = loopbackSink.tryEmitNext(message);

                Sinks.EmitResult sinkResult = publishMessage(message);

                if (sinkResult.isFailure()){
                    if (emitResultHandlers.containsKey(sinkResult)){

                        logger.info(
                                String.format(
                                        adapterStatusMsgTemplate,
                                        adapterName.toUpperCase(),
                                        "emitResultError "+sinkResult,
                                        "subscribers count "+ loopbackSink.currentSubscriberCount()
                                )
                        );

                        emitResultHandlers.get(sinkResult).accept(message);
                    }
                }
            } else {
                logger.info(
                        String.format(
                                errMsgTemplate,
                                adapterName.toUpperCase(),
                                "attempt to stream a null value"
                        )
                );
            }
        };
    }
}
