package com.donateblood.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.util.CitasYSangre;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class CitaController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        for (GrantedAuthority rol : auth.getAuthorities()) {
            if (rol.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }

    private boolean esPropietario(Cita cita) {
        if (cita == null) return false;
        if (esAdmin()) return true;
        String usuarioActual = obtenerUsuarioActual();
        return usuarioActual != null && usuarioActual.equals(cita.getUsuario());
    }

    @GetMapping("/agendarCita")
    public String mostrarFormulario(Model model) {
        model.addAttribute("cita", new Cita());
        model.addAttribute("bancos", usuarioRepository.findByRol("ROLE_BANCO"));
        return "agendarCita";
    }

    @PostMapping("/agendarCita")
    public String guardarCita(@ModelAttribute Cita cita) {

        String usuario = obtenerUsuarioActual();
        cita.setUsuario(usuario);

        cita.setEstado("PENDIENTE");

        if (!CitasYSangre.horaValida(cita.getHora())) {
            return "redirect:/agendarCita?error=hora-invalida";
        }

        citaRepository.save(cita);

        if (esAdmin()) {
            return "redirect:/admin/misCitas";
        }

        return "redirect:/misCitas";
    }

    @GetMapping("/misCitas")
    public String verMisCitas(Model model) {

        String usuario = obtenerUsuarioActual();
        List<Cita> misCitas = citaRepository.findByUsuario(usuario);

        model.addAttribute("citas", misCitas);
        return "verCitas";
    }

    public List<Cita> obtenerTodas() {
        return citaRepository.findAll();
    }

    @GetMapping("/realizarCita/{id}")
    public String realizarCita(@PathVariable String id) {

        Cita cita = citaRepository.findById(id).orElse(null);

        if (cita != null && "PENDIENTE".equals(cita.getEstado())) {
            cita.setEstado("REALIZADA");
            citaRepository.save(cita);
        }

        return "redirect:/banco/citas";
    }

    @GetMapping("/cancelarCita/{id}")
    public String cancelarCita(@PathVariable String id) {

        Cita cita = citaRepository.findById(id).orElse(null);

        if (!esPropietario(cita)) {
            return "redirect:/misCitas?error=no-autorizado";
        }

        if ("PENDIENTE".equals(cita.getEstado())) {
            cita.setEstado("CANCELADA");
            citaRepository.save(cita);
        }

        return "redirect:/misCitas";
    }

    @GetMapping("/editarCita/{id}")
    public String editarCita(@PathVariable String id, Model model) {

        Cita cita = citaRepository.findById(id).orElse(null);

        if (!esPropietario(cita)) {
            return "redirect:/misCitas?error=no-autorizado";
        }

        model.addAttribute("cita", cita);

        model.addAttribute("bancos", usuarioRepository.findByRol("ROLE_BANCO"));

        return "editarCita";
    }

    @PostMapping("/actualizarCita")
    public String actualizarCita(@ModelAttribute Cita cita) {

        if (cita.getId() == null) {
            return "redirect:/misCitas";
        }

        Cita existente = citaRepository.findById(cita.getId()).orElse(null);

        if (!esPropietario(existente)) {
            return "redirect:/misCitas?error=no-autorizado";
        }

        if (!CitasYSangre.horaValida(cita.getHora())) {
            return "redirect:/editarCita/" + cita.getId() + "?error=hora-invalida";
        }

        if ("PENDIENTE".equals(existente.getEstado())) {
            cita.setEstado("PENDIENTE");
            cita.setUsuario(existente.getUsuario());

            citaRepository.save(cita);
        }

        if (esAdmin()) {
            return "redirect:/admin/misCitas";
        }

        return "redirect:/misCitas";
    }
}