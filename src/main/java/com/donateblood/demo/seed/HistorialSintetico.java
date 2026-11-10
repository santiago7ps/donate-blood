package com.donateblood.demo.seed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Donacion;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.util.CitasYSangre;

/**
 * Datos de prueba con lógica: cada donante tiene su propia constancia (unos casi siempre
 * van, otros casi siempre cancelan), así el historial sirve para predecir.
 * Cada cita REALIZADA genera su donación COMPLETADA.
 */
public final class HistorialSintetico {

    private HistorialSintetico() {}

    public record Resultado(List<Cita> citas, List<Donacion> donaciones) {}

    public static Resultado generar(List<Usuario> donantes, List<String> bancos,
                                    int citasPasadas, int citasFuturas, LocalDate hoy, Random random) {
        List<Cita> citas = new ArrayList<>();
        List<Donacion> donaciones = new ArrayList<>();
        List<String> horas = new ArrayList<>(CitasYSangre.franjasValidas());

        Map<String, Double> constancia = new HashMap<>();   // probabilidad de ir, de 0 a 1
        Map<String, String> tipoSangre = new HashMap<>();
        for (Usuario d : donantes) {
            constancia.put(d.getUsername(), random.nextDouble());
            tipoSangre.put(d.getUsername(), CitasYSangre.TIPOS_SANGRE[random.nextInt(8)]);
        }

        for (int i = 0; i < citasPasadas + citasFuturas; i++) {
            Usuario d = donantes.get(random.nextInt(donantes.size()));
            boolean pasada = i < citasPasadas;

            Cita c = new Cita();
            c.setNombre(d.getNombre());
            c.setUsuario(d.getUsername());
            c.setLugar(bancos.get(random.nextInt(bancos.size())));
            c.setHora(horas.get(random.nextInt(horas.size())));
            c.setTipoSangre(tipoSangre.get(d.getUsername()));
            c.setFecha((pasada ? hoy.minusDays(1 + random.nextInt(365)) : hoy.plusDays(random.nextInt(45))).toString());

            if (!pasada) {
                c.setEstado("PENDIENTE");
            } else if (random.nextDouble() < constancia.get(d.getUsername())) {
                c.setEstado("REALIZADA");
                Donacion don = new Donacion();
                don.setDonante(c.getUsuario());
                don.setTipoSangre(c.getTipoSangre());
                don.setCentroMedico(c.getLugar());
                don.setCantidadMl(450.0);
                don.setFecha(c.getFecha());
                don.setEstado("COMPLETADA");
                donaciones.add(don);
            } else {
                c.setEstado("CANCELADA");
            }
            citas.add(c);
        }
        return new Resultado(citas, donaciones);
    }
}
