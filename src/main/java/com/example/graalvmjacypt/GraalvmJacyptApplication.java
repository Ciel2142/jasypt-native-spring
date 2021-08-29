package com.example.graalvmjacypt;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.nativex.hint.*;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@NativeHint(
        types = @TypeHint(
                types = {
                        java.text.Normalizer.class,
                        com.ibm.icu.text.Normalizer.class
                }),
        resources = {@ResourceHint(patterns = "com/ibm/icu/impl/data/icudt67b.*")}
)
@SpringBootApplication
public class GraalvmJacyptApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraalvmJacyptApplication.class, args);
    }

    @Bean(name = "prodTextEcn")
    BasicTextEncryptor productionTextEncryptor(@Value("${jacypt.prod.password}") String password) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
        return textEncryptor;
    }

    @Bean(name = "playTextEnc")
    BasicTextEncryptor playTextEncryptor(@Value("${jacypt.play.password}") String password) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword(password);
        return textEncryptor;
    }

    @Bean
    RouterFunction<?> resourceRoute() {
        return RouterFunctions.resources("/**", new ClassPathResource("/"));
    }

    @Bean
    RouterFunction<?> routerFunction(BasicTextEncryptor prodTextEcn, BasicTextEncryptor playTextEnc) {
        return route()
                .POST("/api/v1/encrypt", serverRequest -> ServerResponse.ok().body(Mono.defer(() -> {
                    EncType enc = EncType.valueOf(serverRequest.queryParam("encType")
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "encType is missing")));
                    String value = serverRequest.queryParam("value").
                            orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "value is missing"));

                    if (enc == EncType.PLAY) return Mono.just(playTextEnc.encrypt(value));
                    else return Mono.just(prodTextEcn.encrypt(value));
                }), String.class))
                .build();
    }
}

enum EncType {
    PRODUCTION,
    PLAY
}
