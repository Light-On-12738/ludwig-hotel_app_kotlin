package com.example.ludwighotel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ImagenHabitacion(
    val id_imagen: String,
    val id_habitacion: String,
    val ruta: String
)

@Serializable
data class Resena(
    val id_resena: String,
    val id_habitacion: String,
    val id_usuario: String,
    val comentario: String? = null,
    val puntuacion: Int
)

class HabitacionDetalleRepository {

    suspend fun obtenerHabitacionPorId(id: String): Habitacion {
        return SupabaseClientProvider.client
            .from("habitaciones")
            .select {
                filter { eq("id_habitacion", id) }
            }
            .decodeSingle<Habitacion>()
    }

    suspend fun obtenerImagenesPorHabitacion(id: String): List<ImagenHabitacion> {
        return SupabaseClientProvider.client
            .from("imagenes_habitacion")
            .select {
                filter { eq("id_habitacion", id) }
            }
            .decodeList<ImagenHabitacion>()
    }

    suspend fun obtenerResenasPorHabitacion(id: String): List<Resena> {
        return SupabaseClientProvider.client
            .from("resenas")
            .select {
                filter { eq("id_habitacion", id) }
            }
            .decodeList<Resena>()
    }
}

class HabitacionDetalleViewModel(
    private val habitacionId: String
) : ViewModel() {

    private val repo = HabitacionDetalleRepository()

    var habitacion by mutableStateOf<Habitacion?>(null)
        private set

    var imagenes by mutableStateOf<List<String>>(emptyList())
        private set

    var promedioEstrellas by mutableStateOf(0.0)
        private set

    var totalResenas by mutableStateOf(0)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            isLoading = true

            val habitacionData = repo.obtenerHabitacionPorId(habitacionId)
            habitacion = habitacionData

            val galeria = repo.obtenerImagenesPorHabitacion(habitacionId)
            val rutas = mutableListOf<String>()
            habitacionData.imagen_habitacion
                ?.takeIf { it.isNotBlank() }
                ?.let { rutas.add(it) }
            galeria.forEach { imagen ->
                if (imagen.ruta.isNotBlank() && imagen.ruta !in rutas) {
                    rutas.add(imagen.ruta)
                }
            }
            imagenes = rutas

            val resenas = repo.obtenerResenasPorHabitacion(habitacionId)
            totalResenas = resenas.size
            promedioEstrellas = if (resenas.isNotEmpty()) {
                resenas.sumOf { it.puntuacion }.toDouble() / resenas.size
            } else {
                0.0
            }

            isLoading = false
        }
    }
}

class HabitacionDetalleViewModelFactory(
    private val habitacionId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HabitacionDetalleViewModel(habitacionId) as T
    }
}

@Composable
fun HabitacionDetalleScreen(
    habitacionId: String,
    onBack: () -> Unit,
    onReserve: (Habitacion) -> Unit = {},
    viewModel: HabitacionDetalleViewModel = viewModel(
        factory = HabitacionDetalleViewModelFactory(habitacionId)
    )
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4F4F4)
        ) {
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFA000))
                    }
                }

                viewModel.habitacion == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se encontró la habitación")
                    }
                }

                else -> {
                    HabitacionDetalleContenido(
                        habitacion = viewModel.habitacion!!,
                        imagenes = viewModel.imagenes,
                        promedioEstrellas = viewModel.promedioEstrellas,
                        totalResenas = viewModel.totalResenas,
                        onBack = onBack,
                        onReserve = onReserve
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitacionDetalleContenido(
    habitacion: Habitacion,
    imagenes: List<String>,
    promedioEstrellas: Double,
    totalResenas: Int,
    onBack: () -> Unit,
    onReserve: (Habitacion) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF4F4F4),
        bottomBar = {
            Surface(color = Color(0xFFF4F4F4)) {
                Button(
                    onClick = { onReserve(habitacion) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                ) {
                    Text(
                        text = "Reservar Habitación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ludwing Hotel",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA000)
                    )
                    Spacer(Modifier.width(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.milogo),
                        contentDescription = "Logo de Ludwing Hotel",
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(25.dp))
                    .padding(20.dp)
            ) {
                ImagenesCarrusel(imagenes = imagenes)

                Spacer(Modifier.height(16.dp))

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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = habitacion.subtitulo ?: "",
                            color = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${"%.0f".format(habitacion.precio_noche)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA000)
                        )
                        Text("Noche", color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(14.dp))

                CalificacionPromedio(promedio = promedioEstrellas, total = totalResenas)

                Spacer(Modifier.height(14.dp))

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

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Descripción:",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = habitacion.descripcion,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CalificacionPromedio(promedio: Double, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val icono = when {
                promedio >= index + 1 -> Icons.Default.Star
                promedio >= index + 0.5 -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Color(0xFFFFA000),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        if (total > 0) {
            Text(
                text = "${"%.1f".format(promedio)} · $total reseña${if (total == 1) "" else "s"}",
                color = Color.Gray,
                fontSize = 13.sp
            )
        } else {
            Text("Sin reseñas todavía", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImagenesCarrusel(imagenes: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        if (imagenes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8D7C0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hotel,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFFB07842)
                )
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { imagenes.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = imagenes[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (imagenes.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(imagenes.size) { index ->
                        val activo = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (activo) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (activo) Color.White else Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}
