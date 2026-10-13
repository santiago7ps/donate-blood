package com.donateblood.demo.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Donacion;
import com.donateblood.demo.model.Encargo;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.DonacionRepository;
import com.donateblood.demo.repository.EncargoRepository;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.util.CitasYSangre;
import com.github.javafaker.Faker;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final DonacionRepository donacionRepository;
    private final EncargoRepository encargoRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository,
            DonacionRepository donacionRepository,
            EncargoRepository encargoRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.donacionRepository = donacionRepository;
        this.encargoRepository = encargoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (usuarioRepository.count() > 0) {
            System.out.println("SEEDER OMITIDO: ya existen datos en la base de datos.");
            return;
        }

        System.out.println("SEEDER INICIADO...");

        Faker faker = new Faker(new Locale("es", "CO"));
        Random random = new Random();

        List<Usuario> usuarios = new ArrayList<>();
        List<Usuario> donantes = new ArrayList<>();
        List<String> nombresBancosCreados = new ArrayList<>();
        List<String> nombresCentrosCreados = new ArrayList<>();

        String[] dominios = {
                "@gmail.com",
                "@hotmail.com",
                "@outlook.com",
                "@yahoo.com",
                "@icloud.com"
        };

        for (int i = 0; i < 100; i++) {

            Usuario usuario = new Usuario();

            String username =
                    faker.name().firstName().toLowerCase()
                            + random.nextInt(99999);

            usuario.setNombre(faker.name().fullName());
            usuario.setUsername(username);
            usuario.setTipoDocumento("CC");
            usuario.setDocumento(faker.number().digits(10));
            usuario.setNumPhone("3" + faker.number().digits(9));
            usuario.setEmail(username + dominios[random.nextInt(dominios.length)]);
            usuario.setPassword(passwordEncoder.encode("123456"));
            usuario.setRol("ROLE_USER");

            usuarios.add(usuario);
            donantes.add(usuario);
        }

        String[] nombresBancos = {
                "Cruz Roja",
                "Banco Nacional de Sangre",
                "Hemocentro Distrital",
                "Banco de Sangre San Vicente",
                "Fundación Hemovida",
                "Hemobanco Central",
                "Banco de Sangre Clinica Colombia",
                "Red de Bancos de Sangre del Caribe"
        };

        for (int i = 0; i < 100; i++) {

            Usuario banco = new Usuario();

            String nombreBanco =
                    nombresBancos[random.nextInt(nombresBancos.length)] + " " + (i + 1);
            String username = "banco" + random.nextInt(999999);

            banco.setNombre(nombreBanco);
            banco.setUsername(username);
            banco.setPassword(passwordEncoder.encode("123456"));
            banco.setRol("ROLE_BANCO");

            usuarios.add(banco);
            nombresBancosCreados.add(nombreBanco);
        }

        String[] nombresCentros = {
                "Hospital Central",
                "Clinica Esperanza",
                "Centro Medico San Juan",
                "Hospital del Caribe",
                "Clinica Los Andes",
                "Hospital Universitario del Valle",
                "Clinica Santa Fe",
                "Centro Medico El Bosque",
                "Hospital San Rafael",
                "Clinica del Country"
        };

        for (int i = 0; i < 100; i++) {

            Usuario centro = new Usuario();

            String nombreCentro =
                    nombresCentros[random.nextInt(nombresCentros.length)] + " " + (i + 1);
            String username = "centro" + random.nextInt(999999);

            centro.setNombre(nombreCentro);
            centro.setUsername(username);
            centro.setPassword(passwordEncoder.encode("123456"));
            centro.setRol("ROLE_CENTRO");

            usuarios.add(centro);
            nombresCentrosCreados.add(nombreCentro);
        }

        usuarioRepository.saveAll(usuarios);

        System.out.println(usuarios.size() + " usuarios insertados correctamente.");

        System.out.println("Generando citas...");

        List<Cita> citas = new ArrayList<>();

        String[] tiposSangre = CitasYSangre.TIPOS_SANGRE;

        String[] estadosCita = {
                "PENDIENTE",
                "REALIZADA",
                "CANCELADA"
        };

        String[] franjasValidas = CitasYSangre.franjasValidas().toArray(new String[0]);

        for (int i = 0; i < 100; i++) {

            Cita cita = new Cita();

            Usuario donante = donantes.get(random.nextInt(donantes.size()));

            cita.setNombre(donante.getNombre());
            cita.setUsuario(donante.getUsername());

            cita.setFecha(
                    java.time.LocalDate.now()
                            .plusDays(random.nextInt(90))
                            .toString()
            );

            cita.setHora(franjasValidas[random.nextInt(franjasValidas.length)]);

            cita.setLugar(
                    nombresBancosCreados.get(random.nextInt(nombresBancosCreados.size()))
            );

            cita.setTipoSangre(
                    tiposSangre[random.nextInt(tiposSangre.length)]
            );

            cita.setEstado(
                    estadosCita[random.nextInt(estadosCita.length)]
            );

            citas.add(cita);
        }

        citaRepository.saveAll(citas);

        System.out.println(citas.size() + " citas insertadas correctamente.");

        System.out.println("Generando donaciones...");

        List<Donacion> donaciones = new ArrayList<>();

        String[] estadosDonacion = {
                "COMPLETADA",
                "PENDIENTE",
                "CANCELADA"
        };

        Double[] cantidadesDonacion = {
                250.0,
                300.0,
                350.0,
                400.0,
                450.0
        };

        for (int i = 0; i < 100; i++) {

            Donacion donacion = new Donacion();

            donacion.setDonante(
                    donantes.get(random.nextInt(donantes.size())).getUsername()
            );

            donacion.setTipoSangre(
                    tiposSangre[random.nextInt(tiposSangre.length)]
            );

            donacion.setCentroMedico(
                    nombresBancosCreados.get(random.nextInt(nombresBancosCreados.size()))
            );

            donacion.setCantidadMl(
                    cantidadesDonacion[random.nextInt(cantidadesDonacion.length)]
            );

            donacion.setFecha(
                    java.time.LocalDate.now()
                            .minusDays(random.nextInt(365))
                            .toString()
            );

            donacion.setEstado(
                    estadosDonacion[random.nextInt(estadosDonacion.length)]
            );

            donaciones.add(donacion);
        }

        donacionRepository.saveAll(donaciones);

        System.out.println(donaciones.size() + " donaciones insertadas correctamente.");

        System.out.println("Generando encargos...");

        List<Encargo> encargos = new ArrayList<>();

        String[] estadosEncargo = {
                "PENDIENTE",
                "EN CAMINO",
                "ENTREGADO",
                "CANCELADO"
        };

        Integer[] cantidadesEncargo = {
                30, 50, 80, 100,
                150, 200, 300,
                400, 500
        };

        for (int i = 0; i < 100; i++) {

            Encargo encargo = new Encargo();

            encargo.setCentroMedico(
                    nombresCentrosCreados.get(random.nextInt(nombresCentrosCreados.size()))
            );

            encargo.setBancoSangre(
                    nombresBancosCreados.get(random.nextInt(nombresBancosCreados.size()))
            );

            encargo.setTipoSangre(
                    tiposSangre[random.nextInt(tiposSangre.length)]
            );

            encargo.setCantidad(
                    cantidadesEncargo[random.nextInt(cantidadesEncargo.length)]
            );

            encargo.setFecha(
                    java.time.LocalDate.now()
                            .minusDays(random.nextInt(365))
                            .toString()
            );

            encargo.setEstado(
                    estadosEncargo[random.nextInt(estadosEncargo.length)]
            );

            encargos.add(encargo);
        }

        encargoRepository.saveAll(encargos);

        System.out.println(encargos.size() + " encargos insertados correctamente.");

        System.out.println("SEEDER FINALIZADO.");
    }
}