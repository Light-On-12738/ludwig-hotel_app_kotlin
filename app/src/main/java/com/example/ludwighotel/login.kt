package com.example.ludwighotel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// --- Colores tomados de la interfaz ---
val LudwigOrange = Color(0xFFFF9800)
val LudwigLightGray = Color(0xFFF2F2F2)
val LudwigTextGray = Color(0xFF757575)
val LudwigButtonBlack = Color(0xFF1E1E1E)

object SessionPreferences {
    private const val PREFS_NAME = "ludwig_hotel_session_prefs"
    private const val KEY_REMEMBER_ME = "remember_me"

    fun setRememberMe(context: Context, remember: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMEMBER_ME, remember)
            .apply()
    }

    fun shouldRememberSession(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMEMBER_ME, true)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REMEMBER_ME)
            .apply()
    }
}

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var recoveryStep by remember { mutableStateOf(RecoveryStep.NONE) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryConfirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LudwigLightGray)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado
        LudwigHeader()

        // Card Contenedor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Control Segmentado (Tabs)
                SegmentedControl(
                    selectedIndex = 0,
                    onSegmentSelected = { index ->
                        if (index == 1) {
                            navController.navigate("register")
                        }
                    }
                )

                // Campo Correo
                LudwigInputLabel("Correo electrónico")
                LudwigTextField(
                    value = email,
                    onValueChange = { email = normalizarCorreo(it) },
                    placeholder = "nombre@correo.com",
                    leadingIcon = Icons.Default.Email,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )

                // Campo Contraseña
                LudwigInputLabel("Contraseña")
                LudwigTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    trailingIconClick = { passwordVisible = !passwordVisible },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )

                // Recordarme y Olvidaste tu contraseña
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = LudwigOrange)
                        )
                        Text(text = "Recordarme", color = LudwigTextGray, fontSize = 13.sp)
                    }
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = LudwigOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = !isLoading) {
                            recoveryEmail = email.trim()
                            recoveryStep = RecoveryStep.EMAIL
                        }
                    )
                }

                // Botón Iniciar Sesión
                LudwigButton(
                    text = "Iniciar sesión",
                    isLoading = isLoading,
                    onClick = {
                        val cleanEmail = email.trim()
                        val cleanPassword = password.trim()

                        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
                            Toast.makeText(context, "Completa correo y contraseña", Toast.LENGTH_SHORT).show()
                        } else if (!correoEsValido(cleanEmail)) {
                            Toast.makeText(context, "Escribe un correo electrónico válido", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            scope.launch {
                                try {
                                    SupabaseClientProvider.client.auth.signInWith(Email) {
                                        this.email = cleanEmail
                                        this.password = cleanPassword
                                    }

                                    val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                                    if (user == null) {
                                        Toast.makeText(
                                            context,
                                            "Confirma tu correo antes de iniciar sesión.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        SessionPreferences.setRememberMe(context, rememberMe)
                                        Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(user.id)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Error al iniciar sesión", e)
                                    Toast.makeText(
                                        context,
                                        "Error al ingresar: ${e.message ?: "Verifica tus credenciales"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                )

                // Términos
                Text(
                    text = "Al continuar, aceptas los Términos de servicio y la Política de privacidad.",
                    color = LudwigTextGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        when (recoveryStep) {
            RecoveryStep.EMAIL -> {
                ForgotPasswordEmailDialog(
                    initialEmail = recoveryEmail,
                    onDismiss = { recoveryStep = RecoveryStep.NONE },
                    onCodeSent = { sentEmail ->
                        recoveryEmail = sentEmail
                        recoveryCode = ""
                        recoveryStep = RecoveryStep.CODE
                    }
                )
            }

            RecoveryStep.CODE -> {
                RecoveryCodeDialog(
                    email = recoveryEmail,
                    code = recoveryCode,
                    onCodeChanged = { recoveryCode = it.filter(Char::isDigit).take(4) },
                    onDismiss = { recoveryStep = RecoveryStep.NONE },
                    onVerified = {
                        recoveryNewPassword = ""
                        recoveryConfirmPassword = ""
                        recoveryStep = RecoveryStep.PASSWORD
                    }
                )
            }

            RecoveryStep.PASSWORD -> {
                NewPasswordDialog(
                    email = recoveryEmail,
                    code = recoveryCode,
                    newPassword = recoveryNewPassword,
                    confirmPassword = recoveryConfirmPassword,
                    onNewPasswordChanged = { recoveryNewPassword = it },
                    onConfirmPasswordChanged = { recoveryConfirmPassword = it },
                    onDismiss = { recoveryStep = RecoveryStep.NONE },
                    onPasswordChanged = {
                        recoveryStep = RecoveryStep.NONE
                        recoveryCode = ""
                        recoveryNewPassword = ""
                        recoveryConfirmPassword = ""
                    }
                )
            }

            RecoveryStep.NONE -> Unit
        }
    }
}

@Composable
fun RegisterScreen(navController: NavController) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var dui by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LudwigLightGray)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado
        LudwigHeader()

        // Card Contenedor
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Control Segmentado (Tabs)
                SegmentedControl(
                    selectedIndex = 1,
                    onSegmentSelected = { index ->
                        if (index == 0) {
                            navController.popBackStack()
                        }
                    }
                )

                // Campo Nombre
                LudwigInputLabel("Nombre")
                LudwigTextField(
                    value = nombre,
                    onValueChange = { nombre = formatearNombre(it) },
                    placeholder = "Escribe tu nombre",
                    leadingIcon = Icons.Default.Person,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // Campo Correo
                LudwigInputLabel("Correo electrónico")
                LudwigTextField(
                    value = email,
                    onValueChange = { email = normalizarCorreo(it) },
                    placeholder = "nombre@correo.com",
                    leadingIcon = Icons.Default.Email,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )

                // Campo Teléfono
                LudwigInputLabel("Número telefónico")
                LudwigTextField(
                    value = telefono,
                    onValueChange = { telefono = formatearTelefono(it) },
                    placeholder = "7777-7777",
                    leadingIcon = Icons.Default.Phone,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )

                // Campo DUI
                LudwigInputLabel("DUI")
                LudwigTextField(
                    value = dui,
                    onValueChange = { dui = formatearDui(it) },
                    placeholder = "00000000-0",
                    leadingIcon = Icons.Default.Badge,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )

                // Campo Contraseña
                LudwigInputLabel("Crea una contraseña")
                LudwigTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    trailingIconClick = { passwordVisible = !passwordVisible },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                )

                // Campo Confirmar Contraseña
                LudwigInputLabel("Confirmar contraseña")
                LudwigTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    trailingIconClick = { confirmPasswordVisible = !confirmPasswordVisible },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )

                // Botón Registro
                LudwigButton(
                    text = "Registrarse",
                    isLoading = isLoading,
                    onClick = {
                        val cleanNombre = nombre.trim()
                        val cleanEmail = email.trim()
                        val cleanTelefono = telefono.trim()
                        val cleanDui = dui.trim()
                        val cleanPassword = password.trim()

                        when {
                            cleanNombre.isBlank() || cleanEmail.isBlank() || cleanDui.isBlank() || cleanPassword.isBlank() ->
                                Toast.makeText(context, "Rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                            !nombreEsValido(cleanNombre) ->
                                Toast.makeText(context, "El nombre solo puede contener letras", Toast.LENGTH_SHORT).show()
                            !correoEsValido(cleanEmail) ->
                                Toast.makeText(context, "Correo electrónico no válido", Toast.LENGTH_SHORT).show()
                            cleanTelefono.isNotBlank() && !telefonoEsValido(cleanTelefono) ->
                                Toast.makeText(context, "El teléfono debe tener formato 0000-0000", Toast.LENGTH_SHORT).show()
                            !duiEsValido(cleanDui) ->
                                Toast.makeText(context, "El DUI debe tener formato 00000000-0", Toast.LENGTH_SHORT).show()
                            cleanPassword != confirmPassword.trim() ->
                                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                            cleanPassword.length < 6 ->
                                Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                            else -> {
                                isLoading = true
                                scope.launch {
                                    try {
                                        SupabaseClientProvider.client.auth.signUpWith(Email) {
                                            this.email = cleanEmail
                                            this.password = cleanPassword
                                            data = buildJsonObject {
                                                put("nombre_completo", cleanNombre)
                                                put("telefono", cleanTelefono)
                                                put("dui", cleanDui)
                                                put("rol", "cliente")
                                            }
                                        }

                                        Toast.makeText(
                                            context,
                                            "Cuenta creada. Revisa tu correo y confirma la cuenta.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        Log.e("RegisterScreen", "Error al registrar usuario", e)
                                        Toast.makeText(
                                            context,
                                            "Error al registrar: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                )

                TextButton(onClick = { navController.popBackStack() }, enabled = !isLoading) {
                    Text("Volver al login", color = LudwigTextGray)
                }
            }
        }
    }
}

// --- Recuperación de contraseña ---

/**
 * Envía el correo real de recuperación usando Supabase Auth.
 * El cambio de contraseña NO se hace en la tabla "usuario": Supabase Auth
 * administra la contraseña de forma segura en auth.users.
 */
private enum class RecoveryStep {
    NONE,
    EMAIL,
    CODE,
    PASSWORD
}

/**
 * Paso 1: solicita al backend que genere un código de 4 dígitos y lo envíe
 * al correo del usuario.
 *
 * IMPORTANTE: el código NO se genera ni se valida en el teléfono. Eso debe
 * hacerlo una Edge Function de Supabase para que el usuario no pueda modificar
 * la lógica desde la APK.
 */
@Composable
private fun ForgotPasswordEmailDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onCodeSent: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Recuperar contraseña", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Escribe el correo de tu cuenta. Te enviaremos un código de 4 números.",
                    color = LudwigTextGray
                )

                LudwigTextField(
                    value = email,
                    onValueChange = { email = normalizarCorreo(it) },
                    placeholder = "nombre@correo.com",
                    leadingIcon = Icons.Default.Email,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    val cleanEmail = email.trim()

                    if (!correoEsValido(cleanEmail)) {
                        Toast.makeText(
                            context,
                            "Escribe un correo electrónico válido",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.functions.invoke(
                                function = "password-reset-code",
                                body = buildJsonObject {
                                    put("action", "send")
                                    put("email", cleanEmail)
                                }
                            )

                            Toast.makeText(
                                context,
                                "Te enviamos un código de 4 números a tu correo.",
                                Toast.LENGTH_LONG
                            ).show()
                            onCodeSent(cleanEmail)
                        } catch (e: Exception) {
                            Log.e("ForgotPassword", "Error enviando código", e)
                            Toast.makeText(
                                context,
                                "No se pudo enviar el código: ${e.message ?: "intenta nuevamente"}",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Enviar código", color = LudwigOrange, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismiss) {
                Text("Cancelar", color = LudwigTextGray)
            }
        }
    )
}

/** Paso 2: introduce el código de 4 dígitos recibido por correo. */
@Composable
private fun RecoveryCodeDialog(
    email: String,
    code: String,
    onCodeChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Código de verificación", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ingresa el código de 4 números que enviamos a $email.",
                    color = LudwigTextGray
                )

                LudwigTextField(
                    value = code,
                    onValueChange = { onCodeChanged(it.filter(Char::isDigit).take(4)) },
                    placeholder = "0000",
                    leadingIcon = Icons.Default.Lock,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    if (code.length != 4) {
                        Toast.makeText(
                            context,
                            "El código debe tener 4 números",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.functions.invoke(
                                function = "password-reset-code",
                                body = buildJsonObject {
                                    put("action", "verify")
                                    put("email", email)
                                    put("code", code)
                                }
                            )

                            Toast.makeText(
                                context,
                                "Código correcto. Ahora crea tu nueva contraseña.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onVerified()
                        } catch (e: Exception) {
                            Log.e("RecoveryCode", "Código inválido", e)
                            Toast.makeText(
                                context,
                                "Código incorrecto o vencido.",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verificar", color = LudwigOrange, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismiss) {
                Text("Cancelar", color = LudwigTextGray)
            }
        }
    )
}

/** Paso 3: establece la nueva contraseña después de verificar el código. */
@Composable
private fun NewPasswordDialog(
    email: String,
    code: String,
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onPasswordChanged: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Nueva contraseña", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Crea una nueva contraseña para tu cuenta.",
                    color = LudwigTextGray
                )

                LudwigInputLabel("Nueva contraseña")
                LudwigTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChanged,
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    trailingIconClick = { passwordVisible = !passwordVisible },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                LudwigInputLabel("Confirmar contraseña")
                LudwigTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChanged,
                    placeholder = "••••••••",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    trailingIconClick = { confirmVisible = !confirmVisible },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    val cleanPassword = newPassword.trim()
                    val cleanConfirmation = confirmPassword.trim()

                    when {
                        cleanPassword.length < 6 -> {
                            Toast.makeText(
                                context,
                                "La contraseña debe tener al menos 6 caracteres",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        cleanPassword != cleanConfirmation -> {
                            Toast.makeText(
                                context,
                                "Las contraseñas no coinciden",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            isLoading = true
                            scope.launch {
                                try {
                                    SupabaseClientProvider.client.functions.invoke(
                                        function = "password-reset-code",
                                        body = buildJsonObject {
                                            put("action", "reset")
                                            put("email", email)
                                            put("code", code)
                                            put("new_password", cleanPassword)
                                        }
                                    )

                                    Toast.makeText(
                                        context,
                                        "Contraseña actualizada correctamente.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onPasswordChanged()
                                } catch (e: Exception) {
                                    Log.e("NewPassword", "Error cambiando contraseña", e)
                                    Toast.makeText(
                                        context,
                                        "No se pudo cambiar la contraseña: ${e.message ?: "intenta nuevamente"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cambiar contraseña", color = LudwigOrange, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isLoading, onClick = onDismiss) {
                Text("Cancelar", color = LudwigTextGray)
            }
        }
    )
}


// --- Componentes Visuales Reutilizables ---

@Composable
fun LudwigHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ludwing Hotel",
            fontSize = 22.sp,
            color = LudwigOrange,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Image(
            painter = painterResource(id = R.drawable.milogo),
            contentDescription = "Logo de Ludwing Hotel",
            modifier = Modifier.size(38.dp)
        )
    }

    Text(
        text = "¡Hola!",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Ingresa o crea una cuenta para reservar y ver tus estancias.",
        fontSize = 15.sp,
        color = LudwigTextGray,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SegmentedControl(
    selectedIndex: Int,
    onSegmentSelected: (Int) -> Unit
) {
    val items = listOf("Iniciar sesión", "Registrarse")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = LudwigLightGray
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            items.forEachIndexed { index, text ->
                val isSelected = selectedIndex == index
                Button(
                    onClick = { onSegmentSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) LudwigOrange else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) Color.White else LudwigTextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LudwigInputLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    )
}

@Composable
fun LudwigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    trailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions
) {
    OutlinedTextField(
        value = TextFieldValue(value, TextRange(value.length)),
        onValueChange = { onValueChange(it.text) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, color = LudwigTextGray, fontSize = 14.sp) },
        leadingIcon = { Icon(imageVector = leadingIcon, contentDescription = null, tint = LudwigTextGray) },
        trailingIcon = {
            if (trailingIcon != null && trailingIconClick != null) {
                IconButton(onClick = trailingIconClick) {
                    Icon(imageVector = trailingIcon, contentDescription = null, tint = LudwigTextGray)
                }
            }
        },
        singleLine = true,
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            focusedContainerColor = LudwigLightGray,
            unfocusedContainerColor = LudwigLightGray,
            disabledContainerColor = LudwigLightGray,
            cursorColor = LudwigOrange
        )
    )
}

@Composable
fun LudwigButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LudwigOrange),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        } else {
            Text(text = text, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
