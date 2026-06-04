# Firebase Setup

Este documento es la referencia viva de Firebase para `FlatShare`.
Actualízalo cuando cambie la app, la estructura de datos o las reglas.

## Estado actual

La app usa:

- `Firebase Authentication` con `Email/Password`
- `Cloud Firestore`

Colecciones principales:

- `users`
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
- `house_rules`
- `group_schedules`
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
- `rentSplitMode` (`equal`, `percentage`)
- `rentSplitPercentages` (map `email -> porcentaje`)
- `rentSplitOrder` (orden de edición para reparto porcentual)
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
- `status` (`requested`, `pending`, `confirmed`)
- `dueAt`
- `dueDateText`
- `roomId`
- `roomName`
- `createdAt`

### `payments/{paymentId}`

Pagos entre miembros.

En el flujo actual de la app, `expenses` es el origen principal de deuda y `payments` se usa sobre todo como confirmacion guiada cuando un deudor sube justificante para saldar un pendiente.

Campos principales:

- `groupId`
- `amount`
- `fromEmail`
- `toEmail`
- `concept`
- `targetType` (`habitacion`, `x_habitacion`, `miembro`, `todos`)
- `roomId` (opcional)
- `roomName` (opcional)
- `roomIds` (array opcional): habitaciones seleccionadas cuando aplica.
- `roomNames` (array opcional): nombres de habitaciones seleccionadas cuando aplica.
- `category`
- `priority`
- `status` (`requested`, `pending`, `confirmed`)
- `dueAt`
- `dueDateText`
- `createdAt`
- `seed` (opcional, booleano): marca pagos de demo generados por seed.
- `ticketUri` (opcional): URI del justificante de pago adjunto por el usuario.
- `sourceDebtId` (opcional): id de `payment_deadlines` cuando el pago nace desde un pendiente.
- `sourceDebtType` (opcional): valor `payment_deadline` para trazabilidad del flujo de pendientes.

### `payment_deadlines/{deadlineId}`

Vencimientos y deudas pendientes por miembro.

Campos principales:

- `groupId`
- `groupName`
- `sourceType` (`expense` o `payment`)
- `sourceId`
- `concept`
- `amount`
- `debtorEmail`
- `creditorEmail`
- `priority`
- `status` (`pending`, `submitted`, `confirmed`)
- `dueAt`
- `dueDateText` (opcional)
- `createdAt`

Semantica de estados:

- `pending`: deuda generada y aún no justificada por el deudor.
- `submitted`: el deudor ya ha subido justificante y queda a la espera de aceptación.
- `confirmed`: deuda cerrada y aceptada.
- `submittedAt` (opcional, cuando el inquilino envia justificante)
- `proofUri` (opcional, copia del justificante enviado en flujo pendiente)

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
- `roomId` (opcional)
- `sourceType` (por ejemplo `room_charge`)
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

### `house_rules/{ruleId}`

Normas internas del piso, aplicables a todo el grupo, a una persona concreta o a una habitación.

Campos principales:

- `groupId`
- `title`
- `description`
- `scopeType` (`everyone`, `member`, `room`)
- `targetMemberEmail`
- `targetEmails`
- `roomName`
- `createdAt`
- `createdByUid`
- `createdByEmail`
- `updatedAt`
- `updatedByUid`
- `updatedByEmail`

### `group_schedules/{scheduleId}`

Horarios recurrentes del piso visibles también en calendario.

Campos principales:

- `groupId`
- `title`
- `description`
- `scopeType` (`everyone`, `member`, `room`)
- `targetMemberEmail`
- `targetEmails`
- `roomName`
- `frequency` (`diario`, `semanal`, `mensual`)
- `startAt`
- `startDateText`
- `endAt`
- `endDateText`
- `startTimeText`
- `endTimeText`
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

- Acceso de aplicación restringido a usuarios autenticados con correo verificado (`request.auth.token.email_verified == true`).
- `groups`: solo miembros leen, propietario gestiona.
- `rooms_groups`: miembros leen. El propietario crea/edita/elimina (si está vacía). El inquilino solo puede hacer autoasignación inicial al unirse (sin flujo de cambio posterior); la reasignación/cambio queda para el propietario.
- `expenses`, `payments`, `payment_deadlines`, `activity_logs`: solo miembros del grupo.
- `reminders`: solo miembros leen; actualizar/eliminar solo creador (`ownerUid`) o propietario del piso.
- `payments`: para seed demo, el propietario puede crear pagos con `seed=true` y `fromEmail` tipo `seeduserNNN@seed.flatshare.local` dirigidos a su propio correo.
- `payments`: el propietario puede eliminar pagos del grupo para soportar borrado en cascada del piso.
- `rental_contracts`: lectura de miembros, gestión del propietario.
- `rent_collections`: miembros del grupo pueden crear/leer; solo el propietario puede actualizar o eliminar.
- La app puede generar cobros de `room_charge` en `rent_collections` para reflejar la parte mensual de cada residente de una habitacion.
- Cuando cambia el reparto o los residentes de una habitacion, la app resincroniza el mes actual y la siguiente mensualidad pendiente de esos `room_charge`.
- Para `room_charge`, la app guarda tambien `ownerEmail`, `startAt` y `startDateText` para mostrar la solicitud automatica del propietario con fecha de inicio y vencimiento.
- Los miembros del grupo pueden actualizar o limpiar solo los `rent_collections` de tipo `room_charge` si mantienen fijo `groupId`, `roomId`, `tenantEmail`, `monthKey` y `uniqueKey`, de forma que la resincronizacion automatica funcione aunque el primero en entrar no sea el propietario.
- `maintenance_tickets`: miembros del grupo pueden crear, leer, actualizar y eliminar.
- `house_rules`: lectura para miembros del grupo; gestión reservada al propietario.
- `group_schedules`: los miembros del grupo pueden leer, pero crear, actualizar y eliminar queda reservado al propietario.
- `group_documents`: miembros del grupo pueden crear/leer; solo el propietario puede actualizar o eliminar.
- `audit_events`: miembros leen y crean; no se permite editar. El propietario puede borrar en limpieza del piso.
- `rent_automations` y `event_reminder_rules`: lectura de miembros, gestión del propietario.
- `event_reminder_jobs`: miembros leen y crean; no se permite editar. El propietario puede borrar en limpieza del piso.
- `invitations`: acceso para quien invita o quien recibe.
- `groups`: se permite auto-salida segura del propio usuario (`isSelfLeaveUpdate`) para soportar borrado de cuenta.
- `groups`: el propietario puede actualizar membresía del piso (por ejemplo expulsar inquilinos), manteniendo `members`, `memberEmails` y `roles` alineados.
- `group_codes`: crear/actualizar/eliminar queda ligado al propietario actual del grupo (`groups.ownerId`) para evitar bloqueos tras transferir propiedad.

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
- Verificación de correo obligatoria en flujo de app:
  - Registro envía correo de verificación.
  - Login y Splash bloquean acceso si `emailVerified` es `false`.
  - Firestore exige email verificado mediante reglas.

### Firestore

En `Firestore Database`:

- Crea la base de datos en `Native mode`.
- Aplica las reglas de `firestore.rules`.

## No hace falta crear colecciones a mano

La app crea los documentos automáticamente al crear piso, habitaciones, gastos, pagos, contratos, cobros, incidencias, reglas del piso, horarios, documentos, automatizaciones y reglas de recordatorio.

## Seed de cuentas reales (Auth + Firestore)

Para pruebas multiusuario reales puedes usar el seeder admin:

- Ruta: `tools/firebase-admin-seed`
- Script: `seed.js`

Qué crea:

- Usuarios reales en `Firebase Authentication` (email/password)
- Documentos en `users`
- Pisos en `groups` con tu cuenta como `ownerId`
- Códigos en `group_codes`
- Habitaciones en `rooms_groups`

Uso rápido:

1. Coloca una service account JSON en local (no la subas al repo).
2. Ejecuta:
   - `cd tools/firebase-admin-seed`
   - `npm install`
   - `node seed.js --service-account ./service-account.json --owner-email TU_EMAIL --owner-password TU_PASSWORD --users 12 --groups 4 --rooms 3 --members-per-group 4`
   - O presets hardcodeados solicitados:
     - `npm run seed:sergio-demo` (owner fijo `sergioaripe06@gmail.com`)
     - `npm run seed:sergio-owner-homoerectus` (owner `sergioaripe06@gmail.com` con `homoerectus079@gmail.com` como inquilino real)
3. Revisa el archivo de salida de credenciales en:
   - `tools/firebase-admin-seed/output/seed-users-YYYYMMDD-HHMMSS.json`

## Politica de pagos
- Un pago pendiente (`pending`) se puede confirmar (`confirmed`) por el propietario del piso o por el acreedor del pago (`toEmail`).
- La edición y borrado de pagos se mantiene para propietario o creador del pago (`fromEmail`) solo mientras el pago siga en `requested`, respetando las invariantes de `groupId` y `fromEmail`.
- Cuando una deuda nace desde un gasto, el gasto sigue siendo el documento origen y se crean deudas individuales en `payment_deadlines` por cada deudor según `customSplit`.
- Si el propietario también aparece como deudor dentro de ese `customSplit`, en app se le trata como deudor para ese gasto concreto: debe enviar justificante y no se le ofrece edición/borrado desde el detalle del gasto.
- En ese escenario, el detalle del gasto muestra una acción específica para subir justificante mientras la deuda individual siga en `pending`.
- Si un gasto legado no tuviera aún su documento en `payment_deadlines`, la app puede reconstruir esa deuda individual al iniciar el flujo de justificante del deudor.
- El flujo de justificante bloquea el resto de campos del pago derivado: solo se adjunta la foto, sin permitir editar datos precargados.
- Cuando el deudor sube justificante, la deuda pasa a `submitted`; cuando se acepta el pago, la deuda pasa a `confirmed` y se recalcula el estado del gasto origen.
- En la vista de movimientos del propio deudor, si ya existe un `payment` ligado a `sourceType=expense` y `sourceId=<expenseId>`, se oculta la fila duplicada del gasto original.
- Las sugerencias de categoría para crear gastos se obtienen solo de `expenses` del mismo `groupId`, para no mezclar categorías entre pisos.
- La categoría se normaliza antes de guardarse (minúsculas y espacios estables), de modo que el balance reutiliza la misma categoría lógica cuando el nombre es el mismo.
- La UI de `Movimientos` recalcula el estado efectivo del gasto desde `payment_deadlines` para reflejar de forma consistente confirmaciones enviadas, aceptadas o aún pendientes en todas las cuentas implicadas.
- Para mostrar nombres reales en UI, se lee users.name (con fallback si no existe).
- En registro, el usuario debe aceptar términos y se guardan `users.termsAccepted=true` y `users.termsAcceptedAt`.





## Actualizacion funcional (29/05/2026)

### Variable rent: source of truth
- `groups.variableSplitMode` se mantiene como preferencia por defecto para nuevas habitaciones.
- El reparto efectivo en alquiler variable se resuelve por habitacion en `rooms_groups` con:
- `monthlyCost`
- `rentSplitMode` (`equal` o `percentage`)
- `rentSplitPercentages`

### Join flow and room assignment
- Un usuario que entra por codigo o invitacion se agrega a `groups.members/memberEmails`.
- Al unirse, la app muestra selector de habitación (si hay plazas) para autoasignación del propio usuario.
- Si no hay habitaciones libres o no existen habitaciones, el usuario queda sin asignación hasta nueva gestión.
- Las invitaciones pendientes del mismo piso/correo se marcan como resueltas al unirse para evitar bucles.

### Initial room setup
- En creacion inicial de habitaciones se guardan tambien:
- `rentSplitMode`
- `rentSplitPercentages` (map vacio)
- `rentSplitOrder` (array vacio)

## Actualización funcional (01/06/2026)

### Documentos de alquiler con archivo adjunto (Storage)
- El módulo group_documents ahora permite subir archivo real desde el móvil (no solo URL manual).
- Implementación en app:
  - Selección local con OpenDocument.
  - Subida a Firebase Storage en ruta group_documents/{groupId}/{timestamp}_{filename}.
  - Persistencia en Firestore de la URL final en group_documents.referenceUri.

### Qué activar adicionalmente
- En Firebase Console, verifica que Storage esté habilitado en el proyecto.
- Recomendación: definir reglas de Storage que limiten lectura/escritura a miembros autenticados del piso según vuestra política de seguridad.


## Actualizacion funcional (01/06/2026) - Entrada condicionada a habitacion
- Flujo de app reforzado: para inquilinos, el acceso al workspace de un piso requiere tener habitacion asignada en rooms_groups.
- Si no existe asignacion o no hay plazas, el usuario no entra directamente al piso.
- Reasignaciones posteriores se mantienen restringidas al propietario segun firestore.rules.


## Actualizacion funcional (01/06/2026) - Aceptacion de pagos pendientes
- payments.update ahora permite confirmar un pago pendiente (pending -> confirmed) al propietario del grupo o al acreedor del pago (toEmail).
- Solo se permite actualizar campos status y updatedAt en esa transicion.


## Actualizacion funcional (01/06/2026) - Permisos compartidos en pagos
- En payments, creador del pago y propietario del piso pueden gestionar pagos.
- update permitido para gestion por creador/propietario, preservando groupId y fromEmail.
- delete permitido para creador/propietario.
- Aceptacion de pago restringida a transicion pending -> confirmed.


## Despliegue CLI (01/06/2026)
- El repositorio ya incluye firebase.json y firestore.indexes.json.
- Desde la raiz del proyecto puedes ejecutar: firebase deploy --only firestore:rules


## Validacion previa a defensa (01/06/2026)
- Revisar TEST_CHECKLIST_TFG.md y ejecutar pruebas de reglas sobre union por codigo y permisos de pagos.



## Nota tecnica (01/06/2026) - Refactor UI sin cambio de esquema
- Esta iteracion solo mueve logica de presentacion a clases de servicio/utilidad en Android.
- No se introducen nuevas colecciones, campos ni cambios de reglas Firestore.

## Actualizacion funcional (03/06/2026) - Gastos repartidos por deudor
- No se anaden nuevas colecciones ni campos obligatorios.
- El reparto multiple de un gasto se materializa ahora en varios documentos de `expenses`, uno por cada deudor final.
- Cada uno de esos gastos individuales mantiene su propio `customSplit` al 100% para el deudor afectado y genera su propia entrada en `payment_deadlines`.
- El flujo de aceptacion sigue apoyandose en `payments` y `payment_deadlines`; no ha sido necesario cambiar `firestore.rules` para esta iteracion.

## Actualizacion funcional (04/06/2026) - Reglas alineadas con el flujo real de gastos
- `expenses.read` sigue permitido para miembros del piso.
- `expenses.update` y `expenses.delete` quedan limitados a gastos en estado `requested`.
- Solo puede gestionar un gasto su pagador original o el propietario del piso.
- En `update` se preservan `groupId`, `payerId` y `payerEmail` para evitar reasignaciones manuales del gasto desde cliente.
- `payments`, `payment_deadlines`, `house_rules`, `group_schedules` y `rent_collections` mantienen la logica de permisos ya alineada con los cambios recientes, por lo que no ha sido necesario tocar mas reglas en esta revision.

