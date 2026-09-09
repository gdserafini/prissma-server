package br.pucpr.prissma_server.genai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAIImageProperties.class)
public class OpenAIImageConfig {

    @Bean
    RestClient openAIImageRestClient(RestClient.Builder builder, OpenAIImageProperties properties) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
