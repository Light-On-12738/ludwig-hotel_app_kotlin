package com.example.ludwighotel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ReservationsOrange = Color(0xFFFF9900)
private val ReservationsBackground = Color(0xFFF4F4F4)
private val ReservationsText = Color(0xFF172033)
private val ReservationsMuted = Color(0xFF6B7280)

@Serializable
private data class ReservaDb(
    @SerialName("id_reserva") val id: String,
    @SerialName("fecha_check_in") val checkIn: String,
    @SerialName("fecha_check_out") val checkOut: String,
    @SerialName("precio_total") val total: Double,
    @SerialName("estado") val estado: String,
    @SerialName("id_habitacion") val roomId: String,
    @SerialName("cantidad_huespedes") val guests: Int = 1
)

private data class ReservaUi(
    val reserva: ReservaDb,
    val roomName: String
)

private suspend fun cargarMisReservas(userId: String): List<ReservaUi> {
    val reservas = SupabaseClientProvider.client
        .from("reserva")
        .select {
            filter { eq("id_usuario", userId) }
        }
        .decodeList<ReservaDb>()
        .sortedByDescending { it.checkIn }

    return reservas.map { reserva ->
        val room = SupabaseClientProvider.client
            .from("habitaciones")
            .select {
                filter { eq("id_habitacion", reserva.roomId) }
            }
            .decodeSingleOrNull<Habitacion>()

        ReservaUi(reserva, room?.nombre_habitacion ?: "Habitación")
    }
}

@Composable
fun MisReservasScreen(
    onBack: () -> Unit,
    onNewReservation: () -> Unit,
    onReservationDetails: (String) -> Unit
) {
    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
    var reservations by remember { mutableStateOf<List<ReservaUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            loading = false
            error = "Inicia sesión para ver tus reservas."
            return@LaunchedEffect
        }
        loading = true
        error = null
        runCatching { cargarMisReservas(userId) }
            .onSuccess { reservations = it }
            .onFailure { error = "No se pudieron cargar tus reservas." }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(ReservationsBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp, 20.dp, 24.dp, 94.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).background(Color.White, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ReservationsText)
                    }
                    Spacer(Modifier.weight(1f))
                     Text(
                        text = "Ludwing Hotel",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA000)
                    )


                    Spacer(Modifier.width(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.milogo),
                        contentDescription = "Logo de Ludwing Hotel",
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Mis reservas", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = ReservationsText)
                Text("Revisa tus estancias y gestiona tus viajes.", fontSize = 14.sp, color = ReservationsMuted)
                Spacer(Modifier.height(4.dp))
            }

            when {
                loading -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 90.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ReservationsOrange)
                    }
                }
                error != null -> item {
                    Text(error!!, color = Color(0xFFFF3B47), modifier = Modifier.padding(top = 36.dp))
                }
                reservations.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(top = 150.dp), contentAlignment = Alignment.Center) {
                        Text("No has realizado ninguna reserva", color = ReservationsMuted)
                    }
                }
                else -> items(reservations, key = { it.reserva.id }) { item ->
                    ReservationCard(item, onClick = {
                        onReservationDetails(item.reserva.id)
                    })
                }
            }
        }

        Button(
            onClick = onNewReservation,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp).height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ReservationsOrange, contentColor = Color.White)
        ) {
            Text("Nueva reserva", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ReservationCard(item: ReservaUi, onClick: () -> Unit) {
    val active = item.reserva.estado.equals("Activa", ignoreCase = true)
    val border = if (active) ReservationsOrange else Color(0xFFE5E7EB)
    val estado = if (active) "ACTIVA" else "FINALIZADA"
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFFFFF8ED) else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(estado, color = if (active) ReservationsOrange else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Estancia en Hotel", color = ReservationsText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                ReservationValue("Check-in", formatReservationDate(item.reserva.checkIn), Modifier.weight(1f))
                ReservationValue("Check-out", formatReservationDate(item.reserva.checkOut), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                ReservationValue("Habitación", item.roomName, Modifier.weight(1f))
                ReservationValue("Huéspedes", "${item.reserva.guests} adulto(s)", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = ReservationsOrange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (active) "Ver detalles" else "Ver factura", color = ReservationsOrange, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("$${"%.2f".format(item.reserva.total)}", color = ReservationsText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ReservaDetalleScreen(reservaId: String, onBack: () -> Unit) {
    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
    var reserva by remember { mutableStateOf<ReservaUi?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reservaId, userId) {
        if (userId == null || reservaId.isBlank()) {
            error = "No se encontró la reserva."
            loading = false
            return@LaunchedEffect
        }
        runCatching {
            cargarMisReservas(userId).firstOrNull { it.reserva.id == reservaId }
        }.onSuccess {
            reserva = it
            if (it == null) error = "No se encontró la reserva."
        }.onFailure {
            error = "No se pudo cargar el detalle de la reserva."
        }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(ReservationsBackground)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).background(Color.White, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ReservationsText)
                }
                Spacer(Modifier.width(14.dp))
                Text("Detalle de reserva", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ReservationsText)
            }
            Spacer(Modifier.height(28.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ReservationsOrange)
                }
                error != null -> Text(error!!, color = Color(0xFFFF3B47))
                reserva != null -> ReservationDetailContent(reserva!!)
            }
        }
    }
}

@Composable
private fun ReservationDetailContent(item: ReservaUi) {
    val active = item.reserva.estado.equals("Activa", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) ReservationsOrange else Color(0xFFE5E7EB)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (active) "RESERVA ACTIVA" else "RESERVA FINALIZADA",
                color = if (active) ReservationsOrange else ReservationsMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(item.roomName, color = ReservationsText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            DetailRow("Check-in", formatReservationDate(item.reserva.checkIn))
            DetailRow("Check-out", formatReservationDate(item.reserva.checkOut))
            DetailRow("Huéspedes", "${item.reserva.guests} adulto(s)")
            DetailRow("Total pagado", "$${"%.2f".format(item.reserva.total)}")
            DetailRow("Código", item.reserva.id.take(8).uppercase())
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = ReservationsMuted)
        Text(value, color = ReservationsText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReservationValue(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = ReservationsMuted, fontSize = 12.sp)
        Text(value, color = ReservationsText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatReservationDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es")))
}.getOrDefault(value)
