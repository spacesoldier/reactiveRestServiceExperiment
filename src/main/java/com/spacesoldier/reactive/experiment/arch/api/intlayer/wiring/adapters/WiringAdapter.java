package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class WiringAdapter {
    @NonNull
    private BiConsumer<String, Object> incomingMsgSink;
    @NonNull
    private BiConsumer<Class, Function> routableFunctionSink;

    @NonNull
    private Function<String, Mono> monoProvider;

    @Builder
    private WiringAdapter(
            BiConsumer<String, Object> incomingMsgSink,
            Function<String,Mono> monoProvider,
            BiConsumer<Class,Function> routableFunctionSink
    ){
        this.incomingMsgSink = incomingMsgSink;
        this.monoProvider = monoProvider;
        this.routableFunctionSink = routableFunctionSink;
    }

    Map<Class,Boolean> featureActiveFlags = new HashMap<>();

    public void registerFeature(
            Class inputType,
            Function featureFunction
    ){
        if (routableFunctionSink != null){
            routableFunctionSink.accept(inputType, featureFunction);
            featureActiveFlags.put(inputType,Boolean.TRUE);
        }
    }

    Map<Class, Set<String>> requestProcessDependencies = new HashMap<>();

    public boolean typeProcessHasDependencies(Class typeToProcess){
        return requestProcessDependencies.containsKey(typeToProcess);
    }

    public boolean isProcessDependenciesReady(Class typeToProcess){
        boolean result = true;

        if (typeProcessHasDependencies(typeToProcess)){
            result = actionsComplete.containsAll(requestProcessDependencies.get(typeToProcess));
        }

        return result;
    }

    public void activateFeature(Class typeToProcess){
        if (featureActiveFlags.containsKey(typeToProcess)){
            featureActiveFlags.put(typeToProcess,Boolean.TRUE);
            log.info("[WIRING]: processing the "+typeToProcess.getSimpleName()+" activated");
        }
    }

    public boolean featureIsActive(Class typeToProcess){
        boolean isActive = false;

        if (featureActiveFlags.containsKey(typeToProcess)){
            // normally we obtain true
            isActive = featureActiveFlags.get(typeToProcess);
        }

        return isActive;
    }
    public void registerFeature(
            Class inputType,
            Function featureFunction,
            Set<String> dependsOn
    ){
        if (!typeProcessHasDependencies(inputType)){
            requestProcessDependencies.put(inputType,dependsOn);
            featureActiveFlags.put(inputType,Boolean.FALSE);
        }

        if (routableFunctionSink != null){
            routableFunctionSink.accept(inputType, featureFunction);
        }
    }

    // here is a map with the dependency requirements for each init action
    // the action could be invoked only when all dependencies are met
    Map<String, Set<String>> actionDependencies = new HashMap<>();

    // here are the list of actions in progress
    Set<String> actionsInProgress = new HashSet<>();

    // here are the actions which had been completed
    // to match with dependency requirements of other init actions
    Set<String> actionsComplete = new HashSet<>();

    // here are the list of actions awaiting the requirements to be satisfied
    Set<String> actionsWaitList = new HashSet<>();

    private boolean actionHasDependencies(String actionName){
        return actionDependencies.containsKey(actionName);
    }

    private boolean actionRequirementsMet(String actionName){
        boolean result = true;

        if (actionHasDependencies(actionName)){
            Set<String> actionRequirements = actionDependencies.get(actionName);
            result = actionsComplete.containsAll(actionRequirements);
        }

        return result;
    }

    private boolean isActionInProgress(String actionName){
        return actionsInProgress.contains(actionName);
    }

    private void invokeWaitingActions(){
        actionsWaitList.forEach(
                actionName -> invokeInitAction(actionName,initActions.get(actionName))
        );
    }

    private Map<String, Object> pausedRequests = new ConcurrentHashMap<>();
    private Map<Class,List<String>> pausedRequestsIndex = new ConcurrentHashMap<>();

    private void addRequestToIndex(Class rqType, String rqId){
        if (!pausedRequestsIndex.containsKey(rqType)){
            pausedRequestsIndex.put(rqType, new ArrayList<>());
        }
        pausedRequestsIndex.get(rqType).add(rqId);
    }
    private void pauseRequest(String rqId, Object payload){
        if (!pausedRequests.containsKey(rqId)){
            pausedRequests.put(rqId, payload);
            addRequestToIndex(payload.getClass(),rqId);
        }
    }
    private void revokePausedRequests(String featureReady){
        Set<Class> featuresActivated = new HashSet<>();
        List<String> revokedRequests = pausedRequestsIndex.entrySet()
                .stream()
                .filter(entry -> isProcessDependenciesReady(entry.getKey()))
                .peek(entry -> {
                    activateFeature(entry.getKey());
                    log.info("[WIRING]: activate processing "+entry.getKey().getSimpleName());
                    featuresActivated.add(entry.getKey());
                })
                .flatMap(
                        entry -> entry.getValue().stream()
                )
                .map(
                        rqId -> {
                            receiveSingleRequest(rqId,pausedRequests.get(rqId));
                            log.info("[WIRING]: revoke request id "+rqId);
                            return rqId;
                        }
                )
                .toList();
        featuresActivated.forEach(
                activeType -> {
                    pausedRequestsIndex.remove(activeType);
                }
        );

        revokedRequests.forEach(
                rqId -> pausedRequests.remove(rqId)
        );
    }

    private void activateFeaturesByDependencies(){
        Set<Class> featuresActivated = requestProcessDependencies.entrySet()
                                    .stream()
                                        .map    (   Map.Entry::getKey                               )
                                        .filter (   feature -> !featureIsActive(feature)            )
                                        .filter (   this::isProcessDependenciesReady                )
                                        .peek   (   this::activateFeature                           )
                                    .collect(Collectors.toSet());
        featuresActivated.forEach(
                feature -> log.info("[WIRING]: ready to receive "+feature.getSimpleName())
        );
    }

    private boolean isRequestInPausedQueue(String rqId){
        return pausedRequests.containsKey(rqId);
    }

    private final Scheduler monoPool = Schedulers.newSingle("init_thread");

    private void invokeInitAction(
            String actionName,
            Supplier initAction
    ){
        boolean clearToRun = true;
        if (actionHasDependencies(actionName)){
            if (!actionRequirementsMet(actionName)){
                actionsWaitList.add(actionName);
                clearToRun = false;
            }
        }

        if (clearToRun){

            if (!isActionInProgress(actionName)){
                if (incomingMsgSink != null){
                    String rqId = ">|< " + UUID.randomUUID();

                    if (monoProvider != null){
                        Mono initActionOutput = monoProvider.apply(rqId);

                        initActionOutput
                                // need to publish on a single thread to avoid work duplication
                                // and brain shocking consequences
                                .publishOn(monoPool)
                                .subscribe(
                                        result -> {
                                            // remove action from wait list
                                            stopWaiting(actionName);
                                            // remove action from in progress list
                                            progressCompleted(actionName);
                                            // remember completed action name
                                            actionsComplete.add(actionName);
                                            log.info("[INIT]: initialize action "+actionName+" completed");

                                            // now we can perform some waiting init actions
                                            invokeWaitingActions();

                                            // and perform any paused requests which may depend on
                                            // and were paused until required services are ready to run
                                            revokePausedRequests(actionName);

                                            // check and activate features
                                            // when no paused requests obtained during init stage
                                            activateFeaturesByDependencies();
                                        },
                                        error -> {
                                            log.info("[INIT FAIL]: initialize action "+actionName+" failed due to error "+error.getClass());
                                        }
                                );
                    }
                    actionsInProgress.add(actionName);
                    incomingMsgSink.accept(rqId,initAction.get());
                    log.info("[INIT]: invoke action \"" + actionName + "\"");
                }
            } else {
                log.info("[INIT REPEAT]: action "+actionName+" already in progress");
            }

        }

    }

    private void stopWaiting(String actionName) {
        if (actionsWaitList.contains(actionName)){
            actionsWaitList.remove(actionName);
        }
    }

    private void progressCompleted(String actionName) {
        if (actionsInProgress.contains(actionName)){
            actionsInProgress.remove(actionName);
        }
    }

    public Map<String, Supplier> initActions = new HashMap<>();

    public void registerInitAction(
            String actionName,
            Supplier initAction
    ){
        log.info("[WIRING]: register init action \"" + actionName + "\"");
        initActions.put(actionName,initAction);
    }

    public void registerInitAction(
            String actionName,
            Supplier initAction,
            Set<String> dependsOn
    ){
        if (!actionHasDependencies(actionName)){
            actionDependencies.put(actionName,dependsOn);
        }
        log.info("[WIRING]: register init action \"" + actionName + "\"");
        initActions.put(actionName,initAction);
    }

    public boolean isAllActionRequirementsCouldBeMet(){
        Set<String> allActions = initActions.keySet();
        Set<String> allRequirements = calcAllRequirementsSet();
        return allActions.containsAll(allRequirements);
    }

    @NotNull
    private Set<String> calcAllRequirementsSet() {
        Set<String> allRequirements = actionDependencies.entrySet()
                .stream()
                .flatMap(
                        entry -> entry.getValue().stream()
                )
                .collect(Collectors.toSet());
        return allRequirements;
    }

    public Runnable invokeInitActions(){
        return () -> {

            if (isAllActionRequirementsCouldBeMet()){
                initActions.forEach(
                        (name, action) -> invokeInitAction(name, action)
                );
            } else {
                Set<String> missingRequirements = calcAllRequirementsSet();
                missingRequirements.removeAll(initActions.keySet());
                String missingReqStr = missingRequirements.stream()
                        .reduce(
                                "",
                                (report, item) -> report + " "+item+", "
                        );

                log.info("[INIT ERROR]: could not start due to missing dependencies: "+missingReqStr);
            }


        };
    }

    public Mono initSingleRequest(String wireId){
        // we can initialize the request process chain regardless of the results of application initialization stage
        return monoProvider.apply(wireId);
    }

    private final Scheduler incomingRequests = Schedulers.newBoundedElastic(
            8,
            15000,
            "incoming requests"
    );

    String pauseProcessMsgTemplate = "[WIRING]: pause process of the request %s id %s";

    // request processing may depend on results of the application initialization stage results
    public void receiveSingleRequest(String rqId, Object payload){

        boolean clearToProcess = featureIsActive(payload.getClass());

        if (clearToProcess){
            if (incomingMsgSink != null){
                // it is important to start processing the messages in a separate thread pool
                // otherwise the current thread could be a bit overloaded by all messages coming here
                // and believe me this is the one of the key points of the whole application built on top
                incomingRequests.schedule(
                        () -> incomingMsgSink.accept(rqId,payload)
                );
            }
        } else {
            log.info(
                    String.format(
                            pauseProcessMsgTemplate,
                            payload.getClass().getSimpleName(),
                            rqId
                    )
            );
            if (!isRequestInPausedQueue(rqId)){
                pauseRequest(rqId,payload);
            } else {
                log.info("[WIRING]: request "+ rqId + " still on pause");
            }
        }
    }

}
