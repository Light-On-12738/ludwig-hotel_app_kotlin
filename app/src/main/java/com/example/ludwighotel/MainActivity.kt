package com.example.ludwighotel

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay
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
// IDENTIDAD DEL USUARIO AUTENTICADO
// ============================================================================
// Centralizado para que todas las pantallas usen la misma fuente de verdad:
// primero Supabase Auth y, si no hay sesión, la tabla "usuario".
// ============================================================================

private fun correoDeSesion(): String? =
    SupabaseClientProvider.client
        .auth
        .currentUserOrNull()
        ?.email
        ?.takeIf { it.isNotBlank() }

private fun idDeSesion(): String? =
    SupabaseClientProvider.client
        .auth
        .currentUserOrNull()
        ?.id
        ?.takeIf { it.isNotBlank() }

/**
 * Perfil del usuario actual leído desde la base de datos.
 * Se busca por id de Auth y, como respaldo, por correo electrónico.
 */
private suspend fun cargarPerfilActual(correoRespaldo: String): UsuarioTabla? {
    val porId = idDeSesion()?.let { obtenerUsuarioPorId(it) }
    if (porId != null) return porId

    val correo = correoDeSesion() ?: correoRespaldo
    return obtenerUsuarioPorCorreo(correo)
}

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            // Pantalla inicial: se decide DESPUÉS de comprobar si ya hay una
            // sesión de Supabase persistida en disco (y si el usuario pidió
            // que se recordara con el checkbox "Recordarme"). Mientras tanto
            // se muestra un loader para no "parpadear" hacia el login.
            var startDestination by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                // supabase-kt carga la sesión guardada desde disco de forma
                // asíncrona; hay que esperar a que termine antes de preguntar
                // por la sesión actual, o siempre dará null.
                SupabaseClientProvider.client.auth.awaitInitialization()

                val sesionGuardada = SupabaseClientProvider.client.auth.currentSessionOrNull()
                val debeRecordar = SessionPreferences.shouldRememberSession(this@MainActivity)

                startDestination = if (sesionGuardada != null && debeRecordar) {
                    val correo = sesionGuardada.user?.email.orEmpty()
                    val encodedEmail = URLEncoder.encode(
                        correo,
                        StandardCharsets.UTF_8.toString()
                    )
                    "hotel/$encodedEmail"
                } else {
                    if (sesionGuardada != null && !debeRecordar) {
                        // El usuario no marcó "Recordarme": se invalida la
                        // sesión persistida para que deba loguearse de nuevo.
                        try {
                            SupabaseClientProvider.client.auth.signOut()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    "login"
                }
            }

            val destinoInicial = startDestination

            if (destinoInicial == null) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            } else {

            val navController = rememberNavController()

            // La habitación seleccionada se conserva mientras el usuario avanza
            // por las pantallas del flujo de reserva.
            var habitacionParaReservar by remember {
                mutableStateOf<RoomForReservation?>(null)
            }

            NavHost(
                navController = navController,
                startDestination = destinoInicial
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
                        },
                        onReserve = { habitacion ->
                            habitacionParaReservar = RoomForReservation(
                                id = habitacion.id_habitacion,
                                name = habitacion.nombre_habitacion,
                                pricePerNight = habitacion.precio_noche,
                                description = habitacion.descripcion,
                                imageUrl = habitacion.imagen_habitacion
                            )

                            navController.navigate("reserva")
                        }
                    )
                }

                // ------------------------------------------------------------
                // FLUJO DE RESERVA
                // ------------------------------------------------------------
                composable("reserva") {
                    val room = habitacionParaReservar
                    val userId = SupabaseClientProvider.client
                        .auth
                        .currentUserOrNull()
                        ?.id

                    when {
                        room == null -> {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }

                        userId == null -> {
                            LaunchedEffect(Unit) {
                                navController.navigate("login") {
                                    popUpTo("login") {
                                        inclusive = true
                                    }
                                }
                            }
                        }

                        else -> {
                            ReservationFlowScreen(
                                room = room,
                                userId = userId,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onReservationCreated = {
                                    habitacionParaReservar = null
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }

                composable("mis_reservas") {
                    MisReservasScreen(
                        onBack = { navController.popBackStack() },
                        onNewReservation = { navController.popBackStack() },
                        onReservationDetails = { reservaId ->
                            navController.navigate("detalle_reserva/$reservaId")
                        }
                    )
                }

                composable(
                    route = "detalle_reserva/{reservaId}",
                    arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
                ) { entry ->
                    ReservaDetalleScreen(
                        reservaId = entry.arguments?.getString("reservaId") ?: "",
                        onBack = { navController.popBackStack() }
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
                        onReservationsClick = {
                            navController.navigate("mis_reservas")
                        }
                    )
                }
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
    onLogout: () -> Unit,
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
    val scope = rememberCoroutineScope()

    // Nombre real del usuario: se toma de la tabla "usuario". Mientras carga se
    // muestra un nombre derivado del correo de la sesión.
    var nombreUsuario by remember {
        mutableStateOf(formatearNombreDesdeCorreo(correoDeSesion() ?: userEmail))
    }

    LaunchedEffect(userEmail) {

        cargarPerfilActual(userEmail)?.let { perfil ->
            nombreUsuario = perfil.nombreCompleto
        }
    }

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
                            }
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

                    nombreUsuario = nombreUsuario,

                    onProfileClick = {

                        isMenuOpen = false

                        onProfileClick()
                    },

                    onSelectTab = { tab ->
                        if (tab == 2) {
                            navController.navigate("mis_reservas")
                        } else {
                            selectedTab = tab
                        }
                        isMenuOpen = false
                    },

                    onLogout = {

                        isMenuOpen = false

                        scope.launch {

                            try {

                                SupabaseClientProvider
                                    .client
                                    .auth
                                    .signOut()

                                SessionPreferences.clear(context)

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
    nombreUsuario: String,
    onProfileClick: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onProfileClick()
                }
                .padding(vertical = 6.dp),

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
                text = nombreUsuario,
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
            icon = Icons.Default.CalendarToday,
            label = "Reserva"
        ) {
            onSelectTab(2)
        }

        Spacer(
            Modifier.height(10.dp)
        )


        // Acceso a Perfil: únicamente desde el encabezado (nombre y foto arriba).
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
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(55.dp)
                .background(Color.White, RoundedCornerShape(50.dp))
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menú")
        }

        Text(
            text = "Ludwing Hotel",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFA000)
        )

        val logoDrawableId = resolveDrawableId("milogo")
        if (logoDrawableId != 0) {
            Image(
                painter = painterResource(id = logoDrawableId),
                contentDescription = "Logo de Ludwing Hotel",
                modifier = Modifier.size(38.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Spacer(Modifier.size(38.dp))
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
                text = "Buscar habitaciones, servicios...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

        singleLine = true,
        maxLines = 1
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
// Se accede exclusivamente desde el menú hamburguesa. Los datos salen de la
// tabla "usuario" de Supabase: el correo mostrado es el correo real, nunca el
// id. La edición escribe directamente en la base de datos y después vuelve a
// leer el registro para reflejar la información guardada.
// ============================================================================

@Composable
fun ProfileScreen(
    userEmail: String,
    onBack: () -> Unit,
    onReservationsClick: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val correoAuth = remember { correoDeSesion() }

    var usuario by remember { mutableStateOf<UsuarioTabla?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var recarga by remember { mutableIntStateOf(0) }

    // Carga inicial y recarga después de guardar.
    LaunchedEffect(recarga) {

        cargando = true
        errorCarga = null

        val perfil = cargarPerfilActual(correoAuth ?: userEmail)

        usuario = perfil

        if (perfil == null) {
            errorCarga = "No se pudo cargar tu perfil desde la base de datos."
        }

        cargando = false
    }

    // Actualización automática del perfil.
    // Se consulta periódicamente la fila del usuario para mantener la UI
    // sincronizada con la base de datos sin depender de una API experimental.
    LaunchedEffect(usuario?.id) {
        val idUsuario = usuario?.id ?: return@LaunchedEffect

        while (true) {
            try {
                val usuarioActualizado = obtenerUsuarioPorId(idUsuario)

                if (usuarioActualizado != null) {
                    usuario = usuarioActualizado
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            delay(2000)
        }
    }

    val perfil = usuario

    val correoReal = perfil?.email
        ?: correoAuth
        ?: userEmail

    val nombreCompleto = perfil?.nombreCompleto
        ?: formatearNombreDesdeCorreo(correoReal)

    val telefono = perfil?.telefono?.takeIf { it.isNotBlank() }
        ?: "No registrado"

    val dui = perfil?.dui?.takeIf { it.isNotBlank() }
        ?: "No registrado"

    val initials = remember(nombreCompleto) {
        nombreCompleto
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "US" }
    }


    if (mostrarDialogo && perfil != null) {

        EditarPerfilDialog(
            usuario = perfil,

            onDismiss = {
                mostrarDialogo = false
            },

            onGuardar = { nombre, tel, duiNuevo, onResultado ->

                scope.launch {

                    val resultado = actualizarUsuario(
                        id = perfil.id,
                        nombreCompleto = nombre,
                        telefono = tel,
                        dui = duiNuevo
                    )

                    resultado.fold(

                        onSuccess = {

                            // Se relee el registro para mostrar exactamente lo
                            // que quedó almacenado en la base de datos.
                            val actualizado = obtenerUsuarioPorId(perfil.id)

                            if (actualizado == null) {

                                onResultado(
                                    "Se guardó, pero no se pudo releer el perfil. Revisa tu conexión."
                                )

                            } else {

                                usuario = actualizado
                                mostrarDialogo = false

                                Toast.makeText(
                                    context,
                                    "Perfil actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onResultado(null)

                                recarga++
                            }
                        },

                        onFailure = { e ->

                            onResultado(
                                "No se pudo guardar: ${e.message ?: "error de conexión"}"
                            )
                        }
                    )
                }
            }
        )
    }


    Scaffold(

        topBar = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F9FC))
                    .padding(
                        horizontal = 20.dp,
                        vertical = 14.dp
                    ),

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                IconButton(
                    onClick = onBack,

                    modifier = Modifier
                        .size(45.dp)
                        .background(Color.White, CircleShape)
                ) {

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFF1E293B)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Ludwing Hotel",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA000)
                    )

                    Spacer(Modifier.width(8.dp))

                    // Logo de la app, ubicado en res/drawable (archivo "miLogo.png"),
                    // resuelto por nombre en tiempo de ejecución, junto al nombre.
                    val logoDrawableId = resolveDrawableId("milogo")

                    if (logoDrawableId != 0) {

                        Image(
                            painter = painterResource(id = logoDrawableId),
                            contentDescription = "Logo de Ludwing Hotel",
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
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


            if (cargando) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = Color(0xFFFFA000)
                        )
                    }
                }
            }


            errorCarga?.let { mensaje ->

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEE2E2)
                        )
                    ) {

                        Text(
                            text = mensaje,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp
                        )
                    }
                }
            }


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
                            .padding(24.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(

                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        Color(0xFFFFE0B2),
                                        CircleShape
                                    ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(

                                    text =
                                        initials,

                                    fontSize =
                                        22.sp,

                                    fontWeight =
                                        FontWeight.ExtraBold,

                                    color =
                                        Color(0xFFFFA000)
                                )
                            }

                            Spacer(
                                Modifier.width(16.dp)
                            )

                            Column {

                                Text(
                                    text = nombreCompleto,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )

                                Text(
                                    text = correoReal,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(
                            Modifier.height(16.dp)
                        )

                        Text(
                            text =
                                "Bienvenido a tu perfil. Aquí puedes revisar tus reservas y datos de cuenta.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(
                            Modifier.height(16.dp)
                        )

                    }
                }
            }

// INFORMACIÓN DEL PERFIL
            // ---------------------------------------------------------------

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),

                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(

                        text =
                            "Información del Perfil",

                        fontSize = 16.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color(0xFF1E293B),

                        modifier =
                            Modifier.padding(start = 4.dp)
                    )

                    TextButton(
                        onClick = {
                            mostrarDialogo = true
                        },
                        enabled = perfil != null
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "Editar",
                            color = Color(0xFFFFA000),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
                                vertical = 4.dp
                            )
                    ) {

                        PerfilDatoRow(
                            etiqueta = "Nombre completo",
                            valor = nombreCompleto
                        )

                        HorizontalDivider(
                            color =
                                Color(0xFFF1F5F9)
                        )

                        PerfilDatoRow(
                            etiqueta = "Correo electrónico",
                            valor = correoReal
                        )

                        HorizontalDivider(
                            color =
                                Color(0xFFF1F5F9)
                        )

                        PerfilDatoRow(
                            etiqueta = "Teléfono",
                            valor = telefono
                        )

                        HorizontalDivider(
                            color =
                                Color(0xFFF1F5F9)
                        )

                        PerfilDatoRow(
                            etiqueta = "DUI",
                            valor = dui
                        )
                    }
                }
            }


            // ---------------------------------------------------------------
            // ACCIONES RÁPIDAS
            // ---------------------------------------------------------------

            item {

                Text(

                    text =
                        "Acciones Rápidas",

                    fontSize = 16.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF1E293B),

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
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                    ) {

                        ProfileOptionItem(
                            icon = Icons.Default.CalendarToday,
                            title = "Mis reservas",
                            onClick = onReservationsClick
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// DIÁLOGO DE EDICIÓN DEL PERFIL
// ============================================================================
// Solo edita los campos que la base de datos permite cambiar al propio
// usuario: nombre, teléfono y DUI. El correo y el id no son editables.
// ============================================================================

@Composable
private fun EditarPerfilDialog(
    usuario: UsuarioTabla,
    onDismiss: () -> Unit,
    onGuardar: (
        nombre: String,
        telefono: String?,
        dui: String,
        onResultado: (String?) -> Unit
    ) -> Unit
) {

    var nombre by remember { mutableStateOf(usuario.nombreCompleto) }
    var telefono by remember { mutableStateOf(formatearTelefono(usuario.telefono ?: "")) }
    var dui by remember { mutableStateOf(formatearDui(usuario.dui)) }

    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nombreValido = nombreEsValido(nombre)
    val telefonoValido = telefono.isBlank() || telefonoEsValido(telefono)
    val duiValido = duiEsValido(dui)
    val puedeGuardar = nombreValido && telefonoValido && duiValido && !guardando


    AlertDialog(

        onDismissRequest = {
            if (!guardando) onDismiss()
        },

        title = {
            Text(
                text = "Editar información",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = formatearNombre(it) },
                    label = { Text("Nombre completo") },
                    isError = !nombreValido,
                    singleLine = true,
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = formatearTelefono(it) },
                    label = { Text("Teléfono (0000-0000)") },
                    isError = !telefonoValido,
                    singleLine = true,
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dui,
                    onValueChange = { dui = formatearDui(it) },
                    label = { Text("DUI (00000000-0)") },
                    isError = !duiValido,
                    singleLine = true,
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Correo: ${usuario.email} (no editable)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                error?.let {

                    Text(
                        text = it,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp
                    )
                }

                if (guardando) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA000)
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = "Guardando en la base de datos…",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                enabled = puedeGuardar,
                onClick = {

                    error = null
                    guardando = true

                    onGuardar(
                        nombre.trim(),
                        telefono.takeIf { it.isNotBlank() },
                        dui
                    ) { mensaje ->

                        guardando = false
                        error = mensaje
                    }
                }
            ) {

                Text(
                    text = "Guardar",
                    color = Color(0xFFFFA000),
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {

            TextButton(
                enabled = !guardando,
                onClick = onDismiss
            ) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

// ============================================================================
// NOMBRE A PARTIR DEL CORREO
// ============================================================================

private fun formatearNombreDesdeCorreo(correo: String): String {
    val parteLocal = correo.substringBefore("@")

    return parteLocal
        .split(".", "_", "-")
        .filter { it.isNotBlank() }
        .joinToString(" ") { palabra ->
            palabra.replaceFirstChar { it.uppercase() }
        }
        .ifBlank { "Usuario" }
}

// ============================================================================
// DATO DEL PERFIL (etiqueta / valor en la misma fila)
// ============================================================================

@Composable
private fun PerfilDatoRow(
    etiqueta: String,
    valor: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),

        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = etiqueta,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = valor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
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
