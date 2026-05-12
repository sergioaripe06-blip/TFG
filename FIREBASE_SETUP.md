# Firebase Setup

Este documento es la referencia viva de Firebase para `FlatShare`.
Actualízalo cuando cambie la app, la estructura de datos o las reglas.

## Estado actual

La app usa:

- `Firebase Authentication` con `Email/Password`
- `Cloud Firestore`
- Colecciones principales:
  - `users`
  - `usernames`
  - `groups`
  - `group_codes`
  - `rooms_groups`
  - `expenses`
  - `payments`
  - `payment_deadlines`
  - `invitations`
  - `reminders`
  - `activity_logs`

## Qué hace cada colección

### `groups/{groupId}`

Piso o grupo compartido.

Campos principales:

- `name`
- `description`
- `location` (`street`, `portal`, `postalCode`, `city`, `province`)
- `ownerId`
- `roles`
- `members`
- `memberEmails`
- `shareCode`
- `roomCount`
- `billingModel` (`fixed`, `variable`)
- `variableSplitMode` (`equal`, `percentage`)
- `createdAt`

### `rooms_groups/{roomId}`

Habitaciones de un piso y residentes asignados.

Campos:

- `groupId`
- `roomNumber`
- `name`
- `capacity`
- `monthlyCost`
- `memberEmails`
- `memberCount`
- `createdByUid`
- `updatedByUid`
- `createdAt`
- `updatedAt`

### `expenses/{expenseId}`

Gastos del piso.

Campos principales:

- `groupId`
- `concept`
- `amount`
- `payerId`
- `payerEmail`
- `customSplit`
- `category`
- `priority`
- `ticketUri`
- `dueAt`
- `dueDateText`
- `roomId`
- `roomName`
- `createdAt`

### `payments/{paymentId}`

Pagos entre miembros.

Campos principales:

- `groupId`
- `amount`
- `fromEmail`
- `toEmail`
- `concept`
- `targetType` (`habitacion`, `miembro`, `todos`)
- `roomId` (opcional)
- `roomName` (opcional)
- `category`
- `priority`
- `status`
- `dueAt`
- `dueDateText`
- `createdAt`

### `reminders/{reminderId}`

Recordatorios del piso (visibles en pestaña de Recordatorios y calendario).

Campos principales:

- `groupId`
- `groupName`
- `ownerUid`
- `ownerEmail`
- `title`
- `startAt`
- `startDateText`
- `interval` (`diario`, `semanal`, `mensual`, `personalizado`)
- `intervalDays` (entero, para personalizado)
- `targetType` (`todos`, `miembro`, `habitacion`, `x_habitacion`)
- `targetEmails` (array)
- `targetMemberEmail` (opcional)
- `roomId` (opcional)
- `roomName` (opcional)
- `roomIds` (array opcional)
- `roomNames` (array opcional)
- `attachmentUri` (opcional)
- `reminderCode` (int, para alarmas locales)
- `createdAt`

## Reglas recomendadas

El archivo fuente de reglas es [firestore.rules](./firestore.rules).

Resumen:

- `groups`: solo miembros leen, propietario gestiona.
- `rooms_groups`: miembros leen, propietario crea/edita/elimina.
- `expenses`, `payments`, `payment_deadlines`, `reminders`, `activity_logs`: solo miembros del grupo.
- `invitations`: acceso para quien invita o quien recibe.

## Cómo aplicarlo en Firebase

1. Abre `Firebase Console`.
2. Entra en tu proyecto.
3. Ve a `Firestore Database`.
4. Abre la pestaña `Rules`.
5. Copia el contenido de `firestore.rules`.
6. Pulsa `Publish`.

## Qué activar

### Authentication

En `Authentication > Sign-in method` activa:

- `Email/Password`

### Firestore

En `Firestore Database`:

- Crea la base de datos en `Native mode`.
- Aplica las reglas de `firestore.rules`.

## No hace falta crear colecciones a mano

La app crea los documentos automáticamente al crear piso, habitaciones, gastos, pagos y recordatorios.
