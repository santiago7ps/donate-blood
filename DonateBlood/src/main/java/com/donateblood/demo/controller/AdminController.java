package com.donateblood.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.util.CitasYSangre;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class AdminController {

    @Autowired
    private CitaRepository citaRepository;

    @GetMapping("/admin/panel")
    public String panel() {
        return "admin/admin_panel";
    }

    @GetMapping("/admin/unidades")
    public String unidades(Model model) {
        model.addAttribute("unidades", BancoController.getUnidadesSangre());
        return "admin/admin_unidades";
    }

    @GetMapping("/admin/modificar")
    public String modificar(Model model) {
        model.addAttribute("tipos", CitasYSangre.TIPOS_SANGRE);
        return "admin/admin_modificar";
    }

    @GetMapping("/admin/pedidos")
    public String pedidos() {
        return "admin/admin_pedidos";
    }

    @GetMapping("/admin/misCitas")
    public String misCitas(Model model) {
        model.addAttribute("citas", citaRepository.findAll());
        return "admin/admin_misCitas";
    }

    @GetMapping("/admin/agendar")
    public String agendar(Model model) {
        model.addAttribute("cita", new Cita());
        return "admin/admin_agendar";
    }

    @PostMapping("/admin/agendarCita")
    public String guardarCitaAdmin(@ModelAttribute Cita cita) {

        cita.setUsuario("ADMIN");
        cita.setEstado("PENDIENTE");

        if (!CitasYSangre.horaValida(cita.getHora())) {
            return "redirect:/admin/agendar?error=hora-invalida";
        }

        citaRepository.save(cita);

        return "redirect:/admin/misCitas";
    }

    @GetMapping("/admin/realizarCita/{id}")
    public String realizarCita(@PathVariable String id) {

        Cita cita = citaRepository.findById(id).orElse(null);

        if (cita != null && "PENDIENTE".equals(cita.getEstado())) {
            cita.setEstado("REALIZADA");
            citaRepository.save(cita);
        }

        return "redirect:/admin/misCitas";
    }

    @GetMapping("/admin/cancelarCita/{id}")
    public String cancelarCita(@PathVariable String id) {

        Cita cita = citaRepository.findById(id).orElse(null);

        if (cita != null && "PENDIENTE".equals(cita.getEstado())) {
            cita.setEstado("CANCELADA");
            citaRepository.save(cita);
        }

        return "redirect:/admin/misCitas";
    }
}