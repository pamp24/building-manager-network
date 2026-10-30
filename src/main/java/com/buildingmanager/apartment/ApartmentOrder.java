package com.buildingmanager.apartment;

import java.math.BigInteger;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ApartmentOrder {
    private static final String[] GREEK_FLOORS = {"Α", "Β", "Γ", "Δ", "Ε", "ΣΤ", "Ζ", "Η", "Θ", "Ι", "ΙΑ", "ΙΒ", "ΙΓ", "ΙΔ", "ΙΕ", "ΙΣΤ", "ΙΖ", "ΙΗ", "ΙΘ", "Κ"};
    private static final Pattern PARTS = Pattern.compile("\\d+|\\D+");
    private static final Pattern FLOOR_CODE = Pattern.compile("^([Α-Ω]+|\\d+)[΄ʹ'’]*(?:[\\s.\\-'_/]*\\d+|[\\s.'’]*ΟΡΟΦΟΣ)?$");

    private ApartmentOrder() {}

    public static int compare(String floorA, String numberA, String floorB, String numberB) {
        String a = normalize(floorA);
        String b = normalize(floorB);
        int rankA = floorRank(a);
        int rankB = floorRank(b);
        int result = Integer.compare(rankA, rankB);
        if (result != 0) return result;
        if (rankA == Integer.MAX_VALUE - 2) {
            result = naturalCompare(a, b);
            if (result != 0) return result;
        }
        return naturalCompare(apartmentNumber(a, numberA), apartmentNumber(b, numberB));
    }

    public static final Comparator<Apartment> COMPARATOR = (a, b) -> {
        int result = compare(a.getFloor(), a.getNumber(), b.getFloor(), b.getNumber());
        return result != 0 ? result : Comparator.nullsLast(Integer::compareTo).compare(a.getId(), b.getId());
    };

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private static int floorRank(String floor) {
        if (floor.isEmpty()) return Integer.MAX_VALUE - 1;
        if (floor.startsWith("ΗΜΙΥΠΟΓ")) return -2;
        if (floor.startsWith("ΥΠΟΓ") || floor.equals("Υ")) return -3;
        if (floor.startsWith("ΗΜΙΟΡΟΦ") || floor.startsWith("MEZZANINE")) return 1;
        if (floor.startsWith("ΙΣΟΓ") || floor.equals("GROUND")) return 0;
        if (floor.matches("Ι(?:[\\s.\\-_/]*\\d+)?")) return 0;
        if (floor.startsWith("ΔΩΜ") || floor.startsWith("ΡΕΤΙΡ") || floor.equals("ROOF")) return Integer.MAX_VALUE;
        Matcher code = FLOOR_CODE.matcher(floor);
        String prefix = code.matches() ? code.group(1) : floor;
        if (prefix.equals("Υ")) return -3;
        for (int i = 0; i < GREEK_FLOORS.length; i++) {
            if (GREEK_FLOORS[i].equals(prefix)) return i + 2;
        }
        if (prefix.matches("\\d+")) {
            BigInteger numeric = new BigInteger(prefix);
            if (numeric.compareTo(BigInteger.valueOf(Integer.MAX_VALUE - 3L)) < 0) return numeric.signum() == 0 ? 0 : numeric.intValue() + 1;
        }
        return Integer.MAX_VALUE - 2;
    }

    private static String apartmentNumber(String floor, String number) {
        String normalized = normalize(number);
        if (normalized.isEmpty()) return "";
        Matcher code = FLOOR_CODE.matcher(floor);
        String prefix = code.matches() ? code.group(1) : floor;
        if (!prefix.isEmpty() && normalized.startsWith(prefix)
                && (!Character.isDigit(prefix.charAt(0)) || normalized.substring(prefix.length()).matches("^[\\s.\\-'_/].*"))) {
            String remainder = normalized.substring(prefix.length()).replaceFirst("^[\\s.\\-'_/]+", "");
            if (!remainder.isEmpty() && Character.isDigit(remainder.charAt(0))) return remainder;
        }
        return normalized;
    }

    private static int naturalCompare(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() == b.isEmpty() ? 0 : a.isEmpty() ? 1 : -1;
        Matcher left = PARTS.matcher(a);
        Matcher right = PARTS.matcher(b);
        while (left.find() && right.find()) {
            String x = left.group();
            String y = right.group();
            int result = Character.isDigit(x.charAt(0)) && Character.isDigit(y.charAt(0))
                    ? new BigInteger(x).compareTo(new BigInteger(y)) : x.compareTo(y);
            if (result != 0) return result;
        }
        return Integer.compare(a.length(), b.length());
    }
}
