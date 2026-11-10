package com.donateblood.demo.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.donateblood.demo.ml.Prediccion;
import com.donateblood.demo.model.Cita;
import com.donateblood.demo.repository.CitaRepository;

/** Citas pendientes próximas, de menor a mayor probabilidad de asistir, con buscador por donante. */
@Controller
public class PrediccionController {

    private static final int MAXIMO_FILAS = 100;

    @Autowired
    private CitaRepository citaRepository;

    @GetMapping("/banco/prediccion")
    public String ver(@RequestParam(required = false) String buscar, Model model) {
        List<Cita> todas = citaRepository.findAll();
        Prediccion prediccion = Prediccion.desde(todas);
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        // Solo citas pendientes de hoy en adelante (las pendientes con fecha pasada ya no se pueden cumplir)
        List<Cita> proximas = todas.stream()
                .filter(c -> "PENDIENTE".equals(c.getEstado()) && !esPasada(c, hoy))
                .sorted(Comparator.comparingInt(c -> prediccion.porcentaje(c.getUsuario())))
                .collect(Collectors.toList());

        long altos = proximas.stream().filter(c -> prediccion.riesgo(c.getUsuario()).equals("alto")).count();
        long medios = proximas.stream().filter(c -> prediccion.riesgo(c.getUsuario()).equals("medio")).count();
        double esperadas = proximas.stream().mapToInt(c -> prediccion.porcentaje(c.getUsuario())).sum() / 100.0;

        // Buscador: por nombre o usuario del donante
        String texto = buscar == null ? "" : buscar.trim().toLowerCase();
        List<Cita> filtradas = texto.isEmpty() ? proximas : proximas.stream()
                .filter(c -> contiene(c.getNombre(), texto) || contiene(c.getUsuario(), texto))
                .collect(Collectors.toList());

        model.addAttribute("total", proximas.size());
        model.addAttribute("altos", altos);
        model.addAttribute("medios", medios);
        model.addAttribute("bajos", proximas.size() - altos - medios);
        model.addAttribute("esperadas", Math.round(esperadas));
        model.addAttribute("encontradas", filtradas.size());
        model.addAttribute("pendientes", filtradas.stream().limit(MAXIMO_FILAS).collect(Collectors.toList()));
        model.addAttribute("maximo", MAXIMO_FILAS);
        model.addAttribute("buscar", buscar == null ? "" : buscar);
        model.addAttribute("prediccion", prediccion);
        return "banco_prediccion";
    }

    private static boolean esPasada(Cita c, LocalDate hoy) {
        try {
            return LocalDate.parse(c.getFecha()).isBefore(hoy);
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }
}
