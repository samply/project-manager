package de.samply.cache;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheConfigurationTest {
    @Test
    void usesConfiguredPolicyAndSafeDefault() {
        CacheConfiguration configuration = new CacheConfiguration(60, 3600);
        EnumMap<CacheResource, CachePolicy> policies = new EnumMap<>(CacheResource.class);
        policies.put(CacheResource.BACKEND_ASSETS, CachePolicy.SHORT);
        configuration.setConfig(policies);

        assertThat(configuration.policy(CacheResource.BACKEND_ASSETS)).isEqualTo(CachePolicy.SHORT);
        assertThat(configuration.policy(CacheResource.PROJECT_DETAIL)).isEqualTo(CachePolicy.NO_STORE);
        assertThat(configuration.policy(CacheResource.BACKEND_ASSETS)).isEqualTo(CachePolicy.SHORT);
        assertThat(configuration.policy(CacheResource.PUBLIC_INFORMATION)).isEqualTo(CachePolicy.LONG);
    }

    @Test
    void rejectsNonPositiveDurations() {
        CacheConfiguration configuration = new CacheConfiguration(0, 3600);

        assertThatThrownBy(configuration::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cache max-age durations must be positive");
    }
}
