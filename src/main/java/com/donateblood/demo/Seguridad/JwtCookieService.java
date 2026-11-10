package com.donateblood.demo.Seguridad;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class JwtCookieService {

    public static final String JWT_COOKIE_NAME = "jwt";

    public void addJwtCookie(
            HttpServletResponse response,
            String token
    ) {

        ResponseCookie cookie = ResponseCookie
                .from(JWT_COOKIE_NAME, token)

                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(24 * 60 * 60)

                .build();

        response.addHeader(
                "Set-Cookie",
                cookie.toString()
        );
    }

    public void clearJwtCookie(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie
                .from(JWT_COOKIE_NAME, "")

                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)

                .build();

        response.addHeader(
                "Set-Cookie",
                cookie.toString()
        );
    }
}
