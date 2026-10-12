package com.realtime.delivery.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * @LoadBalanced lets us call other services by their Eureka application
     * name (e.g. "http://delivery-service/...") instead of a hardcoded
     * host:port - Spring Cloud LoadBalancer resolves it at call time.
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
