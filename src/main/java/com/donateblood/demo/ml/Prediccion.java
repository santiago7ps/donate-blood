package com.donateblood.demo.ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.donateblood.demo.model.Cita;

/**
 * Probabilidad de que un donante asista a su próxima cita.
 *
 * Se calcula con su propio historial: citas REALIZADAS entre las citas que ya terminaron
 * (REALIZADA o CANCELADA). Si casi no tiene historial, se acerca al promedio de todos.
 *
 *   probabilidad = (realizadas + 2 * promedioGeneral) / (citas terminadas + 2)
 *
 * Ejemplo: cumplió 3 de 4 citas y el promedio general es 60 %  ->  (3 + 1.2) / (4 + 2) = 70 %
 */
public class Prediccion {

    private final Map<String, int[]> historial = new HashMap<>(); // donante -> {realizadas, terminadas}
    private final double promedioGeneral;

    public static Prediccion desde(List<Cita> citas) {
        return new Prediccion(citas);
    }

    private Prediccion(List<Cita> citas) {
        int realizadas = 0, terminadas = 0;
        for (Cita c : citas) {
            boolean realizada = "REALIZADA".equals(c.getEstado());
            if (!realizada && !"CANCELADA".equals(c.getEstado())) continue; // PENDIENTE no cuenta

            int[] h = historial.computeIfAbsent(c.getUsuario(), k -> new int[2]);
            h[1]++;
            terminadas++;
            if (realizada) { h[0]++; realizadas++; }
        }
        promedioGeneral = terminadas == 0 ? 0.5 : (double) realizadas / terminadas;
    }

    /** Probabilidad de asistir, de 0 a 100. */
    public int porcentaje(String donante) {
        int[] h = historial.getOrDefault(donante, new int[2]);
        return (int) Math.round(100 * (h[0] + 2 * promedioGeneral) / (h[1] + 2));
    }

    /** "alto" = probablemente no asista, "medio", "bajo". */
    public String riesgo(String donante) {
        int p = porcentaje(donante);
        return p < 50 ? "alto" : (p < 75 ? "medio" : "bajo");
    }

    /** Texto para mostrar: "cumplió 3 de 4 citas". */
    public String resumen(String donante) {
        int[] h = historial.get(donante);
        return h == null ? "sin historial" : "cumplió " + h[0] + " de " + h[1] + " citas";
    }

    /** Etiqueta corta en palabras: "Muy probable", "Probable" o "Poco probable". */
    public String etiqueta(String donante) {
        String r = riesgo(donante);
        return r.equals("alto") ? "Poco probable" : (r.equals("medio") ? "Probable" : "Muy probable");
    }

    /** Qué significa el resultado, en una frase. */
    public String interpretacion(String donante) {
        String r = riesgo(donante);
        String frase = r.equals("alto") ? "Es poco probable que asista"
                : r.equals("medio") ? "Es probable que asista, pero no es seguro"
                : "Es muy probable que asista";
        return frase + " (" + resumen(donante) + ").";
    }
}
