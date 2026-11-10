package com.donateblood.demo.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.donateblood.demo.model.Cita;

class PrediccionTest {

    private static Cita cita(String usuario, String estado) {
        Cita c = new Cita();
        c.setUsuario(usuario);
        c.setEstado(estado);
        return c;
    }

    @Test
    void laProbabilidadSalePorElHistorialDelDonante() {
        List<Cita> citas = new ArrayList<>();
        // Ana cumplió 3 de 4; Luis canceló 4 de 4. Promedio general: 3 de 8 = 37,5 %
        for (int i = 0; i < 3; i++) citas.add(cita("ana", "REALIZADA"));
        citas.add(cita("ana", "CANCELADA"));
        for (int i = 0; i < 4; i++) citas.add(cita("luis", "CANCELADA"));
        citas.add(cita("ana", "PENDIENTE")); // no cuenta

        Prediccion p = Prediccion.desde(citas);

        assertEquals(63, p.porcentaje("ana"));   // (3 + 0.75) / 6
        assertEquals(13, p.porcentaje("luis"));  // (0 + 0.75) / 6
        assertEquals(38, p.porcentaje("nuevo")); // sin historial: el promedio general
        assertEquals("cumplió 3 de 4 citas", p.resumen("ana"));
        assertEquals("medio", p.riesgo("ana"));
        assertEquals("alto", p.riesgo("luis"));
        assertEquals("Probable", p.etiqueta("ana"));
        assertEquals("Es poco probable que asista (cumplió 0 de 4 citas).", p.interpretacion("luis"));
    }
}
