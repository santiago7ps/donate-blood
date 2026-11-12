package com.donateblood.demo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donateblood.demo.Seguridad.JwtService;
import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.service.CapacidadService;
import com.donateblood.demo.util.CitasYSangre;

/**
 * API REST (JSON) para la app Flutter del donante.
 * Autenticación: header "Authorization: Bearer <token>" (el mismo JWT de la web).
 */
@RestController
@RequestMapping("/api")
public class ApiMovilController {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtService jwtService;
    @Autowired private CapacidadService capacidadService;

    // ---------- Respuestas de error ----------

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }

    private Usuario usuarioActual(Authentication auth) {
        List<Usuario> encontrados = usuarioRepo.findAllByUsername(auth.getName());
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    private Map<String, Object> usuarioAJson(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nombre", u.getNombre());
        m.put("username", u.getUsername());
        m.put("tipoDocumento", u.getTipoDocumento());
        m.put("documento", u.getDocumento());
        m.put("numPhone", u.getNumPhone());
        m.put("edad", u.getEdad());
        m.put("email", u.getEmail());
        return m;
    }

    // ---------- Registro ----------

    public record RegistroRequest(
            String nombre, String username, String tipoDocumento, String documento,
            String numPhone, Integer edad, String email,
            String password, String confirmPassword) {}

    @PostMapping("/auth/register")
    public ResponseEntity<?> registrar(@RequestBody RegistroRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (r.nombre() == null || r.nombre().trim().length() < 5)
            errores.put("nombre", "El nombre debe tener al menos 5 caracteres");
        if (r.numPhone() == null || !r.numPhone().matches("\\d{10}"))
            errores.put("numPhone", "El teléfono debe tener exactamente 10 dígitos");
        if (r.email() == null || r.email().trim().isEmpty())
            errores.put("email", "El correo es obligatorio");
        if (r.edad() == null)
            errores.put("edad", "La edad es obligatoria");
        else if (r.edad() < 18)
            errores.put("edad", "Usted es menor de edad, por lo tanto no puede donar");
        else if (r.edad() > 120)
            errores.put("edad", "Ingresa una edad válida");
        if (r.tipoDocumento() == null || r.tipoDocumento().isBlank())
            errores.put("tipoDocumento", "Selecciona el tipo de documento");
        if (r.documento() == null || !r.documento().trim().matches("\\d{6,15}"))
            errores.put("documento", "El número de documento debe tener entre 6 y 15 dígitos");
        if (r.username() == null || r.username().trim().isEmpty())
            errores.put("username", "El usuario es obligatorio");
        else if (!usuarioRepo.findAllByUsername(r.username().trim()).isEmpty())
            errores.put("username", "El usuario ya existe");
        if (r.password() == null || r.password().length() < 6)
            errores.put("password", "La contraseña debe tener mínimo 6 caracteres");
        if (r.password() == null || !r.password().equals(r.confirmPassword()))
            errores.put("confirmPassword", "Las contraseñas no coinciden");

        if (!errores.isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("mensaje", "Revisa los datos del formulario");
            body.put("errores", errores);
            return ResponseEntity.badRequest().body(body);
        }

        Usuario u = new Usuario();
        u.setNombre(r.nombre().trim());
        u.setUsername(r.username().trim());
        u.setTipoDocumento(r.tipoDocumento());
        u.setDocumento(r.documento().trim());
        u.setNumPhone(r.numPhone());
        u.setEdad(r.edad());
        u.setEmail(r.email().trim());
        u.setPassword(passwordEncoder.encode(r.password()));
        u.setRol("ROLE_USER"); // la app es solo para donantes

        try {
            usuarioRepo.save(u);
        } catch (Exception e) {
            return error(HttpStatus.CONFLICT, "No se pudo completar el registro. Intenta con otro usuario.");
        }

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("mensaje", "Registro exitoso");
        return ResponseEntity.status(HttpStatus.CREATED).body(ok);
    }

    // ---------- Login ----------

    public record LoginRequest(String username, String password) {}

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest r) {
        if (r.username() == null || r.username().isBlank() || r.password() == null || r.password().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Ingresa tu usuario y contraseña");
        }
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(r.username().trim(), r.password()));

            boolean esDonante = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
            if (!esDonante) {
                return error(HttpStatus.FORBIDDEN, "Esta aplicación es solo para donantes");
            }

            String token = jwtService.generateToken((UserDetails) auth.getPrincipal());
            Usuario u = usuarioRepo.findAllByUsername(auth.getName()).get(0);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("token", token);
            body.put("usuario", usuarioAJson(u));
            return ResponseEntity.ok(body);

        } catch (AuthenticationException e) {
            return error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
    }

    // ---------- Perfil ----------

    @GetMapping("/perfil")
    public ResponseEntity<?> perfil(Authentication auth) {
        Usuario u = usuarioActual(auth);
        if (u == null) return error(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        return ResponseEntity.ok(usuarioAJson(u));
    }

    // ---------- Citas ----------

    /** Bancos registrados donde se puede agendar (el "lugar" de la cita es su nombre). */
    @GetMapping("/bancos-citas")
    public List<String> bancosParaCitas() {
        return usuarioRepo.findByRol("ROLE_BANCO").stream()
                .map(b -> (b.getNombre() != null && !b.getNombre().isBlank()) ? b.getNombre() : b.getUsername())
                .collect(Collectors.toList());
    }

    @GetMapping("/disponibilidad")
    public List<Map<String, Object>> disponibilidad(@RequestParam String lugar, @RequestParam String fecha) {
        return capacidadService.cuposPorHora(lugar, fecha, null).entrySet().stream()
                .map(e -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("hora", e.getKey());
                    fila.put("libres", e.getValue());
                    return fila;
                })
                .collect(Collectors.toList());
    }

    public record CitaRequest(String lugar, String fecha, String hora, String tipoSangre) {}

    @PostMapping("/citas")
    public ResponseEntity<?> agendar(@RequestBody CitaRequest r, Authentication auth) {
        Usuario u = usuarioActual(auth);
        if (u == null) return error(HttpStatus.NOT_FOUND, "Usuario no encontrado");

        if (r.lugar() == null || r.lugar().isBlank())
            return error(HttpStatus.BAD_REQUEST, "Selecciona un banco de sangre");
        if (r.tipoSangre() == null || !Arrays.asList(CitasYSangre.TIPOS_SANGRE).contains(r.tipoSangre()))
            return error(HttpStatus.BAD_REQUEST, "Selecciona un tipo de sangre válido");

        try {
            LocalDate fecha = LocalDate.parse(r.fecha());
            if (fecha.isBefore(LocalDate.now()))
                return error(HttpStatus.BAD_REQUEST, "La fecha no puede ser anterior a hoy");
        } catch (DateTimeParseException | NullPointerException e) {
            return error(HttpStatus.BAD_REQUEST, "Fecha inválida (usa el formato AAAA-MM-DD)");
        }

        if (!CitasYSangre.horaValida(r.hora()))
            return error(HttpStatus.BAD_REQUEST, "Hora inválida");

        Cita cita = new Cita();
        cita.setNombre(u.getNombre());
        cita.setUsuario(u.getUsername());
        cita.setLugar(r.lugar());
        cita.setFecha(r.fecha());
        cita.setHora(r.hora());
        cita.setTipoSangre(r.tipoSangre());
        cita.setEstado("PENDIENTE");

        if (!capacidadService.guardarSiHayCupo(cita)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("mensaje", "Esa hora ya no tiene cupo. Elige otra.");
            body.put("horasLibres", new ArrayList<>(capacidadService.horasConCupo(r.lugar(), r.fecha(), null)));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("mensaje", "Cita agendada");
        ok.put("id", cita.getId());
        ok.put("lugar", cita.getLugar());
        ok.put("fecha", cita.getFecha());
        ok.put("hora", cita.getHora());
        ok.put("estado", cita.getEstado());
        return ResponseEntity.status(HttpStatus.CREATED).body(ok);
    }
}
