package com.maritime.platform.common.feign.client;

import com.maritime.platform.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Intra-cluster Feign client for calling iam-query-service via Nacos service discovery (lb://). */
@FeignClient(name = "iam-query-service", contextId = "internalIamQueryClient")
public interface InternalIamQueryClient {

    @GetMapping("/api/internal/permissions/check")
    R<Boolean> checkPermission(@RequestParam("userId") String userId,
                               @RequestParam("systemCode") String systemCode,
                               @RequestParam("resourceCode") String resourceCode);
}
