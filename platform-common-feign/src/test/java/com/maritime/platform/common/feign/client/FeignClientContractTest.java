package com.maritime.platform.common.feign.client;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compile-level contract test that guards against accidental renaming of
 * FeignClient name attributes, which would break Nacos service resolution.
 * No Spring context required — pure reflection.
 */
class FeignClientContractTest {

    @Test
    void internalIamQueryClient_isInterfaceWithCorrectFeignClientName() {
        assertTrue(InternalIamQueryClient.class.isInterface(),
                "InternalIamQueryClient must be an interface");

        FeignClient annotation = InternalIamQueryClient.class.getAnnotation(FeignClient.class);
        assertNotNull(annotation, "InternalIamQueryClient must be annotated with @FeignClient");
        assertEquals("iam-query-service", annotation.name(),
                "FeignClient name must be 'iam-query-service' for Nacos resolution");
    }

    @Test
    void internalIamAuthClient_isInterfaceWithCorrectFeignClientName() {
        assertTrue(InternalIamAuthClient.class.isInterface(),
                "InternalIamAuthClient must be an interface");

        FeignClient annotation = InternalIamAuthClient.class.getAnnotation(FeignClient.class);
        assertNotNull(annotation, "InternalIamAuthClient must be annotated with @FeignClient");
        assertEquals("iam-auth-service", annotation.name(),
                "FeignClient name must be 'iam-auth-service' for Nacos resolution");
    }
}
