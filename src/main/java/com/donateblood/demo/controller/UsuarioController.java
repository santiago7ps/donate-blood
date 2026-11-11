package com.donateblood.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.donateblood.demo.ml.Recordatorio;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.model.Cita;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.util.ImageOptimizer;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CitaRepository citaRepo;

    @Value("${app.uploads.dir:uploads/perfiles}")
    private String uploadsDir;

    private String validarRolPublico(String rol) {
        if ("ROLE_CENTRO".equals(rol) || "ROLE_BANCO".equals(rol)) {
            return rol;
        }
        return "ROLE_USER";
    }

    @GetMapping("/register")
    public String mostrarRegistro(@RequestParam(required = false) String rol, Model model) {

        rol = validarRolPublico(rol);

        model.addAttribute("rol", rol);

        if ("ROLE_CENTRO".equals(rol) || "ROLE_BANCO".equals(rol)) {
            return "register/register";
        }

        return "register/register";
    }

    @PostMapping("/register")
    public String registrarUsuario(
            @ModelAttribute Usuario usuario,
            @RequestParam String confirmPassword,
            @RequestParam(required = false) String rol,
            Model model) {

        rol = validarRolPublico(rol);

        model.addAttribute("rol", rol);
        model.addAttribute("usuario", usuario);

        boolean hayError = false;

        if ("ROLE_USER".equals(rol)) {

            if (usuario.getNombre() == null || usuario.getNombre().trim().length() < 5) {
                model.addAttribute("errorNombre", "El nombre debe tener al menos 5 caracteres");
                hayError = true;
            }

            if (usuario.getNumPhone() == null || !usuario.getNumPhone().matches("\\d{10}")) {
                model.addAttribute("errorPhone", "El teléfono debe tener exactamente 10 dígitos");
                hayError = true;
            }

            if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
                model.addAttribute("errorEmail", "El correo es obligatorio");
                hayError = true;
            }

            // ===== VALIDACIÓN DE EDAD (NUEVO) =====
            if (usuario.getEdad() == null) {
                model.addAttribute("errorEdad", "La edad es obligatoria");
                hayError = true;
            } else if (usuario.getEdad() < 18) {
                model.addAttribute("errorEdad", "Usted es menor de edad, por lo tanto no puede donar");
                hayError = true;
            } else if (usuario.getEdad() > 120) {
                model.addAttribute("errorEdad", "Ingresa una edad válida");
                hayError = true;
            }
            // ======================================

            if (usuario.getDocumento() == null || !usuario.getDocumento().trim().matches("\\d{6,15}")) {
                model.addAttribute("errorDocumento", "El número de documento debe tener entre 6 y 15 dígitos");
                hayError = true;
            }
        }

        if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()) {
            model.addAttribute("errorUsername", "El usuario es obligatorio");
            hayError = true;
        } else if (!usuarioRepo.findAllByUsername(usuario.getUsername()).isEmpty()) {
            model.addAttribute("errorUsername", "El usuario ya existe");
            hayError = true;
        }

        if (usuario.getPassword() == null || usuario.getPassword().length() < 6) {
            model.addAttribute("errorPassword", "La contraseña debe tener mínimo 6 caracteres");
            hayError = true;
        }

        if (confirmPassword == null || usuario.getPassword() == null ||
                !usuario.getPassword().equals(confirmPassword)) {
            model.addAttribute("errorConfirm", "Las contraseñas no coinciden");
            hayError = true;
        }

        if (hayError) {
            if ("ROLE_CENTRO".equals(rol) || "ROLE_BANCO".equals(rol)) {
                return "register/register";
            }
            return "register/register";
        }

        usuario.setRol(rol);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        if ("ROLE_CENTRO".equals(rol) || "ROLE_BANCO".equals(rol)) {
            if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
                usuario.setNombre(usuario.getUsername());
            }
            if (usuario.getDocumento() == null || usuario.getDocumento().isBlank()) {
                usuario.setDocumento(usuario.getUsername());
            }
            if (usuario.getNumPhone() == null || usuario.getNumPhone().isBlank()) {
                usuario.setNumPhone(usuario.getUsername());
            }
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                usuario.setEmail(usuario.getUsername() + "@donateblood.local");
            }
        }

        try {
            usuarioRepo.save(usuario);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorUsername",
                    "No se pudo completar el registro. Intenta con otro usuario o inténtalo de nuevo.");
            if ("ROLE_CENTRO".equals(rol) || "ROLE_BANCO".equals(rol)) {
                return "register/register";
            }
            return "register/register";
        }

        return "redirect:/login?rol=" + rol;
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "logins/login";
    }

    @GetMapping("/donantePanel")
    public String panelDonante(Model model, Authentication auth) {

        String username = auth.getName();

        List<Usuario> encontrados = usuarioRepo.findAllByUsername(username);
        Usuario usuario = encontrados.isEmpty() ? null : encontrados.get(0);

        model.addAttribute("usuario", usuario);
        model.addAttribute("recordatorios",
                Recordatorio.para(citaRepo.findByUsuario(username), LocalDate.now(ZoneId.of("America/Bogota"))));

        return "donantePanel";
    }

    @GetMapping("/perfilDonante")
    public String verMiPerfil(Model model, Authentication auth) {
        String username = auth.getName();
        List<Usuario> encontrados = usuarioRepo.findAllByUsername(username);
        Usuario usuario = encontrados.isEmpty() ? null : encontrados.get(0);

        model.addAttribute("usuario", usuario);

        return "MiPerfil";
    }

    private Usuario usuarioActual(Authentication auth) {
        List<Usuario> encontrados = usuarioRepo.findAllByUsername(auth.getName());
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }

    @GetMapping("/editarPerfil")
    public String mostrarEditarPerfil(Model model, Authentication auth) {
        Usuario usuario = usuarioActual(auth);

        if (usuario == null || !"ROLE_USER".equals(usuario.getRol())) {
            return "redirect:/";
        }

        model.addAttribute("usuario", usuario);
        return "editarPerfil";
    }

    @PostMapping("/editarPerfil")
    public String guardarEditarPerfil(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String numPhone,
            @RequestParam(required = false) Integer edad,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmNewPassword,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile foto,
            Model model,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioActual(auth);

        if (usuario == null || !"ROLE_USER".equals(usuario.getRol())) {
            return "redirect:/";
        }

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String docLimpio = documento == null ? "" : documento.trim();
        String telLimpio = numPhone == null ? "" : numPhone.trim();
        boolean hayError = false;

        if (nombreLimpio.length() < 3) {
            model.addAttribute("errorNombre", "Ingresa tu nombre completo");
            hayError = true;
        }
        if (tipoDocumento == null || !List.of("CC", "CE", "PA", "PEP").contains(tipoDocumento)) {
            model.addAttribute("errorTipoDocumento", "Selecciona un tipo de documento");
            hayError = true;
        }
        if (!docLimpio.matches("\\d{6,15}")) {
            model.addAttribute("errorDocumento", "El número de documento debe tener entre 6 y 15 dígitos");
            hayError = true;
        }
        if (!telLimpio.matches("\\d{10}")) {
            model.addAttribute("errorPhone", "El teléfono debe tener exactamente 10 dígitos");
            hayError = true;
        }
        if (edad == null) {
            model.addAttribute("errorEdad", "La edad es obligatoria");
            hayError = true;
        } else if (edad < 18) {
            model.addAttribute("errorEdad", "Usted es menor de edad, por lo tanto no puede donar");
            hayError = true;
        } else if (edad > 120) {
            model.addAttribute("errorEdad", "Ingresa una edad válida");
            hayError = true;
        }

        boolean quiereCambiarPassword = newPassword != null && !newPassword.isBlank();
        boolean sinPassword = usuario.getPassword() == null || usuario.getPassword().isBlank();

        if (quiereCambiarPassword) {
            if (!sinPassword && (currentPassword == null
                    || !passwordEncoder.matches(currentPassword, usuario.getPassword()))) {
                model.addAttribute("errorCurrentPassword", "La contraseña actual no es correcta");
                hayError = true;
            }
            if (newPassword.length() < 6) {
                model.addAttribute("errorNewPassword", "La nueva contraseña debe tener mínimo 6 caracteres");
                hayError = true;
            }
            if (confirmNewPassword == null || !newPassword.equals(confirmNewPassword)) {
                model.addAttribute("errorConfirmNewPassword", "Las contraseñas no coinciden");
                hayError = true;
            }
        }

        if (foto != null && !foto.isEmpty()) {
            String tipo = foto.getContentType();
            if (tipo == null || !tipo.startsWith("image/")) {
                model.addAttribute("errorFoto", "El archivo debe ser una imagen (JPG o PNG)");
                hayError = true;
            } else if (foto.getSize() > 5L * 1024 * 1024) {
                model.addAttribute("errorFoto", "La imagen no puede superar 5 MB");
                hayError = true;
            }
        }

        if (hayError) {
            // Se devuelve lo que escribió (sin guardar) para que no tenga que repetirlo
            usuario.setNombre(nombreLimpio);
            usuario.setTipoDocumento(tipoDocumento);
            usuario.setDocumento(docLimpio);
            usuario.setNumPhone(telLimpio);
            usuario.setEdad(edad);
            model.addAttribute("usuario", usuario);
            return "editarPerfil";
        }

        usuario.setNombre(nombreLimpio);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setDocumento(docLimpio);
        usuario.setNumPhone(telLimpio);
        usuario.setEdad(edad);

        if (quiereCambiarPassword) {
            usuario.setPassword(passwordEncoder.encode(newPassword));
        }

        if (foto != null && !foto.isEmpty()) {
            try {
                String nombreAnterior = usuario.getFotoPerfil();
                String nombreNuevo = usuario.getId() + "_" + System.currentTimeMillis() + ".jpg";
                Path dir = Paths.get(uploadsDir).toAbsolutePath().normalize();
                Path destino = dir.resolve(nombreNuevo);

                ImageOptimizer.guardarOptimizada(foto, destino, 400, 0.8f);
                usuario.setFotoPerfil(nombreNuevo);

                if (nombreAnterior != null && !nombreAnterior.isBlank()) {
                    Files.deleteIfExists(dir.resolve(nombreAnterior));
                }
            } catch (IOException e) {
                model.addAttribute("errorFoto", "No se pudo procesar la imagen");
                model.addAttribute("usuario", usuario);
                return "editarPerfil";
            }
        }

        usuarioRepo.save(usuario);

        redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente.");
        return "redirect:/perfilDonante";
    }

    @GetMapping("/completarPerfil")
    public String mostrarCompletarPerfil(Model model, Authentication auth) {
        Usuario usuario = usuarioActual(auth);

        if (usuario == null || !"ROLE_USER".equals(usuario.getRol())) {
            return "redirect:/";
        }
        if (usuario.tienePerfilCompleto()) {
            return "redirect:/donantePanel";
        }

        model.addAttribute("usuario", usuario);
        return "completarPerfil";
    }

    @PostMapping("/completarPerfil")
    public String guardarCompletarPerfil(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) String numPhone,
            @RequestParam(required = false) Integer edad,
            Model model,
            Authentication auth) {

        Usuario usuario = usuarioActual(auth);

        if (usuario == null || !"ROLE_USER".equals(usuario.getRol())) {
            return "redirect:/";
        }

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String docLimpio = documento == null ? "" : documento.trim();
        String telLimpio = numPhone == null ? "" : numPhone.trim();
        boolean hayError = false;

        if (nombreLimpio.length() < 3) {
            model.addAttribute("errorNombre", "Ingresa tu nombre completo");
            hayError = true;
        }
        if (tipoDocumento == null || !List.of("CC", "CE", "PA", "PEP").contains(tipoDocumento)) {
            model.addAttribute("errorTipoDocumento", "Selecciona un tipo de documento");
            hayError = true;
        }
        if (!docLimpio.matches("\\d{6,15}")) {
            model.addAttribute("errorDocumento", "El número de documento debe tener entre 6 y 15 dígitos");
            hayError = true;
        }
        if (!telLimpio.matches("\\d{10}")) {
            model.addAttribute("errorPhone", "El teléfono debe tener exactamente 10 dígitos");
            hayError = true;
        }
        if (edad == null) {
            model.addAttribute("errorEdad", "La edad es obligatoria");
            hayError = true;
        } else if (edad < 18) {
            model.addAttribute("errorEdad", "Usted es menor de edad, por lo tanto no puede donar");
            hayError = true;
        } else if (edad > 120) {
            model.addAttribute("errorEdad", "Ingresa una edad válida");
            hayError = true;
        }

        if (hayError) {
            // Se devuelve lo que escribió (sin guardar) para que no tenga que repetirlo
            usuario.setNombre(nombreLimpio);
            usuario.setTipoDocumento(tipoDocumento);
            usuario.setDocumento(docLimpio);
            usuario.setNumPhone(telLimpio);
            usuario.setEdad(edad);
            model.addAttribute("usuario", usuario);
            return "completarPerfil";
        }

        usuario.setNombre(nombreLimpio);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setDocumento(docLimpio);
        usuario.setNumPhone(telLimpio);
        usuario.setEdad(edad);
        usuarioRepo.save(usuario);

        return "redirect:/donantePanel";
    }

    @GetMapping("/verCitas")
    public String verCitas(Model model, Authentication auth) {

        String username = auth.getName();

        List<Cita> citas = citaRepo.findByUsuario(username);

        model.addAttribute("citas", citas);

        return "verCitas";
    }
}