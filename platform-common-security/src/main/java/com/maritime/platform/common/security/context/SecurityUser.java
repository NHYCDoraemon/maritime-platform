package com.maritime.platform.common.security.context;

import java.util.Collections;
import java.util.List;

/**
 * Immutable user context carried through the request lifecycle.
 *
 * <p>The {@code systemScope} list is defensively copied to an
 * unmodifiable list on construction.
 */
public record SecurityUser(
        String userId,
        String userName,
        String activeOrgCode,
        List<String> systemScope,
        String sessionId
) {

    public SecurityUser {
        systemScope = systemScope == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(systemScope);
    }
}
