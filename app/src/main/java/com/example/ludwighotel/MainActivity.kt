package com.example.ludwighotel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            LudwigHotelApp()
        }
    }
}

@Composable
fun LudwigHotelApp() {
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
            color = Color(0xFFF4F4F4)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Header() }
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
    }
}

@Composable
fun Header() {
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
