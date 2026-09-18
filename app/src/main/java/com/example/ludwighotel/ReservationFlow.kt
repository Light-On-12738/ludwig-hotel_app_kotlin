package com.example.ludwighotel

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import coil.compose.AsyncImage
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
private val FieldBackground = Color(0xFFFAFAFA)
private val FieldBorder = Color(0xFFE6E6E6)
private val TextPrimary = Color(0xFF172033)
private val TextSecondary = Color(0xFF6B7280)

/** Datos mínimos que la pantalla necesita de una habitación. */
data class RoomForReservation(
    val id: String, // UUID de la tabla habitaciones en Supabase
    val name: String,
    val pricePerNight: Double,
    val description: String = "",
    val imageUrl: String? = null
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
    val context = LocalContext.current
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
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F4F4)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReservationTopBar(onBack = onBack)
            Spacer(Modifier.height(18.dp))
            ReservationHeader(room, step, nights, total)
            Spacer(Modifier.height(10.dp))
            Text("Por favor, complete los siguientes campos:", color = Color.Gray, fontSize = 13.sp)
        }

        item {
            when (step) {
                1 -> GuestStep(room, guest) { guest = it }
                2 -> CalendarStep(
                    month = month,
                    onPreviousMonth = {
                        if (month.isAfter(YearMonth.now())) month = month.minusMonths(1)
                    },
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
                    onExpiryChange = { expiry = formatearVencimiento(it) },
                    cvv = cvv,
                    onCvvChange = { cvv = it.filter(Char::isDigit).take(4) }
                )
            }
        }

        error?.let { message -> item { Text(message, color = OccupiedRed) } }

        item {
            val enabled = when (step) {
                1 -> duiReservaEsValido(guest.document) && nombreEsValido(guest.firstName) &&
                    nombreEsValido(guest.lastName) && (guest.guests.toIntOrNull() ?: 0) > 0
                2 -> checkIn != null && checkOut != null
                else -> cardNumber.length in 12..16 && vencimientoEsValido(expiry) && cvv.length in 3..4
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotelOrange,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFFFD194),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    if (step == 3) "Confirmar pago (3/3)"
                    else "Confirmar reservación (${step}/3)",
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
            }.onSuccess {
                // El audio se reproduce solo después de que Supabase confirma
                // que la reserva se insertó correctamente.
                MediaPlayer.create(context, R.raw.reserva_confirmada)?.apply {
                    setOnCompletionListener { player -> player.release() }
                    start()
                }
                onReservationCreated()
            }
                .onFailure { exception ->
                    submitting = false
                    // No se muestra exception.message: puede incluir cabeceras y el
                    // token de sesión. Se entrega un mensaje útil y seguro.
                    val safeError = exception.message
                        .orEmpty()
                        .substringBefore("URL:")
                        .substringBefore("Headers:")
                        .trim()
                        .take(350)

                    error = if (safeError.contains("usuario_dui_check")) {
                        "El DUI debe tener el formato 00000000-0."
                    } else if (safeError.isNotBlank()) {
                        safeError
                    } else {
                        "No se pudo crear la reserva. Verifica disponibilidad e inténtalo nuevamente."
                    }
                }
        }
    }
}

@Composable
private fun ReservationHeader(room: RoomForReservation, step: Int, nights: Int, total: Double) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(room.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("Habitación de hotel", color = TextSecondary, fontSize = 13.sp)
                    if (nights > 0) {
                        Text(
                            "$nights noche(s) · Total $${"%.2f".format(total)}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    "$${"%.2f".format(room.pricePerNight)}\nNoche",
                    color = HotelOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!room.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                RoomReservationImage(room)
            }
        }
    }
}

@Composable
private fun ReservationTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Icon(
            Icons.Default.ArrowBack,
            "Volver",
            Modifier.size(44.dp).background(Color.White, RoundedCornerShape(50.dp))
                .padding(10.dp).clickable(onClick = onBack)
        )
        Text("Ludwing Hotel", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = HotelOrange)
        Image(
            painter = painterResource(id = R.drawable.milogo),
            contentDescription = "Logo de Ludwing Hotel",
            modifier = Modifier.size(38.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("1. Datos", "2. Fechas", "3. Pago").forEachIndexed { index, label ->
            val selected = step == index + 1
            Text(
                label,
                color = if (selected) HotelOrange else Color.Gray,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun GuestStep(room: RoomForReservation, value: GuestData, onChange: (GuestData) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Completa los datos de la reserva", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text("La información se usará durante el check-in.", color = Color.Gray, fontSize = 13.sp)
            ReservationField(
                "DUI (00000000-0)",
                value.document,
                { onChange(value.copy(document = formatearDuiReserva(it))) },
                Icons.Default.Badge
            )
            ReservationField("Nombre", value.firstName, { onChange(value.copy(firstName = formatearNombre(it))) }, Icons.Default.Person)
            ReservationField("Apellido", value.lastName, { onChange(value.copy(lastName = formatearNombre(it))) }, Icons.Default.Person)
            ReservationField("Número de huéspedes", value.guests, { onChange(value.copy(guests = it.filter(Char::isDigit))) }, Icons.Default.Group)
        }
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
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Elige los días a reservar", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Rojo: ocupado · Verde: tu estancia", fontSize = 12.sp, color = Color.Gray)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                fontSize = 28.sp,
                color = if (month.isAfter(YearMonth.now())) TextPrimary else Color.Transparent,
                modifier = Modifier.clickable(enabled = month.isAfter(YearMonth.now()), onClick = onPreviousMonth)
            )
            Text(month.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() } + " ${month.year}", fontWeight = FontWeight.Bold)
            Text("›", fontSize = 28.sp, modifier = Modifier.clickable(onClick = onNextMonth))
        }
        if (loading) Text("Consultando disponibilidad…") else MonthGrid(month, checkIn, checkOut, occupied, onDaySelected)
    }
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
                    if (date == null || date < LocalDate.now()) {
                        Spacer(Modifier.weight(1f).height(42.dp))
                    }
                    else {
                        val occupiedDay = isOccupied(date, occupied)
                        val unavailable = occupiedDay
                        val selected = checkIn != null && ((checkOut == null && date == checkIn) || (checkOut != null && !date.isBefore(checkIn) && date.isBefore(checkOut)))
                        val color = when {
                            occupiedDay -> OccupiedRed
                            selected -> SelectedGreen
                            else -> NeutralDay
                        }
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp)
                                .background(color, RoundedCornerShape(8.dp))
                                .clickable(enabled = !unavailable) { onDaySelected(date) },
                            contentAlignment = Alignment.Center
                        ) { Text(date.dayOfMonth.toString(), color = if (occupiedDay || selected) Color.White else Color.DarkGray) }
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
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Método de pago", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Débito", "Crédito").forEach { method ->
                Button(
                    onClick = { onPaymentMethodChange(method) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (paymentMethod == method) HotelOrange else Color(0xFFF5F5F5),
                        contentColor = if (paymentMethod == method) Color.White else TextSecondary
                    )
                ) { Text(method) }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = HotelOrange), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("LUDWIG HOTEL", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Text(if (cardNumber.isBlank()) "•••• •••• •••• ••••" else cardNumber.chunked(4).joinToString(" "), color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))
                Text("${paymentMethod.uppercase()} · ${if (expiry.isBlank()) "MM/AA" else expiry}", color = Color.White, fontSize = 12.sp)
            }
        }
        ReservationField("Número de tarjeta", cardNumber, onCardNumberChange, Icons.Default.CreditCard, password = true)
        ReservationField("Fecha de vencimiento (MM/AA)", expiry, onExpiryChange, Icons.Default.CalendarMonth)
        ReservationField("CVV / CVC", cvv, onCvvChange, Icons.Default.CreditCard, password = true)
        Text("Demostración: la app nunca guarda CVV ni el número completo de tarjeta.", fontSize = 12.sp, color = Color.Gray)
    }
    }
}

/** Muestra una única imagen de la habitación en el primer paso. */
@Composable
private fun RoomReservationImage(room: RoomForReservation) {
    if (!room.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = room.imageUrl,
            contentDescription = "Imagen de ${room.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

@Composable
private fun reservationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = FieldBackground,
    unfocusedContainerColor = FieldBackground,
    focusedBorderColor = HotelOrange,
    unfocusedBorderColor = FieldBorder,
    focusedLabelColor = HotelOrange,
    unfocusedLabelColor = TextSecondary,
    cursorColor = HotelOrange,
    focusedLeadingIconColor = HotelOrange,
    unfocusedLeadingIconColor = TextSecondary
)

@Composable
private fun ReservationField(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector, password: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { androidx.compose.material3.Icon(icon, null, tint = Color.Gray) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = reservationFieldColors(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit, password: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = reservationFieldColors(),
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

/** Convierte hasta 9 dígitos al formato requerido por usuario.dui: 00000000-0. */
private fun formatearDuiReserva(input: String): String {
    val digits = input.filter(Char::isDigit).take(9)
    return if (digits.length <= 8) digits else "${digits.take(8)}-${digits.last()}"
}

private fun duiReservaEsValido(value: String): Boolean =
    Regex("^\\d{8}-\\d$").matches(value)

/** Convierte números a MM/AA y rechaza meses inexistentes. */
private fun formatearVencimiento(input: String): String {
    val digits = input.filter(Char::isDigit).take(4)
    return if (digits.length <= 2) digits else "${digits.take(2)}/${digits.drop(2)}"
}

private fun vencimientoEsValido(value: String): Boolean {
    if (!Regex("^\\d{2}/\\d{2}$").matches(value)) return false
    return value.take(2).toIntOrNull() in 1..12
}
