package com.donateblood.demo.Seguridad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PerfilCompletoInterceptor perfilCompletoInterceptor;

    @Value("${app.uploads.dir:uploads/perfiles}")
    private String uploadsDir;

    public WebConfig(PerfilCompletoInterceptor perfilCompletoInterceptor) {
        this.perfilCompletoInterceptor = perfilCompletoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(perfilCompletoInterceptor).addPathPatterns(
                "/donantePanel",
                "/perfilDonante",
                "/editarPerfil",
                "/agendarCita",
                "/misCitas",
                "/verCitas",
                "/editarCita/**",
                "/cancelarCita/**",
                "/actualizarCita"
        );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path ruta = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            // Se crea al arrancar para que la ruta sea válida desde el inicio
            Files.createDirectories(ruta);
        } catch (IOException ignored) {
            // Si no se puede crear, se registra igual y se verá el error al subir
        }
        String location = ruta.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/perfiles/**")
                .addResourceLocations(location);
    }
}
