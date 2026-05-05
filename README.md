# FlatShare (Android Studio + Java)

Proyecto base funcional para TFG, en Android nativo con Java:
- Registro e inicio de sesión (Firebase Auth)
- Gestión de pisos/grupos
- Invitaciones por email a grupos (aceptar/rechazar)
- Registro de gastos con reparto equitativo o personalizado por porcentaje
- Registro de pagos entre miembros por email
- Balance automático teniendo en cuenta gastos y pagos
- Recordatorios periódicos locales (diario/semanal/mensual)
- Historial simple en pantalla de gastos

## Cómo abrirlo
1. Abre Android Studio.
2. `File > Open` y selecciona esta carpeta: `FlatShareApp`.
3. Espera al `Gradle Sync`.
4. Crea un proyecto en Firebase y añade Android app con package `com.sergio.flatshare`.
5. Descarga `google-services.json` y colócalo en `app/google-services.json`.
6. Ejecuta en emulador/dispositivo.

## Notas
- La estructura está lista para ampliar invitaciones por email, reparto avanzado por porcentajes y recordatorios periódicos.
- Si Android Studio pide actualización de Gradle/AGP, acepta la migración automática.

