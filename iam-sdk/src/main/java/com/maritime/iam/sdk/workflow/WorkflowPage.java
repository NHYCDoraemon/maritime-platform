package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Pagination envelope for workflow-lookup results. Independent of
 * {@code com.maritime.platform.common.core.page.PageResult} so
 * consumers who don't depend on platform-common-core can still read
 * workflow responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowPage<T>(
        List<T> records,
        long total,
        int pageNo,
        int pageSize) {

    public WorkflowPage {
        records = records == null ? List.of() : List.copyOf(records);
    }

    public static <T> WorkflowPage<T> empty(int pageNo, int pageSize) {
        return new WorkflowPage<>(List.of(), 0L, pageNo, pageSize);
    }
}
