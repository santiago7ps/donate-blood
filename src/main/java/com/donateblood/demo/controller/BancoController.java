
package com.donateblood.demo.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.ml.Prediccion;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.DonacionRepository;
import com.donateblood.demo.service.CapacidadService;
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

    @Autowired
    private CapacidadService capacidadService;

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
            redirectAttributes.addFlashAttribute("error",
                    "La cantidad debe ser mayor que cero");

            if ("admin".equals(origen)) {
                return "redirect:/admin/modificar";
            }
            return "redirect:/banco/modificar";
        }

        int actual = unidadesSangre.getOrDefault(tipo, 0);
        unidadesSangre.put(tipo, actual + cantidad);

        redirectAttributes.addFlashAttribute("mensaje",
                "Unidades modificadas correctamente");

        if ("admin".equals(origen)) {
            return "redirect:/admin/unidades";
        }

        return "redirect:/banco/unidades";
    }

    @GetMapping("/capacidad")
    public String verCapacidad(Authentication auth, Model model) {
        model.addAttribute("capacidad",
                capacidadService.capacidadDeUsername(auth.getName()));

        model.addAttribute("maxima", CapacidadService.CAPACIDAD_MAXIMA);
        return "banco_capacidad";
    }

    @PostMapping("/capacidad")
    public String guardarCapacidad(
            @RequestParam int capacidad,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            capacidadService.actualizarCapacidad(auth.getName(), capacidad);

            redirectAttributes.addFlashAttribute("mensaje",
                    "Capacidad de atención actualizada: "
                            + capacidad + " donantes por hora");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/banco/capacidad";
    }

    @GetMapping("/donantes")
    public String donantes(Model model) {
        var donantes = donacionRepository.findAll();
        model.addAttribute("donantes", donantes);
        return "banco_donantes";
    }

    // CONSULTA PAGINADA DE CITAS
    @GetMapping("/citas")
    public String verCitas(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String cedula,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        // Solo se permiten estos tamaños de página
        if (size != 25 && size != 50 && size != 100) {
            size = 25;
        }

        // Evitar páginas negativas
        page = Math.max(0, page);

        // Limpiar los filtros vacíos
        String tipoFiltro = (tipo == null || tipo.isBlank())
                ? null : tipo.trim();

        String usuarioFiltro = (cedula == null || cedula.isBlank())
                ? null : cedula.trim();

        // Ordenar las citas por fecha y hora
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "fecha")
                        .and(Sort.by(Sort.Direction.ASC, "hora"))
        );

        // Consultar las citas aplicando los filtros
        Page<Cita> citas = buscarCitas(
                tipoFiltro,
                usuarioFiltro,
                pageable
        );

        // Si la página solicitada ya no existe,
        // regresar a la última página disponible
        if (citas.getTotalPages() > 0
                && page >= citas.getTotalPages()) {

            page = citas.getTotalPages() - 1;

            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Direction.ASC, "fecha")
                            .and(Sort.by(Sort.Direction.ASC, "hora"))
            );

            citas = buscarCitas(
                    tipoFiltro,
                    usuarioFiltro,
                    pageable
            );
        }

        // Se conserva el cálculo original de la predicción
        model.addAttribute(
                "prediccion",
                Prediccion.desde(citaRepository.findAll())
        );

        model.addAttribute("citas", citas);

        model.addAttribute("tipoSeleccionado",
                tipoFiltro == null ? "" : tipoFiltro);

        model.addAttribute("cedulaBusqueda",
                usuarioFiltro == null ? "" : usuarioFiltro);

        model.addAttribute("contador", citas.getTotalElements());

        model.addAttribute("tamanioPagina", size);

        // Calcular el rango de registros mostrados
        long desde = citas.getTotalElements() == 0
                ? 0
                : (long) citas.getNumber() * size + 1;

        long hasta = desde == 0
                ? 0
                : desde + citas.getNumberOfElements() - 1;

        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        // Mostrar un máximo de cinco números de página
        int paginaInicio = Math.max(0, citas.getNumber() - 2);

        int paginaFin = Math.min(
                citas.getTotalPages() - 1,
                paginaInicio + 4
        );

        paginaInicio = Math.max(0, paginaFin - 4);

        model.addAttribute("paginaInicio", paginaInicio);
        model.addAttribute("paginaFin", paginaFin);

        return "banco_citas";
    }

    // CONSULTA DE CITAS CON FILTROS Y PAGINACIÓN
    private Page<Cita> buscarCitas(
            String tipo,
            String usuario,
            Pageable pageable) {

        if (tipo != null && usuario != null) {

            return citaRepository
                    .findByTipoSangreIgnoreCaseAndUsuarioContainingIgnoreCase(
                            tipo, usuario, pageable);

        } else if (tipo != null) {

            return citaRepository
                    .findByTipoSangreIgnoreCase(tipo, pageable);

        } else if (usuario != null) {

            return citaRepository
                    .findByUsuarioContainingIgnoreCase(usuario, pageable);

        } else {

            return citaRepository.findAll(pageable);
        }
    }

    @PostMapping("/pedidos")
    public String procesarPedidoWeb(
            @RequestParam String tipo,
            @RequestParam int cantidad,
            @RequestParam(required = false) String origen,
            RedirectAttributes redirectAttributes) {

        Integer stockActual = unidadesSangre.get(tipo);

        if (stockActual == null || stockActual < cantidad) {

            redirectAttributes.addFlashAttribute("error",
                    "Cantidad insuficiente de sangre tipo " + tipo);

            if ("admin".equals(origen)) {
                return "redirect:/admin/pedidos";
            }

            return "redirect:/pedidos";
        }

        unidadesSangre.put(tipo, stockActual - cantidad);

        redirectAttributes.addFlashAttribute("mensaje",
                "Pedido enviado con éxito");

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
