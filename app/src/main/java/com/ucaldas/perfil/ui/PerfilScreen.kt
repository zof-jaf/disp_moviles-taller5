package com.ucaldas.perfil.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PerfilScreen() {
    // --- Estado que sobrevive a la rotación (Parte 3) ---
    // rememberSaveable guarda el valor en el Bundle de la Activity, por lo que
    // se conserva al rotar (onDestroy/onCreate). remember lo perdería.
    var publicaciones by rememberSaveable { mutableStateOf(0) }
    var nombre by rememberSaveable { mutableStateOf("Ana Gómez") }
    var editando by rememberSaveable { mutableStateOf(false) }
    var borrador by rememberSaveable { mutableStateOf("") }

    // --- Estado de la lista (Parte 2) ---
    // mutableStateListOf es observable por Compose (SnapshotStateList). Al agregar
    // un elemento se dispara la recomposición y la pantalla se actualiza.
    // NOTA: mutableStateListOf NO funciona directamente con rememberSaveable
    // (no hay Saver por defecto para listas), así que se deja con remember.
    val logros = remember { mutableStateListOf("Primer commit") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Parte 1.4 — Avatar circular con iniciales (Box superpone texto sobre círculo)
        Avatar(iniciales = inicialesDe(nombre))

        // Nombre y carrera (Parte 1.5)
        Text(
            text = nombre.ifBlank { "Usuario sin nombre" },
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Ingeniería de Sistemas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Parte 1.6 + 3.1 — Tarjeta de publicaciones con contador interactivo
        TarjetaPublicaciones(
            contador = publicaciones,
            onIncrement = { publicaciones++ },
            onDecrement = { if (publicaciones > 0) publicaciones-- }
        )

        // Parte 2 — Sección de logros (ahora se actualiza al agregar)
        SeccionLogros(logros = logros)

        // Parte 3.5 — Formulario de edición de perfil
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(
            onClick = {
                borrador = nombre
                editando = !editando
            }
        ) {
            Text(if (editando) "Cancelar" else "Editar perfil")
        }

        // Parte 4.A — Animación de aparición del formulario
        AnimatedVisibility(visible = editando) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = borrador,
                    onValueChange = { borrador = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        nombre = borrador.ifBlank { "Usuario sin nombre" }
                        editando = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar") }
            }
        }
    }
}

/**
 * Genera las iniciales de un nombre (máximo 2, mayúsculas).
 * Parte 4.C — Avatar dinámico.
 */
fun inicialesDe(nombre: String): String =
    nombre.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

@Composable
private fun Avatar(iniciales: String) {
    Box(
        modifier = Modifier
            // Orden obligatorio: size → clip → background.
            // Si background va antes que clip, el fondo queda cuadrado y no se recorta.
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun TarjetaPublicaciones(
    contador: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Publicaciones", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$contador",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onDecrement,
                    enabled = contador > 0
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrementar"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onIncrement) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Incrementar"
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionLogros(logros: MutableList<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Logros", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        logros.forEach { logro ->
            Text(text = "• $logro", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Parte 2.4 — mutableStateListOf notifica a Compose al agregar.
        // El Log permite comprobar en Logcat que el elemento sí se agrega.
        androidx.compose.material3.Button(
            onClick = {
                val nuevo = "Nuevo logro ${logros.size + 1}"
                logros.add(nuevo)
                Log.d("Taller4", "Agregado: $nuevo | Tamaño: ${logros.size}")
            }
        ) {
            Text("Agregar logro")
        }
    }
}

@Preview(showBackground = true, name = "Perfil")
@Composable
fun PerfilScreenPreview() {
    MaterialTheme {
        PerfilScreen()
    }
}
