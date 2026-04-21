package com.maritime.platform.common.tenant.aspect;

import com.maritime.platform.common.tenant.annotation.RequireTenantContext;
import com.maritime.platform.common.tenant.config.TenantAutoConfiguration;
import com.maritime.platform.common.tenant.context.TenantContext;
import com.maritime.platform.common.tenant.exception.TenantContextMissingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {TenantAutoConfiguration.class, RequireTenantContextAspectTest.TestService.class})
class RequireTenantContextAspectTest {

    @Autowired
    private TestService testService;

    @Test
    void invoke_withActiveTenant_succeeds() {
        TenantContext.set("tenant-1");
        try {
            assertThat(testService.annotatedMethod()).isEqualTo("done");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void invoke_withoutTenant_throwsException() {
        TenantContext.clear();
        assertThatThrownBy(() -> testService.annotatedMethod())
                .isInstanceOf(TenantContextMissingException.class);
    }

    @Test
    void invoke_withBlankTenant_throwsException() {
        TenantContext.set("");
        try {
            assertThatThrownBy(() -> testService.annotatedMethod())
                    .isInstanceOf(TenantContextMissingException.class);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void invoke_withoutAnnotation_noCheck() {
        TenantContext.clear();
        assertThat(testService.unannotatedMethod()).isEqualTo("unchecked");
    }

    @Service
    static class TestService {
        @RequireTenantContext
        public String annotatedMethod() {
            return "done";
        }

        public String unannotatedMethod() {
            return "unchecked";
        }
    }
}
