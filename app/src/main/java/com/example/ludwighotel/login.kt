package com.example.ludwighotel

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ----------------------------------------------------
// PANTALLA DE LOGIN 
// ----------------------------------------------------
@Composable
fun LoginScreen(
    navController: NavController, 
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ludwig Hotel - Acceso",
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineLarge
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Usuario o Correo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // 1. Validación universal solicitada
                    if (email == "admin" && password == "1234") {
                        Toast.makeText(context, "¡Bienvenido, Administrador!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess() 
                    } 
                    else {
                        // 2. Validación contra la Base de Datos (ej. Supabase o repositorio local)
                        // Aquí llamas a tu función de BD o ViewModel, por ejemplo:
                        // val existeEnDb = AuthRepository.verificarUsuarioEnDB(email, password)
                        val existeEnDb = false // Reemplaza esto con tu consulta real a la BD
                        
                        if (existeEnDb) {
                            Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Ingresar", fontSize = 16.sp)
            }

            // Enlace para ir a la ventana de registro
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "¿No tienes cuenta? ")
                Text(
                    text = "Regístrate aquí",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        navController.navigate("register")
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// PANTALLA DE REGISTRO 
// ----------------------------------------------------
@Composable
fun RegisterScreen(navController: NavController) {
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Registro de Usuario",
                fontSize = 26.sp,
                style = MaterialTheme.typography.headlineLarge
            )

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Correo o Usuario nuevo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (newEmail.isBlank() || newPassword.isBlank()) {
                        Toast.makeText(context, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                    } else if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    } else {
                        // TODO: Lógica para insertar el usuario en tu Base de Datos (ej. Supabase)
                        // AuthRepository.registrarUsuarioEnDB(newEmail, newPassword)

                        Toast.makeText(context, "¡Usuario registrado con éxito!", Toast.LENGTH_LONG).show()
                        navController.popBackStack() // Regresa al login automáticamente
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Registrarse", fontSize = 16.sp)
            }

            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(text = "Volver al Login")
            }
        }
    }
}