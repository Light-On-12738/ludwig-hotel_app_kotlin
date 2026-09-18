package com.example.ludwighotel

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioTabla(
    @SerialName("id_usuario") val id: String,
    @SerialName("nombre_completo") val nombreCompleto: String,
    @SerialName("correo_electronico") val email: String,
    @SerialName("telefono") val telefono: String? = null,
    @SerialName("rol") val rol: String = "cliente",
    @SerialName("dui") val dui: String
)

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

suspend fun obtenerUsuarioPorCorreo(correo: String): UsuarioTabla? = try {
    SupabaseClientProvider.client
        .from("usuario")
        .select {
            filter { eq("correo_electronico", correo) }
        }
        .decodeSingleOrNull<UsuarioTabla>()
} catch (_: Exception) {
    null
}

@Serializable
private data class UsuarioActualizacion(
    @SerialName("nombre_completo") val nombreCompleto: String,
    @SerialName("telefono") val telefono: String?,
    @SerialName("dui") val dui: String
)

suspend fun actualizarUsuario(
    id: String,
    nombreCompleto: String,
    telefono: String?,
    dui: String
): Result<Unit> = try {

    SupabaseClientProvider.client
        .from("usuario")
        .update(
            UsuarioActualizacion(
                nombreCompleto = nombreCompleto,
                telefono = telefono,
                dui = dui
            )
        ) {
            filter { eq("id_usuario", id) }
        }

    Result.success(Unit)
} catch (e: Exception) {
    Result.failure(e)
}

fun formatearDui(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(9)
    return if (digitos.length <= 8) digitos else "${digitos.take(8)}-${digitos.last()}"
}

fun formatearTelefono(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(8)
    return if (digitos.length <= 4) digitos else "${digitos.take(4)}-${digitos.drop(4)}"
}

fun formatearNombre(entrada: String): String = entrada
    .filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }
    .replace(Regex("\\s+"), " ")
    .take(80)

fun nombreEsValido(nombre: String): Boolean =
    Regex("^[\\p{L}][\\p{L} '\\-]{1,79}$").matches(nombre.trim())

fun normalizarCorreo(entrada: String): String =
    entrada.filterNot(Char::isWhitespace).lowercase().take(254)

fun duiEsValido(dui: String) = Regex("^\\d{8}-\\d$").matches(dui)
fun telefonoEsValido(telefono: String) = Regex("^\\d{4}-\\d{4}$").matches(telefono)
fun correoEsValido(correo: String) =
    Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$")
        .matches(correo)
