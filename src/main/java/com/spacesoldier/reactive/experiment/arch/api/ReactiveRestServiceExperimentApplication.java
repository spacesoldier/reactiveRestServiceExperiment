package com.spacesoldier.reactive.experiment.arch.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

@OpenAPIDefinition(info = @Info(title = "Reactive service Demo", version = "0.1", description = "Documentation APIs v0.1"))

public class ReactiveRestServiceExperimentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveRestServiceExperimentApplication.class, args);
	}

}
