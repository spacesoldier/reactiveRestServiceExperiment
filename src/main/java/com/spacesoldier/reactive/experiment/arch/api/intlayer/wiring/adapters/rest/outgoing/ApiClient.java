package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing;

import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.ApiCallSpec;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.client.CollectionFormat;
import com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model.client.QueryParamConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class ApiClient {

    private Function<QueryParamConfig,MultiValueMap<String, String>> paramToMVMapConverter;

    public MultiValueMap<String, String> parameterToMultiValueMap(
            CollectionFormat collectionFormat,
            String name,
            Object value
    ) {
        MultiValueMap<String, String> result = null;

        if (paramToMVMapConverter != null){
            result = paramToMVMapConverter.apply(
                    QueryParamConfig.builder()
                                .collectionFormat(collectionFormat)
                                .name(name)
                                .value(value)
                            .build()
            );
        }

        return result;
    }

    private Function<String[],List<MediaType>> headersToMediaTypeConverter;

    public List<MediaType> selectHeaderAccept (String [] accepts){
        List<MediaType> result = null;

        if (headersToMediaTypeConverter != null){
            result = headersToMediaTypeConverter.apply(accepts);
        }

        return result;
    }

    private Function<String[],MediaType> headerContentTypeConverter;

    public MediaType selectHeaderContentType(String[] contentTypes){
        // by default why not
        MediaType result = MediaType.APPLICATION_JSON;

        if (headersToMediaTypeConverter != null){
            result = headerContentTypeConverter.apply(contentTypes);
        }

        return result;
    }


    private Function<ApiCallSpec, ResponseSpec> apiCallClientProxy;

    public <T> ResponseSpec invokeAPI(
            String path,
            HttpMethod method,
            Map<String, Object> pathParams,
            MultiValueMap<String, String> queryParams,
            Object body,
            HttpHeaders headerParams,
            MultiValueMap<String, String> cookieParams,
            MultiValueMap<String, Object> formParams,
            List<MediaType> accept,
            MediaType contentType,
            String[] authNames,
            ParameterizedTypeReference<T> returnType
    ) throws RestClientException
    {
        ResponseSpec responseSpec = null;

        if (apiCallClientProxy != null){
            responseSpec = apiCallClientProxy.apply(
                        ApiCallSpec.<T>builder()
                                    .path(path)
                                    .method(method)
                                    .pathParams(pathParams)
                                    .queryParams(queryParams)
                                    .body(body)
                                    .headerParams(headerParams)
                                    .cookieParams(cookieParams)
                                    .formParams(formParams)
                                    .accept(accept)
                                    .contentType(contentType)
                                    .authNames(authNames)
                                    .returnType(returnType)
                                .build()
            );
        }

        return responseSpec;
    }

}
