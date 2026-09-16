package com.example.ludwighotel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ============================================================================
// MODELO
// ============================================================================

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

// ============================================================================
// REPOSITORY
// ============================================================================

class HabitacionRepository {

suspend fun obtenerHabitaciones(): List<Habitacion> {
    return SupabaseClientProvider.client
        .from("habitaciones")
        .select()
        .decodeList<Habitacion>()
}

}

// ============================================================================
// VIEWMODEL
// ============================================================================

class HotelViewModel : ViewModel() {

private val repo = HabitacionRepository()

var habitaciones by mutableStateOf<List<Habitacion>>(emptyList())
    private set

init {
    cargarHabitaciones()
}

private fun cargarHabitaciones() {
    viewModelScope.launch {
        try {
            habitaciones = repo.obtenerHabitaciones()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

}

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {

        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "login"
        ) {

            // ----------------------------------------------------------------
            // LOGIN
            // ----------------------------------------------------------------

            composable("login") {

                LoginScreen(
                    navController = navController,
                    onLoginSuccess = { userEmail ->

                        val encodedEmail = URLEncoder.encode(
                            userEmail,
                            StandardCharsets.UTF_8.toString()
                        )

                        navController.navigate("hotel/$encodedEmail") {

                            popUpTo("login") {
                                inclusive = true
                            }
                        }
                    }
                )
            }


            // ----------------------------------------------------------------
            // REGISTRO
            // ----------------------------------------------------------------

            composable("register") {

                RegisterScreen(navController)
            }


            // ----------------------------------------------------------------
            // HOTEL
            // ----------------------------------------------------------------

            composable(
                route = "hotel/{userEmail}",
                arguments = listOf(
                    navArgument("userEmail") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val rawEmail =
                    backStackEntry.arguments?.getString("userEmail")
                        ?: "Usuario"

                val userEmail = URLDecoder.decode(
                    rawEmail,
                    StandardCharsets.UTF_8.toString()
                )

                LudwigHotelApp(
                    navController = navController,
                    userEmail = userEmail,
                    onProfileClick = {

                        val encodedEmail = URLEncoder.encode(
                            userEmail,
                            StandardCharsets.UTF_8.toString()
                        )

                        navController.navigate("profile/$encodedEmail")
                    }
                )
            }


            // ----------------------------------------------------------------
            // DETALLE DE HABITACIÓN
            // ----------------------------------------------------------------

            composable(
                route = "habitacion/{habitacionId}",
                arguments = listOf(
                    navArgument("habitacionId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val habitacionId =
                    backStackEntry.arguments?.getString("habitacionId")
                        ?: ""

                HabitacionDetalleScreen(
                    habitacionId = habitacionId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }


            // ----------------------------------------------------------------
            // PERFIL
            // ----------------------------------------------------------------

            composable(
                route = "profile/{userEmail}",
                arguments = listOf(
                    navArgument("userEmail") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->

                val rawEmail =
                    backStackEntry.arguments?.getString("userEmail")
                        ?: "Usuario"

                val userEmail = URLDecoder.decode(
                    rawEmail,
                    StandardCharsets.UTF_8.toString()
                )

                ProfileScreen(
                    userEmail = userEmail,

                    onBack = {
                        navController.popBackStack()
                    },

                    onLogout = {

                        navController.navigate("login") {

                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}
}

// ============================================================================
// PANTALLA PRINCIPAL DEL HOTEL
// ============================================================================

@Composable
fun LudwigHotelApp(
navController: NavHostController,
userEmail: String,
onProfileClick: () -> Unit,
viewModel: HotelViewModel = viewModel()
) {
val habitaciones = viewModel.habitaciones

var selectedTab by remember {
    mutableStateOf(0)
}

var isMenuOpen by remember {
    mutableStateOf(false)
}

var searchQuery by remember {
    mutableStateOf("")
}

val context = LocalContext.current

val habitacionesFiltradas = remember(
    habitaciones,
    searchQuery
) {

    habitaciones.filter {
        it.coincideCon(searchQuery)
    }
}


MaterialTheme {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF7F9FC)
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = 20.dp
                ),

                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ------------------------------------------------------------
                // HEADER
                // ------------------------------------------------------------

                item {

                    Header(
                        userEmail = userEmail,
                        onMenuClick = {
                            isMenuOpen = true
                        },
                        onProfileClick = onProfileClick
                    )
                }


                // ------------------------------------------------------------
                // BUSCADOR
                // ------------------------------------------------------------

                item {

                    SearchBar(
                        query = searchQuery,
                        onQueryChange = {
                            searchQuery = it
                        }
                    )
                }


                // ------------------------------------------------------------
                // CONTENIDO
                // ------------------------------------------------------------

                if (selectedTab == 0) {

                    if (
                        habitacionesFiltradas.isEmpty() &&
                        searchQuery.isNotBlank()
                    ) {

                        item {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),

                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.SearchOff,

                                        contentDescription = null,

                                        tint = Color.Gray,

                                        modifier = Modifier.size(40.dp)
                                    )

                                    Spacer(
                                        Modifier.height(10.dp)
                                    )

                                    Text(
                                        text =
                                            "No se encontraron habitaciones para \"$searchQuery\"",

                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                    } else {

                        items(
                            items = habitacionesFiltradas,
                            key = {
                                it.id_habitacion
                            }
                        ) { habitacion ->

                            HabitacionCard(
                                habitacion = habitacion,

                                onClick = {

                                    val encodedId =
                                        URLEncoder.encode(
                                            habitacion.id_habitacion,
                                            StandardCharsets.UTF_8.toString()
                                        )

                                    navController.navigate(
                                        "habitacion/$encodedId"
                                    )
                                }
                            )
                        }
                    }

                } else {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),

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


        // ====================================================================
        // FONDO OSCURO DEL MENÚ
        // ====================================================================

        AnimatedVisibility(
            visible = isMenuOpen
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.4f)
                    )
                    .clickable {
                        isMenuOpen = false
                    }
            )
        }


        // ====================================================================
        // MENÚ LATERAL
        // ====================================================================

        AnimatedVisibility(
            visible = isMenuOpen,

            enter = slideInHorizontally(
                initialOffsetX = {
                    -it
                }
            ),

            exit = slideOutHorizontally(
                targetOffsetX = {
                    -it
                }
            )
        ) {

            HamburgerMenu(

                onSelectTab = { tab ->

                    selectedTab = tab
                    isMenuOpen = false
                },

                onLogout = {

                    isMenuOpen = false

                    Toast.makeText(
                        context,
                        "Sesión cerrada",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            )
        }
    }
}
}

// ============================================================================
// MENÚ HAMBURGUESA
// ============================================================================

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

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(45.dp)
                .background(
                    Color(0xFFFFE0B2),
                    CircleShape
                ),

            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFFFA000)
            )
        }

        Spacer(
            Modifier.width(12.dp)
        )

        Text(
            text = "Ludwig Martínez",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }


    Spacer(
        Modifier.height(20.dp)
    )

    HorizontalDivider()

    Spacer(
        Modifier.height(20.dp)
    )


    MenuOption(
        icon = Icons.Default.Home,
        label = "Inicio"
    ) {
        onSelectTab(0)
    }

    Spacer(
        Modifier.height(10.dp)
    )


    MenuOption(
        icon = Icons.Default.GridView,
        label = "Catálogo"
    ) {
        onSelectTab(1)
    }

    Spacer(
        Modifier.height(10.dp)
    )


    MenuOption(
        icon = Icons.Default.CalendarToday,
        label = "Reserva"
    ) {
        onSelectTab(2)
    }


    Spacer(
        Modifier.weight(1f)
    )


    HorizontalDivider()

    Spacer(
        Modifier.height(16.dp)
    )


    MenuOption(
        icon = Icons.Default.Logout,
        label = "Cerrar sesión",
        tint = Color(0xFFE53935),
        onClick = onLogout
    )
}
}

// ============================================================================
// OPCIÓN DEL MENÚ
// ============================================================================

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
        .clickable {
            onClick()
        }
        .padding(
            vertical = 12.dp
        ),

    verticalAlignment =
        Alignment.CenterVertically,

    horizontalArrangement =
        Arrangement.SpaceBetween
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color(0xFFF1F5F9),
                    RoundedCornerShape(10.dp)
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(
            Modifier.width(14.dp)
        )

        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }


    Icon(
        imageVector =
            Icons.AutoMirrored.Filled.KeyboardArrowRight,

        contentDescription = null,

        tint = Color.Gray
    )
}
}

// ============================================================================
// HEADER
// ============================================================================

@Composable
fun Header(
userEmail: String,
onMenuClick: () -> Unit,
onProfileClick: () -> Unit
) {
val initials =
    if (userEmail.length >= 2)
        userEmail.take(2).uppercase()
    else
        "LI"


Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 5.dp),

    verticalAlignment =
        Alignment.CenterVertically,

    horizontalArrangement =
        Arrangement.SpaceBetween
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

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


        Spacer(
            Modifier.width(12.dp)
        )


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
            .background(
                Color(0xFFFFA000),
                CircleShape
            )
            .clickable {
                onProfileClick()
            },

        contentAlignment =
            Alignment.Center
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

// ============================================================================
// BARRA DE BÚSQUEDA
// ============================================================================

@Composable
fun SearchBar(
query: String,
onQueryChange: (String) -> Unit
) {
OutlinedTextField(

    value = query,

    onValueChange = onQueryChange,

    modifier = Modifier
        .fillMaxWidth()
        .height(65.dp),

    placeholder = {
        Text(
            "Buscar habitaciones, servicios..."
        )
    },

    leadingIcon = {

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar"
        )
    },

    trailingIcon = {

        if (query.isNotEmpty()) {

            IconButton(
                onClick = {
                    onQueryChange("")
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription =
                        "Limpiar búsqueda"
                )
            }
        }
    },

    shape = RoundedCornerShape(18.dp),

    colors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor =
            Color.White,

        focusedContainerColor =
            Color.White,

        unfocusedBorderColor =
            Color.Transparent,

        focusedBorderColor =
            Color(0xFFFFA000)
    ),

    singleLine = true
)
}

// ============================================================================
// NORMALIZACIÓN DE TEXTO
// ============================================================================

private fun normalizarTexto(
texto: String
): String {
val sinAcentos =
    java.text.Normalizer
        .normalize(
            texto,
            java.text.Normalizer.Form.NFD
        )
        .replace(
            Regex("\\p{Mn}+"),
            ""
        )

return sinAcentos
    .lowercase()
    .trim()
}

// ============================================================================
// FILTRO DE HABITACIONES
// ============================================================================

private fun Habitacion.coincideCon(
query: String
): Boolean {
if (query.isBlank()) {
    return true
}

val queryNormalizada =
    normalizarTexto(query)


return normalizarTexto(
    nombre_habitacion
).contains(queryNormalizada)

        ||

        normalizarTexto(
            descripcion
        ).contains(queryNormalizada)

        ||

        normalizarTexto(
            subtitulo ?: ""
        ).contains(queryNormalizada)
}

// ============================================================================
// OBTENER DRAWABLE
// ============================================================================

@Composable
fun resolveDrawableId(
nombre: String?
): Int {
val context =
    LocalContext.current

if (nombre.isNullOrBlank()) {
    return 0
}

val id =
    context.resources.getIdentifier(
        nombre,
        "drawable",
        context.packageName
    )

android.util.Log.d(
    "LudwigHotel",
    "Buscando drawable '$nombre' -> ID encontrado: $id"
)

return id
}

// ============================================================================
// TARJETA DE HABITACIÓN
// ============================================================================

@Composable
fun HabitacionCard(
habitacion: Habitacion,
onClick: () -> Unit
) {
Card(

    modifier = Modifier
        .fillMaxWidth()
        .clickable(
            onClick = onClick
        ),

    shape =
        RoundedCornerShape(25.dp),

    colors =
        CardDefaults.cardColors(
            containerColor = Color.White
        ),

    elevation =
        CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
) {

    Column(
        Modifier.padding(20.dp)
    ) {

        Box(

            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Color(0xFFE8D7C0),
                    RoundedCornerShape(20.dp)
                ),

            contentAlignment =
                Alignment.Center
        ) {

            if (
                !habitacion.imagen_habitacion
                    .isNullOrBlank()
            ) {

                AsyncImage(

                    model =
                        habitacion.imagen_habitacion,

                    contentDescription =
                        habitacion.nombre_habitacion,

                    modifier = Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(20.dp)
                        ),

                    contentScale =
                        ContentScale.Crop
                )

            } else {

                Icon(

                    imageVector =
                        Icons.Default.Hotel,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(70.dp),

                    tint =
                        Color(0xFFB07842)
                )
            }
        }


        Spacer(
            Modifier.height(15.dp)
        )


        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column(
                Modifier.weight(1f)
            ) {

                Text(

                    text =
                        habitacion.nombre_habitacion,

                    fontSize = 21.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF263238)
                )


                Spacer(
                    Modifier.height(7.dp)
                )


                Text(

                    text =
                        habitacion.subtitulo
                            ?: habitacion.descripcion,

                    color =
                        Color.Gray
                )
            }


            Column(
                horizontalAlignment =
                    Alignment.End
            ) {

                Text(

                    text =
                        "$${"%.0f".format(
                            habitacion.precio_noche
                        )}",

                    fontSize = 24.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFFFFA000)
                )


                Text(
                    text = "Noche",
                    color = Color.Gray
                )
            }
        }


        Spacer(
            Modifier.height(15.dp)
        )


        Row(

            modifier = Modifier
                .background(
                    Color(0xFFF3F3F3),
                    RoundedCornerShape(12.dp)
                )
                .padding(10.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    Icons.Default.Person,

                contentDescription =
                    "Capacidad",

                modifier =
                    Modifier.size(19.dp),

                tint =
                    Color.DarkGray
            )


            Spacer(
                Modifier.width(5.dp)
            )


            Text(

                text =
                    "${habitacion.capacidad_huespedes} huéspedes",

                color =
                    Color.DarkGray
            )
        }
    }
}
}

// ============================================================================
// PERFIL
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
userEmail: String,
onBack: () -> Unit,
onLogout: () -> Unit
) {
val context =
    LocalContext.current

val scope =
    rememberCoroutineScope()


val initials =
    if (userEmail.length >= 2)
        userEmail.take(2).uppercase()
    else
        "US"


val userUuid =
    remember {

        SupabaseClientProvider
            .client
            .auth
            .currentUserOrNull()
            ?.id
            ?: "No disponible"
    }


Scaffold(

    topBar = {

        TopAppBar(

            title = {

                Text(
                    "Mi Perfil",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 20.sp,
                    color =
                        Color(0xFF1E293B)
                )
            },

            navigationIcon = {

                IconButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription =
                            "Volver",

                        tint =
                            Color(0xFF1E293B)
                    )
                }
            },

            colors =
                TopAppBarDefaults
                    .topAppBarColors(
                        containerColor =
                            Color(0xFFF7F9FC)
                    )
        )
    },

    containerColor =
        Color(0xFFF7F9FC)
) { padding ->


    LazyColumn(

        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(
                horizontal = 20.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp),

        contentPadding =
            PaddingValues(
                bottom = 30.dp
            )
    ) {


        // ---------------------------------------------------------------
        // CABECERA DEL PERFIL
        // ---------------------------------------------------------------

        item {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(24.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
            ) {

                Column(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        contentAlignment =
                            Alignment.BottomEnd
                    ) {

                        Box(

                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    Color(0xFFFFA000),
                                    CircleShape
                                ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(

                                text =
                                    initials,

                                fontSize =
                                    34.sp,

                                fontWeight =
                                    FontWeight.ExtraBold,

                                color =
                                    Color.White
                            )
                        }


                        Box(

                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Color(0xFF1E293B)
                                )
                                .border(
                                    2.dp,
                                    Color.White,
                                    CircleShape
                                ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Edit,

                                contentDescription =
                                    null,

                                tint =
                                    Color.White,

                                modifier =
                                    Modifier.size(14.dp)
                            )
                        }
                    }


                    Spacer(
                        Modifier.height(16.dp)
                    )


                    Text(

                        text =
                            userEmail
                                .substringBefore("@")
                                .replaceFirstChar {
                                    it.uppercase()
                                },

                        fontSize = 22.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color(0xFF1E293B)
                    )


                    Text(

                        text = userEmail,

                        fontSize = 14.sp,

                        color = Color.Gray,

                        modifier =
                            Modifier.padding(
                                top = 2.dp
                            )
                    )


                    Spacer(
                        Modifier.height(12.dp)
                    )


                    Surface(

                        color =
                            Color(0xFFFFF8E1),

                        shape =
                            RoundedCornerShape(50)
                    ) {

                        Row(

                            modifier =
                                Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Fingerprint,

                                contentDescription =
                                    null,

                                tint =
                                    Color(0xFFFFA000),

                                modifier =
                                    Modifier.size(16.dp)
                            )


                            Spacer(
                                Modifier.width(6.dp)
                            )


                            Text(

                                text =
                                    "ID: ${
                                        if (userUuid.length > 12)
                                            userUuid.take(12) + "..."
                                        else
                                            userUuid
                                    }",

                                fontSize = 12.sp,

                                fontWeight =
                                    FontWeight.Medium,

                                color =
                                    Color(0xFFB57200)
                            )
                        }
                    }
                }
            }
        }


        // ---------------------------------------------------------------
        // INFORMACIÓN GENERAL
        // ---------------------------------------------------------------

        item {

            Text(

                text =
                    "Información General",

                fontSize = 16.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color(0xFF64748B),

                modifier =
                    Modifier.padding(
                        start = 4.dp,
                        top = 8.dp
                    )
            )
        }


        item {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
            ) {

                Column(

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                ) {

                    ProfileInfoItem(

                        icon =
                            Icons.Default.Email,

                        title =
                            "Correo Registrado",

                        value =
                            userEmail
                    )


                    HorizontalDivider(
                        color =
                            Color(0xFFF1F5F9)
                    )


                    ProfileInfoItem(

                        icon =
                            Icons.Default.VerifiedUser,

                        title =
                            "Estado de Cuenta",

                        value =
                            "Usuario Activo",

                        valueColor =
                            Color(0xFF2E7D32)
                    )
                }
            }
        }


        // ---------------------------------------------------------------
        // AJUSTES
        // ---------------------------------------------------------------

        item {

            Text(

                text =
                    "Ajustes y Preferencias",

                fontSize = 16.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color(0xFF64748B),

                modifier =
                    Modifier.padding(
                        start = 4.dp,
                        top = 8.dp
                    )
            )
        }


        item {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
            ) {

                Column(

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                ) {

                    ProfileOptionItem(
                        icon =
                            Icons.Default.CreditCard,
                        title =
                            "Métodos de Pago",
                        onClick = {}
                    )


                    HorizontalDivider(
                        color =
                            Color(0xFFF1F5F9)
                    )


                    ProfileOptionItem(
                        icon =
                            Icons.Default.Notifications,
                        title =
                            "Notificaciones",
                        onClick = {}
                    )


                    HorizontalDivider(
                        color =
                            Color(0xFFF1F5F9)
                    )


                    ProfileOptionItem(
                        icon =
                            Icons.Default.HelpOutline,
                        title =
                            "Centro de Ayuda",
                        onClick = {}
                    )
                }
            }
        }


        // ---------------------------------------------------------------
        // CERRAR SESIÓN
        // ---------------------------------------------------------------

        item {

            Spacer(
                Modifier.height(8.dp)
            )


            Button(

                onClick = {

                    scope.launch {

                        try {

                            SupabaseClientProvider
                                .client
                                .auth
                                .signOut()

                        } catch (e: Exception) {

                            e.printStackTrace()
                        }


                        Toast.makeText(
                            context,
                            "Sesión cerrada",
                            Toast.LENGTH_SHORT
                        ).show()


                        onLogout()
                    }
                },

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFFFEE2E2)
                    ),

                shape =
                    RoundedCornerShape(16.dp),

                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {

                Icon(

                    imageVector =
                        Icons.Default.ExitToApp,

                    contentDescription =
                        null,

                    tint =
                        Color(0xFFDC2626)
                )


                Spacer(
                    Modifier.width(8.dp)
                )


                Text(

                    text =
                        "Cerrar Sesión",

                    color =
                        Color(0xFFDC2626),

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}
}

// ============================================================================
// INFORMACIÓN DEL PERFIL
// ============================================================================

@Composable
private fun ProfileInfoItem(
icon: ImageVector,
title: String,
value: String,
valueColor: Color =
Color(0xFF1E293B)
) {
Row(

    modifier = Modifier
        .fillMaxWidth()
        .padding(
            vertical = 12.dp
        ),

    verticalAlignment =
        Alignment.CenterVertically
) {

    Box(

        modifier = Modifier
            .size(40.dp)
            .background(
                Color(0xFFFFF8E1),
                RoundedCornerShape(10.dp)
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Icon(

            imageVector = icon,

            contentDescription =
                null,

            tint =
                Color(0xFFFFA000),

            modifier =
                Modifier.size(20.dp)
        )
    }


    Spacer(
        Modifier.width(14.dp)
    )


    Column {

        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight =
                FontWeight.SemiBold,
            color = valueColor
        )
    }
}
}

// ============================================================================
// OPCIÓN DEL PERFIL
// ============================================================================

@Composable
private fun ProfileOptionItem(
icon: ImageVector,
title: String,
onClick: () -> Unit
) {
Row(

    modifier = Modifier
        .fillMaxWidth()
        .clickable(
            onClick = onClick
        )
        .padding(
            vertical = 14.dp,
            horizontal = 8.dp
        ),

    verticalAlignment =
        Alignment.CenterVertically
) {

    Box(

        modifier = Modifier
            .size(40.dp)
            .background(
                Color(0xFFF1F5F9),
                RoundedCornerShape(10.dp)
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Icon(

            imageVector = icon,

            contentDescription =
                null,

            tint =
                Color(0xFFFFA000),

            modifier =
                Modifier.size(20.dp)
        )
    }


    Spacer(
        Modifier.width(12.dp)
    )


    Text(

        text = title,

        color =
            Color(0xFF1E293B),

        fontWeight =
            FontWeight.Medium
    )


    Spacer(
        Modifier.weight(1f)
    )


    Icon(

        imageVector =
            Icons.AutoMirrored.Filled.KeyboardArrowRight,

        contentDescription =
            null,

        tint =
            Color.Gray
    )
}
}
