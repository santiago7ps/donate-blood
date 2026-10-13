package com.donateblood.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.model.Cita;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.repository.CitaRepository;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CitaRepository citaRepo;

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

    @GetMapping("/verCitas")
    public String verCitas(Model model, Authentication auth) {

        String username = auth.getName();

        List<Cita> citas = citaRepo.findByUsuario(username);

        model.addAttribute("citas", citas);

        return "verCitas";
    }
}