package com.example.hola_mundo

data class Producto(
    val codigoQr: String,
    val nombre: String,
    val descripcion: String,
    val categoria: String,
    val tipo: String,
    val precio: Double,
    val stock: Int,
    val foto: String
)