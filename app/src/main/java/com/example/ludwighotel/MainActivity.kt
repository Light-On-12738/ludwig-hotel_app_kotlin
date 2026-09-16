package com.example.ludwighotel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class Habitacion(
    val nombre: String,
    val descripcion: String,
    val precio: String,
    val servicios: List<String>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "login"
            ) {
                composable("login") {
                    LoginScreen(
                        navController = navController,
                        onLoginSuccess = { userId ->
                            val encodedEmail = URLEncoder.encode(userId, StandardCharsets.UTF_8.toString())
                            navController.navigate("hotel/$encodedEmail") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(navController)
                }

                composable(
                    route = "hotel/{userEmail}",
                    arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
                ) { backStackEntry ->
                    val rawEmail = backStackEntry.arguments?.getString("userEmail") ?: "Usuario"
                    val userEmail = URLDecoder.decode(rawEmail, StandardCharsets.UTF_8.toString())
                    
                    LudwigHotelApp(
                        userEmail = userEmail,
                        onProfileClick = {
                            val encodedEmail = URLEncoder.encode(userEmail, StandardCharsets.UTF_8.toString())
                            navController.navigate("profile/$encodedEmail")
                        }
                    )
                }

                composable(
                    route = "profile/{userEmail}",
                    arguments = listOf(navArgument("userEmail") { type = NavType.StringType })
                ) { backStackEntry ->
                    val rawEmail = backStackEntry.arguments?.getString("userEmail") ?: "Usuario"
                    val userEmail = URLDecoder.decode(rawEmail, StandardCharsets.UTF_8.toString())

                    ProfileScreen(
                        userEmail = userEmail,
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LudwigHotelApp(
    userEmail: String,
    onProfileClick: () -> Unit
) {
    val habitaciones = listOf(
        Habitacion(
            "Suite Presidencial",
            "Habitación de lujo • 2 camas",
            "$450",
            listOf("Wi-Fi", "Desayuno")
        ),
        Habitacion(
            "Habitación Deluxe",
            "Vista al mar • 1 cama",
            "$320",
            listOf("A/C", "TV 4K")
        ),
        Habitacion(
            "Habitación Familiar",
            "Espaciosa • 3 camas",
            "$280",
            listOf("Wi-Fi", "TV 4K")
        )
    )

    var selectedTab by remember { mutableStateOf(0) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF7F9FC)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Header(userEmail = userEmail, onProfileClick = onProfileClick) }
                item { SearchBar() }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Habitaciones", "Catálogo", "Reservas")
                            .forEachIndexed { index, title ->
                                TextButton(
                                    onClick = { selectedTab = index }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = title,
                                            color = if (selectedTab == index)
                                                Color(0xFFFFA000)
                                            else
                                                Color.DarkGray,
                                            fontSize = 15.sp,
                                            fontWeight = if (selectedTab == index)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
                                        )

                                        if (selectedTab == index) {
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(36.dp)
                                                    .height(3.dp)
                                                    .background(
                                                        Color(0xFFFFA000),
                                                        RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                    }
                }

                if (selectedTab == 0) {
                    items(habitaciones) { habitacion ->
                        HabitacionCard(habitacion)
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = if (selectedTab == 1)
                                    "Catálogo de servicios"
                                else
                                    "Mis reservas",
                                modifier = Modifier.padding(24.dp),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================================
 * REDISEÑO DEL APARTADO DE PERFIL (FRONTEND COMPOSE)
 * ============================================================================ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userEmail: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initials = if (userEmail.length >= 2) userEmail.take(2).uppercase() else "US"

    // Mantenemos intacto el Backend/Supabase
    val userUuid = remember {
        SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: "No disponible"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mi Perfil", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1E293B)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Volver",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F9FC))
            )
        },
        containerColor = Color(0xFFF7F9FC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            // Header del perfil con Avatar e Info principal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(
                                        color = Color(0xFFFFA000), 
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            // Badge decorativo de edición
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = userEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )

                        Text(
                            text = userEmail,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chip con el UUID
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ID: ${if (userUuid.length > 12) userUuid.take(12) + "..." else userUuid}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB57200)
                                )
                            }
                        }
                    }
                }
            }

            // Sección de detalles de la cuenta
            item {
                Text(
                    text = "Información General",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ProfileInfoItem(
                            icon = Icons.Default.Email,
                            title = "Correo Registrado",
                            value = userEmail
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileInfoItem(
                            icon = Icons.Default.VerifiedUser,
                            title = "Estado de Cuenta",
                            value = "Usuario Activo",
                            valueColor = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Opciones y Preferencias
            item {
                Text(
                    text = "Ajustes y Preferencias",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ProfileOptionItem(
                            icon = Icons.Default.CreditCard,
                            title = "Métodos de Pago",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileOptionItem(
                            icon = Icons.Default.Notifications,
                            title = "Notificaciones",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        ProfileOptionItem(
                            icon = Icons.Default.HelpOutline,
                            title = "Centro de Ayuda",
                            onClick = {}
                        )
                    }
                }
            }

            // Botón de Cerrar Sesión (Mantiene exactamente la misma lógica de Supabase)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                SupabaseClientProvider.client.auth.signOut()
                            } catch (_: Exception) {}
                            Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cerrar Sesión",
                        color = Color(0xFFDC2626),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Componente helper para información estática del perfil
@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    valueColor: Color = Color(0xFF1E293B)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFFFF8E1), shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

// Componente helper para opciones clickeables del perfil
@Composable
private fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

/* ============================================================================
 * RESTO DE COMPONENTES DE LA APP
 * ============================================================================ */
@Composable
fun Header(
    userEmail: String,
    onProfileClick: () -> Unit
) {
    val initials = if (userEmail.length >= 2) userEmail.take(2).uppercase() else "LI"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(55.dp)
                    .background(
                        Color.White,
                        RoundedCornerShape(50.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú"
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = "Ludwig Hotel",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFA000)
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFFFFA000), shape = CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SearchBar() {
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp),
        placeholder = {
            Text("Buscar habitaciones, servicios...")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar"
            )
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color(0xFFFFA000)
        ),
        singleLine = true
    )
}

@Composable
fun HabitacionCard(habitacion: Habitacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Color(0xFFE8D7C0),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hotel,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = Color(0xFFB07842)
                )
            }

            Spacer(Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = habitacion.nombre,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = habitacion.descripcion,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = habitacion.precio,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA000)
                    )

                    Text(
                        text = "Noche",
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(15.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                habitacion.servicios.forEach { servicio ->
                    ServicioChip(servicio)
                }
            }
        }
    }
}

@Composable
fun ServicioChip(servicio: String) {
    val icon = when (servicio) {
        "Wi-Fi" -> Icons.Default.Wifi
        "Desayuno" -> Icons.Default.FreeBreakfast
        "A/C" -> Icons.Default.AcUnit
        "TV 4K" -> Icons.Default.Tv
        else -> Icons.Default.Check
    }

    Row(
        modifier = Modifier
            .background(
                Color(0xFFF3F3F3),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = servicio,
            modifier = Modifier.size(19.dp),
            tint = Color.DarkGray
        )

        Spacer(Modifier.width(5.dp))

        Text(
            text = servicio,
            color = Color.DarkGray
        )
    }
}
