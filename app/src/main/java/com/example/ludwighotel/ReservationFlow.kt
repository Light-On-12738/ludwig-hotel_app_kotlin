package com.example.ludwighotel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HotelOrange = Color(0xFFFF9900)
private val OccupiedRed = Color(0xFFFF3B47)
private val SelectedGreen = Color(0xFF22C55E)
private val NeutralDay = Color(0xFFF1F3F5)

/** Datos mínimos que la pantalla necesita de una habitación. */
data class RoomForReservation(
    val id: String, // UUID de la tabla habitaciones en Supabase
    val name: String,
    val pricePerNight: Double,
    val description: String = ""
)

@Serializable
private data class ReservationRow(
    @SerialName("fecha_check_in") val checkIn: String,
    @SerialName("fecha_check_out") val checkOut: String
)

private data class GuestData(
    val document: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val guests: String = ""
)

private class ReservationRepository {
    /**
     * Devuelve reservas que se cruzan con el rango de calendario solicitado.
     * La fecha de salida queda libre: una estancia que sale el 10 permite
     * que otra entre el 10.
     */
    suspend fun occupiedRanges(roomId: String, from: LocalDate, until: LocalDate): List<ClosedRange<LocalDate>> =
        withContext(Dispatchers.IO) {
            SupabaseClientProvider.client.postgrest
                .rpc(
                    "fechas_ocupadas",
                    buildJsonObject {
                        put("p_id_habitacion", roomId)
                        put("p_desde", from.toString())
                        put("p_hasta", until.toString())
                    }
                )
                .decodeList<ReservationRow>()
                .map { LocalDate.parse(it.checkIn)..LocalDate.parse(it.checkOut).minusDays(1) }
        }

    /**
     * El RPC de SQL hace la comprobación final y el INSERT en una operación
     * atómica. Nunca almacenar CVV ni el número completo de tarjeta.
     */
    suspend fun createReservation(
        roomId: String,
        userId: String,
        guest: GuestData,
        checkIn: LocalDate,
        checkOut: LocalDate,
        total: Double,
        paymentMethod: String,
        cardLast4: String
    ) = withContext(Dispatchers.IO) {
        SupabaseClientProvider.client.postgrest.rpc(
            "crear_reserva_segura",
            buildJsonObject {
                put("p_habitacion_id", roomId)
                put("p_usuario_id", userId)
                put("p_documento", guest.document)
                put("p_nombre", guest.firstName)
                put("p_apellido", guest.lastName)
                put("p_huespedes", guest.guests.toInt())
                put("p_fecha_entrada", checkIn.toString())
                put("p_fecha_salida", checkOut.toString())
                put("p_total", total)
                put("p_metodo_pago", paymentMethod)
                put("p_tarjeta_ultimos4", cardLast4)
            }
        )
    }
}

/**
 * Pantalla completa de reserva. Llamarla desde el botón "Reservar habitación".
 * userId debe ser el id de Supabase Auth del usuario autenticado.
 */
@Composable
fun ReservationFlowScreen(
    room: RoomForReservation,
    userId: String,
    onBack: () -> Unit,
    onReservationCreated: () -> Unit
) {
    val repository = remember { ReservationRepository() }
    var step by remember { mutableIntStateOf(1) }
    var guest by remember { mutableStateOf(GuestData()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var checkIn by remember { mutableStateOf<LocalDate?>(null) }
    var checkOut by remember { mutableStateOf<LocalDate?>(null) }
    var occupied by remember { mutableStateOf<List<ClosedRange<LocalDate>>>(emptyList()) }
    var loadingCalendar by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf("Débito") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(room.id, month) {
        loadingCalendar = true
        error = null
        runCatching {
            repository.occupiedRanges(room.id, month.atDay(1), month.plusMonths(1).atDay(1))
        }.onSuccess { occupied = it }
            .onFailure { error = "No fue posible consultar la disponibilidad." }
        loadingCalendar = false
    }

    val nights = if (checkIn != null && checkOut != null) {
        java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut).toInt()
    } else 0
    val total = nights * room.pricePerNight

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("‹", fontSize = 36.sp, modifier = Modifier.clickable(onClick = onBack))
            ReservationHeader(room, step, nights, total)
        }

        item {
            when (step) {
                1 -> GuestStep(guest) { guest = it }
                2 -> CalendarStep(
                    month = month,
                    onPreviousMonth = { month = month.minusMonths(1) },
                    onNextMonth = { month = month.plusMonths(1) },
                    checkIn = checkIn,
                    checkOut = checkOut,
                    occupied = occupied,
                    loading = loadingCalendar,
                    onDaySelected = { day ->
                        when {
                            day < LocalDate.now() || isOccupied(day, occupied) -> Unit
                            checkIn == null || checkOut != null -> {
                                checkIn = day
                                checkOut = null
                            }
                            day <= checkIn -> checkIn = day
                            hasOccupiedDate(checkIn!!, day, occupied) ->
                                error = "Ese rango incluye días ya ocupados."
                            else -> checkOut = day.plusDays(1)
                        }
                    }
                )
                else -> PaymentStep(
                    paymentMethod = paymentMethod,
                    onPaymentMethodChange = { paymentMethod = it },
                    cardNumber = cardNumber,
                    onCardNumberChange = { cardNumber = it.filter(Char::isDigit).take(16) },
                    expiry = expiry,
                    onExpiryChange = { expiry = it.take(5) },
                    cvv = cvv,
                    onCvvChange = { cvv = it.filter(Char::isDigit).take(4) }
                )
            }
        }

        error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }

        item {
            val enabled = when (step) {
                1 -> guest.document.isNotBlank() && guest.firstName.isNotBlank() &&
                    guest.lastName.isNotBlank() && (guest.guests.toIntOrNull() ?: 0) > 0
                2 -> checkIn != null && checkOut != null
                else -> cardNumber.length in 12..16 && expiry.length == 5 && cvv.length in 3..4
            } && !submitting

            Button(
                enabled = enabled,
                onClick = {
                    error = null
                    if (step < 3) {
                        step++
                    } else {
                        submitting = true
                        // La función SQL vuelve a validar el cruce por seguridad.
                        // Si otra persona reservó antes, mostrará un error y no insertará nada.
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HotelOrange)
            ) {
                Text(
                    if (step == 3) "Confirmar pago ($${"%.2f".format(total)})"
                    else "Continuar (${step}/3)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // El bloque de confirmación se mantiene fuera del onClick para poder llamar
    // a suspend functions desde un scope de Compose sin guardar datos de tarjeta.
    if (submitting) {
        LaunchedEffect(Unit) {
            runCatching {
                repository.createReservation(
                    roomId = room.id,
                    userId = userId,
                    guest = guest,
                    checkIn = checkIn!!,
                    checkOut = checkOut!!,
                    total = total,
                    paymentMethod = paymentMethod,
                    cardLast4 = cardNumber.takeLast(4)
                )
            }.onSuccess { onReservationCreated() }
                .onFailure {
                    submitting = false
                    error = "La habitación ya no está disponible o no se pudo crear la reserva."
                }
        }
    }
}

@Composable
private fun ReservationHeader(room: RoomForReservation, step: Int, nights: Int, total: Double) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(room.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Paso $step de 3", color = Color.Gray)
                if (nights > 0) Text("$nights noche(s) · Total $${"%.2f".format(total)}")
            }
            Text("$${"%.2f".format(room.pricePerNight)}\nNoche", color = HotelOrange, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GuestStep(value: GuestData, onChange: (GuestData) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Datos de la reserva", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Field(
            label = "Documento de identidad",
            value = value.document,
            onValueChange = { onChange(value.copy(document = it)) }
        )
        Field(
            label = "Nombre completo",
            value = value.firstName,
            onValueChange = { onChange(value.copy(firstName = it)) }
        )
        Field(
            label = "Apellido completo",
            value = value.lastName,
            onValueChange = { onChange(value.copy(lastName = it)) }
        )
        Field(
            label = "Número de huéspedes",
            value = value.guests,
            onValueChange = { onChange(value.copy(guests = it.filter(Char::isDigit))) }
        )
    }
}

@Composable
private fun CalendarStep(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    checkIn: LocalDate?,
    checkOut: LocalDate?,
    occupied: List<ClosedRange<LocalDate>>,
    loading: Boolean,
    onDaySelected: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Elige llegada y salida", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Rojo: ocupado · Verde: seleccionado. La salida no cuenta como noche ocupada.", fontSize = 12.sp, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onPreviousMonth))
            Text(month.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() } + " ${month.year}", fontWeight = FontWeight.Bold)
            Text("›", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onNextMonth))
        }
        if (loading) Text("Consultando disponibilidad…") else MonthGrid(month, checkIn, checkOut, occupied, onDaySelected)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    checkIn: LocalDate?,
    checkOut: LocalDate?,
    occupied: List<ClosedRange<LocalDate>>,
    onDaySelected: (LocalDate) -> Unit
) {
    val firstOffset = month.atDay(1).dayOfWeek.value % 7 // domingo primero
    val cells = List(firstOffset) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val labels = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth()) { labels.forEach { Text(it, Modifier.weight(1f), fontSize = 11.sp, color = Color.Gray) } }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).height(42.dp))
                    else {
                        val unavailable = date < LocalDate.now() || isOccupied(date, occupied)
                        val selected = checkIn != null && checkOut != null && !date.isBefore(checkIn) && date.isBefore(checkOut)
                        val color = when {
                            unavailable -> OccupiedRed
                            selected -> SelectedGreen
                            else -> NeutralDay
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp)
                                .background(color, RoundedCornerShape(8.dp))
                                .clickable(enabled = !unavailable) { onDaySelected(date) },
                            contentAlignment = Alignment.Center
                        ) { Text(date.dayOfMonth.toString(), color = if (unavailable || selected) Color.White else Color.DarkGray) }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(42.dp)) }
            }
        }
    }
}

@Composable
private fun PaymentStep(
    paymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiry: String,
    onExpiryChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Método de pago", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Débito", "Crédito").forEach { method ->
                Button(onClick = { onPaymentMethodChange(method) }, colors = ButtonDefaults.buttonColors(containerColor = if (paymentMethod == method) HotelOrange else Color.LightGray)) { Text(method) }
            }
        }
        Field("Número de tarjeta", cardNumber, onCardNumberChange, password = true)
        Field("MM/AA", expiry, onExpiryChange)
        Field("CVV / CVC", cvv, onCvvChange, password = true)
        Text("Demostración: la app nunca guarda CVV ni el número completo de tarjeta.", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit, password: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun isOccupied(day: LocalDate, ranges: List<ClosedRange<LocalDate>>) = ranges.any { day in it }

private fun hasOccupiedDate(start: LocalDate, end: LocalDate, ranges: List<ClosedRange<LocalDate>>): Boolean {
    var day = start
    while (day <= end) {
        if (isOccupied(day, ranges)) return true
        day = day.plusDays(1)
    }
    return false
}
