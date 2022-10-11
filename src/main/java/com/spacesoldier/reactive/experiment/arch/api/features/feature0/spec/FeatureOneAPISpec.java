package com.spacesoldier.reactive.experiment.arch.api.features.feature0.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})

@RouterOperations({
        @RouterOperation(
                method = RequestMethod.GET,
                operation = @Operation(
                        description = "feature one",
                        operationId = "getFeatureOneRs",
                        tags = "feature0",
                        responses = {
                                @ApiResponse(
                                        responseCode = "200",
                                        description = "infopanel response",
                                        content = {
                                                @Content(
                                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                        schema = @Schema(
                                                                implementation = FeatureOneResponseSchema.class
                                                        )
                                                )
                                        }
                                )
                        }
                )
        )
})

public @interface FeatureOneAPISpec { }
