package io.myforevermusic.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyForeverMusicApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyForeverMusicApiApplication.class, args);
    }
}
