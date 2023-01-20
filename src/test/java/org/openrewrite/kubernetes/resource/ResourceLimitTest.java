package org.openrewrite.kubernetes.resource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLimitTest {

    @ParameterizedTest
    @EnumSource(ResourceLimit.ResourceValue.Unit.class)
    void givenABaseUnitMustParseIntoResourceValue() {
        for (ResourceLimit.ResourceValue.Unit unit : ResourceLimit.ResourceValue.Unit.values()) {
            Set.of(10L, 100L, 1000L).forEach(i -> {
                ResourceLimit.ResourceValue value = new ResourceLimit.ResourceValue("$i$unit");
                assertThat(value.getUnit()).isEqualTo(unit);
                assertThat(unit.fromAbsoluteValue(value.getAbsoluteValue())).isEqualTo(i);
            });
        }
    }
}
