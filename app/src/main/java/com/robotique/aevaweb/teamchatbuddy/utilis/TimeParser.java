package com.robotique.aevaweb.teamchatbuddy.utilis;

import android.util.Log;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TimeParser {

    /**
     * Parse des chaînes comme :
     * "12:06", "12h06", "12AM", "12:06 PM", "8pm", "08H00", etc.
     * Renvoie un int[] : [hour24, minute]
     */
    public static int[] parseTo24h(String raw) {
        if (raw == null || raw.trim().isEmpty())
            throw new IllegalArgumentException("Heure vide");

        // Normalisation rapide
        String input = raw.trim().toUpperCase(Locale.ROOT);
        // Remplacer 'H' par ':' si pas déjà de double ':' pour uniformiser
        input = input.replaceAll("([0-9])H([0-9])", "$1:$2");

        // Patterns acceptés
        List<DateTimeFormatter> formats = Arrays.asList(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("h:mma"),   // 12h ex "12:06PM"
                DateTimeFormatter.ofPattern("h a"),     // 12h ex "12 PM"
                DateTimeFormatter.ofPattern("ha"),      // 12h ex "12PM"
                DateTimeFormatter.ofPattern("H")        // ex "8" ou "20"
        );

        for (DateTimeFormatter fmt : formats) {
            try {
                LocalTime t = LocalTime.parse(cleanAMPM(input), fmt);
                return new int[]{ t.getHour(), t.getMinute() };
            } catch (DateTimeParseException ignored) {
                Log.e("TimeParser", "Erreur parsing ALERT_HOURS : " + ignored.getMessage());
            }
        }
        throw new IllegalArgumentException("Format d'heure non reconnu : " + raw);
    }

    private static String cleanAMPM(String s) {
        // retire espace entre nombre et AM/PM pour les patterns sans espace
        return s.replaceAll("(?i)\\s*(AM|PM)$", "$1");
    }
}
