package de.coolemod.donut.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility-Klasse für Zahlenformatierung
 * Unterstützt k (Tausend), m (Million), b (Milliarde)
 */
public class NumberFormatter {
    private static final String[] SUFFIXES = {"", "k", "m", "b", "t", "q", "qq"};

    private static final DecimalFormat DECIMAL_FORMAT;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^([0-9]+\\.?[0-9]*)(k|m|b|t|q|qq)?$", Pattern.CASE_INSENSITIVE);

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT = new DecimalFormat("#.##", symbols);
    }

    public static String format(double number) {
        if (number < 0) return "-" + format(-number);
        return formatCompact(number);
    }

    private static String formatCompact(double number) {
        int suffixIndex = 0;
        while (number >= 1_000 && suffixIndex < SUFFIXES.length - 1) {
            number /= 1_000;
            suffixIndex++;
        }

        return DECIMAL_FORMAT.format(number) + SUFFIXES[suffixIndex];
    }

    private static double applySuffix(double number, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return number;
        }

        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> number * 1_000;
            case "m" -> number * 1_000_000;
            case "b" -> number * 1_000_000_000;
            case "t" -> number * 1_000_000_000_000L;
            case "q" -> number * 1_000_000_000_000_000L;
            case "qq" -> number * 1_000_000_000_000_000_000L;
            default -> -1;
        };
    }

    private static String formatDetailed(double number) {
        if (number < 10_000) {
            return String.format(Locale.US, "%.2f", number);
        }

        return formatCompact(number);
    }

    public static String formatMoney(double number) {
        return "$" + format(number);
    }

    public static String formatFull(double number) {
        if (number < 0) return "-" + formatFull(-number);
        return formatDetailed(number);
    }

    public static String formatMoneyFull(double number) {
        return "$" + formatFull(number);
    }

    public static String formatInt(int number) {
        return format(number);
    }

    public static String formatLong(long number) {
        return format(number);
    }

    public static double parse(String input) {
        if (input == null || input.isEmpty()) return -1;
        input = input.trim().toLowerCase();
        Matcher matcher = NUMBER_PATTERN.matcher(input);
        if (!matcher.matches()) return -1;
        try {
            double number = Double.parseDouble(matcher.group(1));
            number = applySuffix(number, matcher.group(2));
            if (number < 0) {
                return -1;
            }
            return number;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean isValidNumber(String input) {
        return parse(input) >= 0;
    }
}
