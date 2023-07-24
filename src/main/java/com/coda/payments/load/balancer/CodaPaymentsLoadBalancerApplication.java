package com.coda.payments.load.balancer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

@SpringBootApplication
@RestController
public class CodaPaymentsLoadBalancerApplication {

	private final Logger logger = LoggerFactory.getLogger(CodaPaymentsLoadBalancerApplication.class);
	private final WebClient.Builder loadBalancedWebClientBuilder;
	private final ReactorLoadBalancerExchangeFilterFunction lbFunction;

	public CodaPaymentsLoadBalancerApplication(WebClient.Builder webClientBuilder,
						   ReactorLoadBalancerExchangeFilterFunction lbFunction) {
		this.loadBalancedWebClientBuilder = webClientBuilder;
		this.lbFunction = lbFunction;
	}

	public static void main(String[] args) {
		SpringApplication.run(CodaPaymentsLoadBalancerApplication.class, args);
	}

	@RequestMapping(value = "/route", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Mono<String>> routeRequest(@RequestBody String request) {
		logger.info("Routing request : {}", request);
		if (isRequestValidJson(request)) {
			return new ResponseEntity<>(loadBalancedWebClientBuilder.build().post()
					.uri("http://routing/response")
					.body(Mono.just(request), String.class)
					.retrieve()
					.bodyToMono(String.class), HttpStatus.OK);
		}
		return new ResponseEntity<>(Mono.just("{\"message\":\"Invalid request format\"}"), HttpStatus.BAD_REQUEST);
	}

	private boolean isRequestValidJson(String jsonString ) {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(jsonString);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
