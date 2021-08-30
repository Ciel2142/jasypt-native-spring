package com.example.graalvmjacypt;

import org.jasypt.util.text.BasicTextEncryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class GraalvmJacyptApplicationTests {

    @Test
    void contextLoad() {
    }

    @Test
    void encryptor() {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword("test");
        String word = UUID.randomUUID().toString();
        String t = encryptor.encrypt(word);
        Assertions.assertEquals(word, encryptor.decrypt(t));
    }
}
