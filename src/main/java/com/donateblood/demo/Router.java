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

    @GetMapping("/")
    public String landing(Model model) {
        // Cifras reales de asistencia (solo datos agregados). Si la base falla, la página se muestra igual.
        try {
            model.addAttribute("patrones", Patrones.desde(citaRepository.findAll()));
        } catch (RuntimeException e) {
            model.addAttribute("patrones", null);
        }
        return "vistasPagina/landing";
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