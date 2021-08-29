package com.example.graalvmjacypt;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.nativex.hint.*;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Properties;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@NativeHint(
        trigger = org.jasypt.normalization.Normalizer.class,
        types = @TypeHint(
                types = {
                        java.text.Normalizer.class,
                        com.ibm.icu.text.Normalizer.class
                }),
        resources = {
                @ResourceHint(patterns = "com/ibm/icu/impl/data/icudt67b/.*"),
                @ResourceHint(patterns = "com/ibm/icu/impl/data/icudt67b.*"),
        },
        initialization = @InitializationHint(types = org.jasypt.normalization.Normalizer.class,
                initTime = InitializationTime.BUILD)
)
@SpringBootApplication
public class GraalvmJacyptApplication {
    private static final Class<?>[] AUTO_CONFIG_CLASSES = {
            WebFluxAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            ReactiveWebServerFactoryAutoConfiguration.class,
            HttpHandlerAutoConfiguration.class,
            ErrorWebFluxAutoConfiguration.class,
            ErrorWebExceptionHandler.class
    };

    private static final String PLAY_ENCRYPTOR = "playEncryptor";
    private static final String PRODUCTION_ENCRYPTOR = "productionEncryptor";

    public static void main(String[] args) {
        appBuild().run(args);
    }

    private static SpringApplication appBuild() {
        return new SpringApplicationBuilder()
                .sources(AUTO_CONFIG_CLASSES)
                .initializers((GenericApplicationContext ctx) -> {
                    ctx.registerBean("appProps", Properties.class, () -> {
                        Properties properties = new Properties();
                        try {
                            properties.load(GraalvmJacyptApplication.class
                                    .getClassLoader()
                                    .getResourceAsStream("application.properties"));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return properties;
                    });

                    ctx.registerBean(RouterFunction.class,
                            () -> RouterFunctions.resources("/**", new ClassPathResource("/")));

                    ctx.registerBean(PRODUCTION_ENCRYPTOR, BasicTextEncryptor.class, () -> {
                        Properties properties = ctx.getBean("appProps", Properties.class);
                        BasicTextEncryptor encryptor = new BasicTextEncryptor();
                        encryptor.setPassword(properties.getProperty("jacypt.prod.password"));
                        return encryptor;
                    });

                    ctx.registerBean(PLAY_ENCRYPTOR, BasicTextEncryptor.class, () -> {
                        Properties properties = ctx.getBean("appProps", Properties.class);
                        BasicTextEncryptor encryptor = new BasicTextEncryptor();
                        encryptor.setPassword(properties.getProperty("jacypt.play.password"));
                        return encryptor;
                    });

                    ctx.registerBean(RouterFunction.class, () -> {
                        BasicTextEncryptor productionEncryptor = ctx.getBean(PRODUCTION_ENCRYPTOR, BasicTextEncryptor.class);
                        BasicTextEncryptor playEncryptor = ctx.getBean(PLAY_ENCRYPTOR, BasicTextEncryptor.class);
                        return route()
                                .POST("/api/v1/encrypt", serverRequest -> ServerResponse.ok().body(Mono.defer(() -> {
                                    EncType enc = EncType.valueOf(serverRequest.queryParam("encType")
                                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "encType is missing")));
                                    String value = serverRequest.queryParam("value").
                                            orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "value is missing"));

                                    if (enc == EncType.PLAY) return Mono.just(playEncryptor.encrypt(value));
                                    else return Mono.just(productionEncryptor.encrypt(value));

                                }), String.class))
                                .build();
                    });
                })
                .build();
    }
}

enum EncType {
    PRODUCTION,
    PLAY
}