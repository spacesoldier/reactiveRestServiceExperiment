package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class RateLimiterMonitor {
    // ------------------------------------------------------------------
    // pause management: when the rate limiter is overloaded,
    // so it could not proceed any new requests
    // we catch these moments and analyze them
    // ------------------------------------------------------------------
    private final List<Long> pausesPerMinute = Collections.synchronizedList(new ArrayList<>());
    OffsetDateTime pauseStarted = null;
    long minPausePeriodMs = 0;
    long maxPausePeriodMs = 0;
    long pauseCount = 0;
    long cumulativePauseMs = 0;

    Supplier<Integer> queuedRequestsCount = null;

    @Builder
    private RateLimiterMonitor(
            Supplier<Integer> queuedRequestsCount
    ){
        this.queuedRequestsCount = queuedRequestsCount;
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

    private final List<Long> requestsDurationsPerMinute = Collections.synchronizedList(new ArrayList<>());

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




    private final String logPausesTemplate = "[RATE LIMITER]: %s new pauses, %s pauses total, max = %s ms, min = %s ms, avg = %s ms, median = %s ms";
    private final String logRequestsDoneTemplate = "[RATE LIMITER]: %s delayed requests processed, avg wait = %s ms, median = %s ms";
    private final String logQueueTemplate = "[RATE LIMITER]: %s requests delayed in queue, max queue length %s";

    private long oldPausesCount = 0;

    public void reportStatsPerMinute(){

        long avgPauseMs = 0;
        long medianPause = 0;
        if (pausesPerMinute.size()>0){

            List<Long> pausesPerMinuteSorted = pausesPerMinute.stream().sorted().toList();

            avgPauseMs = pausesPerMinuteSorted.stream().reduce(0L, Long::sum) / pausesPerMinuteSorted.size();

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

        oldPausesCount = newPausesTotal;
        pausesPerMinute.clear();

        long avgRqWaitMs = 0;
        long medianRqWaitMs = 0;

        if (requestsDurationsPerMinute.size() > 0){
            List<Long> rqWaitSorted = requestsDurationsPerMinute.stream().sorted().toList();
            avgRqWaitMs = rqWaitSorted.stream().reduce(0L, Long::sum) / rqWaitSorted.size();
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
    }

}
