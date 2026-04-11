package de.coolemod.schulcore.utils;

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
    
    private static final DecimalFormat DECIMAL_FORMAT;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^([0-9]+\\.?[0-9]*)([kmb])?$", Pattern.CASE_INSENSITIVE);
    
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT = new DecimalFormat("#.##", symbols);
    }
    
    /**
     * Formatiert eine Zahl in kompakte Form (10k, 1.5m, 2.3b)
     * @param number Die zu formatierende Zahl
     * @return Formatierte Zeichenkette
     */
    public static String format(double number) {
        if (number < 0) {
            return "-" + format(-number);
        }
        
        if (number >= 1_000_000_000) {
            return DECIMAL_FORMAT.format(number / 1_000_000_000) + "b";
        } else if (number >= 1_000_000) {
            return DECIMAL_FORMAT.format(number / 1_000_000) + "m";
        } else if (number >= 1_000) {
            return DECIMAL_FORMAT.format(number / 1_000) + "k";
        } else {
            return DECIMAL_FORMAT.format(number);
        }
    }
    
    /**
     * Formatiert eine Zahl mit Währungssymbol
     * @param number Die zu formatierende Zahl
     * @return Formatierte Zeichenkette mit $
     */
    public static String formatMoney(double number) {
        return "$" + format(number);
    }
    
    /**
     * Formatiert eine Zahl mit voller Genauigkeit (für kleine Beträge)
     * @param number Die zu formatierende Zahl
     * @return Formatierte Zeichenkette
     */
    public static String formatFull(double number) {
        if (number >= 1_000_000_000) {
            return DECIMAL_FORMAT.format(number / 1_000_000_000) + "b";
        } else if (number >= 1_000_000) {
            return DECIMAL_FORMAT.format(number / 1_000_000) + "m";
        } else if (number >= 10_000) {
            return DECIMAL_FORMAT.format(number / 1_000) + "k";
        } else {
            return String.format("%.2f", number);
        }
    }
    
    /**
     * Formatiert Geld mit voller Genauigkeit für kleine Beträge
     */
    public static String formatMoneyFull(double number) {
        return "$" + formatFull(number);
    }
    
    /**
     * Parst eine Zahl aus einem String (unterstützt k, m, b Suffixe)
     * @param input Der zu parsende String (z.B. "10k", "1.5m", "100000")
     * @return Die geparste Zahl, oder -1 bei ungültigem Input
     */
    public static double parse(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }
        
        input = input.trim().toLowerCase();
        Matcher matcher = NUMBER_PATTERN.matcher(input);
        
        if (!matcher.matches()) {
            return -1;
        }
        
        try {
            double number = Double.parseDouble(matcher.group(1));
            String suffix = matcher.group(2);
            
            if (suffix != null) {
                switch (suffix.toLowerCase()) {
                    case "k":
                        number *= 1_000;
                        break;
                    case "m":
                        number *= 1_000_000;
                        break;
                    case "b":
                        number *= 1_000_000_000;
                        break;
                }
            }
            
            return number;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Prüft ob ein String eine gültige Zahl ist (mit oder ohne Suffix)
     */
    public static boolean isValidNumber(String input) {
        return parse(input) >= 0;
    }
    
    /**
     * Berechnet die Shards die man für einen bestimmten Geldbetrag bekommt
     * @param money Der verdiente Geldbetrag
     * @return Anzahl der Shards (1 pro 1000 verdient)
     */
    public static int calculateShardsFromMoney(double money) {
        return (int) (money / 1000);
    }
    
    /**
     * Formatiert Integer-Zahlen kompakt
     */
    public static String formatInt(int number) {
        return format(number);
    }
    
    /**
     * Formatiert Integer-Zahlen kompakt (long)
     */
    public static String formatLong(long number) {
        return format(number);
    }
}
