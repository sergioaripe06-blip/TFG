# Arquitectura del proyecto FlatShareApp

Este proyecto está modularizado por **capas** y por **dominios funcionales** para facilitar mantenimiento, escalabilidad y defensa técnica ante tribunal.

## Estructura principal (Java)

- `app/src/main/java/com/sergio/flatshare/features/`
- `app/src/main/java/com/sergio/flatshare/core/`
- `app/src/main/java/com/sergio/flatshare/shared/`

## Features (casos de uso)

- `features/auth/`: acceso y registro.
  - `LoginActivity`, `RegisterActivity`
- `features/shell/`: arranque y navegación principal.
  - `SplashActivity`, `MainActivity`
- `features/groups/`: gestión de pisos/grupos y habitaciones del propietario.
  - `GroupsFragment`, `OwnerRoomsActivity`
  - `features/groups/services/`: servicios de grupos e invitaciones (`GroupService`, `InvitationService`, `InitialRoomsSetupFlow`).
- `features/workspace/`: operación del piso activo (gastos, saldos, recordatorios, gestión alquiler).
  - `ExpensesFragment`, `BalancesFragment`, `PersonalBalanceFragment`, `CalendarFragment`, `TenantsFragment`, `RentalManagementFragment`
  - `features/workspace/services/`: servicios de dominio para gastos/pagos/recordatorios y utilidades de diálogo (`CategorySuggestionsRepository`, `PaymentService`, `ExpenseService`, `ReminderService`, `ExpenseDialogs`, `ContractsService`, `CollectionsService`).
- `features/profile/`: perfil de usuario.
  - `ProfileFragment`
- `features/settings/`: ajustes funcionales de la app.
  - `SettingsFragment`
- `features/reminders/`: receptor de notificaciones.
  - `ReminderReceiver`

## Core (servicios base de app)

- `core/session/`: estado de sesión/contexto actual (`SessionStore`).
- `core/settings/`: preferencias globales (`SettingsStore`).
- `core/notifications/`: planificación de recordatorios (`ReminderScheduler`).
- `core/sync/`: sincronización de usuario con Firestore (`UserSync`).

## Shared (reutilizable transversal)

- `shared/ui/`: utilidades de UI compartidas (`DialogUtils`).
- `shared/widgets/`: componentes gráficos reutilizables (`PieChartView`).

## Recursos Android

Por limitación propia de Android, los XML de UI deben permanecer en carpetas estándar como `res/layout`, `res/drawable`, etc. Aun así, la lógica Java ya está separada por dominios y capas para que el flujo técnico sea claro.

## Convención de navegación rápida para defensa

- Si te preguntan por autenticación: ir a `features/auth`.
- Si te preguntan por flujos de piso: ir a `features/groups` y `features/workspace`.
- Si te preguntan por configuración global: ir a `core/settings`.
- Si te preguntan por recordatorios: `core/notifications` + `features/reminders`.
- Si te preguntan por piezas reutilizables de interfaz: `shared/ui` y `shared/widgets`.
