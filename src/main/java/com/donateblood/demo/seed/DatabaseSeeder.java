package com.donateblood.demo.seed;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

@Component
public class DatabaseSeeder implements CommandLineRunner {

    // ---- Cantidades (≈ 15.000 datos entre donantes, citas y donaciones) ----
    // Las que no se usan para el modelo (centros y encargos) quedan en 1, solo de referencia.
    private static final int NUM_DONANTES = 1900;      // 5 citas por donante = 9.500 citas
    private static final int CITAS_POR_DONANTE = 5;
    private static final int NUM_BANCOS = 20;          // las citas necesitan varios bancos como "lugar"
    private static final int NUM_CENTROS = 1;
    private static final int NUM_ENCARGOS = 1;

    // ---- Reglas con las que se decide si una cita se realiza o se cancela ----
    // Perfil de cada donante: HABITUAL (casi siempre va), OCASIONAL (a veces) y RARA_VEZ (casi nunca).
    private static final double PROB_HABITUAL = 0.45;
    private static final double PROB_OCASIONAL = 0.15;   // el resto (0.40) es RARA_VEZ
    private static final double ASISTE_HABITUAL = 0.93;
    private static final double ASISTE_OCASIONAL = 0.55;
    private static final double ASISTE_RARA_VEZ = 0.07;
    private static final double DESCUENTO_TARDE = 0.07;  // en la tarde (desde las 14:00) se cancela algo más
    private static final int DIAS_MINIMOS_ENTRE_DONACIONES = 56;  // un donante no puede donar de nuevo antes de 8 semanas
    private static final double ASISTE_SI_NO_PUEDE_DONAR = 0.05;
    private static final double PROB_ULTIMA_CITA_PENDIENTE = 0.52;  // ≈ 10 % de las citas quedan a futuro

    // Solo nombres propios (sin "Señor", "Señora", "Dr.", etc.): así en las citas se ve "Nombre Apellido Apellido".
    private static final String[] NOMBRES = {
            "Juan", "Carlos", "Andrés", "Luis", "Jorge", "Miguel", "José", "Santiago", "Camilo", "Daniel",
            "David", "Sebastián", "Felipe", "Alejandro", "Diego", "Nicolás", "Mateo", "Samuel", "Julián", "Esteban",
            "Fernando", "Ricardo", "Héctor", "Óscar", "Iván", "Rafael", "Pablo", "Mauricio", "Gustavo", "Álvaro",
            "María", "Laura", "Valentina", "Camila", "Sofía", "Daniela", "Natalia", "Andrea", "Paola", "Carolina",
            "Juliana", "Isabella", "Mariana", "Luisa", "Diana", "Katherine", "Sandra", "Claudia", "Liliana", "Marcela",
            "Angélica", "Lorena", "Viviana", "Yesenia", "Johana", "Gloria", "Patricia", "Esperanza", "Alejandra", "Tatiana"
    };

    private static final String[] APELLIDOS = {
            "García", "Rodríguez", "Martínez", "López", "González", "Pérez", "Sánchez", "Ramírez", "Torres", "Díaz",
            "Vargas", "Castro", "Rojas", "Moreno", "Jiménez", "Ortiz", "Gómez", "Herrera", "Medina", "Aguilar",
            "Castillo", "Ríos", "Suárez", "Mendoza", "Cardona", "Quintero", "Restrepo", "Guerrero", "Navarro", "Salazar",
            "Pineda", "Ospina", "Cortés", "Arias", "Mejía", "Becerra", "Cabrera", "Ruiz", "Peña", "Duarte",
            "Barrios", "Escobar", "Acosta", "Montoya", "Valencia", "Cano", "Blanco", "Arrieta", "Ceballos", "Lozano",
            "Zapata", "Vélez", "Franco", "Caro", "Bermúdez", "Polo", "Pacheco", "Mercado", "Ibarra", "Palacio"
    };

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

        Random random = new Random();
        LocalDate hoy = LocalDate.now();

        // Todos los usuarios de prueba usan la contraseña 123456: se cifra una sola vez (BCrypt es lento)
        String passwordCifrada = passwordEncoder.encode("123456");
        Set<String> usernamesUsados = new HashSet<>();

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

        // ---------- Donantes ----------
        for (int i = 0; i < NUM_DONANTES; i++) {

            String nombre = NOMBRES[random.nextInt(NOMBRES.length)];
            String apellido1 = APELLIDOS[random.nextInt(APELLIDOS.length)];
            String apellido2 = APELLIDOS[random.nextInt(APELLIDOS.length)];
            String username = usernameUnico(nombre, usernamesUsados, random);

            Usuario usuario = new Usuario();
            usuario.setNombre(nombre + " " + apellido1 + " " + apellido2);
            usuario.setUsername(username);
            usuario.setTipoDocumento("CC");
            usuario.setDocumento(digitos(random, 10));
            usuario.setNumPhone("3" + digitos(random, 9));
            usuario.setEdad(18 + random.nextInt(48)); // edad entre 18 y 65
            usuario.setTipoSangre(CitasYSangre.TIPOS_SANGRE[Math.floorMod(username.hashCode(), CitasYSangre.TIPOS_SANGRE.length)]);
            usuario.setEmail(username + dominios[random.nextInt(dominios.length)]);
            usuario.setPassword(passwordCifrada);
            usuario.setRol("ROLE_USER");

            usuarios.add(usuario);
            donantes.add(usuario);
        }

        // ---------- Bancos de sangre ----------
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

        for (int i = 0; i < NUM_BANCOS; i++) {

            Usuario banco = new Usuario();

            String nombreBanco = nombresBancos[random.nextInt(nombresBancos.length)] + " " + (i + 1);
            String username;
            do {
                username = "banco" + random.nextInt(999999);
            } while (!usernamesUsados.add(username));

            banco.setNombre(nombreBanco);
            banco.setUsername(username);
            banco.setPassword(passwordCifrada);
            banco.setRol("ROLE_BANCO");
            // Capacidades distintas entre bancos (5, 6, 7 y 8) para probar cómo se llenan las horas
            banco.setCapacidadAtencion(5 + (i % 4));

            usuarios.add(banco);
            nombresBancosCreados.add(nombreBanco);
        }

        // ---------- Centros médicos (de Cartagena) ----------
        String[] nombresCentros = {
                "Hospital Universitario del Caribe",
                "Clinica Blas de Lezo",
                "Clinica Crecer",
                "Hospital Naval de Cartagena",
                "Clinica Madre Bernarda",
                "Clinica Bocagrande",
                "Centro Medico Cartagena",
                "Clinica San Juan de Dios",
                "Clinica Medihelp",
                "Hospital Infantil Napoleon Franco Pareja"
        };

        for (int i = 0; i < NUM_CENTROS; i++) {

            Usuario centro = new Usuario();

            String nombreCentro = nombresCentros[random.nextInt(nombresCentros.length)] + " " + (i + 1);
            String username;
            do {
                username = "centro" + random.nextInt(999999);
            } while (!usernamesUsados.add(username));

            centro.setNombre(nombreCentro);
            centro.setUsername(username);
            centro.setPassword(passwordCifrada);
            centro.setRol("ROLE_CENTRO");

            usuarios.add(centro);
            nombresCentrosCreados.add(nombreCentro);
        }

        usuarioRepository.saveAll(usuarios);

        System.out.println(usuarios.size() + " usuarios insertados correctamente.");

        // ---------- Citas y donaciones ----------
        System.out.println("Generando citas y donaciones (historial coherente para el modelo predictivo)...");

        List<Cita> citas = new ArrayList<>();
        List<Donacion> donaciones = new ArrayList<>();
        generarCitasYDonaciones(donantes, nombresBancosCreados, hoy, random, citas, donaciones);

        citaRepository.saveAll(citas);
        System.out.println(citas.size() + " citas insertadas correctamente.");

        donacionRepository.saveAll(donaciones);
        System.out.println(donaciones.size() + " donaciones insertadas correctamente.");

        // ---------- Encargos ----------
        String[] tiposSangre = CitasYSangre.TIPOS_SANGRE;

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

        for (int i = 0; i < NUM_ENCARGOS; i++) {

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
                    hoy.minusDays(random.nextInt(365)).toString()
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

    /**
     * Cada donante tiene 5 citas separadas entre 45 y 99 días. Si le queda una a futuro, esa es PENDIENTE.
     * Las pasadas se deciden en orden cronológico con estas reglas:
     *  1) El perfil del donante (habitual, ocasional o rara vez) marca su probabilidad de asistir.
     *  2) Si la cita cae a menos de 56 días de su última donación completada, casi seguro se cancela.
     *  3) En la tarde (desde las 14:00) la probabilidad baja un poco.
     * Cada cita REALIZADA genera su donación COMPLETADA (mismo donante, fecha y lugar).
     */
    private void generarCitasYDonaciones(List<Usuario> donantes, List<String> bancos, LocalDate hoy,
                                         Random random, List<Cita> citas, List<Donacion> donaciones) {

        List<String> horas = new ArrayList<>(CitasYSangre.franjasValidas());

        for (Usuario donante : donantes) {

            double perfil = random.nextDouble();
            double probAsistir;
            if (perfil < PROB_HABITUAL) {
                probAsistir = ASISTE_HABITUAL;
            } else if (perfil < PROB_HABITUAL + PROB_OCASIONAL) {
                probAsistir = ASISTE_OCASIONAL;
            } else {
                probAsistir = ASISTE_RARA_VEZ;
            }

            String tipoSangre = CitasYSangre.TIPOS_SANGRE[random.nextInt(CitasYSangre.TIPOS_SANGRE.length)];

            boolean ultimaPendiente = random.nextDouble() < PROB_ULTIMA_CITA_PENDIENTE;
            int pasadas = ultimaPendiente ? CITAS_POR_DONANTE - 1 : CITAS_POR_DONANTE;

            // Fechas pasadas: separadas entre 45 y 99 días y todas antes de hoy
            int[] separaciones = new int[pasadas - 1];
            int total = 0;
            for (int g = 0; g < separaciones.length; g++) {
                separaciones[g] = 45 + random.nextInt(55);
                total += separaciones[g];
            }
            int desdeHoy = total + 1;
            if (desdeHoy < 366) {
                desdeHoy += random.nextInt(366 - desdeHoy);
            }
            LocalDate fecha = hoy.minusDays(desdeHoy);

            LocalDate ultimaDonacion = null;

            for (int k = 0; k < pasadas; k++) {
                if (k > 0) {
                    fecha = fecha.plusDays(separaciones[k - 1]);
                }

                String hora = horas.get(random.nextInt(horas.size()));
                String lugar = bancos.get(random.nextInt(bancos.size()));

                double prob = probAsistir;
                if (Integer.parseInt(hora.substring(0, 2)) >= 14) {
                    prob -= DESCUENTO_TARDE;
                }
                if (ultimaDonacion != null
                        && ChronoUnit.DAYS.between(ultimaDonacion, fecha) < DIAS_MINIMOS_ENTRE_DONACIONES) {
                    prob = ASISTE_SI_NO_PUEDE_DONAR;
                }

                Cita cita = nuevaCita(donante, fecha, hora, lugar, tipoSangre);

                if (random.nextDouble() < prob) {
                    cita.setEstado("REALIZADA");
                    ultimaDonacion = fecha;

                    Donacion donacion = new Donacion();
                    donacion.setDonante(donante.getUsername());
                    donacion.setTipoSangre(tipoSangre);
                    donacion.setCentroMedico(lugar);
                    donacion.setCantidadMl(450.0);
                    donacion.setFecha(fecha.toString());
                    donacion.setEstado("COMPLETADA");
                    donaciones.add(donacion);
                } else {
                    cita.setEstado("CANCELADA");
                }
                citas.add(cita);
            }

            if (ultimaPendiente) {
                Cita pendiente = nuevaCita(
                        donante,
                        hoy.plusDays(1 + random.nextInt(45)),
                        horas.get(random.nextInt(horas.size())),
                        bancos.get(random.nextInt(bancos.size())),
                        tipoSangre);
                pendiente.setEstado("PENDIENTE");
                citas.add(pendiente);
            }
        }
    }

    private Cita nuevaCita(Usuario donante, LocalDate fecha, String hora, String lugar, String tipoSangre) {
        Cita cita = new Cita();
        cita.setNombre(donante.getNombre());
        cita.setUsuario(donante.getUsername());
        cita.setFecha(fecha.toString());
        cita.setHora(hora);
        cita.setLugar(lugar);
        cita.setTipoSangre(tipoSangre);
        return cita;
    }

    /** username = nombre sin tildes en minúscula + número; garantiza que no se repita (el campo es único). */
    private String usernameUnico(String nombre, Set<String> usados, Random random) {
        String base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        String username;
        do {
            username = base + random.nextInt(99999);
        } while (!usados.add(username));
        return username;
    }

    private String digitos(Random random, int cantidad) {
        StringBuilder sb = new StringBuilder();
        sb.append(1 + random.nextInt(9));
        for (int i = 1; i < cantidad; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}