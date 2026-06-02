# Diario de proyecto - FlatShareApp

## Alcance y criterio de veracidad
Este diario esta preparado para entrega academica y sigue estas reglas:
- No se inventan commits ni hashes.
- Los commits reales se reflejan solo cuando existen en el historial Git.
- Entre fechas sin commits, se documenta avance de trabajo reconstruido (analisis, diseno, pruebas, implementacion local) para explicar la evolucion del proyecto.

## Convencion de registro
- `Tipo: Historial Git` -> entrada trazable a commit real.
- `Tipo: Avance reconstruido` -> trabajo realizado en el periodo sin commit publico en este repositorio.

---

## 2026-06-02
- Tipo: Avance reconstruido
- Flujo de deudas desde gasto afinado para que encaje con el modelo mental de "me deben este gasto".
- Se mantiene `expenses` como documento origen y se refuerza que `payment_deadlines` crea una deuda individual por deudor según `customSplit`, evitando duplicar el gasto principal.
- Ajustados permisos de aceptación de pagos pendientes para que puedan confirmar el propietario del piso o el acreedor del pago (`toEmail`), alineando app, reglas y documentación.
- Mejorados textos en `Movimientos` para hablar de pendientes por confirmar y de confirmación de gasto cuando la deuda nace desde un gasto.
- Refinada la UI de pagos pendientes para mostrar `Enviar confirmación`, `Aceptar confirmación` y estados de confirmación cuando el justificante pertenece a una deuda originada por gasto.
- Añadido preset de seed `sergio-owner-homoerectus` con `sergioaripe06@gmail.com` como propietario y `homoerectus079@gmail.com` como inquilino real, incluyendo gastos, pagos, vencimientos y recordatorios.
- Se unifica la acción principal del workspace alrededor de `Nuevo gasto`: desaparece el pago libre visible en UI y el mismo flujo se habilita también en alquiler fijo.
- Los saldos de `alquiler fijo` pasan a incorporar también gastos compartidos además de la renta base de habitaciones.

## 2026-03-03
- Tipo: Avance reconstruido
- Inicio formal del tramo documentado para TFG.
- Definicion del alcance: app Android para gestion de pisos compartidos.
- Casos de uso principales definidos: autenticacion, pisos, gastos, pagos, recordatorios, perfil.

## 2026-03-07
- Tipo: Avance reconstruido
- Diseno inicial de modelo de datos en Firestore (usuarios, pisos, membresia, gastos, pagos).
- Decision de usar Firebase Authentication con Email/Password.

## 2026-03-11
- Tipo: Avance reconstruido
- Boceto de navegacion principal de la app (flujo Splash -> Login/Main).
- Definicion del patron visual base para pantallas de negocio.

## 2026-03-15
- Tipo: Avance reconstruido
- Especificacion funcional de modulo Pisos:
  - crear piso,
  - unirse por codigo/QR,
  - detalle de miembros,
  - gestion de habitaciones.

## 2026-03-19
- Tipo: Avance reconstruido
- Diseno del modulo Workspace:
  - lista de movimientos,
  - creacion de gastos,
  - creacion de pagos,
  - estados y filtros.

## 2026-03-23
- Tipo: Avance reconstruido
- Diseno de logica de balances:
  - reparto por partes iguales,
  - reparto personalizado,
  - efecto de pagos en saldo neto.

## 2026-03-27
- Tipo: Avance reconstruido
- Definicion de recordatorios y calendario:
  - vencimientos de pago,
  - recordatorios manuales,
  - estructura de visualizacion por fecha.

## 2026-03-31
- Tipo: Avance reconstruido
- Planificacion de perfil y ajustes:
  - edicion de datos personales,
  - idioma,
  - tema,
  - notificaciones.

## 2026-04-04
- Tipo: Avance reconstruido
- Decision de modularizacion por capas y dominio:
  - `features`,
  - `core`,
  - `shared`.

## 2026-04-08
- Tipo: Avance reconstruido
- Preparacion del entorno de trabajo Android/Firebase para implementacion continua.
- Estructura preliminar de paquetes y recursos UI.

## 2026-04-12
- Tipo: Avance reconstruido
- Prototipado de formularios para alta de gastos y pagos.
- Definicion de validaciones minimas de entrada (importe, fecha, destinatarios).

## 2026-04-16
- Tipo: Avance reconstruido
- Diseno de seguridad de datos por pertenencia a grupo.
- Borrador de reglas Firestore orientadas a `owner/member`.

## 2026-04-20
- Tipo: Avance reconstruido
- Preparacion de flujos de invitacion y codigos de acceso a piso.
- Definicion de estados de invitacion pendientes/aceptadas/rechazadas.

## 2026-04-24
- Tipo: Avance reconstruido
- Definicion del flujo de alta de habitaciones iniciales tras crear piso.
- Ajuste del comportamiento para evitar pisos incompletos.

## 2026-04-28
- Tipo: Avance reconstruido
- Diseno de soporte para gestion de alquiler:
  - contratos,
  - cobros,
  - incidencias,
  - documentos,
  - auditoria,
  - automatizaciones.

## 2026-05-02
- Tipo: Avance reconstruido
- Revision de consistencia general de arquitectura y preparacion para subida a repositorio.

## 2026-05-05
- Tipo: Historial Git
- Commit real: `f960593` - `first commit`.
- Alta inicial del repositorio y base del proyecto.

## 2026-05-07
- Tipo: Historial Git
- Commit real: `d54ef8a` - `upgrades`.
- Mejoras iterativas sobre la base inicial.

## 2026-05-08
- Tipo: Historial Git
- Commit real: `448b7fa` - `upgrade`.
- Evolucion incremental de codigo y recursos.

## 2026-05-10
- Tipo: Historial Git
- Commit real: `d9aff05` - `rooms`.
- Avance focalizado en gestion de habitaciones.

## 2026-05-11
- Tipo: Historial Git
- Commit real: `26b97ff` - `upgrades`.
- Ajustes funcionales y de estabilidad.

## 2026-05-12
- Tipo: Historial Git
- Commit real: `fd199e9`.
- Refactor UI/UX en pisos y balances; mejora de formularios por pasos.

## 2026-05-12
- Tipo: Historial Git
- Commit real: `8020ad5`.
- Mejora UX en pisos: cierre rapido y alta de habitaciones obligatoria.

## 2026-05-13
- Tipo: Historial Git
- Commit real: `d182ec1`.
- Estructura base del proyecto por modulos y capas.

## 2026-05-13
- Tipo: Historial Git
- Commit real: `85fc8dd`.
- Implementacion de componentes UI, dialogos y navegacion de funcionalidades principales.

## 2026-05-14
- Tipo: Historial Git
- Commit real: `22d74b5`.
- Herramienta debug para seed, mejoras de interfaz y ampliacion de tablas/colecciones.

## 2026-05-18
- Tipo: Avance reconstruido (sesion actual, aun no consolidado en commit en esta rama)
- Ajustes recientes de compilacion y flujo de autenticacion.
- Recuperacion de contrasena por correo en login.
- Mejoras visuales en login.
- Seeder Firebase ampliado con datos de prueba completos:
  - usuarios,
  - pisos,
  - habitaciones,
  - gastos,
  - pagos,
  - vencimientos,
  - recordatorios.
- Arranque de documentacion viva:
  - `diary.md`,
  - `MANUAL_USUARIO.md`.
- Ajuste de alcance para entorno sin coste (Spark):
  - se mantiene invitacion interna por Firestore sin envio SMTP automatico.
- Mejora de invitaciones internas:
  - al abrir la app se muestran invitaciones pendientes con detalle del piso,
  - opcion directa de `Unirme` o `Rechazar` para el usuario invitado.

## 2026-05-21
- Tipo: Avance reconstruido (sesion actual)
- Mejora UX del dialogo de registro de pagos:
  - se anade etiqueta visible para `Prioridad`,
  - se ordenan y etiquetan mejor `Categoria` y `Destino`,
  - en destino `Miembro` se habilitan lineas dinamicas (`Anadir/Quitar`) con minimo 1 y borrado solo desde 2.
- En alquiler variable, la categoria pasa a ser texto libre con autocompletado historico por piso:
  - sin lista fija inicial,
  - al registrar nuevas categorias (ej. `luz`, `agua`) quedan sugeridas en siguientes registros.
- Balance personal actualizado para categorias dinamicas:
  - las categorias nuevas aparecen automaticamente,
  - cada categoria conserva un color estable y ese mismo color se refleja en leyenda y grafico.
- Mejora en `Pisos`:
  - nuevo buscador en la cabecera para filtrar por nombre o direccion,
  - filtrado en tiempo real sobre la lista y mensaje especifico cuando no hay coincidencias.
- Endurecimiento de reglas Firestore en gestion de alquiler:
  - en `rent_collections`, `maintenance_tickets` y `group_documents`,
  - solo el propietario puede `update/delete` (miembros mantienen `create/read`).
- Refuerzo de permisos UI en pisos:
  - `Editar` y `Eliminar piso` bloqueados para no propietarios tambien en interfaz,
  - validacion adicional en codigo para impedir accion aunque se fuerce la UI.
- Ajuste de pagos para alquiler fijo/variable:
- Refactor incremental (strangler pattern) para reducir complejidad en fragments:
  - `ExpensesFragment`: extraidos `CategorySuggestionsRepository` (con cache/limit), `PaymentService`, `ExpenseService`, `ReminderService` y `ExpenseDialogs`.
  - `GroupsFragment`: extraidos `GroupService`, `InvitationService` e `InitialRoomsSetupFlow`.
  - `RentalManagementFragment`: extraidos servicios por modulo para contratos y cobros (`ContractsService`, `CollectionsService`).
  - Se mantiene UI en fragment y se desplaza logica de negocio/datos a servicios.
- Validacion tecnica:
  - compilacion `assembleDebug` completada en verde.
- Archivos principales afectados:
  - `app/src/main/res/layout/dialog_payment.xml`
  - `app/src/main/res/layout/dialog_expense.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/PersonalBalanceFragment.java`
  - `app/src/main/res/layout/fragment_groups.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `firestore.rules`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`
  - `MANUAL_USUARIO.md`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/CategorySuggestionsRepository.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/ExpenseService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/ReminderService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/ExpenseDialogs.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/services/GroupService.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/services/InvitationService.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/services/InitialRoomsSetupFlow.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/ContractsService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/CollectionsService.java`
- Firebase:
  - revisado `FIREBASE_SETUP.md`; sin cambios de esquema ni reglas en esta tarea.

## 2026-05-21
- Tipo: Ajuste tecnico de control de versiones
- Se refuerza `.gitignore` para evitar incluir artefactos locales y de build:
  - se anaden `.gradle-home/` y exclusion completa de `.idea/`,
  - se ignora `crash.txt` y logs de error JVM.
- Se limpian del indice Git archivos no necesarios:
  - `.idea/*`
  - `crash.txt`
- Impacto:
  - evita commits masivos con miles de archivos temporales de entorno local.

## 2026-05-22
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
- Motivo:
  - la app mostraba errores de networking (timeouts/Wi-Fi) al acceder a Firebase.
- Impacto:
  - restablece la conectividad para login, lecturas y escrituras en Firestore.
- Archivos afectados:
  - `app/src/main/AndroidManifest.xml`
- Firebase:
  - revisado `FIREBASE_SETUP.md`; sin cambios de esquema ni de reglas.

## 2026-05-25
- Endurecimiento de acceso:
  - login bloquea usuarios con `emailVerified=false`,
  - reglas Firestore pasan a requerir usuario autenticado y correo verificado.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/auth/RegisterActivity.java`
  - `app/src/main/java/com/sergio/flatshare/features/auth/LoginActivity.java`
  - `app/src/main/java/com/sergio/flatshare/features/auth/VerifyEmailActivity.java`
  - `app/src/main/java/com/sergio/flatshare/features/shell/SplashActivity.java`
  - `app/src/main/java/com/sergio/flatshare/core/sync/UserSync.java`
  - `app/src/main/res/layout/activity_verify_email.xml`
  - `app/src/main/AndroidManifest.xml`
  - `firestore.rules`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`
## 2026-05-25
- Borrado funcional implementado:
  - elimina perfil en `users` y alias en `usernames`,
  - elimina invitaciones relacionadas,
  - saca al usuario de grupos (`members`, `memberEmails`, `roles`),
  - transfiere propiedad de grupo al primer miembro restante si el usuario era owner,
- Reglas Firestore reforzadas para soportar el flujo:
  - `usernames` permite delete del propio usuario.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/res/raw/elevator_music.wav`
  - `app/src/main/java/com/sergio/flatshare/core/settings/SettingsStore.java`
  - `firestore.rules`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`
## 2026-05-25
- Ajustes UI solicitados:
- Pago en alquiler fijo:
  - se muestra `Alquiler` como campo de solo lectura con ayuda contextual.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `app/src/main/res/raw/elevator_music.wav`
  - `app/src/main/res/layout/dialog_payment.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/drawable/bg_button_pill_danger.xml`

## 2026-05-25
  - `Borrar cuenta` pasa a estilo exclusivo rojo con `MaterialButton` para evitar herencia de tint turquesa.
  - mismo rojo en modo claro y oscuro (`status_danger` unificado).
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/res/values-night/colors.xml`

## 2026-05-25
- Mejora visual del modo claro por contraste:
  - ajuste de paleta clara (fondo/superficies/divisores/texto secundario),
  - barra inferior con fondo propio (no transparente),
- Archivos principales afectados:
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/color/bottom_nav_item_colors.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Sonidos de feedback en acciones clave:
  - nuevo helper `AppSoundFx` para reproducir efectos por nombre desde `res/raw`,
  - sonido al crear piso (`sfx_group_created`),
  - sonido al confirmar pago/gasto (`sfx_expense_accepted`, cuando estado pasa a `confirmed`),
  - fallback con tono breve si el audio aun no existe.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/core/sound/AppSoundFx.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Ajustes simplificados por UX:
  - se elimina por completo el bloque de `Volumen` en Ajustes,
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `app/src/main/res/values/strings.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Seguridad en borrado de cuenta:
  - se comprueba que el correo introducido coincide con la cuenta autenticada antes de reautenticar,
  - mensaje de error unificado para credenciales incorrectas.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `MANUAL_USUARIO.md`
---

## Resumen Firebase (estado funcional)
- Authentication: Email/Password.
- Firestore: reglas por pertenencia a grupo y rol.
- Colecciones activas de negocio:
  - `users`, `groups`, `group_codes`, `rooms_groups`,
  - `expenses`, `payments`, `payment_deadlines`, `reminders`, `activity_logs`, `invitations`,
  - `rental_contracts`, `rent_collections`, `maintenance_tickets`, `group_documents`, `audit_events`, `rent_automations`, `event_reminder_rules`, `event_reminder_jobs`.

## Mantenimiento obligatorio del diario
En cada avance relevante anadir:
1. Fecha.
2. Tipo (`Historial Git` o `Avance reconstruido`).
3. Que se hizo.
4. Impacto funcional/tecnico.
5. Archivos principales afectados.





## 2026-05-25
  - se mantiene el reparto anterior en pagos no vinculados a alquiler fijo para evitar regresiones.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
  - se guardan nuevos campos en `rooms_groups`: `rentSplitMode`, `rentSplitPercentages`, `rentSplitOrder`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`
## 2026-05-25
- Archivos principales afectados:
  - `app/src/main/res/layout/activity_owner_rooms.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Ajuste visual del encabezado del piso en Workspace:
  - los tabs `Movimientos`, `Recordatorios` y `Gestion` pasan a estar junto al nombre del piso;
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_groups.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/OwnerRoomsActivity.java`
  - `app/src/main/res/values-en/strings.xml`
## 2026-05-25
  - si hay 2+ inquilinos, se puede elegir reparto `equitativo` o `porcentual`;
- Persistencia en `rooms_groups`:
  - `memberEmails` (ordenado por plaza), `rentSplitMode`, `rentSplitPercentages`, `rentSplitOrder`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/OwnerRoomsActivity.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Ajuste visual en pantalla `Pisos`:
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_groups.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesion actual)
- Flujo de pagos pendientes para inquilino en alquiler variable:
  - en `Movimientos` se anade listado de pendientes propios desde `payment_deadlines` con acceso directo al pago,
  - al pulsar un pendiente, `Registrar pago` abre con importe/concepto/fecha/destino precargados y bloqueados,
  - el justificante en foto pasa a ser obligatorio y el boton `Enviar pago` solo se habilita al adjuntarlo,
  - si no hay deuda, se muestra `No tienes pagos pendientes` y el pago generico queda desactivado para este perfil.
- Persistencia y trazabilidad:
  - en `payments` se guardan `ticketUri`, `sourceDebtId`, `sourceDebtType` cuando aplica,
  - el pendiente original en `payment_deadlines` pasa a estado `submitted` y guarda `submittedAt` y `proofUri`.
- Validacion tecnica:
  - no fue posible ejecutar compilacion local porque el entorno activo usa JVM 8 y Gradle exige JVM 17.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/res/layout/dialog_payment.xml`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`

## 2026-05-25
- Tipo: Avance reconstruido (sesion actual)
- Mejora premium de flujo en `Movimientos` para gastos y pagos:
  - se habilita registro de gasto tambien en alquiler fijo y se normaliza el flujo en ambos modelos,
  - estados visuales unificados por color: `Solicitado` (rojo) -> `Pendiente` (naranja) -> `Pagado` (verde),
  - los deudores deben adjuntar foto justificante para enviar pago,
  - el propietario (o acreedor) valida y confirma el pago para cerrar el flujo.
- Restricciones de seguridad funcional:
  - gastos: solo se pueden editar/eliminar en estado `Solicitado`,
  - pagos: solo se pueden eliminar en estado `Solicitado`; fuera de ese estado no se permite.
- Persistencia:
  - `expenses.status` (`requested|pending|confirmed`),
  - `payments.status` ampliado con `requested` y trazabilidad de deuda origen,
  - sincronizacion de `payment_deadlines` y recalculo automatico de estado del gasto.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`

## 2026-05-25
- Ajuste solicitado en `Movimientos`:
  - se oculta la opcion `Nuevo gasto` cuando el piso esta en `alquiler fijo`,
  - `Nuevo gasto` queda disponible solo para `alquiler variable`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Piso: mejora en `Editar piso` (solo propietario):
  - ahora permite cambiar el modelo de alquiler entre `fijo` y `variable`,
  - si se selecciona `variable`, permite elegir reparto `equitativo` o `porcentual`.
- Persistencia:
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/res/layout/dialog_edit_group.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
  - nuevo preset `sergio-demo` con owner fijo `sergioaripe06@gmail.com`,
  - crea 1 piso demo con inquilinos inventados, habitaciones con distintos importes, gastos, pagos y recordatorios.
  - nuevo script `npm run seed:sergio-demo` para ejecutar el preset en un comando.
- Archivos principales afectados:
  - `tools/firebase-admin-seed/seed.js`
  - `tools/firebase-admin-seed/package.json`
  - `tools/firebase-admin-seed/README.md`
  - `FIREBASE_SETUP.md`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Cabecera de Workspace (`ExpensesFragment`):
  - los botones `Movimientos`, `Recordatorios` y `Gestion` quedan centrados horizontalmente cuando se muestran debajo del nombre del piso.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Recordatorios en `Movimientos` (`ExpensesFragment`):
  - se elimina la herencia de color por estado para filas tipo `reminder`,
  - ahora se renderizan en blanco neutro (texto normal, sin rojo).
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Calendario (`CalendarFragment`):
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Mejora visual de componente
- Balance personal (`PieChartView`):
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/shared/widgets/PieChartView.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Calendario:
- Archivos principales afectados:
  - `app/src/main/res/color/calendar_day_text_selector.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste funcional en pagos
- `Movimientos` (`ExpensesFragment`):
  - las acciones manuales sobre pago quedan disponibles solo en estado `Solicitado`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Calendario (`CalendarFragment`):
  - la etiqueta `Fecha seleccionada` se muestra en formato `dd/MM/yyyy` (ejemplo: `26/05/2026`).
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Workspace (`ExpensesFragment`):
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Mejora UX de textos
- Recordatorios (`ExpensesFragment` y `CalendarFragment`):
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Detalle de recordatorios:
  - se aplica tanto en `Movimientos` como en `Calendario` para mantener consistencia visual.
- Infraestructura compartida:
  - nuevo helper reutilizable `DialogUtils.createInfoRowsView(...)` para ventanas de datos.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/shared/ui/DialogUtils.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste de layout en detalle de recordatorio
- Detalle de recordatorios (movimientos y calendario):
  - `Nombre del piso` en tarjeta dedicada,
  - `Frecuencia` en tarjeta propia,
- Infraestructura compartida:
  - nuevo helper reutilizable `DialogUtils.createReminderDetailView(...)`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/shared/ui/DialogUtils.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Detalle de gasto en `Movimientos`:
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste visual de compatibilidad (calendario)
- Calendario (`CalendarView`):
- Archivos principales afectados:
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
  - se crean colores dedicados `calendar_day_text` y `calendar_day_text_muted` en `values` y `values-night`,
  - se actualiza `calendar_day_text_selector` y `TextAppearance.FlatShare.CalendarDate` para usar esos colores dedicados,
- Resultado:
- Archivos principales afectados:
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values-night/colors.xml`
  - `app/src/main/res/color/calendar_day_text_selector.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Archivos principales afectados:
  - `app/src/main/res/color/bottom_nav_item_colors.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- CTA principal de `Movimientos`:
  - se oculta la etiqueta inferior separada para simplificar el bloque CTA.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Cabecera de Workspace:
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual de cabecera
- Workspace (`ExpensesFragment`):
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Avance reconstruido
- Ajuste visual en pantalla de Movimientos para mejorar el anclaje del CTA inferior y evitar solape visual con la lista de gastos.
- El bloque de accion principal queda mas cercano al menu inferior, manteniendo separacion para no verse pegado.
- Se recalculo el inset inferior dinamico de listas para que los items finales no queden ocultos bajo el CTA.


## 2026-05-26
- Tipo: Avance reconstruido
- Corregido comportamiento del formulario largo de Nuevo gasto para mejorar scroll tras ocultar teclado en dialogos.
- Reforzada la apertura del selector de habitaciones en el formulario de gasto para evitar que se quede bloqueado en una unica opcion visible.


## 2026-05-26
- Tipo: Avance reconstruido
- Homogeneizado el comportamiento de dialogos tipo formulario en toda la app desde DialogUtils.show(...).


## 2026-05-26
- Tipo: Avance reconstruido
- Ajuste correctivo: se retira la captura global de toque en DialogUtils.show(...) porque interferia con selectores desplegables (Spinner) en formularios como Nuevo gasto.
- Se mantiene la mejora de scroll/teclado en formularios largos via ExpenseDialogs, evitando bloquear seleccion de habitacion y persona en reparto.


## 2026-05-26
- Tipo: Avance reconstruido
- Si el email ya existe, se muestra aviso claro al usuario: Ese correo ya tiene una cuenta registrada.


## 2026-05-26
- Tipo: Avance reconstruido
- Pantalla Pisos: el buscador de pisos se integra dentro de la tarjeta principal de listado, en la parte superior, para un bloque visual unico.
- Se ajusta espaciado interno para mantener separacion entre buscador y lista.


## 2026-05-26
- Tipo: Avance reconstruido
- Ventana de detalle del piso (long press en nombre): se sustituye el bloque de miembros por bloque de habitaciones con tarjetas clicables.
- Cada habitacion abre acciones: Ver informacion, Editar habitacion, Eliminar habitacion (editar/eliminar solo para propietario).
- Se mantiene visible el Modelo de reparto dentro del resumen superior del piso.


## 2026-05-26
- Tipo: Avance reconstruido


## 2026-05-26
- Tipo: Avance reconstruido
- Pagos en Movimientos: el detalle deja de mostrarse como notificacion (Toast) y pasa a ventana/modal de detalle, consistente con gastos y recordatorios.
- Se incluye en el detalle: piso, concepto, de, para, habitacion, estado, vencimiento e importe.


## 2026-05-26
- Tipo: Avance reconstruido
- Movimientos (pagos): al tocar una fila de pago se abre directamente el modal de detalle.
- Estados Solicitado y Pendiente: el modal muestra acciones inferiores Editar y Borrar (segun permisos).
- Estado Pagado: modal en solo lectura (sin acciones de modificacion).


## 2026-05-26
- Tipo: Avance reconstruido
  - rojo cuando falta importe por repartir o hay exceso,


## 2026-05-26
- Tipo: Avance reconstruido


## 2026-05-27
- Tipo: Avance reconstruido
  - muestra asignado/total,
  - avisa en rojo si falta importe o si se supera,
  - confirma en verde cuando el reparto es correcto.


## 2026-05-27
- Tipo: Avance reconstruido


## 2026-05-27
- Tipo: Avance reconstruido
- Actualizado `tools/firebase-admin-seed/package.json` con scripts directos para ejecutar ambos presets.
- Actualizado `tools/firebase-admin-seed/README.md` con comandos y detalles de uso de los nuevos presets.


## 2026-05-27
- Tipo: Avance reconstruido
- Corregido scroll en el modal de `Registrar pago pendiente`.
- Se evita el bloqueo al deslizar hacia abajo en formularios largos de pago.


## 2026-05-27
- Tipo: Avance reconstruido
- Corregidos textos visibles con caracteres corruptos en:
  - `ExpensesFragment.java`
  - `GroupsFragment.java`
  - `fragment_groups.xml`


## 2026-05-27
- Tipo: Avance reconstruido


## 2026-05-27
- Tipo: Avance reconstruido


## 2026-05-27
- Tipo: Avance reconstruido
- Pagos pendientes: endurecimiento de validaciones y claridad de estados.
- OCR ya no sobreescribe el importe cuando el pago nace de una deuda pendiente bloqueada.
- Recordatorios: permisos alineados en UI y `firestore.rules` para que solo creador o propietario puedan actualizar/eliminar.


## 2026-05-27
- Tipo: Hotfix UX
- Nuevo gasto: corregido bloqueo de los desplegables de habitaciones e inquilinos en el modal.


## 2026-05-27
- Fondo en modo claro para pantallas de acceso (`activity_login` y `activity_register`): blanco plano sin degradado.


## 2026-05-27
- Cobertura en:


## 2026-05-27
- Tipo: Ajuste de registro
- Campo `Fecha de nacimiento` en `RegisterActivity`: se inicializa con la fecha actual en formato `YYYY-MM-DD`.


## 2026-05-27
- Tipo: UX de validaciones Auth


## 2026-05-27
- Registro: ahora solicita y guarda solo `nombre completo` como identidad visible (sin alias).
- Sync de perfil: `users.displayName` y `users.fullName` quedan alineados con nombre completo.


## 2026-05-27
- Se consultan colecciones por `uid` y/o `email` y se actualizan campos de nombre existentes (`displayName`, `fullName`, `name` y derivados) sin crear campos nuevos.


## 2026-05-27


## 2026-05-27
- Tipo: Ajuste UX de teclado en registro
- `RegisterActivity`: activado `windowSoftInputMode=adjustResize` y ajuste del contenedor del formulario a `wrap_content`.


## 2026-05-27
- Objetivo: mismo comportamiento de visibilidad de campos con teclado en todas las pantallas de acceso.

## 2026-05-27
- Si el usuario sale del piso activo, se limpia `SessionStore` (`currentGroup/currentRoom`) para evitar referencias colgantes.

## 2026-05-27
- Tipo: Ajuste UI en detalle de piso

## 2026-05-27

## 2026-05-27
- Tipo: Mejora de reparto en Nuevo gasto
- El reparto sigue persistiendo por persona en Firestore, manteniendo compatibilidad con balances y vencimientos actuales.

## 2026-05-27
- Tipo: Mejora de reparto en Nuevo cobro/pago

## 2026-05-27
- Corregido texto mojibake y caracteres de reemplazo (`?`) en Java y XML de UI.

## 2026-05-27


## 2026-05-28
- Tipo: Mejora de referencia visual de inquilinos
- `GroupsFragment`: en `Expulsar inquilino` la lista pasa a mostrar `Nombre` y debajo `correo`, y la confirmacion de expulsion tambien muestra ambos datos.
- `RentalManagementFragment`: en los selectores de persona (`Inquilino` y `Responsable`) se muestra `Nombre` + `correo` en dos lineas.
- `RentalManagementFragment`: se valida que no se guarden cobros/incidencias/automatizaciones sin persona seleccionada valida.
- `RentalManagementFragment`: en listados de cobros, incidencias y automatizaciones se muestra la persona en formato de dos lineas para mejorar identificacion.

## 2026-05-28
- Tipo: UX de avisos en Ajustes
- En `SettingsFragment`, los avisos del flujo de borrado de cuenta dejan de usar mensajes fugaces y pasan a ventana emergente con `Aceptar`.
- Se introduce `NoticeUtils` para mostrar avisos persistentes en formato dialogo (`Aviso` + boton `Aceptar`).

## 2026-05-28
- Tipo: Ajuste visual de Login

## 2026-05-28
- Tipo: Ajuste UX en Nuevo gasto (reparto por personas)
- En `ExpensesFragment`, el reparto por personas arranca con una sola fila tanto en `Reparto personalizado` como en `Reparto equitativo`.

## 2026-05-28
- Tipo: Correccion de compilacion Android
- Se restauro el package com.sergio.flatshare.features.auth; en LoginActivity, RegisterActivity y VerifyEmailActivity tras una incidencia de codificacion.
- Se repuso import android.widget.Toast; en las pantallas que aun usan Toast.makeText(...) para evitar errores cannot find symbol Toast.
- Se validaron archivos Java en UTF-8 sin BOM para evitar fallos de caracteres ilegales.

## 2026-05-28
- Tipo: Correccion de textos UTF-8 y UI de registro
- Se mejora el comportamiento con teclado en Crear cuenta para evitar que tape campos (scroll y espaciado inferior).

## 2026-05-28
- Tipo: Limpieza global de mojibake en textos visibles

## 2026-05-29
- Se corrigen permisos para evitar fallos `PERMISSION_DENIED` en borrado en cascada de piso.
- Cambios de reglas:
  - `payments.delete`: ahora permitido al propietario del grupo para soportar limpieza completa del piso.
  - `audit_events.delete` y `event_reminder_jobs.delete`: permitido al propietario del grupo (se mantiene `update` bloqueado).

## 2026-05-29
- `SettingsFragment` ahora compara y selecciona idioma usando el locale activo de `AppCompatDelegate`, con fallback a preferencias.

## 2026-05-29
- Se migran textos hardcoded a recursos traducibles en:
  - `activity_login.xml`
  - `activity_register.xml`
  - `activity_verify_email.xml`
  - `LoginActivity.java`
  - `RegisterActivity.java`
  - `VerifyEmailActivity.java`
  - `BalancesFragment.java`
  - `values/strings.xml` (ES base)
  - `values-en/strings.xml` (EN)

## 2026-05-29
- Tipo: Correccion de build Android por recurso i18n EN
- Se corrige `register_terms_content` en `values-en/strings.xml` para evitar un escape conflictivo durante mergeDebugResources (Invalid unicode escape sequence).
- Se revisan escapes en el archivo y se deja en UTF-8 sin BOM.

## 2026-05-31
  - en API `>= 29` se mantiene `MediaStore.Downloads`,
  - en API `< 29` se usa fallback a almacenamiento externo de la app (`getExternalFilesDir`) para evitar acceso a APIs no disponibles.
- Se elimina `firebase-analytics` de `app/build.gradle`:
  - objetivo: evitar incompatibilidades de metadata Kotlin asociadas a `play-services-measurement` durante `lint`.
  - `clean assembleDebug testDebugUnitTest` en verde.
  - ya no aparecen errores de `MediaStore.Downloads` ni mensajes de incompatibilidad Kotlin metadata en `lint`.
- Nota:
  - `lintDebug` sigue fallando por incidencias previas del proyecto no relacionadas con este ajuste (por ejemplo `UseAppTint` en layouts).

## 2026-05-31
- Tipo: Cierre de calidad (lint en verde)
- Se resuelven errores bloqueantes de lint:
  - `UseAppTint` en layouts de perfil y fila de pisos.
  - `PermissionImpliesUnsupportedChromeOsHardware` en `AndroidManifest.xml` declarando `android.hardware.camera` como opcional.
  - `MissingTranslation` en locale `es` sincronizando `values-es/strings.xml` con las claves activas.
- Verificacion final:
  - `lintDebug`: OK.
  - `assembleDebug`: OK.

## 2026-05-31
- Tipo: Estandarizacion de fechas UI (`DD/MM/AAAA`)
- Se unifica el formato de fecha visible en la app a `DD/MM/AAAA`:
  - Gastos/pagos/recordatorios (workspace y calendario).
- Compatibilidad retroactiva:
  - Al mostrar/guardar de nuevo, se normaliza a `DD/MM/AAAA`.
- Archivos actualizados:
  - Nueva utilidad compartida: `DateInputUtils`.
  - Textos y hints en layouts/strings para reflejar `DD/MM/AAAA`.

## 2026-05-31
  - `Cobros`
  - `Incidencias`
  - `Documentos`
- Se elimina del flujo visible de UI:
  - `Contrato`
  - `Reglas push`
  - `assembleDebug` en verde tras el cambio.

## 2026-05-31
- Se renombra el acceso visible `Gestion` a `Cobros y soporte` para describir mejor su contenido.
- En la pantalla de `RentalManagementFragment`:
- Objetivo del ajuste:

## 2026-06-01
  - `ProfileFragment` normaliza `birthDate` al cargar desde Firestore (`DateInputUtils.normalizeToDisplay`).
- Ajuste de texto UI:
  - En `dialog_edit_profile.xml`, el hint pasa de `Ejemplo: 1999-05-14` a `Ejemplo: 14/05/1999`.

## 2026-06-01
- Se corrigen textos corruptos en:
  - `ExpensesFragment.java`
  - `RentalManagementFragment.java`
- Resultado:
- Ajuste puntual adicional:

## 2026-06-01
- Motivo:
  - `fragment_expenses.xml`: se retira `addRoomQuickBtn` y el spinner pasa a ocupar todo el ancho.

## 2026-06-01
- Reglas Firebase:
- UX de detalle:

## 2026-06-01
  - `Inquilino` en el desplegable de cobro pasa a formato visible `Nombre - email`.

## 2026-06-01
- Flujo nuevo:
  - El usuario selecciona un archivo con el selector del sistema (OpenDocument).
  - La app sube el archivo a Firebase Storage en group_documents/{groupId}/....
  - Se guarda en Firestore (group_documents.referenceUri) la URL de descarga generada.
- Compatibilidad:
  - Se mantiene la entrada manual de Referencia/URL (opcional) para casos sin adjunto.

## 2026-06-01

## 2026-06-01
- Tipo: Limpieza de archivos obsoletos/locales
- Se corrige .gitignore para ignorar firebase-debug.log y .android-home/.

## 2026-06-01

## 2026-06-01

## 2026-06-01
- Tipo: Endurecimiento de reglas Firebase para habitaciones
ooms_groups para eliminar la auto-salida/cambio directo del inquilino (isSelfRoomLeaveUpdate).

## 2026-06-01
- Se corrige rebindeo de Spinner en ExpensesFragment (paymentMembersContainer / paymentRoomsContainer).

## 2026-06-01
- Tipo: Asignacion obligatoria de habitacion al entrar en piso
- Se refuerza GroupsFragment para que, al abrir un piso como inquilino, primero verifique si el usuario ya pertenece a una habitacion de rooms_groups.
- Si no tiene habitacion, se muestra selector obligatorio y no se abre el workspace hasta completar la asignacion.
- Si no hay habitaciones creadas o no hay plazas libres, no se permite entrar directamente; el propietario debe gestionar la asignacion.


## 2026-06-01
- Tipo: Control de acceso en union por QR/codigo
- En GroupService.joinGroupByCode se valida primero si el usuario (uid/email) ya pertenece al piso.
- Si ya es miembro, se bloquea la union y se muestra: 'Acceso denegado: ya estas en este piso'.


## 2026-06-01
- Tipo: Correccion de desplegables de miembros en Pagos y Recordatorios
- Se elimina el rebindeo automatico al cambiar seleccion en lineas de destino para evitar que el Spinner se cierre o bloquee al desplegar.
- Afecta a Registrar pago (miembros/habitaciones) y Nuevo recordatorio (miembros/habitaciones), mejorando la seleccion manual estable.


## 2026-06-01
- Tipo: Saneamiento global de codificacion
- Se eliminan lineas historicas corruptas en diario para mantener UTF-8 legible.
- Se normalizan archivos con BOM a UTF-8 sin BOM y se revisan textos visibles.

## 2026-06-01
- Tipo: Flujo de pagos pendiente->aceptado y limpieza de duplicados visuales
- En Movimientos para inquilino se muestran solo pagos enviados por el propio inquilino; el resto de deudas se gestiona desde pagos pendientes para evitar duplicados/confusion.
- Al tocar una deuda pendiente se abre Registrar pago con justificante obligatorio; al enviar pasa a estado pending.
- La aceptacion ahora se permite cuando el pago esta en pending, tanto para propietario como para el acreedor (toEmail).


## 2026-06-01
- Tipo: Permisos compartidos de pagos (creador + propietario)
- Se actualiza firestore.rules para que el creador del pago (fromEmail) y el propietario del piso puedan gestionar pagos del grupo.
- Ambos pueden aceptar un pago pendiente (pending -> confirmed) y eliminar pagos.
- Se habilita actualizacion de pagos por creador/propietario manteniendo invariantes de groupId y fromEmail.


## 2026-06-01
- Tipo: Nuevo pago habilitado para todos en ambos modelos de alquiler
- En Nueva accion, Registrar pago queda disponible para todos los miembros (fijo y variable).
- Si el inquilino tiene deudas pendientes, aparece ademas acceso rapido Pagar pendiente.
- En detalle de pago pendiente, el boton de accion muestra texto Aceptar pago.


## 2026-06-01
- Tipo: Correccion de desplegables en Nuevo gasto
- Se refuerza el manejo tactil de Spinner en el dialogo de gasto (tipo de reparto, repartir por y filas de miembro/habitacion).
- Se evita que el ScrollView intercepte el toque y bloquee la apertura del desplegable en algunos dispositivos.


## 2026-06-01
- Tipo: Correccion de union por codigo con permisos de lectura restringidos
- GroupService.joinGroupByCode ahora, si falla la lectura de groups/{groupId} por PERMISSION_DENIED, hace fallback a alta directa con arrayUnion.
- Resultado: el no-miembro puede unirse por codigo sin error de permisos insuficientes.


## 2026-06-01
- Tipo: Correccion de duplicados en listado de Movimientos
- Se añade control de version de carga en ExpensesFragment (expensesLoadVersion) para ignorar respuestas async antiguas.
- Resultado: al recargar en paralelo no se agregan filas duplicadas de pagos/gastos.


## 2026-06-01
- Tipo: Saneamiento global de codigo y despliegue Firebase
- Limpieza de imports duplicados en todos los archivos Java (especialmente Toast repetido), manteniendo UTF-8 sin BOM.
- Se agrega configuracion de proyecto Firebase en repo: firebase.json y firestore.indexes.json.
- Resultado: despliegue de reglas disponible desde la raiz del proyecto con firebase deploy --only firestore:rules.


## 2026-06-01
- Tipo: Refactor y saneamiento tecnico (fase TFG)
- Se extrae politica de permisos de pagos a servicio dedicado: PaymentAccessPolicy.
- ExpensesFragment reduce logica de autorizacion en metodos de pago usando servicio externo.
- Limpieza de codificacion en codigo/recursos de app y correccion de textos corruptos residuales.
- Se agrega TEST_CHECKLIST_TFG.md con pruebas funcionales y de seguridad para validacion final.


## 2026-06-01
- Tipo: Refactor incremental de fragmentos gigantes (fase 2 TFG)
- Se extrae catalogo de provincias/ciudades de GroupsFragment a ProvinceCityCatalog.
- Se extrae formateo de etiquetas de miembro a MemberLabelFormatter para reutilizar en ExpensesFragment y RentalManagementFragment.
- Se extrae formateo de textos de modulos de alquiler a RentalTextFormatter.
- Se actualiza TEST_CHECKLIST_TFG.md con bloque de regresion por refactor.


## 2026-06-01
- Tipo: Guia de validacion final en 10 minutos
- Se agrega validacion10minutos.md con checklist cronometrado para cierre pre-entrega (union, habitaciones, gastos, pagos, duplicados y smoke final).

