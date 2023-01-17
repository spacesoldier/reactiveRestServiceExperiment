package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutedObjectEnvelope;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RouterBypassRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.function.*;
import java.util.stream.IntStream;

@Slf4j
public class TokenBucketRateLimiter {
    @Getter
    private String name;

    private int bucketCapacity = 450; // default value corresponds to 90% of connections Netty can initiate instantly

    private Stack<String> tokenBucket = new Stack<>();

    private Stack<String> spentCoins = new Stack<>();

    private BiConsumer<String,Object> requestSink;

    private Consumer<Object> reportOverloadStart;
    private Consumer<Object> reportOverloadEnd;

    public void connectOverloadMonitoring(
            Consumer<Object> reportOverloadStart,
            Consumer<Object> reportOverloadEnd
    ){
        this.reportOverloadStart = reportOverloadStart;
        this.reportOverloadEnd = reportOverloadEnd;
    }

    private Consumer<RouterBypassRequest> parkRequestSink;
    private Supplier<RouterBypassRequest> parkRequestSource;

    @Builder
    public TokenBucketRateLimiter(
            int bucketCapacity,
            BiConsumer<String, Object> requestSink,
            Consumer<RouterBypassRequest> parkRequestSink,
            Supplier<RouterBypassRequest> parkRequestSource,
            String limiterName,
            Consumer<Object> reportOverloadStart,
            Consumer<Object> reportOverloadEnd
    ){
        this.name = limiterName;
        this.bucketCapacity = bucketCapacity;
        this.requestSink = requestSink;

        // connect the external stuff to park queued requests
        this.parkRequestSink = parkRequestSink;
        this.parkRequestSource = parkRequestSource;

        // monitoring stuff
        this.reportOverloadStart = reportOverloadStart;
        this.reportOverloadEnd = reportOverloadEnd;

        // and now some gold
        // $$$$$        x_0
        IntStream.range(0,this.bucketCapacity).forEach(
                num -> tokenBucket.push(
                        //"coin"
                        UUID.randomUUID().toString()
                )
        );
    }

    public BiFunction<Object,RoutedObjectEnvelope,RoutedObjectEnvelope> aggregateBypass(){
        return (requestObj, envelope) -> {
            RouterBypassRequest bypassRequest = null;
            try{
                bypassRequest = (RouterBypassRequest) requestObj;
            } catch (Exception e){}

            if (bypassRequest != null){
                bypassRequest.setRequestId(envelope.getRqId());
                bypassRequest.setPriority(envelope.getPriority());
            }

            return envelope;
        };
    }



    public Consumer<RouterBypassRequest> receiveBypassRequest(){
        return bypassRequest -> {

            if (parkRequestSink != null){
                parkRequestSink.accept(bypassRequest);
            }
        };
    }

    private Map<String,Integer> cashFlowHistory = new HashMap<>();



    private void updateHistory(String coin) {
        if (!cashFlowHistory.containsKey(coin)){
            cashFlowHistory.put(coin,1);
        } else {
            cashFlowHistory.put(coin, cashFlowHistory.get(coin) + 1);
        }
    }

    private synchronized void spendCoin(){
        if (!tokenBucket.empty()){
            String coin = tokenBucket.pop();
            spentCoins.push(coin);

            updateHistory(coin);

            if (reportOverloadEnd != null){
                reportOverloadEnd.accept("");
            }
        } else {
            if (reportOverloadStart != null) {
                reportOverloadStart.accept("");
            }
        }
    }

    private Function tryObtainToken (){

        return envelope -> {
            //log.info("[LIMITER]: try get token "+envelope.getClass().getSimpleName());

            if (tokenBucket.empty()){
                //log.info("[LIMITER]: no coins left");
                envelope = RouterBypassRequest.builder()
                                                    .correlId(UUID.randomUUID().toString())
                                                    .bypassStart(
                                                            OffsetDateTime.now()
                                                    )
                                                    .payload(envelope)
                                                .build();
            } else {
                spendCoin();
            }

            return envelope;
        };
    }

    public void revokeParkedRequest(){

        RouterBypassRequest queuedRequest = null;
        if (parkRequestSource != null){
            queuedRequest = parkRequestSource.get();
        }

        if (queuedRequest != null){
            if (requestSink != null){
                requestSink.accept(queuedRequest.getRequestId(), queuedRequest.getPayload());
            }
        }
    }



    private synchronized void returnCoin(){

        //tokenBucket.push("coin");
        if (!spentCoins.empty()){
            String coin = spentCoins.pop();
            tokenBucket.push(coin);
        } else {
            tokenBucket.push(UUID.randomUUID().toString());
            log.info("[LIMITER]: emit new coin");
        }

        revokeParkedRequest();
    }

    private Function tryReleaseToken () {

        return envelope -> {

            // let's check if input object is a result of request bypass knee action
            if (envelope instanceof RouterBypassRequest){
                // there were no tokens to get, so bypass knee was used
                // the request will be queued
            } else {
                // the original function was activated,
                // so we obtain its output here
                // let's check the sort of the output object
                if( envelope instanceof CorePublisher<?>) {
                    // some async action happen
                    // so the token will be disposed later

                    Flux fluxItem = null;

                    try {
                        fluxItem = (Flux) envelope;
                    } catch (Exception e){}

                    if (fluxItem == null){

                        Mono monoItem = null;
                        try {
                            monoItem = (Mono) envelope;
                        } catch (Exception e){}

                        if (monoItem != null){

                            monoItem = monoItem.doFinally(
                                    signal -> {
                                        returnCoin();
                                    }
                            );
                            //log.info("[LIMITER]: item is Mono");

                            envelope = monoItem;
                        }

                    } else {
                        //log.info("[LIMITER]: item is Flux");
                        fluxItem = fluxItem.doFinally(
                                signal -> {
                                    returnCoin();
                                }
                        );

                        envelope = fluxItem;
                    }
                } else {
                    // no async actions detected,
                    // dispose the token now
                    returnCoin();
                }

            }

            return envelope;
        };
    }

    private Function bypassTransmissionKnee(){
        return payload -> payload;
    }
    public Function<Function, Function> takeControlOverTransmission() {

        return transmissionFn ->
                    tryObtainToken()
                    .andThen(
                                envelope -> {
                                    Function output = transmissionFn;
                                    if (envelope instanceof RouterBypassRequest){
                                        output = bypassTransmissionKnee();
                                    }
                                    return output.apply(envelope);
                                }
                            )
                    .andThen(   tryReleaseToken()  );
    }
}
