
package com.cypher.cardload.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    private static final String[] BASE_RPC_URLS = {
            "https://mainnet.base.org",
            "https://base.llamarpc.com",
            "https://base-mainnet.public.blastapi.io",
            "https://base-rpc.publicnode.com",
            "https://1rpc.io/base"
    };

    private static int currentRpcIndex = 0;

//    @Bean
//    public Web3j web3j() {
//        return createWeb3jWithFallback();
//    }

//    private Web3j createWeb3jWithFallback() {
//        String rpcUrl = BASE_RPC_URLS[currentRpcIndex];
//        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
//
//        try {
//            // Test connection
//            web3j.ethBlockNumber().send();
//            return web3j;
//        } catch (Exception e) {
//            // Try next RPC endpoint
//            currentRpcIndex = (currentRpcIndex + 1) % BASE_RPC_URLS.length;
//            System.out.println("\n\n\n\n\n\n\n\n");
//
//            System.out.println("Current RPC INDEX : "+currentRpcIndex);
//            if (currentRpcIndex == 0) {
//                // We've tried all endpoints
//                throw new RuntimeException("Failed to connect to any Base RPC endpoint", e);
//            }
//            return createWeb3jWithFallback();
//        }
//    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    //TODO : ADD caffine cache - moved to CacheConfig
//    @Bean
//    public CacheManager cacheManager() {
//        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
//        cacheManager.setCaffeine(Caffeine.newBuilder()
//                .expireAfterWrite(Duration.ofHours(1))
//                .maximumSize(1000));
//        return cacheManager;
//    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*");
    }


}
