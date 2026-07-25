package com.maritime.iam.sdk.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowUserContextViewCompatibilityTest {

    @Test
    void legacySixFieldConstructor_keepsNewCollectionsEmpty() {
        WorkflowUserContextView view = new WorkflowUserContextView(
                "user-1",
                "User One",
                "org-1",
                List.of("root", "org-1"),
                "position-1",
                List.of("role-1")
        );

        assertThat(view.orgUnits()).isEmpty();
        assertThat(view.duties()).isEmpty();
        assertThat(view.regionCodes()).isEmpty();
    }
}
