package com.donateblood.demo.ml;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.donateblood.demo.model.Cita;

/**
 * Recordatorios para el donante, armados solo con sus propias citas guardadas en la aplicación
 * (fecha, hora, lugar y su historial de citas realizadas y canceladas).
 */
public final class Recordatorio {

    private Recordatorio() {}

    public static List<String> para(List<Cita> citasDelDonante, LocalDate hoy) {
        List<String> mensajes = new ArrayList<>();

        List<Cita> proximas = citasDelDonante.stream()
                .filter(c -> "PENDIENTE".equals(c.getEstado()) && fecha(c) != null && !fecha(c).isBefore(hoy))
                .sorted(Comparator.comparing(Recordatorio::fecha).thenComparing(c -> String.valueOf(c.getHora())))
                .toList();

        for (Cita c : proximas) {
            long dias = ChronoUnit.DAYS.between(hoy, fecha(c));
            String cuando = dias == 0 ? "HOY" : dias == 1 ? "MAÑANA" : "en " + dias + " días";
            mensajes.add("Tu cita de donación es " + cuando + " (" + c.getFecha() + ") a las "
                    + c.getHora() + " en " + c.getLugar() + ".");
        }

        if (proximas.isEmpty()) {
            mensajes.add("No tienes citas próximas. Agenda una para seguir ayudando con tu donación.");
            return mensajes;
        }

        long realizadas = citasDelDonante.stream().filter(c -> "REALIZADA".equals(c.getEstado())).count();
        long canceladas = citasDelDonante.stream().filter(c -> "CANCELADA".equals(c.getEstado())).count();
        if (canceladas > 0) {
            mensajes.add("Has cumplido " + realizadas + " de " + (realizadas + canceladas)
                    + " citas anteriores. Si no puedes asistir, cancela o cambia tu cita para que otro donante use ese cupo.");
        }
        return mensajes;
    }

    private static LocalDate fecha(Cita c) {
        try {
            return LocalDate.parse(c.getFecha());
        } catch (Exception e) {
            return null;
        }
    }
}
