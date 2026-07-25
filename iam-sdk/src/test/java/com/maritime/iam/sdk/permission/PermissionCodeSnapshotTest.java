package com.maritime.iam.sdk.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionCodeSnapshotTest {

    @Test
    void parseTrimsAndDeduplicatesWhilePreservingOrder() {
        PermissionCodeSnapshot snapshot =
                PermissionCodeSnapshot.parse(
                        "1.7",
                        "todo:read, todo:write,todo:read");

        assertThat(snapshot.version()).isEqualTo("1.7");
        assertThat(snapshot.codes()).containsExactly(
                "todo:read", "todo:write");
        assertThat(snapshot.headerValue())
                .isEqualTo("todo:read,todo:write");
    }

    @Test
    void emptyValueProducesCacheableEmptySnapshot() {
        PermissionCodeSnapshot snapshot =
                PermissionCodeSnapshot.parse("8", "");

        assertThat(snapshot.version()).isEqualTo("8");
        assertThat(snapshot.codes()).isEmpty();
        assertThat(snapshot.headerValue()).isEmpty();
    }
}
