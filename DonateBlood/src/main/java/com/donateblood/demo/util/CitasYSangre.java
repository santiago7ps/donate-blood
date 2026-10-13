package com.donateblood.demo.util;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CitasYSangre {

    private CitasYSangre() {

    }

    public static final String[] TIPOS_SANGRE = {
            "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"
    };

    public static Set<String> franjasValidas() {
        Set<String> slots = new LinkedHashSet<>();
        for (int h = 8; h <= 16; h++) {
            if (h >= 12 && h < 14) continue;
            slots.add(String.format("%02d:00", h));
            slots.add(String.format("%02d:30", h));
        }
        return slots;
    }

    public static boolean horaValida(String hora) {
        return hora != null && franjasValidas().contains(hora);
    }
}
