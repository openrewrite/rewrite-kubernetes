package org.openrewrite.kubernetes.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ResourceValueTest {

    @ParameterizedTest
    @MethodSource("resourceUnits")
    fun `given a base unit, must parse into ResourceValue`(unit: ResourceValue.Unit) =
        setOf(10L, 100L, 1000L).forEach { i ->
            val value = ResourceValue.parseResourceString("$i$unit")
            assertThat(value.unit).isEqualTo(unit)
            assertThat(unit.fromAbsoluteValue(value.absoluteValue)).isEqualTo(i)
        }

    private companion object {
        @JvmStatic
        fun resourceUnits() = Stream.of(
            Arguments.of(ResourceValue.Unit.K),
            Arguments.of(ResourceValue.Unit.M),
            Arguments.of(ResourceValue.Unit.G),
            Arguments.of(ResourceValue.Unit.T),
            Arguments.of(ResourceValue.Unit.P),
            Arguments.of(ResourceValue.Unit.Ki),
            Arguments.of(ResourceValue.Unit.Mi),
            Arguments.of(ResourceValue.Unit.Gi),
            Arguments.of(ResourceValue.Unit.Ti),
            Arguments.of(ResourceValue.Unit.Pi),
        )
    }

}