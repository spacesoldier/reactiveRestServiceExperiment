package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.model.RequestCallBill;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class RateLimiterMonitor {
    // ------------------------------------------------------------------
    // pause management: when the rate limiter is overloaded,
    // so it could not proceed any new requests
    // we catch these moments and analyze them
    // ------------------------------------------------------------------
    private final Deque<Long> pausesPerMinute = new ConcurrentLinkedDeque<>();

    private int bandwidth = -1;
    OffsetDateTime pauseStarted = null;
    long minPausePeriodMs = 0;
    long maxPausePeriodMs = 0;
    long pauseCount = 0;
    long cumulativePauseMs = 0;

    Supplier<Integer> queuedRequestsCount = null;

    @Builder
    private RateLimiterMonitor(
            Supplier<Integer> queuedRequestsCount,
            int bandwidth
    ){
        this.queuedRequestsCount = queuedRequestsCount;
        this.bandwidth = bandwidth;
    }

    private void updatePauseMeasurement() {
        if (pauseStarted != null){
            Duration pausePeriod = Duration.between(
                    pauseStarted,
                    OffsetDateTime.now()
            );

            long pauseDurationMs = pausePeriod.toMillis();

            if (pauseDurationMs < minPausePeriodMs){
                minPausePeriodMs = pauseDurationMs;
            }
            if (pauseDurationMs > maxPausePeriodMs){
                maxPausePeriodMs = pauseDurationMs;
            }
            pauseCount += 1;

            pausesPerMinute.add(pauseDurationMs);

            if (pauseCount == 1){
                cumulativePauseMs = pauseDurationMs;
            } else {
                cumulativePauseMs = (cumulativePauseMs + pauseDurationMs) / 2;
            }

            pauseStarted = null;
        }
    }

    public Consumer checkOverloadEnd(){
        return pauseEnd -> {
            updatePauseMeasurement();
        };
    }

    private void registerPause() {
        if (pauseStarted == null){
            pauseStarted = OffsetDateTime.now();
        }
    }

    public Consumer setOverloadStart(){
        return event -> registerPause();
    }
    // ------------------------------------------------------------------
    // END of pause management part
    // ------------------------------------------------------------------


    // ------------------------------------------------------------------
    // request duration management
    // here we monitor the waiting time for queued requests
    // ------------------------------------------------------------------

    private final Deque<Long> requestsDurationsPerMinute = new ConcurrentLinkedDeque<>();

    private void saveRequestDuration(
            OffsetDateTime requestQueuedStart,
            OffsetDateTime requestProceed
    ) {
        requestsDurationsPerMinute.add(
                Duration.between(
                                requestQueuedStart,
                                requestProceed
                        )
                        .toMillis()
        );
    }

    public BiConsumer<OffsetDateTime,OffsetDateTime> requestDurationMeterSink(){
        return (start, end) -> saveRequestDuration(start,end);
    }

    private int maxParkedRequestCount = 0;

    private int countParkedRequests(){
        int requestsQueuedCount = -1;

        if (queuedRequestsCount != null){
            requestsQueuedCount = queuedRequestsCount.get();
        }

        if (requestsQueuedCount > maxParkedRequestCount){
            maxParkedRequestCount = requestsQueuedCount;
        }

        return requestsQueuedCount;
    }


    private final Map<String, RequestCallBill> requestBills = new ConcurrentHashMap<>();

    public BiFunction<String,String, String> requestCheckIn(){
        return (rqId, coinId) ->{

            RequestCallBill callBill = RequestCallBill.builder()
                                                            .billId(UUID.randomUUID().toString())
                                                            .requestId(rqId)
                                                            .coinId(coinId)
                                                            .requestStart(OffsetDateTime.now())
                                                        .build();
            if (!requestBills.containsKey(callBill.getBillId())){
                requestBills.put(callBill.getBillId(), callBill);
            } else {
                log.info("[RATE LIMITER]: same bill received!");
            }

            return callBill.getBillId();
        };
    }

    public Consumer<String> requestCheckOut(){
        return billId -> {
            if (requestBills.containsKey(billId)){
                RequestCallBill closeBill = requestBills.get(billId);
                closeBill.setRequestFinish(OffsetDateTime.now());
            }
        };
    }

    private void calcBillStats(){

        List<RequestCallBill> closedCallBills = requestBills.values()
                                                                .stream()
                                                                .filter(
                                                                    bill -> bill.getRequestFinish() != null
                                                                )
                                                            .toList();

        List<RequestCallBill> openCallBills = requestBills.values()
                                                                .stream()
                                                                .filter(
                                                                        bill -> bill.getRequestFinish() == null
                                                                )
                                                            .toList();

        List<String> closedBillIds = closedCallBills.stream().map(RequestCallBill::getBillId).toList();
        int callCompletedCount = closedBillIds.size();
        log.info("[RATE LIMITER]: "+callCompletedCount+" calls processed");

        if (!closedBillIds.isEmpty()){
            List<Duration> durations = closedCallBills.stream()
                    .map(
                            bill ->
                                    Duration.between(
                                            bill.getRequestStart(),
                                            bill.getRequestFinish()
                                    )
                    )
                    .sorted()
                    .toList();
            double medianDuration = -1.0;
            if (durations.size() %2 == 0){
                medianDuration = (double) (durations.get(durations.size()/2).toMillis() +
                        durations.get(durations.size()/2 - 1).toMillis()) / 2;
            } else {
                medianDuration = (double) durations.get(durations.size()/2).toMillis();
            }

            Long sumDuration = durations.stream()
                                            .map(Duration::toMillis)
                                            .reduce(Long::sum)
                                            .orElse(0L);

            double avgDuration = durations.isEmpty() ?
                    0.0                                         :
                    (double) (sumDuration / durations.size())   ;

            // clean billing history
            closedBillIds.forEach(
                    requestBills::remove
            );

            if (pauseStarted != null){
                log.info("[RATE LIMITER]: OVERLOAD");
            }

            log.info("[RATE LIMITER]: "+openCallBills.size()+" requests in process");

            if (
                    bandwidth > 0
                 && openCallBills.size() == bandwidth
            ){
                log.info("[RATE LIMITER]: FULL THROTTLE "+openCallBills.size()+"calls in process");
            }

            log.info("[RATE LIMITER]: average API call duration "+avgDuration+" ms");
            log.info("[RATE LIMITER]: median API call duration "+medianDuration+" ms");
        }

    }

    private final String logPausesTemplate = "[RATE LIMITER]: %s new pauses, %s pauses total, max = %s ms, min = %s ms, avg = %s ms, median = %s ms";
    private final String logRequestsDoneTemplate = "[RATE LIMITER]: %s delayed requests processed, avg wait = %s ms, median = %s ms";
    private final String logQueueTemplate = "[RATE LIMITER]: %s requests delayed in queue, max queue length %s";

    private long oldPausesCount = 0;

    public void reportStatsPerMinute(){

        long avgPauseMs = 0;
        long medianPause = 0;
        if (pausesPerMinute.size()>0){

            List<Long> pausesPerMinuteSorted = pausesPerMinute.stream().sorted().toList();

            avgPauseMs =    pausesPerMinuteSorted.isEmpty()         ?
                                                                0   :
                            pausesPerMinuteSorted
                                        .stream()
                                        .reduce(0L, Long::sum)
                                    / pausesPerMinuteSorted.size();

            int medianPos = pausesPerMinuteSorted.size()/2;
            medianPause = pausesPerMinuteSorted.get(medianPos);
        }

        long newPausesTotal = pauseCount - oldPausesCount;

        log.info(
                String.format(
                        logPausesTemplate,
                        newPausesTotal,
                        pauseCount,
                        maxPausePeriodMs,
                        minPausePeriodMs,
                        avgPauseMs,
                        medianPause
                )
        );

        oldPausesCount = pauseCount;
        pausesPerMinute.clear();

        long avgRqWaitMs = 0;
        long medianRqWaitMs = 0;

        if (requestsDurationsPerMinute.size() > 0){
            List<Long> rqWaitSorted = requestsDurationsPerMinute.stream().sorted().toList();
            avgRqWaitMs =   rqWaitSorted.isEmpty()              ?
                                                            0   :
                            rqWaitSorted
                                    .stream()
                                    .reduce(0L, Long::sum)
                                    / rqWaitSorted.size();
            int medianPos = rqWaitSorted.size() / 2;
            medianRqWaitMs = rqWaitSorted.get(medianPos);
        }

        log.info(
                String.format(
                        logRequestsDoneTemplate,
                        requestsDurationsPerMinute.size(),
                        avgRqWaitMs,
                        medianRqWaitMs
                )
        );

        requestsDurationsPerMinute.clear();

        int queuedRequests = countParkedRequests();

        log.info(
                String.format(
                        logQueueTemplate,
                        queuedRequests > 0 ? queuedRequests : "no data",
                        maxParkedRequestCount
                )
        );

        calcBillStats();
    }

}
