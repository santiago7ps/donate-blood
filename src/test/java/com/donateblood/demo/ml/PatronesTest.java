package com.donateblood.demo.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.donateblood.demo.model.Cita;

class PatronesTest {

    private static Cita cita(String fecha, String hora, String estado) {
        Cita c = new Cita();
        c.setFecha(fecha);
        c.setHora(hora);
        c.setEstado(estado);
        return c;
    }

    @Test
    void calculaCifrasYMejorFranjaYDia() {
        List<Cita> citas = new ArrayList<>();
        // Martes 2026-11-03, mañana: 9 de 10 realizadas
        for (int i = 0; i < 10; i++) citas.add(cita("2026-11-03", "09:00", i < 9 ? "REALIZADA" : "CANCELADA"));
        // Viernes 2026-11-06, tarde: 4 de 10 realizadas
        for (int i = 0; i < 10; i++) citas.add(cita("2026-11-06", "15:00", i < 4 ? "REALIZADA" : "CANCELADA"));
        // Lunes 2026-11-09, mañana: 10 de 10 realizadas
        for (int i = 0; i < 10; i++) citas.add(cita("2026-11-09", "10:00", "REALIZADA"));
        citas.add(cita("2026-11-20", "10:00", "PENDIENTE"));

        Patrones p = Patrones.desde(citas);

        assertTrue(p.disponible());
        assertEquals(31, p.agendadas());
        assertEquals(23, p.realizadas());
        assertEquals(77, p.tasaGeneral()); // 23 de 30
        assertEquals("la mañana", p.mejorFranja());
        assertEquals(95, p.tasaMejorFranja()); // 19 de 20
        assertEquals("lunes", p.mejorDia());
        assertEquals(100, p.tasaMejorDia());

        // Gráfica: lunes, martes y viernes tienen 10 citas cada uno
        var dias = p.asistenciaPorDia();
        assertEquals(3, dias.size());
        assertEquals("lunes", dias.get(0).nombre());
        assertTrue(dias.get(0).mejor());
        assertEquals(90, dias.get(1).porcentaje()); // martes
        assertEquals(40, dias.get(2).porcentaje()); // viernes
    }

    @Test
    void conPocasCitasNoSeMuestraNada() {
        List<Cita> citas = new ArrayList<>();
        for (int i = 0; i < 5; i++) citas.add(cita("2026-11-03", "09:00", "REALIZADA"));
        Patrones p = Patrones.desde(citas);
        assertFalse(p.disponible());
        assertTrue(p.mejorFranja() == null);
        assertTrue(p.mejorDia() == null);
    }
}
