# Firebase Setup

Este documento es la referencia viva de Firebase para `FlatShare`. La idea es ir actualizandolo cuando cambie la app, la estructura de datos o las reglas.

## Estado actual

La app usa:

- `Firebase Authentication` con `Email/Password`
- `Cloud Firestore`
- Colecciones principales:
  - `users`
  - `usernames`
  - `groups`
  - `group_codes`
  - `expenses`
  - `payments`
  - `invitations`
  - `reminders`

## Que hace cada coleccion

### `users/{uid}`

Documento privado del usuario autenticado.

Campos actuales:

- `uid`
- `email`
- `displayName`
- `fullName`
- `username`
- `phone`
- `city`
- `profileCompleted`

### `usernames/{username}`

Indice publico para poder iniciar sesion usando nombre de usuario en vez de email.

Campos actuales:

- `uid`
- `email`
- `displayName`

### `groups/{groupId}`

Piso o grupo compartido.

Campos actuales:

- `name`
- `description`
- `ownerId`
- `members`
- `memberEmails`
- `shareCode`
- `createdAt`

### `group_codes/{shareCode}`

Indice para entrar a un piso mediante codigo.

Campos actuales:

- `groupId`
- `ownerId`
- `name`

### `expenses/{expenseId}`

Gastos del piso.

Campos actuales:

- `groupId`
- `concept`
- `amount`
- `payerId`
- `payerEmail`
- `customSplit`
- `createdAt`

### `payments/{paymentId}`

Pagos entre miembros.

Campos actuales:

- `groupId`
- `amount`
- `fromEmail`
- `toEmail`
- `createdAt`

### `invitations/{invitationId}`

Invitaciones por email.

Campos actuales:

- `groupId`
- `invitedEmail`
- `inviterUid`
- `status`
- `createdAt`

### `reminders/{reminderId}`

Recordatorios internos del usuario dentro de un grupo.

Campos actuales:

- `groupId`
- `title`
- `interval`
- `ownerUid`
- `createdAt`

## Reglas recomendadas

El archivo fuente de reglas del proyecto es [firestore.rules](c:/Users/sarino/Documents/TFG/FlatShareApp/firestore.rules).

Resumen de la estrategia:

- `users`: solo el propio usuario puede leer y escribir su documento
- `usernames`: lectura publica para permitir login con usuario
- `group_codes`: lectura para usuarios autenticados, escritura solo del propietario
- `groups`: solo miembros pueden leer; solo el dueno edita, salvo el caso controlado de auto-union por codigo
- `expenses`, `payments`, `reminders`: solo miembros del grupo
- `invitations`: acceso para quien invita o quien recibe

## Como meterlo en Firebase

1. Abre `Firebase Console`
2. Entra en tu proyecto
3. Ve a `Firestore Database`
4. Abre la pestana `Rules`
5. Copia el contenido de `firestore.rules`
6. Pulsa `Publish`

## Que debes activar en Firebase

### Authentication

En `Authentication > Sign-in method` activa:

- `Email/Password`

### Firestore

En `Firestore Database`:

- crea la base de datos en `Native mode`
- aplica las reglas del archivo `firestore.rules`

## No hace falta crear campos a mano

La app crea los documentos necesarios automaticamente al:

- registrarse un usuario
- crear un piso
- unirse a un piso por codigo
- crear gastos
- registrar pagos
- crear recordatorios

## Mantenimiento

Actualiza este archivo cuando cambie cualquiera de estas cosas:

- nuevas colecciones
- nuevos campos
- cambios en login o registro
- cambios en invitaciones o codigos de piso
- cambios en reglas de seguridad
