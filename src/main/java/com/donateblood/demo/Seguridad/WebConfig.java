package com.donateblood.demo.Seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PerfilCompletoInterceptor perfilCompletoInterceptor;

    public WebConfig(PerfilCompletoInterceptor perfilCompletoInterceptor) {
        this.perfilCompletoInterceptor = perfilCompletoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(perfilCompletoInterceptor).addPathPatterns(
                "/donantePanel",
                "/perfilDonante",
                "/agendarCita",
                "/misCitas",
                "/verCitas",
                "/editarCita/**",
                "/cancelarCita/**",
                "/actualizarCita"
        );
    }
}
