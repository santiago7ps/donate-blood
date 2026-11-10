package com.donateblood.demo.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.donateblood.demo.model.Cita;

class RecordatorioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    private static Cita cita(String fecha, String hora, String estado) {
        Cita c = new Cita();
        c.setFecha(fecha);
        c.setHora(hora);
        c.setLugar("Cruz Roja");
        c.setEstado(estado);
        return c;
    }

    @Test
    void recuerdaLaCitaConSusDatosYElHistorial() {
        List<Cita> citas = List.of(
                cita("2026-11-05", "09:00", "PENDIENTE"),
                cita("2026-08-01", "10:00", "REALIZADA"),
                cita("2026-09-01", "10:00", "CANCELADA"));

        List<String> r = Recordatorio.para(citas, HOY);

        assertEquals(2, r.size());
        assertEquals("Tu cita de donación es MAÑANA (2026-11-05) a las 09:00 en Cruz Roja.", r.get(0));
        assertTrue(r.get(1).startsWith("Has cumplido 1 de 2 citas anteriores."));
    }

    @Test
    void sinCitasProximasInvitaAAgendar() {
        List<String> r = Recordatorio.para(List.of(cita("2026-10-01", "09:00", "REALIZADA")), HOY);
        assertEquals(1, r.size());
        assertTrue(r.get(0).startsWith("No tienes citas próximas"));
    }
}
