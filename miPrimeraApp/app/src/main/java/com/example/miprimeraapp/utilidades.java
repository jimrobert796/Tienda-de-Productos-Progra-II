package com.example.miprimeraapp;

import java.util.Base64;

public class utilidades {
    static String url_consulta = "http://192.168.1.8:5984/tienda_producto/_design/tienda_producto/_view/tienda_producto";

    // "http://10.148.226.151:5984/tienda_producto/_design/tienda_producto/_view/tienda_producto"
    static String url_mantenimiento = "http://192.168.1.8:5984/tienda_producto"; // CRUD Insertar, Actualizar, Borrar y Buscar
    //http://10.148.226.151:5984/tienda_producto
    static String user = "jimbo";
    static String passwd = "070906";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }
}

