package com.example.ludwighotel

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Perfil público de un usuario. La contraseña vive solamente en Supabase Auth. */
@Serializable
data class UsuarioTabla(
    @SerialName("id_usuario") val id: String,
    @SerialName("nombre_completo") val nombreCompleto: String,
    @SerialName("correo_electronico") val email: String,
    @SerialName("telefono") val telefono: String? = null,
    @SerialName("rol") val rol: String = "cliente",
    @SerialName("dui") val dui: String
)

/** Recupera únicamente el perfil del usuario que ya inició sesión. */
suspend fun obtenerUsuarioPorId(id: String): UsuarioTabla? = try {
    SupabaseClientProvider.client
        .from("usuario")
        .select {
            filter { eq("id_usuario", id) }
        }
        .decodeSingleOrNull<UsuarioTabla>()
} catch (_: Exception) {
    null
}

fun formatearDui(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(9)
    return if (digitos.length <= 8) digitos else "${digitos.take(8)}-${digitos.last()}"
}

fun formatearTelefono(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(8)
    return if (digitos.length <= 4) digitos else "${digitos.take(4)}-${digitos.drop(4)}"
}

fun duiEsValido(dui: String) = Regex("^\\d{8}-\\d$").matches(dui)
fun telefonoEsValido(telefono: String) = Regex("^\\d{4}-\\d{4}$").matches(telefono)
fun correoEsValido(correo: String) = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$").matches(correo)
