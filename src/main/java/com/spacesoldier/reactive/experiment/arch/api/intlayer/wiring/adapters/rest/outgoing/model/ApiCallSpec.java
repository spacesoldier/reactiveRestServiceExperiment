package com.spacesoldier.reactive.experiment.arch.api.intlayer.wiring.adapters.rest.outgoing.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Data @Builder
public class ApiCallSpec<T> {
    String path;
    HttpMethod method;
    Map<String, Object> pathParams;
    MultiValueMap<String, String> queryParams;
    Object body;
    HttpHeaders headerParams;
    MultiValueMap<String, String> cookieParams;
    MultiValueMap<String, Object> formParams;
    List<MediaType> accept;
    MediaType contentType;
    String[] authNames;
    ParameterizedTypeReference<T> returnType;

//    @Builder
//    private ApiCallSpec (
//                String path,
//                HttpMethod method,
//                Map<String, Object> pathParams,
//                MultiValueMap<String, String> queryParams,
//                Object body,
//                HttpHeaders headerParams,
//                MultiValueMap<String, String> cookieParams,
//                MultiValueMap<String, Object> formParams,
//                List<MediaType> accept,
//                MediaType contentType,
//                String[] authNames,
//                ParameterizedTypeReference<T> returnType
//            ){
//                this.path = path;
//                this.method = method;
//                this.pathParams = pathParams;
//                this.queryParams = queryParams;
//                this.body = body;
//                this.headerParams = headerParams;
//                this.cookieParams = cookieParams;
//                this.formParams = formParams;
//                this.accept = accept;
//                this.contentType = contentType;
//                this.authNames = authNames;
//                this.returnType = returnType;
//            }

}
