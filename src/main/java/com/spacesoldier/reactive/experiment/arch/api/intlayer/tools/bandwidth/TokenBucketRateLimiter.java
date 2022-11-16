package com.spacesoldier.reactive.experiment.arch.api.intlayer.tools.bandwidth;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.model.RoutedObjectEnvelope;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.tools.bandwidth.model.RouterBypassRequest;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

@Slf4j
public class TokenBucketRateLimiter {

    int bucketCapacity = 450; // default value corresponds to 90% of connections Netty can initiate instantly

    private Stack<String> tokenBucket = new Stack<>();

    private Stack<String> spentCoins = new Stack<>();

    private BiConsumer<String,Object> requestSink;

    @Builder
    public TokenBucketRateLimiter(
            int bucketCapacity,
            BiConsumer<String, Object> requestSink
    ){
        this.bucketCapacity = bucketCapacity;
        this.requestSink = requestSink;

        IntStream.range(0,bucketCapacity).forEach(
                num -> tokenBucket.push(
                        //"coin"
                        UUID.randomUUID().toString()
                )
        );
    }

    public BiFunction<Object, RoutedObjectEnvelope,Object> aggregateBypass(){
        return (requestObj, envelope) -> {
            RouterBypassRequest request = null;
            try{
                request = (RouterBypassRequest) requestObj;
            } catch (Exception e){}

            if (request != null){
                request.setRequestId(envelope.getRqId());
            }

            return request == null ? requestObj : request;
        };
    }

    private int maxParkedRequestCount = 0;
    private void countParkedRequests(){
        int parkedRequestsCount = bypassRequestParking.size();
        if (parkedRequestsCount > maxParkedRequestCount){
            maxParkedRequestCount = parkedRequestsCount;
        }
    }

    public Consumer<RouterBypassRequest> receiveBypassRequest(){
        return request -> {
            if (request.getRequestId() != null){

                String correlId = request.getCorrelId();

                if (bypassRequestParking.containsKey(correlId)){
                    bypassRequestParking.get(correlId)
                                            .setRequestId(request.getRequestId());
                }
            }
        };
    }

    private Map<String,Integer> cashFlowHistory = new HashMap<>();

    private synchronized void spendCoin(){
        if (!tokenBucket.empty()){
            String coin = tokenBucket.pop();
            spentCoins.push(coin);

            updateHistory(coin);
        }
    }

    private void updateHistory(String coin) {
        if (!cashFlowHistory.containsKey(coin)){
            cashFlowHistory.put(coin,1);
        } else {
            cashFlowHistory.put(coin, cashFlowHistory.get(coin) + 1);
        }
    }

    private Function tryObtainToken (){

        return envelope -> {
            //log.info("[LIMITER]: try get token "+envelope.getClass().getSimpleName());

            if (tokenBucket.empty()){
                //log.info("[LIMITER]: no coins left");
                envelope = RouterBypassRequest.builder()
                                                    .correlId(UUID.randomUUID().toString())
                                                    .payload(envelope)
                                                .build();
            } else {
                spendCoin();
            }

            return envelope;
        };
    }

    public void revokeParkedRequest(){
        if (!bypassRequestsQueue.isEmpty()){
            String parkedRqCorrId = bypassRequestsQueue.poll();

            if (parkedRqCorrId != null){
                if (bypassRequestParking.containsKey(parkedRqCorrId)){

                    RouterBypassRequest parkedRequest = bypassRequestParking.get(parkedRqCorrId);

                    if (requestSink != null){
                        requestSink.accept(
                                parkedRequest.getRequestId(),
                                parkedRequest.getPayload()
                        );
                    }

                    bypassRequestParking.remove(parkedRqCorrId);
                }
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

    private Queue<String> bypassRequestsQueue = new ConcurrentLinkedQueue<>();

    private Map<String, RouterBypassRequest> bypassRequestParking = new ConcurrentHashMap<>();

    private Function tryReleaseToken () {

        return envelope -> {
            //log.info("[LIMITER]: try release token " + envelope.getClass().getSimpleName());
            if (envelope instanceof RouterBypassRequest){
                // park request
                RouterBypassRequest parkRequest = (RouterBypassRequest) envelope;
                bypassRequestParking.put(parkRequest.getCorrelId(), parkRequest);
                bypassRequestsQueue.offer(parkRequest.getCorrelId());

                countParkedRequests();
            } else {
                if( envelope instanceof CorePublisher<?>) {
                    //log.info("[LIMITER]: async out detected");

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
//                            monoItem.doOnSuccess(
//                                    signal -> {
//                                        returnCoin();
//                                    }
//                            );

//                            monoItem.doOnTerminate(
//                                    () -> {
//                                        returnCoin();
//                                    }
//                            );


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
                }

            }

            return envelope;
        };
    }

    private Function bypassTransmissionKnee(){
        return payload -> payload;
    }
    public Function<Function, Function> takeControlOverTransmission() {

        return transmissionFn -> tryObtainToken()
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
