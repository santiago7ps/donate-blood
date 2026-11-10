package com.donateblood.demo.ml;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.donateblood.demo.model.Cita;

/**
 * Cifras generales de asistencia calculadas con las citas reales de la aplicación.
 * Solo datos agregados (sin nombres), por eso se pueden mostrar en la página pública.
 */
public class Patrones {

    /** Mínimo de citas terminadas para mostrar cifras, y para comparar mañana/tarde o días. */
    static final int MINIMO_TOTAL = 30;
    static final int MINIMO_GRUPO = 10;

    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private final int agendadas;
    private final int realizadas;
    private final int terminadas;

    // {realizadas, terminadas} por franja (0 = mañana, 1 = tarde) y por día de la semana
    private final int[][] franja = new int[2][2];
    private final int[][] dia = new int[7][2];

    public static Patrones desde(List<Cita> citas) {
        return new Patrones(citas);
    }

    private Patrones(List<Cita> citas) {
        int real = 0, term = 0;
        for (Cita c : citas) {
            boolean realizada = "REALIZADA".equals(c.getEstado());
            if (!realizada && !"CANCELADA".equals(c.getEstado())) continue;
            term++;
            if (realizada) real++;

            int f = esTarde(c.getHora()) ? 1 : 0;
            franja[f][1]++;
            if (realizada) franja[f][0]++;
            try {
                int d = LocalDate.parse(c.getFecha()).getDayOfWeek().getValue() - 1;
                dia[d][1]++;
                if (realizada) dia[d][0]++;
            } catch (Exception e) {
                // fecha inválida: no cuenta para los días
            }
        }
        agendadas = citas.size();
        realizadas = real;
        terminadas = term;
    }

    private static boolean esTarde(String hora) {
        try {
            return Integer.parseInt(hora.substring(0, 2)) >= 12;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean disponible() {
        return terminadas >= MINIMO_TOTAL;
    }

    public int agendadas() { return agendadas; }

    public int realizadas() { return realizadas; }

    /** De cada 100 citas ya terminadas, cuántas terminaron en donación. */
    public int tasaGeneral() {
        return porcentaje(realizadas, terminadas);
    }

    /** "la mañana" o "la tarde"; null si no hay suficientes citas de cada una para comparar. */
    public String mejorFranja() {
        if (franja[0][1] < MINIMO_GRUPO || franja[1][1] < MINIMO_GRUPO) return null;
        return tasaFranja(0) >= tasaFranja(1) ? "la mañana" : "la tarde";
    }

    public int tasaMejorFranja() {
        return mejorFranja() == null ? 0 : Math.max(tasaFranja(0), tasaFranja(1));
    }

    /** Día de la semana con mayor asistencia; null si ninguno tiene suficientes citas. */
    public String mejorDia() {
        int mejor = indiceMejorDia();
        return mejor < 0 ? null : DIAS[mejor];
    }

    public int tasaMejorDia() {
        int mejor = indiceMejorDia();
        return mejor < 0 ? 0 : porcentaje(dia[mejor][0], dia[mejor][1]);
    }

    /** Un día de la semana con su porcentaje de asistencia; "mejor" marca el día más alto. */
    public record DiaAsistencia(String nombre, int porcentaje, boolean mejor) {}

    /** Días con suficientes citas (para la gráfica), en orden de lunes a domingo. */
    public List<DiaAsistencia> asistenciaPorDia() {
        int mejor = indiceMejorDia();
        List<DiaAsistencia> lista = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            if (dia[d][1] < MINIMO_GRUPO) continue;
            lista.add(new DiaAsistencia(DIAS[d], porcentaje(dia[d][0], dia[d][1]), d == mejor));
        }
        return lista;
    }

    private int indiceMejorDia() {
        int mejor = -1;
        for (int d = 0; d < 7; d++) {
            if (dia[d][1] < MINIMO_GRUPO) continue;
            if (mejor < 0 || porcentaje(dia[d][0], dia[d][1]) > porcentaje(dia[mejor][0], dia[mejor][1])) mejor = d;
        }
        return mejor;
    }

    private int tasaFranja(int f) {
        return porcentaje(franja[f][0], franja[f][1]);
    }

    private static int porcentaje(int parte, int total) {
        return total == 0 ? 0 : (int) Math.round(100.0 * parte / total);
    }
}
