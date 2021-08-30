package com.example.graalvmjacypt;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@NativeHint(
        types = @TypeHint(
                types = {
                        com.ibm.icu.text.Normalizer.class
                }),
        resources = @ResourceHint(patterns = "com/ibm/icu/impl/data/icudt67b.*")
)
@SpringBootApplication
public class GraalvmJacyptApplication {
    private static final Class<?>[] AUTO_CONFIG_CLASSES = {
            WebFluxAutoConfiguration.class,
            ReactiveWebServerFactoryAutoConfiguration.class,
            HttpHandlerAutoConfiguration.class,
            ErrorWebFluxAutoConfiguration.class,
            ErrorWebExceptionHandler.class
    };

    public static void main(String[] args) {
        appBuild().run(args);
    }

    private static SpringApplication appBuild() {
        return new SpringApplicationBuilder()
                .sources(AUTO_CONFIG_CLASSES)
                .initializers((GenericApplicationContext ctx) -> {
                    final String playEncKey = "playEnc";
                    final String prodEncKey = "prodEnc";

                    Environment environment = ctx.getEnvironment();

                    ctx.registerBean(prodEncKey, BasicTextEncryptor.class, () -> {
                        BasicTextEncryptor encryptor = new BasicTextEncryptor();
                        encryptor.setPassword(Objects.requireNonNull(environment.getProperty("jacypt.prod.password")));
                        return encryptor;
                    });

                    ctx.registerBean(playEncKey, BasicTextEncryptor.class, () -> {
                        BasicTextEncryptor encryptor = new BasicTextEncryptor();
                        encryptor.setPassword(Objects.requireNonNull(environment.getProperty("jacypt.play.password")));
                        return encryptor;
                    });

                    ctx.registerBean("mainRouter", RouterFunction.class, () -> {
                        BasicTextEncryptor productionEncryptor = ctx.getBean(prodEncKey, BasicTextEncryptor.class);
                        BasicTextEncryptor playEncryptor = ctx.getBean(playEncKey, BasicTextEncryptor.class);
                        return route().POST("/api/v1/encrypt", serverRequest -> ServerResponse.ok().body(Mono.defer(() -> {
                                    String enc = serverRequest.queryParam("encType").orElse("").toLowerCase();
                                    String value = serverRequest.queryParam("value").
                                            orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "value is missing"));

                                    switch (enc) {
                                        case "play":
                                            return Mono.just(playEncryptor.encrypt(value));
                                        case "production":
                                            return Mono.just(productionEncryptor.encrypt(value));
                                        default:
                                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown encType"));
                                    }
                                }), String.class))
                                .build();
                    });

                    ctx.registerBean("resourceRouter", RouterFunction.class, () -> RouterFunctions.resources("/**", new ClassPathResource("/")));

                })
                .build();
    }
}
