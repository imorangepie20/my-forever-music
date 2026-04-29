package io.myforevermusic.api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI myForeverMusicOpenApi() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("My Forever Music API")
                    .version("v1")
                    .description("Spring Boot API scaffold for My Forever Music")
                    .contact(
                        new Contact()
                            .name("My Forever Music Team")
                    )
            );
    }
}
