package com.maritime.iam.sdk.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IamSdkCacheKeysTest {

    @Test
    void permissionCodesUsesStableLegacyCompatibleKey() {
        assertThat(IamSdkCacheKeys.permissionCodes(
                "TODO", "user-1", "ORG-1"))
                .isEqualTo("iam:perms:TODO:user-1:ORG-1");
    }

    @Test
    void missingOrganizationUsesCanonicalPlaceholder() {
        assertThat(IamSdkCacheKeys.permissionCodes(
                "TODO", "user-1", null))
                .isEqualTo("iam:perms:TODO:user-1:_NO_ORG_");
    }

    @Test
    void systemPatternsCoverAllSupportedBusinessCaches() {
        assertThat(IamSdkCacheKeys.navSystemPattern("TODO"))
                .isEqualTo("biz:nav:TODO:*");
        assertThat(IamSdkCacheKeys.pageSystemPattern("TODO"))
                .isEqualTo("biz:page:TODO:*");
        assertThat(IamSdkCacheKeys
                .permissionCodesSystemPattern("TODO"))
                .isEqualTo("iam:perms:TODO:*");
    }
}
