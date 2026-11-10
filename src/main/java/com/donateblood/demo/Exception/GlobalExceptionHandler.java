package com.donateblood.demo.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.donateblood.demo.controller.EncargoController;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = EncargoController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(
            UsernameNotFoundException ex
    ) {

        Map<String, Object> error = new HashMap<>();

        error.put("status", 404);
        error.put("error", "Usuario no encontrado");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(
            BadCredentialsException ex
    ) {

        Map<String, Object> error = new HashMap<>();

        error.put("status", 401);
        error.put("error", "Credenciales incorrectas");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(
            Exception ex
    ) {

        Map<String, Object> error = new HashMap<>();

        error.put("status", 500);
        error.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}