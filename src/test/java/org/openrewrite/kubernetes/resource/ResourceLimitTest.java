/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.kubernetes.resource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


class ResourceLimitTest {

    @ParameterizedTest
    @EnumSource(ResourceLimit.ResourceValue.Unit.class)
    void unitsShouldParseToValidResource(ResourceLimit.ResourceValue.Unit unit) {
        Stream.of(10L, 100L, 1000L)
          .forEach(i -> {
              var value = new ResourceLimit.ResourceValue(i + unit.toString());
              assertThat(value.getUnit()).isEqualTo(unit);
              assertThat(unit.fromAbsoluteValue(value.getAbsoluteValue())).isEqualTo(i);
          });
    }
}
