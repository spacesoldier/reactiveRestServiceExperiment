package com.spacesoldier.reactive.experiment.arch.api.intlayer.config;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.routing.RoutingHelper;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.incoming.EndpointAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.WiringAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.ApiClient;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.ApiClientAdapter;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.ApiClientImpl;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.tools.bandwidth.TokenBucketRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class IntLayerConfig {

    @Autowired
    WiringAdapter wiringAdapter;

    @Autowired
    RoutingHelper routingHelper;

    @Bean
    public EndpointAdapter initEndpointAdapter(){

        return EndpointAdapter.builder()
                                .monoProvider(
                                        rqId -> wiringAdapter.initSingleRequest(rqId)
                                )
                                .requestSink(
                                        (rqId, payload) -> wiringAdapter.receiveSingleRequest(rqId,payload)
                                )
                                .requestPrioritySetter(
                                        (rqId, priority) -> routingHelper.defineRequestPriority(rqId,priority)
                                )
                .build();
    }

    @Autowired
    private ApiClientImpl apiClientImplementation;

    @Autowired
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @Bean
    public ApiClientAdapter initApiClientAdapter(){
        return ApiClientAdapter.builder()
                .errorHandlerSink(  apiClientImplementation.errorHandlerSink()  )
                .routableFunctionSink(
                        (rqType, handler) -> wiringAdapter.registerFeature(rqType,handler)
                )
                .bandwidthControllerInput(
                        tokenBucketRateLimiter.takeControlOverTransmission()
                )
                .build();

    }

    @Bean
    public ApiClient buildApiClient(){
        return ApiClient.builder()
                .paramToMVMapConverter(
                        queryParamConfig -> apiClientImplementation.parameterToMultiValueMap(
                                                                                queryParamConfig.getCollectionFormat(),
                                                                                queryParamConfig.getName(),
                                                                                queryParamConfig.getValue()
                                                                        )
                )
                .headersToMediaTypeConverter(
                        accepts -> apiClientImplementation.selectHeaderAccept(accepts)
                )
                .headerContentTypeConverter(
                        contentTypes -> apiClientImplementation.selectHeaderContentType(contentTypes)
                )
                .apiCallClientProxy(
                        apiClientImplementation.invokeAPIfnWrapper()
                )
              .build();
    }
}
