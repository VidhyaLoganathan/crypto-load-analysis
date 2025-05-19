
package com.cypher.cardload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    //coinbase url for base fetching
    private static final String BASE_RPC_URL = "https://mainnet.base.org";

    //export web3j as a bean
    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(BASE_RPC_URL));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*");
    }


}
