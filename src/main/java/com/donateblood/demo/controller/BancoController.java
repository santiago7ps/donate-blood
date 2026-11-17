package com.donateblood.demo.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.DonacionRepository;
import com.donateblood.demo.repository.UsuarioRepository;
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

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    // CONSULTA PAGINADA DE CITAS CON BÚSQUEDA POR NOMBRE COMPLETO
    @GetMapping("/citas")
    public String verCitas(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String nombre,
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

        String nombreFiltro = (nombre == null || nombre.isBlank())
                ? null : crearRegexNombre(nombre);

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
                nombreFiltro,
                pageable
        );

        // Si la página solicitada ya no existe, regresar a la última disponible
        if (citas.getTotalPages() > 0 && page >= citas.getTotalPages()) {
            page = citas.getTotalPages() - 1;

            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Direction.ASC, "fecha")
                            .and(Sort.by(Sort.Direction.ASC, "hora"))
            );

            citas = buscarCitas(tipoFiltro, nombreFiltro, pageable);
        }

        model.addAttribute("citas", citas);
        model.addAttribute("tipoSeleccionado", tipoFiltro == null ? "" : tipoFiltro);
        model.addAttribute("nombreBusqueda", nombre == null ? "" : nombre.trim());
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
        int paginaFin = Math.min(citas.getTotalPages() - 1, paginaInicio + 4);
        paginaInicio = Math.max(0, paginaFin - 4);

        model.addAttribute("paginaInicio", paginaInicio);
        model.addAttribute("paginaFin", paginaFin);

        // Enlaces de Gmail con el recordatorio listo (solo citas pendientes con correo)
        model.addAttribute("enlacesCorreo", crearEnlacesCorreo(citas.getContent()));

        return "banco_citas";
    }

    // RECORDATORIO POR CORREO: arma, para cada cita pendiente de la página, un enlace
    // que abre Gmail con el destinatario, el asunto y el mensaje ya escritos
    private Map<String, String> crearEnlacesCorreo(List<Cita> citasPagina) {

        Map<String, String> enlaces = new HashMap<>();

        Set<String> usernames = citasPagina.stream()
                .filter(c -> "PENDIENTE".equals(c.getEstado()) && c.getUsuario() != null)
                .map(Cita::getUsuario)
                .collect(Collectors.toSet());

        if (usernames.isEmpty()) {
            return enlaces;
        }

        // Una sola consulta para traer los correos de todos los donantes de la página
        Map<String, String> correos = new HashMap<>();
        for (Usuario u : usuarioRepository.findByUsernameIn(usernames)) {
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                correos.putIfAbsent(u.getUsername(), u.getEmail().trim());
            }
        }

        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        for (Cita c : citasPagina) {
            if (!"PENDIENTE".equals(c.getEstado())) {
                continue;
            }

            String correo = correos.get(c.getUsuario());
            if (correo == null) {
                continue;
            }

            enlaces.put(c.getId(), crearEnlaceGmail(correo, c, hoy));
        }

        return enlaces;
    }

    private String crearEnlaceGmail(String correo, Cita c, LocalDate hoy) {

        String cuando = "es el " + c.getFecha();
        try {
            long dias = ChronoUnit.DAYS.between(hoy, LocalDate.parse(c.getFecha()));
            if (dias == 0) {
                cuando = "es hoy (" + c.getFecha() + ")";
            } else if (dias == 1) {
                cuando = "es mañana (" + c.getFecha() + ")";
            } else if (dias > 1) {
                cuando = "es el " + c.getFecha() + " (en " + dias + " días)";
            }
        } catch (Exception e) {
            // Si la fecha no se puede leer, se usa el texto tal cual
        }

        String asunto = "Recordatorio de tu cita de donación de sangre";

        String mensaje = "Hola " + c.getNombre() + ",\n\n"
                + "Te recordamos que tu cita de donación de sangre " + cuando
                + " a las " + c.getHora() + " en " + c.getLugar() + ".\n\n"
                + "Si no puedes asistir, por favor cancela o cambia tu cita "
                + "para que otro donante pueda usar ese cupo.\n\n"
                + "¡Gracias por donar!";

        return "https://mail.google.com/mail/?view=cm&fs=1"
                + "&to=" + codificar(correo)
                + "&su=" + codificar(asunto)
                + "&body=" + codificar(mensaje);
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // CONSULTA DE CITAS CON FILTROS Y PAGINACIÓN
    private Page<Cita> buscarCitas(
            String tipo,
            String nombreRegex,
            Pageable pageable) {

        if (tipo != null && nombreRegex != null) {
            return citaRepository.findByTipoSangreIgnoreCaseAndNombreRegex(
                    tipo, nombreRegex, pageable);
        } else if (tipo != null) {
            return citaRepository.findByTipoSangreIgnoreCase(tipo, pageable);
        } else if (nombreRegex != null) {
            return citaRepository.findByNombreRegex(nombreRegex, pageable);
        } else {
            return citaRepository.findAll(pageable);
        }
    }

    // Crear una búsqueda flexible: acepta espacios, comas y palabras en distinto orden
    private String crearRegexNombre(String nombre) {
        String[] palabras = nombre.trim()
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .split("\\s+");

        StringBuilder regex = new StringBuilder("(?i)");

        for (String palabra : palabras) {
            if (!palabra.isBlank()) {
                regex.append("(?=.*")
                        .append(Pattern.quote(palabra))
                        .append(")");
            }
        }

        regex.append(".*");
        return regex.toString();
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
