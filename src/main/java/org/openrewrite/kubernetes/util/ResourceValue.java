package org.openrewrite.kubernetes.util;

import lombok.EqualsAndHashCode;

import java.util.Locale;

@EqualsAndHashCode
public class ResourceValue {

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

    private final long value;
    private final Unit unit;

    private ResourceValue(long value, Unit unit) {
        this.value = value;
        this.unit = unit;
    }

    private ResourceValue(String value, Unit unit) {
        this(unit.toAbsoluteValue(Long.parseLong(value)), unit);
    }

    public static ResourceValue parseResourceString(String s) {
        String val;
        Unit unit;
        int unitLen = 1;
        if (s.endsWith("i")) {
            unitLen = 2;
        }

        val = s.substring(0, s.length() - unitLen);
        unit = Unit.fromString(s.substring(s.length() - unitLen));
        return new ResourceValue(val, unit);
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

}
