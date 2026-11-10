package com.donateblood.demo.model;

/**
 * Banco de sangre que se dibuja en el mapa del donante.
 * Es informativo (no es un usuario registrado): solo datos de contacto y ubicación.
 */
public class BancoMapa {

    private final String nombre;
    private final String direccion;
    private final String telefono;
    private final double latitud;
    private final double longitud;

    public BancoMapa(String nombre, String direccion, String telefono, double latitud, double longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }
}
