package com.donateblood.demo.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Encargo;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.EncargoRepository;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.service.CapacidadService;
import com.donateblood.demo.util.CitasYSangre;

/**
 * Gestión del administrador: usuarios (donantes, bancos de sangre, centros médicos y
 * administradores), citas y pedidos.
 *
 * Es un controlador aparte de AdminController para no modificar lo que ya funciona.
 * Todas las rutas /admin/** ya están protegidas con hasRole("ADMIN") en SecurityConfig.
 */
@Controller
@RequestMapping("/admin")
public class AdminGestionController {

    private static final String ROL_USER = "ROLE_USER";
    private static final String ROL_BANCO = "ROLE_BANCO";
    private static final String ROL_CENTRO = "ROLE_CENTRO";
    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private static final List<String> ROLES = Arrays.asList(ROL_USER, ROL_BANCO, ROL_CENTRO, ROL_ADMIN);
    private static final List<String> TIPOS_DOC = Arrays.asList("CC", "CE", "PA", "PEP");
    private static final List<String> ESTADOS_ENCARGO = Arrays.asList("PENDIENTE", "ENTREGADO", "CANCELADO");

    private static final Map<String, String> ETIQUETAS = new LinkedHashMap<>();
    static {
        ETIQUETAS.put(ROL_USER, "Donante");
        ETIQUETAS.put(ROL_BANCO, "Banco de sangre");
        ETIQUETAS.put(ROL_CENTRO, "Centro médico");
        ETIQUETAS.put(ROL_ADMIN, "Administrador");
    }

    private static final int POR_PAGINA = 15;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private EncargoRepository encargoRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ utilidades

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean contiene(String texto, String minuscula) {
        return texto != null && texto.toLowerCase().contains(minuscula);
    }

    private static String nombreVisible(Usuario u) {
        if (u.getNombre() != null && !u.getNombre().isBlank()) return u.getNombre();
        return u.getUsername() == null ? "" : u.getUsername();
    }

    /** Nombre con el que las citas identifican a un banco (mismo criterio que CapacidadService). */
    private static String lugarDe(Usuario banco) {
        if (banco.getNombre() != null && !banco.getNombre().isBlank()) return banco.getNombre();
        return banco.getUsername();
    }

    private boolean tipoSangreValido(String tipo) {
        return tipo != null && Arrays.asList(CitasYSangre.TIPOS_SANGRE).contains(tipo.trim());
    }

    private void prepararFormulario(Model model, Usuario usuario, boolean esNuevo, Map<String, String> errores) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("esNuevo", esNuevo);
        model.addAttribute("errores", errores);
        model.addAttribute("roles", ROLES);
        model.addAttribute("etiquetas", ETIQUETAS);
        model.addAttribute("tiposSangre", CitasYSangre.TIPOS_SANGRE);
        model.addAttribute("tiposDoc", TIPOS_DOC);
        model.addAttribute("capacidadMax", CapacidadService.CAPACIDAD_MAXIMA);
    }

    // ------------------------------------------------------------------ listado

    @GetMapping("/usuarios")
    public String listar(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            Authentication auth) {

        final String rolFiltro = (rol != null && ROLES.contains(rol)) ? rol : null;
        final String busqueda = limpio(q);
        final String busquedaMin = busqueda.toLowerCase();

        List<Usuario> filtrados = usuarioRepo.findAll().stream()
                .filter(u -> rolFiltro == null || rolFiltro.equals(u.getRol()))
                .filter(u -> busquedaMin.isEmpty()
                        || contiene(u.getNombre(), busquedaMin)
                        || contiene(u.getUsername(), busquedaMin)
                        || contiene(u.getEmail(), busquedaMin)
                        || contiene(u.getDocumento(), busquedaMin))
                .sorted(Comparator.comparing((Usuario u) -> nombreVisible(u).toLowerCase()))
                .collect(Collectors.toList());

        int totalPaginas = Math.max(1, (int) Math.ceil(filtrados.size() / (double) POR_PAGINA));
        int pagina = Math.min(Math.max(page, 0), totalPaginas - 1);
        int desde = pagina * POR_PAGINA;
        int hasta = Math.min(desde + POR_PAGINA, filtrados.size());

        model.addAttribute("usuarios", filtrados.subList(desde, hasta));
        model.addAttribute("total", filtrados.size());
        model.addAttribute("pagina", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("rolFiltro", rolFiltro == null ? "" : rolFiltro);
        model.addAttribute("q", busqueda);
        model.addAttribute("yo", auth.getName());
        model.addAttribute("etiquetas", ETIQUETAS);

        model.addAttribute("cuentaTodos", usuarioRepo.count());
        model.addAttribute("cuentaDonantes", usuarioRepo.countByRol(ROL_USER));
        model.addAttribute("cuentaBancos", usuarioRepo.countByRol(ROL_BANCO));
        model.addAttribute("cuentaCentros", usuarioRepo.countByRol(ROL_CENTRO));
        model.addAttribute("cuentaAdmins", usuarioRepo.countByRol(ROL_ADMIN));

        return "admin/admin_usuarios";
    }

    // ------------------------------------------------------------------ crear

    @GetMapping("/usuarios/nuevo")
    public String nuevo(@RequestParam(required = false) String rol, Model model) {
        Usuario u = new Usuario();
        u.setRol((rol != null && ROLES.contains(rol)) ? rol : ROL_USER);
        prepararFormulario(model, u, true, new LinkedHashMap<>());
        return "admin/admin_usuario_form";
    }

    @PostMapping("/usuarios/crear")
    public String crear(
            @ModelAttribute Usuario datos,
            BindingResult binding,
            @RequestParam(required = false) String confirmPassword,
            Model model,
            RedirectAttributes ra) {

        // Nunca se guarda el objeto recibido del formulario: se copian solo los campos permitidos
        datos.setId(null);
        datos.setUsername(limpio(datos.getUsername()));

        Map<String, String> errores = validar(datos, true, null, datos.getPassword(), confirmPassword);
        marcarErroresNumericos(binding, errores);

        String username = datos.getUsername();
        if (username.isEmpty() || !username.matches("[A-Za-z0-9._@+\\-]{3,50}")) {
            errores.put("username", "Usuario de 3 a 50 caracteres (letras, números y . _ @ + -)");
        } else if (!usuarioRepo.findAllByUsername(username).isEmpty()) {
            errores.put("username", "Ese nombre de usuario ya existe");
        }

        if (!errores.isEmpty()) {
            prepararFormulario(model, datos, true, errores);
            return "admin/admin_usuario_form";
        }

        Usuario u = new Usuario();
        u.setUsername(username);
        u.setRol(datos.getRol());
        u.setPassword(passwordEncoder.encode(datos.getPassword()));
        copiarCampos(datos, u);

        try {
            usuarioRepo.save(u);
        } catch (Exception e) {
            errores.put("username", "No se pudo guardar. Intenta con otro usuario.");
            prepararFormulario(model, datos, true, errores);
            return "admin/admin_usuario_form";
        }

        ra.addFlashAttribute("mensaje", "Usuario \"" + username + "\" creado correctamente.");
        return "redirect:/admin/usuarios?rol=" + u.getRol();
    }

    // ------------------------------------------------------------------ editar

    @GetMapping("/usuarios/{id}/editar")
    public String editar(@PathVariable String id, Model model, RedirectAttributes ra) {
        Usuario u = usuarioRepo.findById(id).orElse(null);
        if (u == null) {
            ra.addFlashAttribute("error", "El usuario no existe o ya fue eliminado.");
            return "redirect:/admin/usuarios";
        }
        u.setPassword(null); // nunca se muestra el hash en el formulario
        prepararFormulario(model, u, false, new LinkedHashMap<>());
        return "admin/admin_usuario_form";
    }

    @PostMapping("/usuarios/{id}/editar")
    public String guardarEdicion(
            @PathVariable String id,
            @ModelAttribute Usuario datos,
            BindingResult binding,
            @RequestParam(required = false) String confirmPassword,
            Authentication auth,
            Model model,
            RedirectAttributes ra) {

        Usuario u = usuarioRepo.findById(id).orElse(null);
        if (u == null) {
            ra.addFlashAttribute("error", "El usuario no existe o ya fue eliminado.");
            return "redirect:/admin/usuarios";
        }

        // El nombre de usuario no se cambia: las citas se enlazan con él
        datos.setId(u.getId());
        datos.setUsername(u.getUsername());

        String nuevaClave = datos.getPassword() == null ? "" : datos.getPassword();
        Map<String, String> errores = validar(datos, false, u.getEmail(),
                nuevaClave.isEmpty() ? null : nuevaClave, confirmPassword);
        marcarErroresNumericos(binding, errores);

        String rolAnterior = u.getRol();
        String rolNuevo = datos.getRol();
        boolean cambiaRol = rolNuevo != null && !rolNuevo.equals(rolAnterior);

        if (cambiaRol && ROLES.contains(rolNuevo)) {
            if (u.getUsername().equals(auth.getName())) {
                errores.put("rol", "No puedes cambiar tu propio rol.");
            } else if (ROL_ADMIN.equals(rolAnterior) && usuarioRepo.countByRol(ROL_ADMIN) <= 1) {
                errores.put("rol", "No puedes quitarle el rol al único administrador.");
            }
        }

        if (!errores.isEmpty()) {
            datos.setPassword(null);
            prepararFormulario(model, datos, false, errores);
            return "admin/admin_usuario_form";
        }

        String lugarAnterior = ROL_BANCO.equals(rolAnterior) ? lugarDe(u) : null;
        String nombreAnterior = u.getNombre();

        u.setRol(rolNuevo);
        copiarCampos(datos, u);
        if (!nuevaClave.isEmpty()) {
            u.setPassword(passwordEncoder.encode(nuevaClave));
        }
        usuarioRepo.save(u);

        // Si se renombró un banco, sus citas deben seguir apuntando a él
        if (lugarAnterior != null && ROL_BANCO.equals(u.getRol())) {
            String lugarNuevo = lugarDe(u);
            if (!lugarAnterior.equals(lugarNuevo)) {
                List<Cita> citas = citaRepo.findByLugar(lugarAnterior);
                for (Cita c : citas) {
                    c.setLugar(lugarNuevo);
                }
                citaRepo.saveAll(citas);
            }
        }

        // Si un donante cambió de nombre, se actualiza en sus citas
        if (ROL_USER.equals(u.getRol()) && !limpio(nombreAnterior).equals(limpio(u.getNombre()))) {
            List<Cita> citas = citaRepo.findByUsuario(u.getUsername());
            for (Cita c : citas) {
                c.setNombre(u.getNombre());
            }
            citaRepo.saveAll(citas);
        }

        ra.addFlashAttribute("mensaje", "Datos de \"" + u.getUsername() + "\" actualizados.");
        return "redirect:/admin/usuarios?rol=" + u.getRol();
    }

    // ------------------------------------------------------------------ eliminar

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminar(@PathVariable String id, Authentication auth, RedirectAttributes ra) {

        Usuario u = usuarioRepo.findById(id).orElse(null);
        if (u == null) {
            ra.addFlashAttribute("error", "El usuario no existe o ya fue eliminado.");
            return "redirect:/admin/usuarios";
        }

        String destino = "redirect:/admin/usuarios" + (u.getRol() != null ? "?rol=" + u.getRol() : "");

        if (u.getUsername() != null && u.getUsername().equals(auth.getName())) {
            ra.addFlashAttribute("error", "No puedes eliminar tu propia cuenta.");
            return destino;
        }

        if (ROL_ADMIN.equals(u.getRol()) && usuarioRepo.countByRol(ROL_ADMIN) <= 1) {
            ra.addFlashAttribute("error", "No se puede eliminar al único administrador.");
            return destino;
        }

        // Las citas pendientes del usuario (o del banco) se cancelan; el historial se conserva
        List<Cita> afectadas = new ArrayList<>();
        if (ROL_USER.equals(u.getRol())) {
            afectadas.addAll(citaRepo.findByUsuario(u.getUsername()));
        } else if (ROL_BANCO.equals(u.getRol())) {
            afectadas.addAll(citaRepo.findByLugar(lugarDe(u)));
        }

        int canceladas = 0;
        for (Cita c : afectadas) {
            if ("PENDIENTE".equals(c.getEstado())) {
                c.setEstado("CANCELADA");
                canceladas++;
            }
        }
        if (!afectadas.isEmpty()) {
            citaRepo.saveAll(afectadas);
        }

        usuarioRepo.deleteById(u.getId());

        String msg = "Usuario \"" + u.getUsername() + "\" eliminado.";
        if (canceladas > 0) {
            msg += " Se cancelaron " + canceladas + " cita(s) pendiente(s).";
        }
        ra.addFlashAttribute("mensaje", msg);
        return destino;
    }

    // ------------------------------------------------------------------ citas

    @PostMapping("/citas/{id}/eliminar")
    public String eliminarCita(@PathVariable String id, RedirectAttributes ra) {
        if (citaRepo.existsById(id)) {
            citaRepo.deleteById(id);
            ra.addFlashAttribute("mensaje", "Cita eliminada.");
        } else {
            ra.addFlashAttribute("error", "La cita no existe o ya fue eliminada.");
        }
        return "redirect:/admin/misCitas";
    }

    // ------------------------------------------------------------------ pedidos (encargos)

    @GetMapping("/encargos")
    public String encargos(Model model) {
        List<Encargo> lista = encargoRepo.findAll().stream()
                .sorted(Comparator.comparing((Encargo e) -> limpio(e.getFecha())).reversed())
                .collect(Collectors.toList());
        model.addAttribute("encargos", lista);
        model.addAttribute("estados", ESTADOS_ENCARGO);
        return "admin/admin_encargos";
    }

    @PostMapping("/encargos/{id}/estado")
    public String cambiarEstadoEncargo(
            @PathVariable String id,
            @RequestParam String estado,
            RedirectAttributes ra) {

        Encargo e = encargoRepo.findById(id).orElse(null);
        if (e == null || !ESTADOS_ENCARGO.contains(estado)) {
            ra.addFlashAttribute("error", "No se pudo actualizar el pedido.");
        } else {
            e.setEstado(estado);
            encargoRepo.save(e);
            ra.addFlashAttribute("mensaje", "Pedido marcado como " + estado + ".");
        }
        return "redirect:/admin/encargos";
    }

    @PostMapping("/encargos/{id}/eliminar")
    public String eliminarEncargo(@PathVariable String id, RedirectAttributes ra) {
        if (encargoRepo.existsById(id)) {
            encargoRepo.deleteById(id);
            ra.addFlashAttribute("mensaje", "Pedido eliminado.");
        } else {
            ra.addFlashAttribute("error", "El pedido no existe o ya fue eliminado.");
        }
        return "redirect:/admin/encargos";
    }

    // ------------------------------------------------------------------ validación y copia de campos

    /**
     * Valida el formulario. En edición los datos del perfil del donante pueden quedar vacíos
     * (por ejemplo, cuentas creadas con Google que aún no completan su perfil); si se escriben, deben ser válidos.
     */
    private Map<String, String> validar(Usuario d, boolean esNuevo, String emailActual,
                                        String clave, String confirmacion) {

        Map<String, String> err = new LinkedHashMap<>();
        String rol = d.getRol();

        if (rol == null || !ROLES.contains(rol)) {
            err.put("rol", "Selecciona un rol válido");
            return err;
        }

        if (limpio(d.getNombre()).length() < 3) {
            err.put("nombre", "El nombre debe tener al menos 3 caracteres");
        }

        String email = limpio(d.getEmail());
        if (email.isEmpty()) {
            if (ROL_USER.equals(rol)) {
                err.put("email", "El correo es obligatorio para los donantes");
            }
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+$")) {
            err.put("email", "Ingresa un correo válido");
        } else if (esNuevo || !email.equalsIgnoreCase(limpio(emailActual))) {
            if (!usuarioRepo.findAllByEmailIgnoreCase(email).isEmpty()) {
                err.put("email", "Ya existe otro usuario con ese correo");
            }
        }

        if (ROL_USER.equals(rol)) {
            String tipoDoc = limpio(d.getTipoDocumento());
            if (tipoDoc.isEmpty() ? esNuevo : !TIPOS_DOC.contains(tipoDoc)) {
                err.put("tipoDocumento", "Selecciona un tipo de documento");
            }

            String doc = limpio(d.getDocumento());
            if ((esNuevo || !doc.isEmpty()) && !doc.matches("\\d{6,15}")) {
                err.put("documento", "El documento debe tener entre 6 y 15 dígitos");
            }

            String tel = limpio(d.getNumPhone());
            if ((esNuevo || !tel.isEmpty()) && !tel.matches("\\d{10}")) {
                err.put("numPhone", "El teléfono debe tener exactamente 10 dígitos");
            }

            if (d.getEdad() == null) {
                if (esNuevo) err.put("edad", "La edad es obligatoria");
            } else if (d.getEdad() < 18 || d.getEdad() > 120) {
                err.put("edad", "La edad debe estar entre 18 y 120");
            }

            String sangre = limpio(d.getTipoSangre());
            if ((esNuevo || !sangre.isEmpty()) && !tipoSangreValido(sangre)) {
                err.put("tipoSangre", "Selecciona un tipo de sangre válido");
            }
        } else {
            if (limpio(d.getDocumento()).length() > 40) {
                err.put("documento", "Máximo 40 caracteres");
            }
            if (limpio(d.getNumPhone()).length() > 40) {
                err.put("numPhone", "Máximo 40 caracteres");
            }
        }

        if (ROL_BANCO.equals(rol) && d.getCapacidadAtencion() != null) {
            int c = d.getCapacidadAtencion();
            if (c < 1 || c > CapacidadService.CAPACIDAD_MAXIMA) {
                err.put("capacidadAtencion",
                        "Debe estar entre 1 y " + CapacidadService.CAPACIDAD_MAXIMA);
            }
        }

        if (esNuevo && (clave == null || clave.length() < 6)) {
            err.put("password", "La contraseña debe tener mínimo 6 caracteres");
        } else if (clave != null && clave.length() < 6) {
            err.put("password", "La contraseña debe tener mínimo 6 caracteres");
        } else if (clave != null && !clave.equals(confirmacion)) {
            err.put("confirmPassword", "Las contraseñas no coinciden");
        }

        return err;
    }

    /** Si en edad o capacidad escribieron algo que no es un número, se muestra el error en ese campo. */
    private void marcarErroresNumericos(BindingResult binding, Map<String, String> errores) {
        if (binding.hasFieldErrors("edad")) {
            errores.put("edad", "Ingresa un número válido");
        }
        if (binding.hasFieldErrors("capacidadAtencion")) {
            errores.put("capacidadAtencion", "Ingresa un número válido");
        }
    }

    /** Copia al usuario guardado solo los campos que el administrador puede editar. */
    private void copiarCampos(Usuario d, Usuario u) {
        String rol = u.getRol();

        u.setNombre(limpio(d.getNombre()));

        String email = limpio(d.getEmail());
        if (email.isEmpty() && !ROL_USER.equals(rol)) {
            email = u.getUsername() + "@donateblood.local";
        }
        u.setEmail(email);

        String doc = limpio(d.getDocumento());
        String tel = limpio(d.getNumPhone());

        if (ROL_USER.equals(rol)) {
            u.setTipoDocumento(limpio(d.getTipoDocumento()).isEmpty() ? u.getTipoDocumento() : limpio(d.getTipoDocumento()));
            u.setDocumento(doc.isEmpty() ? u.getDocumento() : doc);
            u.setNumPhone(tel.isEmpty() ? u.getNumPhone() : tel);
            if (d.getEdad() != null) u.setEdad(d.getEdad());
            if (!limpio(d.getTipoSangre()).isEmpty()) u.setTipoSangre(limpio(d.getTipoSangre()));
        } else {
            // Bancos y centros: el registro público usa el usuario como valor de relleno
            boolean relleno = ROL_BANCO.equals(rol) || ROL_CENTRO.equals(rol);
            u.setDocumento(doc.isEmpty() ? (relleno ? u.getUsername() : null) : doc);
            u.setNumPhone(tel.isEmpty() ? (relleno ? u.getUsername() : null) : tel);
        }

        if (ROL_BANCO.equals(rol) && d.getCapacidadAtencion() != null) {
            u.setCapacidadAtencion(d.getCapacidadAtencion());
        }
    }
}
