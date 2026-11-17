package com.donateblood.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.donateblood.demo.ml.Patrones;
import com.donateblood.demo.repository.CitaRepository;

@Controller
public class Router {

    @Autowired
    private CitaRepository citaRepository;

    // Cifras de la landing guardadas en memoria para no recalcularlas en cada visita.
    private Patrones patronesCache;
    private long patronesHasta = 0;

    @GetMapping("/")
    public String landing(Model model) {
        // Cifras reales de asistencia (solo datos agregados). Si la base falla, la página se muestra igual.
        try {
            model.addAttribute("patrones", obtenerPatrones());
        } catch (RuntimeException e) {
            model.addAttribute("patrones", null);
        }
        return "vistasPagina/landing";
    }

    // Recalcula las cifras como máximo cada 10 minutos, en lugar de cargar todas las citas en cada visita.
    private synchronized Patrones obtenerPatrones() {
        long ahora = System.currentTimeMillis();
        if (patronesCache == null || ahora > patronesHasta) {
            patronesCache = Patrones.desde(citaRepository.findAll());
            patronesHasta = ahora + 10 * 60 * 1000;
        }
        return patronesCache;
    }

    @GetMapping("/info")
    public String info() {
        return "vistasPagina/projectInfo";
    }

    @GetMapping("/verBancos")
    public String verBancos() {
        return "verBancos";
    }

    @GetMapping("/pedidos")
    public String pedidos() {
        return "pedidos";
    }

    @GetMapping("/mapaBancos")
    public String mapaBancos() {
        return "mapaBancos";
    }
}
