package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters;

import lombok.Builder;
import lombok.Generated;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    public void registerFeature(
            Class inputType,
            Function featureFunction
    ){
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
                    String rqId = UUID.randomUUID().toString();

                    if (monoProvider != null){
                        Mono initActionOutput = monoProvider.apply(rqId);

                        initActionOutput.subscribe(
                                result -> {
                                    // remove action from wait list
                                    stopWaiting(actionName);
                                    // remove action from in progress list
                                    progressCompleted(actionName);
                                    // remember completed action name
                                    actionsComplete.add(actionName);
                                    log.info("[INIT]: initialize action "+actionName+" completed");

                                    invokeWaitingActions();
                                },
                                error -> {
                                    log.info("[INIT FAIL]: initialize action "+actionName+" failed");
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
                .flatMap(entry -> entry.getValue().stream())
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

        return monoProvider.apply(wireId);
    }

    public void receiveSingleRequest(String rqId, Object payload){
        if (incomingMsgSink != null){
            incomingMsgSink.accept(rqId,payload);
        }
    }

}
