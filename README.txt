LUDWIG HOTEL - PROYECTO ANDROID

Abrir con Android Studio.

IMPORTANTE:
Este proyecto usa Gradle 8.9 mediante Gradle Wrapper.
No selecciones Gradle 9/10 manualmente.

Pasos:
1. Descomprimir el ZIP.
2. Android Studio -> Open.
3. Seleccionar la carpeta LudwigHotel_Corregido.
4. Esperar el Gradle Sync.
5. Seleccionar un dispositivo/emulador.
6. Presionar Run (▶).

La aplicación utiliza Kotlin + Jetpack Compose.

BASE DE DATOS

La conexión a Supabase está lista en:
app/src/main/java/com/example/ludwighotel/SupabaseClient.kt

El proyecto incluye Postgres, Auth, Storage, Realtime, Ktor y serialización.
Usar el cliente desde Kotlin así:

val supabase = SupabaseClientProvider.client

Para los modelos de base de datos, usar @Serializable.
Antes de consultar, habilitar Data API para las tablas y crear políticas RLS
en el panel de Supabase. Nunca usar una clave service_role en Android.

