package com.donateblood.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.DonacionRepository;
import com.donateblood.demo.util.CitasYSangre;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/banco")
public class BancoController {

    private static final Map<String, Integer> unidadesSangre = new ConcurrentHashMap<>();

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private DonacionRepository donacionRepository;

    @GetMapping("/panel")
    public String panelBanco() {
        return "bancopanel";
    }

    @GetMapping("/unidades")
    public String verUnidades(Model model) {
        model.addAttribute("unidades", unidadesSangre);
        return "banco_unidades";
    }

    @GetMapping("/modificar")
    public String mostrarModificar(Model model) {
        model.addAttribute("tipos", CitasYSangre.TIPOS_SANGRE);
        return "banco_modificar";
    }

    @PostMapping("/modificar")
    public String guardarUnidades(
            @RequestParam String tipo,
            @RequestParam int cantidad,
            @RequestParam(required = false) String origen,
            RedirectAttributes redirectAttributes) {

        if (cantidad <= 0) {
            redirectAttributes.addFlashAttribute("error", "La cantidad debe ser mayor que cero");

            if ("admin".equals(origen)) {
                return "redirect:/admin/modificar";
            }
            return "redirect:/banco/modificar";
        }

        int actual = unidadesSangre.getOrDefault(tipo, 0);
        unidadesSangre.put(tipo, actual + cantidad);

        redirectAttributes.addFlashAttribute("mensaje", "Unidades modificadas correctamente");

        if ("admin".equals(origen)) {
            return "redirect:/admin/unidades";
        }

        return "redirect:/banco/unidades";
    }

    @GetMapping("/donantes")
    public String donantes(Model model) {
        var donantes = donacionRepository.findAll();
        model.addAttribute("donantes", donantes);
        return "banco_donantes";
    }

    @GetMapping("/citas")
    public String verCitas(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String cedula,
            Model model) {

        List<com.donateblood.demo.model.Cita> citas = citaRepository.findAll();

        if (tipo != null && !tipo.isEmpty()) {
            citas = citas.stream()
                    .filter(c -> c.getTipoSangre() != null && c.getTipoSangre().equalsIgnoreCase(tipo))
                    .collect(Collectors.toList());
        }

        if (cedula != null && !cedula.isEmpty()) {
            String cedulaLower = cedula.toLowerCase();
            citas = citas.stream()
                    .filter(c -> c.getUsuario() != null && c.getUsuario().toLowerCase().contains(cedulaLower))
                    .collect(Collectors.toList());
        }

        model.addAttribute("citas", citas);
        model.addAttribute("tipoSeleccionado", tipo == null ? "" : tipo);
        model.addAttribute("cedulaBusqueda", cedula == null ? "" : cedula);
        model.addAttribute("contador", citas.size());

        return "banco_citas";
    }

    @PostMapping("/pedidos")
    public String procesarPedidoWeb(
            @RequestParam String tipo,
            @RequestParam int cantidad,
            @RequestParam(required = false) String origen,
            RedirectAttributes redirectAttributes) {

        Integer stockActual = unidadesSangre.get(tipo);

        if (stockActual == null || stockActual < cantidad) {

            redirectAttributes.addFlashAttribute("error", "Cantidad insuficiente de sangre tipo " + tipo);

            if ("admin".equals(origen)) {
                return "redirect:/admin/pedidos";
            }

            return "redirect:/pedidos";
        }

        unidadesSangre.put(tipo, stockActual - cantidad);

        redirectAttributes.addFlashAttribute("mensaje", "Pedido enviado con éxito");

        if ("admin".equals(origen)) {
            return "redirect:/admin/pedidos";
        }

        return "redirect:/centro/encargos";
    }

    public static String procesarPedido(String tipo, int cantidad) {

        Integer stockActual = unidadesSangre.get(tipo);

        if (stockActual == null || stockActual < cantidad) {
            return "Cantidad insuficiente de sangre tipo " + tipo;
        }

        unidadesSangre.put(tipo, stockActual - cantidad);

        return "ok";
    }

    public static Map<String, Integer> getUnidadesSangre() {
        return unidadesSangre;
    }
}