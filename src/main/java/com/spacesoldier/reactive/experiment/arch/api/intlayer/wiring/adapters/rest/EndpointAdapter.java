package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.model.*;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.springframework.web.reactive.function.server.ServerResponse.status;

@Slf4j
public class EndpointAdapter {

    private Map<Class, Function<RestRequestEnvelope,Object>> requestExtractors = new HashMap<>();

    private Map<Class, Function<Object,Mono<ServerResponse>>> responseProcessors = new HashMap<>();

    @NonNull
    private Function<String, Mono> singleRequestsReceiver;

    @NonNull
    private BiConsumer<String, Object> intlayerInputSink;

    private BiConsumer<String, Object> asyncErrorSink;

    private String redefineIOBindingWarning = "[WARN]: binding for %s already esxists - the %s, will replace it with %s";

    public void registerRequestBuilder(
            Class extractType,
            Function<RestRequestEnvelope,Object> extractor
    ){
        if (!requestExtractors.containsKey(extractType)){
            requestExtractors.put(extractType, extractor);
        }
    }

    public void registerResponseProcessor(Class responseType, Function responseProcessor){
        if (!responseProcessors.containsKey(responseType)){
            responseProcessors.put(responseType,responseProcessor);
        }
    }

    @Builder
    private EndpointAdapter(
            Function<String,Mono> monoProvider,
            BiConsumer<String, Object> requestSink,
            BiConsumer<String, Object> errorSink
    ){
        this.singleRequestsReceiver = monoProvider;
        this.intlayerInputSink = requestSink;
        this.asyncErrorSink = errorSink;
    }

    private Function<Object, RestRequestEnvelope> wrapRequestEnvelope(ServerRequest req){

        return reqBody -> {
            RestRequestEnvelope env =
                                        RestRequestEnvelope.builder()
                                                    .requestId(req.exchange().getLogPrefix())
                                                    .pathVariables(req.pathVariables())
                                                    .queryParams(req.queryParams())
                                                    .payload(reqBody)
                                                .build();

            return env;
        };
    }

    private String errorLogTemplate = "A problem with request %s occurred: %s";

    private Consumer dropRequestToIntlayer(String rqId){

        return request -> {
            if (request.getClass() == RestResponseEnvelope.class){
                if (asyncErrorSink != null){
                    asyncErrorSink.accept(rqId, request);
                }
            } else {
                if (intlayerInputSink != null){
                    intlayerInputSink.accept(rqId, request);
                } else {
                    if (asyncErrorSink != null){

                        RestResponseEnvelope errorResponse =
                                RestResponseEnvelope.builder()
                                    .status(OperationStatus.FAIL)
                                    .messages(
                                            new ArrayList<>()
                                            {{
                                                OperationMessage.builder()
                                                        .severity(MessageSeverity.CRITICAL)
                                                        .text(
                                                                "Rest endpoint adapter was not configured properly"
                                                        )
                                                        .build();
                                            }}
                                    )
                                .build();

                        asyncErrorSink.accept(rqId, errorResponse);
                    }
                }
            }
        };
    }

    private void parseRequest(
            String reqId,
            ServerRequest req,
            Class reqPayloadType,
            Class typeToForward
    ){

        Mono bodySource = null;

        if (req.method() != HttpMethod.GET){
            bodySource = req.bodyToMono(reqPayloadType);
        }

        if (bodySource != null){
            // do async stuff when we need to
            bodySource
                    .map        (   wrapRequestEnvelope     (req)                )
                    .map        (   prepareRequestInput     (typeToForward)     )
                    .subscribe  (   dropRequestToIntlayer   (reqId)              );
        } else {
            // when the request does not have a body things get simpler
            RestRequestEnvelope requestEnvelope = wrapRequestEnvelope(req).apply(null);
            Object requestToProcess = prepareRequestInput(typeToForward).apply(requestEnvelope);
            dropRequestToIntlayer(reqId).accept(requestToProcess);
        }
    }

    private Function<RestRequestEnvelope, Object> prepareRequestInput(
            Class inputPayloadType
    ){
        String requestPayloadTypeNotDefinedError = "Request payload type %s was not set up for processing";
        return envelope -> {

            Object result = null;

            if (requestExtractors.containsKey(inputPayloadType)){
                result = requestExtractors.get(inputPayloadType).apply(envelope);
            } else {

                result = RestResponseEnvelope.builder()
                        .status(OperationStatus.FAIL)
                        .messages(
                                new ArrayList<>()
                                    {{
                                        add(
                                                OperationMessage.builder()
                                                    .severity(MessageSeverity.CRITICAL)
                                                    .text(
                                                            String.format(
                                                                    requestPayloadTypeNotDefinedError,
                                                                    inputPayloadType
                                                            )
                                                    )
                                                .build()
                                        );
                                    }}
                        )
                        .build();
            }

            return result;
        };
    }

    private Function<Object, RestResponseEnvelope> prepareResponseEnvelope(
            Class typeToExpectFromLogic
    ){

        return response -> {
            Class responseType = response.getClass();
            RestResponseEnvelope output = null;

            if (responseType == RestResponseEnvelope.class){
                // bypass the response which came in ready state
                output = (RestResponseEnvelope) response;
            } else {
                // let's check if we received the expected stuff from internal logic
                if (responseType == typeToExpectFromLogic){
                    Object responsePayload = response;
                    // good, all systems go
                    if (responseProcessors.containsKey(responseType)){
                        responsePayload = responseProcessors.get(responseType).apply(response);
                    }

                    output = RestResponseEnvelope.builder()
                                                    .status(OperationStatus.OK)
                                                    .data(responsePayload)
                                                .build();
                } else {
                    // unexpected results from logic, let's report something went wrong
                    output = RestResponseEnvelope.builder()
                                                        .status(OperationStatus.PROBLEMS)
                                                        .data(response)
                                                    .build();
                }
            }

            return output;
        };
    }

    private Function<RestResponseEnvelope,RestResponseEnvelope> switchResponseStatusCode(ServerRequest request){

        Map<OperationStatus, HttpStatus> opStatusCorrelations = new HashMap<>(){{
            put(    OperationStatus.OK,         HttpStatus.OK                       );
            put(    OperationStatus.FAIL,       HttpStatus.INTERNAL_SERVER_ERROR    );
            put(    OperationStatus.PROBLEMS,   HttpStatus.FAILED_DEPENDENCY        );
        }};

        return envelope -> {

            if (envelope.getStatus() != OperationStatus.OK){
                if (opStatusCorrelations.containsKey(envelope.getStatus())){
                    request.exchange().getResponse()
                                            .setStatusCode(
                                                    opStatusCorrelations.get(envelope.getStatus())
                                            );
                } else {
                    request.exchange().getResponse()
                                            .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            return envelope;
        };
    }

    private String adapterNotConfiguredErrTemplate = "REST endpoint adapter is not properly configured";

    /**
     * This method could be used in case when request body structure is the same
     * as the one which should be forwarded to the application logic
     *
     * @param requestBodyType
     * @param request
     * @return
     */
    public Mono<ServerResponse> forwardRequestToLogic(
            Class requestBodyType,
            ServerRequest request,
            Class typeToExpectFromLogic
    ){
        return forwardRequestToLogic(
                                        requestBodyType,
                                        requestBodyType,
                                        request,
                                        typeToExpectFromLogic
                                    );
    }

    /**
     *  This method connects the adapter and the router functions configuration
     *
     * @param restRequestBodyType - the type of request payload which will be received as a payload of REST request
     * @param typeToForwardToLogic - the type which will be forwarded inside the application logic
     * @param request - ServerRequest which was received by REST endpoint
     * @return
     */
    public Mono<ServerResponse> forwardRequestToLogic(
            Class restRequestBodyType,
            Class typeToForwardToLogic,
            ServerRequest request,
            Class typeToExpectFromLogic
    ){

        Mono<ServerResponse> output = null;

        String requestId = request.exchange().getLogPrefix();

        if (singleRequestsReceiver != null){
                Mono responsePublisher =
                        singleRequestsReceiver.apply    (   requestId                                           )
                                                .map    (   prepareResponseEnvelope     (typeToExpectFromLogic) )
                                                .map    (   switchResponseStatusCode    (request)               );


                output = status(200).body(responsePublisher, RestResponseEnvelope.class);

                parseRequest(requestId, request, restRequestBodyType, typeToForwardToLogic);

        } else {
                output = status(500).body(
                        adapterNotConfiguredErrTemplate, String.class);
        }

        return output;
    }

}
