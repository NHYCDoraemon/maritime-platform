package com.maritime.platform.common.feign.client;

import com.maritime.platform.common.core.result.R;
import com.maritime.platform.common.feign.dto.SecurityUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Intra-cluster Feign client for calling iam-auth-service via Nacos service discovery (lb://). */
@FeignClient(name = "iam-auth-service", contextId = "internalIamAuthClient")
public interface InternalIamAuthClient {

    @PostMapping("/api/internal/token/validate")
    R<SecurityUserDTO> validateToken(@RequestParam("token") String token);
}
