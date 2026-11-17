# Análisis — Taller 4

Pantalla de perfil de usuario en Jetpack Compose  
Programación para Dispositivos Móviles — Universidad de Caldas  
Clase 05 

---

## 1. Predicción inicial

*Estas respuestas se redactaron antes de escribir una sola línea de código y no se modificaron después.*

### 1.1 `remember { mutableStateOf(0) }` y rotación

**Predicción:** El valor se **pierde** al rotar la pantalla.  
**Por qué:** `remember` solo conserva el valor mientras el Composition esté vivo en la misma instancia del Composable. Al rotar, la Activity se destruye (`onDestroy`) y se recrea (`onCreate`), por lo que el Composition se vuelve a crear desde cero y `remember` devuelve el valor inicial (`0`), no el valor previo.

### 1.2 `remember { mutableListOf(...) }` y `.add()`

**Predicción:** La pantalla **no se actualiza** al agregar un elemento con `.add()`.  
**Por qué:** `mutableListOf()` devuelve una lista de Kotlin común (`MutableList`), que **no es observable por Compose**. Compose no se suscribe a los cambios de esa lista, así que aunque el elemento se agregue en memoria, no se dispara una recomposición. `remember` solo evita que la lista se reconstruya en cada recomposición, pero no la hace "observable".

### 1.3 Diferencia entre `remember` y `rememberSaveable`

**Predicción:** `remember` guarda el estado en la memoria del Composition (sobrevive a recomposiciones, no a la recreación de la Activity). `rememberSaveable` guarda además el valor en el `Bundle` del estado de la instancia (`savedInstanceState`), por lo que sobrevive a cambios de configuración como la rotación de pantalla. `rememberSaveable` requiere que el valor sea de un tipo que se pueda guardar en un `Bundle` (tipos primitivos, String, Parcelable, o con un `Saver` personalizado).

---

## 2. Verificación de la predicción

Se comparó lo predicho con lo que realmente ocurrió al ejecutar la app.

| Pregunta | Predicción | Lo que realmente ocurrió | ¿Se confirmó? |
|---|---|---|---|
| `remember` + rotación | Se pierde (vuelve a 0) | El contador con `remember` volvió a 0 tras rotar. | **Sí** |
| `mutableListOf` + `.add()` | No se actualiza la pantalla | Al presionar "Agregar logro", Logcat mostraba que el tamaño crecía (1, 2, 3…) pero la lista en pantalla no cambiaba. | **Sí** |
| `remember` vs `rememberSaveable` | `rememberSaveable` sobrevive a la rotación | Con `rememberSaveable` el contador se mantuvo en 5 tras rotar; con `remember` se perdió. | **Sí** |

Todas las predicciones se confirmaron. No hubo sorpresas: el comportamiento observado coincidió exactamente con lo esperado, lo que valida que el razonamiento inicial sobre recomposición y `Bundle` era correcto.

---

## 3. Diagnóstico del bug (Parte 2)

### Síntoma
Al presionar el botón "Agregar logro" de la sección de logros, ningún elemento nuevo aparece en pantalla, aunque el botón sí responde al clic.

### ¿Por qué ocurre?
En la versión con el bug la lista se crea con:

```kotlin
val logros = remember { mutableListOf("Primer commit") }
```

- `mutableListOf()` devuelve una `MutableList<String>` de Kotlin (una `ArrayList` común).
- Ese tipo **no implementa ninguna interfaz de estado de Compose** (no es un `State`, no es un `SnapshotStateList`), por lo que Compose **no lo observa**.
- `remember` solo garantiza que la misma instancia de la lista se reutilice entre recomposiciones; no agrega capacidad de notificación.
- Cuando se hace `logros.add(...)`, el elemento se agrega a la lista en memoria, pero como Compose no está "escuchando" esa lista, **no se programa una recomposición** y la pantalla no se vuelve a dibujar.

### Evidencia
Se agregó temporalmente un registro dentro del `onClick`:

```kotlin
OutlinedButton(onClick = {
    logros.add("Nuevo logro ${logros.size + 1}")
    Log.d("Taller4", "Tamaño de la lista: ${logros.size}")
}) { Text("Agregar logro") }
```

En Logcat se observó que el tamaño de la lista sí aumentaba en cada clic (1, 2, 3 …), pero la pantalla seguía mostrando solo "Primer commit". Esto demuestra que el problema **no es la lista** (el elemento sí se agrega), sino que **Compose no se enteró del cambio**.

### Corrección aplicada
Se usó la **Opción A (`mutableStateListOf`)**:

```kotlin
val logros = remember { mutableStateListOf("Primer commit") }
```

### ¿Por qué funciona?
`mutableStateListOf()` devuelve una `SnapshotStateList`, que es una lista **observable por Compose** integrada al sistema de snapshot de Compose. Al hacer `.add()`, la lista notifica a Compose que su estado cambió, lo que dispara una recomposición de la sección afectada y el nuevo logro aparece inmediatamente en pantalla. (La Opción B — `var logros by remember { mutableStateOf(listOf(...)) }` reasignando una lista inmutable — funciona por el mismo principio: la reasignación de una variable de `mutableStateOf` sí notifica a Compose.)

**Limitación conocida:** `mutableStateListOf` no se puede usar directamente con `rememberSaveable` porque no hay un `Saver` por defecto para listas en el `Bundle`. Por eso la lista de logros se dejó con `remember` (no sobrevive a la rotación), mientras que el contador, el nombre y el formulario sí usan `rememberSaveable`.

---

## 4. Experimento de rotación (Parte 3)

### Con `remember`
Se presionó "+" hasta llevar el contador de publicaciones a 5 y se rotó el emulador. El contador volvió a **0**.

**Por qué:** `remember { mutableStateOf(0) }` almacena el valor en el Composition. Al rotar, la Activity pasa por `onDestroy` y se crea de nuevo en `onCreate`; el Composition anterior se descarta y `remember` devuelve el valor inicial (`0`).

### Con `rememberSaveable`
Se cambió a:

```kotlin
var publicaciones by rememberSaveable { mutableStateOf(0) }
```

Se repitió el experimento: contador en 5, rotar, y el valor se mantuvo en **5**.

**Por qué:** `rememberSaveable` guarda el valor en el `Bundle` del estado guardado de la Activity (`savedInstanceState`). Cuando la Activity se recrea tras la rotación, `rememberSaveable` recupera el valor del `Bundle` en lugar de usar el valor inicial.

### Explicación técnica (ciclo de vida de la Activity — Clase 04)
Al rotar la pantalla ocurre un cambio de configuración que, por defecto, destruye la Activity actual (`onPause` → `onStop` → `onDestroy`) y crea una nueva instancia (`onCreate` con el nuevo `savedInstanceState`). Todo el estado que viva solo en memoria del Composition o en variables de la Activity se pierde. `rememberSaveable` aprovecha el `Bundle` que Android pasa a `onCreate(savedInstanceState)` y a `onSaveInstanceState(outState)` para persistir el valor entre la destrucción y la recreación, de modo que el estado sobreviva a la rotación.

---

## 5. Justificación de los Modifier usados

Se eligieron tres `Modifier` del código y se explica su orden y el efecto de moverlos.

### 5.1 `Modifier.fillMaxSize()` (en `Surface` de `MainActivity`)
**Qué hace:** hace que la `Surface` ocupe todo el ancho y alto disponibles de la ventana, proporcionando el lienzo sobre el que se dibuja la pantalla.  
**Por qué en ese orden:** es el modificador de tamaño base; se coloca primero en la cadena.  
**Si se moviera/quitara:** sin `fillMaxSize`, la `Surface` solo ocuparía el espacio mínimo de su contenido y la pantalla podría quedar con sobrante o sin fondo completo.

### 5.2 Cadena del avatar: `.size(96.dp) → .clip(CircleShape) → .background(...)` (Parte 1.4)
**Qué hace cada uno:**
- `.size(96.dp)`: fija el ancho y alto del `Box` en 96 dp.
- `.clip(CircleShape)`: recorta el contenido (y el fondo posterior) a un círculo.
- `.background(primaryContainer)`: pinta el fondo del recorte con el color del tema.

**Por qué ese orden es obligatorio:** el fondo se aplica sobre la forma recortada. Si se pusiera `.background()` **antes** de `.clip()`, el fondo se pintaría sobre el cuadro completo de 96 dp y el recorte circular no se vería (quedaría un cuadrado de color con el texto encima, no un círculo). Esto se comprobó intencionalmente durante el taller.

### 5.3 `Modifier.fillMaxWidth()` (en la `Card` de publicaciones y en el `OutlinedTextField`)
**Qué hace:** hace que el elemento ocupe todo el ancho disponible de su padre.  
**Si se moviera/quitara:** en la `Card`, sin `fillMaxWidth` la tarjeta solo sería tan ancha como su contenido y no tendría el aspecto de "tarjeta de ancho completo". En el `OutlinedTextField`, sin él el campo de texto sería muy estrecho y no sería usable. En la cadena del `Card`, `fillMaxWidth` va antes de `.padding(20.dp)`: primero se define el ancho completo y luego el padding interno, para que el color/sombra de la tarjeta llegue hasta los bordes y el contenido quede con margen interior (si el padding fuera antes del fillMaxWidth, el comportamiento de ancho podría verse afectado).

---

## 6. Dificultades encontradas

1. **Configurar Compose desde cero en un proyecto que partía de una plantilla de Vistas.** Tuve que agregar el plugin de Compose, el BOM de Compose, las dependencias de `material3`, `activity-compose`, `runtime-saveable` y `animation`, y cambiar el tema de la aplicación. Se resolvió revisando la documentación de Compose y probando la compilación hasta que el proyecto ensambló correctamente.
2. **Entender por qué `mutableListOf` no notifica a Compose.** Al principio parecía que el botón no funcionaba; el uso de `Log.d` en Logcat fue clave para confirmar que el elemento sí se agregaba a la lista y que el problema era la falta de observabilidad, no la lista ni el clic.
3. **El orden de los `Modifier` en el avatar.** Probar poner `.background()` antes de `.clip()` permitió ver claramente por qué el orden importa (cuadrado vs. círculo).
4. **`rememberSaveable` no funciona con `mutableStateListOf` directamente.** Al intentar aplicarlo a la lista de logros, no compila/ no hay `Saver` por defecto. Se documentó como limitación conocida y se dejó la lista con `remember`, mientras que los valores simples (contador, nombre, borrador, editando) sí usan `rememberSaveable`.
5. **Validación de nombre vacío.** Se usó `nombre.ifBlank { "Usuario sin nombre" }` tanto al guardar como al mostrar el encabezado, para que nunca quede en blanco.

---

## Evidencia de entrega

- Código completo y compilando en el repositorio.
- `ANALISIS.md` (este archivo) con las 6 secciones requeridas.
- Carpeta `capturas/` con:
  - `01_layout_estatico.png` — Parte 1 (vista previa del layout)
  - `02_bug_antes.png` — Parte 2 (lista que no se actualiza)
  - `03_bug_despues.png` — Parte 2 (lista que sí se actualiza)
  - `04_rotacion_remember.png` — Parte 3 (contador en 0 tras rotar)
  - `05_rotacion_saveable.png` — Parte 3 (contador conservado tras rotar)
  - `06_nombre_vacio.png` — Parte 3 (validación "Usuario sin nombre")
  - `07_animacion.gif` (bonus) — formulario animado con `AnimatedVisibility`

*Nota: para capturas de rotación y comportamiento en tiempo real se usó el emulador (el `@Preview` solo sirve como referencia estática de la Parte 1, no reproduce rotación ni estado en ejecución).*
