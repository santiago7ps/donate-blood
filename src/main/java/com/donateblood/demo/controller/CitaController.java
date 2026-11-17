package com.donateblood.demo.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.donateblood.demo.ml.Recordatorio;
import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.service.CapacidadService;
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

    @Autowired
    private CapacidadService capacidadService;

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

    /** true si la fecha (yyyy-MM-dd) es anterior a hoy en hora de Colombia; un formato raro no se rechaza aquí. */
    private boolean fechaPasada(String fecha) {
        try {
            return LocalDate.parse(fecha).isBefore(LocalDate.now(ZoneId.of("America/Bogota")));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean esPropietario(Cita cita) {
        if (cita == null) return false;
        if (esAdmin()) return true;
        String usuarioActual = obtenerUsuarioActual();
        return usuarioActual != null && usuarioActual.equals(cita.getUsuario());
    }

    /** Perfil del donante dueño de la cita (o null si no existe, p. ej. citas creadas por el administrador). */
    private Usuario perfilDe(String username) {
        if (username == null) return null;
        List<Usuario> encontrados = usuarioRepository.findAllByUsername(username);
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    private String sangreDePerfil(Usuario perfil) {
        return (perfil != null && perfil.getTipoSangre() != null && !perfil.getTipoSangre().isBlank())
                ? perfil.getTipoSangre()
                : null;
    }

    private boolean tipoSangreValido(String tipo) {
        return tipo != null && Arrays.asList(CitasYSangre.TIPOS_SANGRE).contains(tipo);
    }

    /** "banco-cerrado" u "hora-pasada" si la franja no se puede reservar por esos motivos; null en otro caso. */
    private String errorDeFranja(Cita cita, String excluirId) {
        CapacidadService.FranjaInfo f = capacidadService.franja(cita.getLugar(), cita.getFecha(), cita.getHora(), excluirId);
        if (f == null) return null;
        if ("CERRADA".equals(f.estado())) return "banco-cerrado";
        if ("PASADA".equals(f.estado())) return "hora-pasada";
        return null;
    }

    @GetMapping("/agendarCita")
    public String mostrarFormulario(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String lugar,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String hora,
            Model model) {
        model.addAttribute("cita", new Cita());
        model.addAttribute("bancos", usuarioRepository.findByRol("ROLE_BANCO"));

        // El tipo de sangre y el nombre salen del perfil del donante
        Usuario perfil = perfilDe(obtenerUsuarioActual());
        model.addAttribute("tipoSangrePerfil", sangreDePerfil(perfil));
        model.addAttribute("nombrePerfil", perfil == null ? null : perfil.getNombre());
        model.addAttribute("tiposSangre", CitasYSangre.TIPOS_SANGRE);

        // Si se vuelve de un error, se conserva el banco y la fecha elegidos
        model.addAttribute("lugarSel", lugar);
        model.addAttribute("fechaSel", fecha);

        agregarAvisoCupo(model, error, lugar, fecha, hora, null);
        return "agendarCita";
    }

    /** Si la hora elegida se llenó, informa cuál era y qué horas siguen con cupo. */
    private void agregarAvisoCupo(Model model, String error, String lugar, String fecha, String hora, String excluirId) {
        if (!"cupo-lleno".equals(error) || lugar == null || fecha == null) return;
        model.addAttribute("cupoLleno", true);
        model.addAttribute("horaLlena", hora);
        model.addAttribute("bancoLleno", lugar);
        model.addAttribute("fechaLlena", fecha);
        model.addAttribute("horasLibres", capacidadService.horasConCupo(lugar, fecha, excluirId));
    }

    private String redirectCupoLleno(String base, Cita cita) {
        return "redirect:" + base + "?error=cupo-lleno"
                + "&lugar=" + UriUtils.encode(String.valueOf(cita.getLugar()), StandardCharsets.UTF_8)
                + "&fecha=" + UriUtils.encode(String.valueOf(cita.getFecha()), StandardCharsets.UTF_8)
                + "&hora=" + UriUtils.encode(String.valueOf(cita.getHora()), StandardCharsets.UTF_8);
    }

    /** Franjas del banco en esa fecha con capacidad, ocupación y estado (lo consume el selector de horas). */
    @GetMapping("/disponibilidad")
    @ResponseBody
    public List<Map<String, Object>> disponibilidad(
            @RequestParam String lugar,
            @RequestParam String fecha,
            @RequestParam(required = false) String excluir) {

        return capacidadService.detalleHoras(lugar, fecha, excluir).stream()
                .map(f -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("hora", f.hora());
                    fila.put("fin", f.fin());
                    fila.put("capacidad", f.capacidad());
                    fila.put("ocupados", f.ocupados());
                    fila.put("libres", f.libres());
                    fila.put("estado", f.estado());
                    return fila;
                })
                .collect(Collectors.toList());
    }

    /** Próximos 14 días del banco con sus cupos totales (tira de fechas del formulario). */
    @GetMapping("/disponibilidad/dias")
    @ResponseBody
    public List<Map<String, Object>> diasDisponibles(@RequestParam String lugar) {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        return capacidadService.resumenDias(lugar, hoy, 14).stream()
                .map(d -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("fecha", d.fecha());
                    fila.put("libres", d.libres());
                    fila.put("estado", d.estado());
                    return fila;
                })
                .collect(Collectors.toList());
    }

    /** Datos generales del banco para mostrar junto al selector (días que atiende). */
    @GetMapping("/disponibilidad/resumen")
    @ResponseBody
    public Map<String, Object> resumenBanco(@RequestParam String lugar) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("dias", capacidadService.diasTexto(lugar));
        return resumen;
    }

    @PostMapping("/agendarCita")
    public String guardarCita(@ModelAttribute Cita cita) {

        String usuario = obtenerUsuarioActual();
        cita.setUsuario(usuario);

        cita.setEstado("PENDIENTE");

        // El tipo de sangre siempre viene del perfil; solo quien no tiene perfil (p. ej. el admin) lo elige
        Usuario perfil = perfilDe(usuario);
        String sangrePerfil = sangreDePerfil(perfil);
        if (sangrePerfil != null) {
            cita.setTipoSangre(sangrePerfil);
        } else if (!tipoSangreValido(cita.getTipoSangre())) {
            return "redirect:/agendarCita?error=sangre-invalida";
        }
        // El nombre también viene del perfil cuando existe
        if (perfil != null && perfil.getNombre() != null && !perfil.getNombre().isBlank()) {
            cita.setNombre(perfil.getNombre());
        }

        if (!CitasYSangre.horaValida(cita.getHora())) {
            return "redirect:/agendarCita?error=hora-invalida";
        }

        if (fechaPasada(cita.getFecha())) {
            return "redirect:/agendarCita?error=fecha-pasada";
        }

        String errorFranja = errorDeFranja(cita, null);
        if (errorFranja != null) {
            return "redirect:/agendarCita?error=" + errorFranja;
        }

        if (!capacidadService.guardarSiHayCupo(cita)) {
            return redirectCupoLleno("/agendarCita", cita);
        }

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
        model.addAttribute("recordatorios",
                Recordatorio.para(misCitas, LocalDate.now(ZoneId.of("America/Bogota"))));
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
    public String editarCita(
            @PathVariable String id,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String lugar,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String hora,
            Model model) {

        Cita cita = citaRepository.findById(id).orElse(null);

        if (!esPropietario(cita)) {
            return "redirect:/misCitas?error=no-autorizado";
        }

        model.addAttribute("cita", cita);

        model.addAttribute("bancos", usuarioRepository.findByRol("ROLE_BANCO"));

        // Si el dueño de la cita tiene perfil, su tipo de sangre es automático
        Usuario duenio = perfilDe(cita.getUsuario());
        model.addAttribute("tipoSangrePerfil", sangreDePerfil(duenio));
        model.addAttribute("nombrePerfil", duenio == null ? null : duenio.getNombre());
        model.addAttribute("tiposSangre", CitasYSangre.TIPOS_SANGRE);

        agregarAvisoCupo(model, error, lugar, fecha, hora, id);

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

        // Solo se rechaza si cambió la fecha a una pasada (editar otros datos de una cita antigua sigue permitido)
        if (!Objects.equals(cita.getFecha(), existente.getFecha()) && fechaPasada(cita.getFecha())) {
            return "redirect:/editarCita/" + cita.getId() + "?error=fecha-pasada";
        }

        if ("PENDIENTE".equals(existente.getEstado())) {
            cita.setEstado("PENDIENTE");
            cita.setUsuario(existente.getUsuario());

            Usuario duenio = perfilDe(existente.getUsuario());
            if (duenio != null && duenio.getNombre() != null && !duenio.getNombre().isBlank()) {
                cita.setNombre(duenio.getNombre());
            }

            // El tipo de sangre sigue al perfil del dueño; sin perfil se respeta lo enviado (o lo que ya tenía)
            String sangrePerfil = sangreDePerfil(duenio);
            if (sangrePerfil != null) {
                cita.setTipoSangre(sangrePerfil);
            } else if (!tipoSangreValido(cita.getTipoSangre())) {
                cita.setTipoSangre(existente.getTipoSangre());
            }

            boolean mismoHorario = Objects.equals(cita.getLugar(), existente.getLugar())
                    && Objects.equals(cita.getFecha(), existente.getFecha())
                    && Objects.equals(cita.getHora(), existente.getHora());

            if (mismoHorario) {
                // La cita ya ocupa su propio cupo: no se vuelve a validar (aunque el banco haya cambiado su capacidad)
                citaRepository.save(cita);
            } else {
                String errorFranja = errorDeFranja(cita, cita.getId());
                if (errorFranja != null) {
                    return "redirect:/editarCita/" + cita.getId() + "?error=" + errorFranja;
                }

                // Al editar, la propia cita no cuenta contra el cupo de su hora actual
                if (!capacidadService.guardarSiHayCupo(cita)) {
                    return redirectCupoLleno("/editarCita/" + cita.getId(), cita);
                }
            }
        }

        if (esAdmin()) {
            return "redirect:/admin/misCitas";
        }

        return "redirect:/misCitas";
    }
}