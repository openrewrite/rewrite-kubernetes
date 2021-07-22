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

import lombok.EqualsAndHashCode;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Locale;

@EqualsAndHashCode
public class ResourceLimit {
    private final ResourceValue value;

    public ResourceLimit(String value) {
        this.value = new ResourceValue(value);
    }

    public String convertToUnit(Yaml.Scalar scalar) {
        ResourceValue rv = new ResourceValue(scalar.getValue());
        return value.convertTo(rv.getUnit()).toString();
    }

    public ResourceValue getValue() {
        return value;
    }

    public boolean exceeds(ResourceValue rv) {
        return value.getAbsoluteValue() > rv.getAbsoluteValue();
    }

    public boolean exceedsRatio(int ratio, ResourceValue rv) {
        return ((double) rv.getAbsoluteValue() / (double) value.getAbsoluteValue()) > ratio;
    }

    public static class ResourceValue {
        private final Unit unit;
        private final long value;

        public ResourceValue(String resourceLimit) {
            int unitLen = 1;
            if (resourceLimit.endsWith("i")) {
                unitLen = 2;
            }

            this.unit = ResourceValue.Unit.fromString(resourceLimit.substring(resourceLimit.length() - unitLen));
            this.value = unit.toAbsoluteValue(Long.parseLong(resourceLimit.substring(0, resourceLimit.length() - unitLen)));
        }

        public ResourceValue(long value, Unit unit) {
            this.value = value;
            this.unit = unit;
        }

        public long getAbsoluteValue() {
            return value;
        }

        public ResourceValue convertTo(Unit destUnit) {
            return new ResourceValue(value, destUnit);
        }

        public Unit getUnit() {
            return unit;
        }

        @Override
        public String toString() {
            return unit.fromAbsoluteValue(value) + unit.toString();
        }

        public enum Unit {
            K,
            M,
            G,
            T,
            P,
            Ki,
            Mi,
            Gi,
            Ti,
            Pi;

            @Override
            public String toString() {
                return this == K ? "k" : super.toString();
            }

            public static Unit fromString(String s) {
                switch (s.toLowerCase(Locale.ROOT)) {
                    case "k":
                        return K;
                    case "m":
                        return M;
                    case "g":
                        return G;
                    case "t":
                        return T;
                    case "p":
                        return P;
                    case "ki":
                        return Ki;
                    case "mi":
                        return Mi;
                    case "gi":
                        return Gi;
                    case "ti":
                        return Ti;
                    case "pi":
                        return Pi;
                    default:
                        return M;
                }
            }

            public long fromAbsoluteValue(long absoluteValue) {
                switch (this) {
                    case M:
                        return (long) (absoluteValue / Math.pow(1000, 2));
                    case G:
                        return (long) (absoluteValue / Math.pow(1000, 3));
                    case T:
                        return (long) (absoluteValue / Math.pow(1000, 4));
                    case P:
                        return (long) (absoluteValue / Math.pow(1000, 5));
                    case Ki:
                        return absoluteValue / 1024;
                    case Mi:
                        return (long) (absoluteValue / Math.pow(1024, 2));
                    case Gi:
                        return (long) (absoluteValue / Math.pow(1024, 3));
                    case Ti:
                        return (long) (absoluteValue / Math.pow(1024, 4));
                    case Pi:
                        return (long) (absoluteValue / Math.pow(1024, 5));
                    default:
                        return absoluteValue / 1000;
                }
            }

            public long toAbsoluteValue(long relativeValue) {
                switch (this) {
                    case M:
                        return (long) (relativeValue * Math.pow(1000, 2));
                    case G:
                        return (long) (relativeValue * Math.pow(1000, 3));
                    case T:
                        return (long) (relativeValue * Math.pow(1000, 4));
                    case P:
                        return (long) (relativeValue * Math.pow(1000, 5));
                    case Ki:
                        return relativeValue * 1024;
                    case Mi:
                        return (long) (relativeValue * Math.pow(1024, 2));
                    case Gi:
                        return (long) (relativeValue * Math.pow(1024, 3));
                    case Ti:
                        return (long) (relativeValue * Math.pow(1024, 4));
                    case Pi:
                        return (long) (relativeValue * Math.pow(1024, 5));
                    default:
                        return relativeValue * 1000;
                }
            }
        }
    }
}
