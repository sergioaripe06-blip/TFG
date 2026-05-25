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
  - en `Registrar pago`, el destino por `Habitación` pasa de selector simple a selector por líneas (múltiple),
  - se pueden añadir/quitar habitaciones y repartir solo entre inquilinos de esas habitaciones,
  - se guardan `roomIds`/`roomNames` en `payments` para trazabilidad y filtros por contexto de habitación.
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
- Tipo: Corrección técnica (conectividad Android)
- Se corrige `AndroidManifest.xml` añadiendo permisos de red faltantes:
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
- Tipo: Avance reconstruido (sesión actual)
- Implementación de verificación real de correo en el flujo de autenticación:
  - tras registro se envía email de verificación con Firebase Auth,
  - nueva pantalla `VerifyEmailActivity` para reenvío y confirmación,
  - el alta funcional de perfil (`users`, `usernames`, términos) se completa al verificar.
- Endurecimiento de acceso:
  - login bloquea usuarios con `emailVerified=false`,
  - splash cierra sesión no verificada y vuelve a login,
  - reglas Firestore pasan a requerir usuario autenticado y correo verificado.
- Corrección de textos visibles en autenticación con acentos en español.
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
- Tipo: Avance reconstruido (sesión actual)
- Ajustes: nueva barra de volumen y música ambiental en bucle en `SettingsFragment`.
- Cuenta: nuevo botón `Borrar cuenta` con flujo de eliminación controlada.
- Borrado funcional implementado:
  - elimina perfil en `users` y alias en `usernames`,
  - elimina invitaciones relacionadas,
  - saca al usuario de grupos (`members`, `memberEmails`, `roles`),
  - transfiere propiedad de grupo al primer miembro restante si el usuario era owner,
  - conserva históricos (`payments`, `expenses`, etc.).
- Reglas Firestore reforzadas para soportar el flujo:
  - nueva función `isSelfLeaveUpdate` en `groups`,
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
- Tipo: Avance reconstruido (sesión actual)
- Ajustes UI solicitados:
  - botón `Cerrar sesión` movido al final y estilo rojo,
  - etiqueta de música simplificada a `Volumen`,
  - música de fondo ajustada para menor pausa entre notas.
- Pago en alquiler fijo:
  - mejora visual en categoría para evitar apariencia de desplegable bloqueado,
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
- Tipo: Avance reconstruido (sesión actual)
- Corrección visual puntual en Ajustes:
  - `Borrar cuenta` pasa a estilo exclusivo rojo con `MaterialButton` para evitar herencia de tint turquesa.
  - mismo rojo en modo claro y oscuro (`status_danger` unificado).
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/res/values-night/colors.xml`

## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Mejora visual del modo claro por contraste:
  - ajuste de paleta clara (fondo/superficies/divisores/texto secundario),
  - barra inferior con fondo propio (no transparente),
  - color seleccionado de icono/texto en navegación inferior a primario para lectura estable.
- Archivos principales afectados:
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/color/bottom_nav_item_colors.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
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
- Tipo: Avance reconstruido (sesión actual)
- Ajuste de música de fondo en Ajustes:
  - se elimina la aceleración de reproducción para mantener un ambiente más calmado,
  - carga dinámica de pista con prioridad a `ambient_calm` en `res/raw`,
  - fallback automático a `elevator_music` si no existe la pista nueva.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Ajustes simplificados por UX:
  - se elimina por completo el bloque de `Volumen` en Ajustes,
  - se retira la reproducción de música de fondo de `SettingsFragment`.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `app/src/main/res/values/strings.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Seguridad en borrado de cuenta:
  - el flujo de validación previa ahora exige correo y contraseña,
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
  - `users`, `usernames`, `groups`, `group_codes`, `rooms_groups`,
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
- Tipo: Avance reconstruido (sesión actual)
- Reparto de pagos de alquiler por habitación (alquiler fijo):
  - cuando el destino es `Habitación`, el importe se reparte primero entre habitaciones seleccionadas,
  - dentro de cada habitación, si hay 1 residente asume el 100% de su parte, y si hay 2 o más se divide entre residentes,
  - se mantiene el reparto anterior en pagos no vinculados a alquiler fijo para evitar regresiones.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Habitaciones: configuración de reparto de alquiler por habitación (solo propietario):
  - en `Editar inquilinos` se puede definir reparto `equitativo` o `porcentual` cuando hay 2 o más residentes,
  - en modo porcentual se elige un inquilino automático y el resto de porcentajes se editan manualmente,
  - se guardan nuevos campos en `rooms_groups`: `rentSplitMode`, `rentSplitPercentages`, `rentSplitOrder`.
- Cálculo financiero actualizado:
  - en alquiler fijo, los saldos y pagos por habitación respetan el reparto porcentual configurado,
  - si no hay configuración válida, se aplica reparto equitativo como fallback.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`
## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Ajuste de copy en acciones de invitación/alta de residentes:
  - se cambia el texto visible de `Invitar por código/email` a `Añadir por código/email` en los 4 puntos solicitados.
- Archivos principales afectados:
  - `app/src/main/res/layout/activity_owner_rooms.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Ajuste visual del encabezado del piso en Workspace:
  - los tabs `Movimientos`, `Recordatorios` y `Gestion` pasan a estar junto al nombre del piso;
  - si el nombre es largo y no cabe, los tabs se recolocan automáticamente justo debajo del título.
- Limpieza técnica asociada:
  - se elimina la referencia residual a `saldosTabBtn` para evitar error de compilación por id inexistente.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Homogeneización de copy en UI de pisos/habitaciones:
  - se reemplaza `Invitar` por `Añadir` en botones, diálogos y descripciones de alta de residentes;
  - también se ajusta la cadena equivalente en inglés (`Invite` -> `Add`).
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_groups.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/OwnerRoomsActivity.java`
  - `app/src/main/res/values-en/strings.xml`
## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Habitaciones (solo propietario): reparto y posicion por inquilino también en creación y edición.
  - al crear o editar una habitación, se elige explícitamente quién va en `Inquilino 1`, `Inquilino 2`, etc.;
  - si hay 2+ inquilinos, se puede elegir reparto `equitativo` o `porcentual`;
  - en reparto porcentual, el último inquilino queda automático con el porcentaje restante.
- Persistencia en `rooms_groups`:
  - `memberEmails` (ordenado por plaza), `rentSplitMode`, `rentSplitPercentages`, `rentSplitOrder`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/OwnerRoomsActivity.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Ajuste visual en pantalla `Pisos`:
  - se centran los tres botones de acciones del detalle (`Editar`, `Añadir`, `Gestionar habitaciones`).
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
- Tipo: Avance reconstruido (sesión actual)
- Ajuste solicitado en `Movimientos`:
  - se oculta la opcion `Nuevo gasto` cuando el piso esta en `alquiler fijo`,
  - `Nuevo gasto` queda disponible solo para `alquiler variable`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Piso: mejora en `Editar piso` (solo propietario):
  - ahora permite cambiar el modelo de alquiler entre `fijo` y `variable`,
  - si se selecciona `variable`, permite elegir reparto `equitativo` o `porcentual`.
- Persistencia:
  - actualiza `groups.billingModel` y `groups.variableSplitMode` junto a nombre/descripción.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/res/layout/dialog_edit_group.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesión actual)
- Seeder admin: preset hardcodeado para demo rápida solicitada:
  - nuevo preset `sergio-demo` con owner fijo `sergioaripe06@gmail.com`,
  - crea 1 piso demo con inquilinos inventados, habitaciones con distintos importes, gastos, pagos y recordatorios.
- Productivización:
  - nuevo script `npm run seed:sergio-demo` para ejecutar el preset en un comando.
- Archivos principales afectados:
  - `tools/firebase-admin-seed/seed.js`
  - `tools/firebase-admin-seed/package.json`
  - `tools/firebase-admin-seed/README.md`
  - `FIREBASE_SETUP.md`
  - `MANUAL_USUARIO.md`
