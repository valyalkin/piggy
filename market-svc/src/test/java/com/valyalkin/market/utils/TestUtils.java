package com.valyalkin.market.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    public static String readFileFromResources(String path) {
        ClassPathResource resource = new ClassPathResource(path);

        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }
}
