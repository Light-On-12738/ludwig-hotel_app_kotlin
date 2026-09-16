package com.example.ludwighotel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Habitacion(
    val id_habitacion: String,
    val nombre_habitacion: String,
    val descripcion: String,
    val precio_noche: Double,
    val capacidad_huespedes: Int,
    val imagen_habitacion: String? = null,
    val subtitulo: String? = null
)

class HabitacionRepository {
    suspend fun obtenerHabitaciones(): List<Habitacion> {
        return SupabaseClientProvider.client
            .from("habitaciones")
            .select()
            .decodeList<Habitacion>()
    }
}

class HotelViewModel : ViewModel() {
    private val repo = HabitacionRepository()

    var habitaciones by mutableStateOf<List<Habitacion>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            habitaciones = repo.obtenerHabitaciones()
        }
    }
}

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
                        onLoginSuccess = {
                            navController.navigate("hotel") {
                                popUpTo("login") {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

                composable("register") {
                    RegisterScreen(navController)
                }

                composable("hotel") {
                    LudwigHotelApp()
                }
            }
        }
    }
}

@Composable
fun LudwigHotelApp(viewModel: HotelViewModel = viewModel()) {
    val habitaciones = viewModel.habitaciones

    var selectedTab by remember { mutableStateOf(0) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF4F4F4)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Header(onMenuClick = { isMenuOpen = true })
                    }
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
                                                fontSize = 17.sp,
                                                fontWeight = if (selectedTab == index)
                                                    FontWeight.Bold
                                                else
                                                    FontWeight.Normal
                                            )

                                            if (selectedTab == index) {
                                                Spacer(Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(42.dp)
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

            AnimatedVisibility(visible = isMenuOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { isMenuOpen = false }
                )
            }

            AnimatedVisibility(
                visible = isMenuOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                HamburgerMenu(
                    onSelectTab = { tab ->
                        selectedTab = tab
                        isMenuOpen = false
                    },
                    onLogout = {
                        isMenuOpen = false
                        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun HamburgerMenu(
    onSelectTab: (Int) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color.White)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(Color(0xFFFFE0B2), RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFFFA000)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Ludwig Martínez",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(20.dp))

        MenuOption(Icons.Default.Home, "Inicio") { onSelectTab(0) }
        Spacer(Modifier.height(10.dp))
        MenuOption(Icons.Default.GridView, "Catálogo") { onSelectTab(1) }
        Spacer(Modifier.height(10.dp))
        MenuOption(Icons.Default.CalendarToday, "Reserva") { onSelectTab(2) }

        Spacer(Modifier.weight(1f))

        Divider()
        Spacer(Modifier.height(16.dp))
        MenuOption(
            icon = Icons.Default.Logout,
            label = "Cerrar sesión",
            tint = Color(0xFFE53935),
            onClick = onLogout
        )
    }
}

@Composable
fun MenuOption(
    icon: ImageVector,
    label: String,
    tint: Color = Color(0xFFFFA000),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Header(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMenuClick,
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

        Text(
            text = "LI",
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFA000)
        )
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
                        text = habitacion.nombre_habitacion,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = habitacion.subtitulo ?: "",
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${"%.0f".format(habitacion.precio_noche)}",
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

            Row(
                modifier = Modifier
                    .background(Color(0xFFF3F3F3), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Capacidad",
                    modifier = Modifier.size(19.dp),
                    tint = Color.DarkGray
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "${habitacion.capacidad_huespedes} huéspedes",
                    color = Color.DarkGray
                )
            }
        }
    }
}