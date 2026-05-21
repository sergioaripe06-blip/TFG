# Firebase Setup

Este documento es la referencia viva de Firebase para `FlatShare`.
Actualízalo cuando cambie la app, la estructura de datos o las reglas.

## Estado actual

La app usa:

- `Firebase Authentication` con `Email/Password`
- `Cloud Firestore`

Colecciones principales:

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

Nuevas colecciones de gestión de alquiler:

- `rental_contracts`
- `rent_collections`
- `maintenance_tickets`
- `group_documents`
- `audit_events`
- `rent_automations`
- `event_reminder_rules`
- `event_reminder_jobs`

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

Campos principales:

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
- `seed` (opcional, booleano): marca pagos de demo generados por seed.

### `reminders/{reminderId}`

Recordatorios manuales del piso (visibles en pestaña de Recordatorios y calendario).

Campos principales:

- `groupId`
- `groupName`
- `ownerUid`
- `ownerEmail`
- `title`
- `startAt`
- `startDateText`
- `endAt` (opcional)
- `endDateText` (opcional)
- `interval` (`unico`, `diario`, `semanal`, `mensual`, `personalizado`)
- `intervalDays` (entero para personalizado)
- `targetType` (`x_miembro`, `x_habitacion`, `todos_inquilinos`)
- `targetEmails` (array)
- `targetMemberEmail` (opcional)
- `roomId` (opcional)
- `roomName` (opcional)
- `roomIds` (array opcional)
- `roomNames` (array opcional)
- `reminderCode` (entero para alarmas locales)
- `createdAt`

### `rental_contracts/{contractId}`

Contrato de alquiler del piso.

Campos principales:

- `groupId`
- `startDate` (`YYYY-MM-DD`)
- `endDate` (`YYYY-MM-DD`)
- `depositAmount`
- `extensionMonths`
- `clauses`
- `ownerSignerEmail` (obligatorio)
- `tenantSignerEmail` (obligatorio)
- `coSignerEmail` (opcional)
- `guarantorEmail` (opcional)
- `signers` (array de emails derivado para compatibilidad)
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `rent_collections/{rentId}`

Cobros mensuales de renta.

Campos principales:

- `groupId`
- `monthKey` (`YYYY-MM`)
- `roomName`
- `tenantEmail`
- `amountBase`
- `amountPaid`
- `surcharge`
- `status` (`pendiente`, `parcial`, `pagado`, `atrasado`)
- `dueDateText`
- `dueAt`
- `uniqueKey` (evita duplicados)
- `generatedByRuleId` (opcional)
- `prorated` (boolean opcional)
- `occupiedDays` (opcional)
- `daysInMonth` (opcional)
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `maintenance_tickets/{ticketId}`

Incidencias y mantenimiento.

Campos principales:

- `groupId`
- `title`
- `description`
- `roomName`
- `responsibleEmail`
- `status` (`abierta`, `en_progreso`, `resuelta`, `cancelada`)
- `estimatedCost`
- `finalCost`
- `history` (array de eventos)
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `group_documents/{docId}`

Documentación del piso: contrato, facturas, inventario, fotos y actas.

Campos principales:

- `groupId`
- `type` (`contrato`, `factura`, `inventario`, `foto`, `acta`)
- `title`
- `documentDate`
- `referenceUri`
- `notes`
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `audit_events/{eventId}`

Auditoría de movimientos.

Campos principales:

- `groupId`
- `module`
- `action`
- `details`
- `entityId`
- `actorUid`
- `actorEmail`
- `createdAt`

### `rent_automations/{ruleId}`

Reglas de rentas recurrentes y prorrateo.

Campos principales:

- `groupId`
- `roomName`
- `tenantEmail`
- `monthlyRent`
- `billingDay` (1-28)
- `startDate`
- `endDate` (opcional)
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `event_reminder_rules/{ruleId}`

Reglas de recordatorios accionables por evento.

Campos principales:

- `groupId`
- `eventType` (`rent_due`, `rent_overdue`, `contract_ending`, `incident_pending`)
- `offsetDays` (puede ser negativo)
- `titleTemplate`
- `bodyTemplate`
- `enabled`
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `event_reminder_jobs/{jobId}`

Instancias programadas de reglas push.

Campos principales:

- `groupId`
- `ruleId`
- `eventType`
- `targetEmail`
- `title`
- `body`
- `triggerAt`
- `reminderCode`
- `createdAt`

### `invitations/{invitationId}`

Invitaciones a piso por codigo interno y por correo.

Campos principales:

- `groupId`
- `groupName`
- `shareCode`
- `invitedEmail`
- `inviterUid`
- `inviterEmail`
- `status` (`pending`, `accepted`, `rejected`)
- `createdAt`

## Reglas recomendadas

El archivo fuente de reglas es [firestore.rules](./firestore.rules).

Resumen:

- `groups`: solo miembros leen, propietario gestiona.
- `rooms_groups`: miembros leen, propietario crea/edita/elimina.
- `expenses`, `payments`, `payment_deadlines`, `reminders`, `activity_logs`: solo miembros del grupo.
- `payments`: para seed demo, el propietario puede crear pagos con `seed=true` y `fromEmail` tipo `seeduserNNN@seed.flatshare.local` dirigidos a su propio correo.
- `rental_contracts`: lectura de miembros, gestión del propietario.
- `rent_collections`: miembros del grupo pueden crear/leer; solo el propietario puede actualizar o eliminar.
- `maintenance_tickets`: miembros del grupo pueden crear/leer; solo el propietario puede actualizar o eliminar.
- `group_documents`: miembros del grupo pueden crear/leer; solo el propietario puede actualizar o eliminar.
- `audit_events`: miembros leen y crean; no se permite editar ni borrar.
- `rent_automations` y `event_reminder_rules`: lectura de miembros, gestión del propietario.
- `event_reminder_jobs`: miembros leen y crean; no se permite editar ni borrar.
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

La app crea los documentos automáticamente al crear piso, habitaciones, gastos, pagos, contratos, cobros, incidencias, documentos, automatizaciones y reglas de recordatorio.

## Seed de cuentas reales (Auth + Firestore)

Para pruebas multiusuario reales puedes usar el seeder admin:

- Ruta: `tools/firebase-admin-seed`
- Script: `seed.js`

Qué crea:

- Usuarios reales en `Firebase Authentication` (email/password)
- Documentos en `users`
- Documentos en `usernames`
- Pisos en `groups` con tu cuenta como `ownerId`
- Códigos en `group_codes`
- Habitaciones en `rooms_groups`

Uso rápido:

1. Coloca una service account JSON en local (no la subas al repo).
2. Ejecuta:
   - `cd tools/firebase-admin-seed`
   - `npm install`
   - `node seed.js --service-account ./service-account.json --owner-email TU_EMAIL --owner-password TU_PASSWORD --users 12 --groups 4 --rooms 3 --members-per-group 4`
3. Revisa el archivo de salida de credenciales en:
   - `tools/firebase-admin-seed/output/seed-users-YYYYMMDD-HHMMSS.json`

## Politica de pagos (owner-centric)
- Solo el propietario (ownerId) puede cambiar estado de un pago entre pending y confirmed.
- Inquilinos: solo lectura para estados de pago.
- Solo el propietario puede eliminar un pago y solo si dueAt aun no ha vencido.
- Para mostrar nombres reales en UI, se lee users.name (con fallback si no existe).
- En registro, el usuario debe aceptar términos y se guardan `users.termsAccepted=true` y `users.termsAcceptedAt`.


