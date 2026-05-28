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
  - en `Registrar pago`, el destino por `Habitaci�n` pasa de selector simple a selector por l�neas (m�ltiple),
  - se pueden a�adir/quitar habitaciones y repartir solo entre inquilinos de esas habitaciones,
  - se guardan `roomIds`/`roomNames` en `payments` para trazabilidad y filtros por contexto de habitaci�n.
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
- Tipo: Correcci�n t�cnica (conectividad Android)
- Se corrige `AndroidManifest.xml` a�adiendo permisos de red faltantes:
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
- Tipo: Avance reconstruido (sesi�n actual)
- Implementaci�n de verificaci�n real de correo en el flujo de autenticaci�n:
  - tras registro se env�a email de verificaci�n con Firebase Auth,
  - nueva pantalla `VerifyEmailActivity` para reenv�o y confirmaci�n,
  - el alta funcional de perfil (`users`, `usernames`, t�rminos) se completa al verificar.
- Endurecimiento de acceso:
  - login bloquea usuarios con `emailVerified=false`,
  - splash cierra sesi�n no verificada y vuelve a login,
  - reglas Firestore pasan a requerir usuario autenticado y correo verificado.
- Correcci�n de textos visibles en autenticaci�n con acentos en espa�ol.
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
- Tipo: Avance reconstruido (sesi�n actual)
- Ajustes: nueva barra de volumen y m�sica ambiental en bucle en `SettingsFragment`.
- Cuenta: nuevo bot�n `Borrar cuenta` con flujo de eliminaci�n controlada.
- Borrado funcional implementado:
  - elimina perfil en `users` y alias en `usernames`,
  - elimina invitaciones relacionadas,
  - saca al usuario de grupos (`members`, `memberEmails`, `roles`),
  - transfiere propiedad de grupo al primer miembro restante si el usuario era owner,
  - conserva hist�ricos (`payments`, `expenses`, etc.).
- Reglas Firestore reforzadas para soportar el flujo:
  - nueva funci�n `isSelfLeaveUpdate` en `groups`,
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
- Tipo: Avance reconstruido (sesi�n actual)
- Ajustes UI solicitados:
  - bot�n `Cerrar sesi�n` movido al final y estilo rojo,
  - etiqueta de m�sica simplificada a `Volumen`,
  - m�sica de fondo ajustada para menor pausa entre notas.
- Pago en alquiler fijo:
  - mejora visual en categor�a para evitar apariencia de desplegable bloqueado,
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
- Tipo: Avance reconstruido (sesi�n actual)
- Correcci�n visual puntual en Ajustes:
  - `Borrar cuenta` pasa a estilo exclusivo rojo con `MaterialButton` para evitar herencia de tint turquesa.
  - mismo rojo en modo claro y oscuro (`status_danger` unificado).
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/res/values-night/colors.xml`

## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Mejora visual del modo claro por contraste:
  - ajuste de paleta clara (fondo/superficies/divisores/texto secundario),
  - barra inferior con fondo propio (no transparente),
  - color seleccionado de icono/texto en navegaci�n inferior a primario para lectura estable.
- Archivos principales afectados:
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/color/bottom_nav_item_colors.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
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
- Tipo: Avance reconstruido (sesi�n actual)
- Ajuste de m�sica de fondo en Ajustes:
  - se elimina la aceleraci�n de reproducci�n para mantener un ambiente m�s calmado,
  - carga din�mica de pista con prioridad a `ambient_calm` en `res/raw`,
  - fallback autom�tico a `elevator_music` si no existe la pista nueva.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Ajustes simplificados por UX:
  - se elimina por completo el bloque de `Volumen` en Ajustes,
  - se retira la reproducci�n de m�sica de fondo de `SettingsFragment`.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_settings.xml`
  - `app/src/main/java/com/sergio/flatshare/features/settings/SettingsFragment.java`
  - `app/src/main/res/values/strings.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Seguridad en borrado de cuenta:
  - el flujo de validaci�n previa ahora exige correo y contrase�a,
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
- Tipo: Avance reconstruido (sesi�n actual)
- Reparto de pagos de alquiler por habitaci�n (alquiler fijo):
  - cuando el destino es `Habitaci�n`, el importe se reparte primero entre habitaciones seleccionadas,
  - dentro de cada habitaci�n, si hay 1 residente asume el 100% de su parte, y si hay 2 o m�s se divide entre residentes,
  - se mantiene el reparto anterior en pagos no vinculados a alquiler fijo para evitar regresiones.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Habitaciones: configuraci�n de reparto de alquiler por habitaci�n (solo propietario):
  - en `Editar inquilinos` se puede definir reparto `equitativo` o `porcentual` cuando hay 2 o m�s residentes,
  - en modo porcentual se elige un inquilino autom�tico y el resto de porcentajes se editan manualmente,
  - se guardan nuevos campos en `rooms_groups`: `rentSplitMode`, `rentSplitPercentages`, `rentSplitOrder`.
- C�lculo financiero actualizado:
  - en alquiler fijo, los saldos y pagos por habitaci�n respetan el reparto porcentual configurado,
  - si no hay configuraci�n v�lida, se aplica reparto equitativo como fallback.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/services/PaymentService.java`
  - `MANUAL_USUARIO.md`
  - `FIREBASE_SETUP.md`
  - `firestore.rules`
## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Ajuste de copy en acciones de invitaci�n/alta de residentes:
  - se cambia el texto visible de `Invitar por c�digo/email` a `A�adir por c�digo/email` en los 4 puntos solicitados.
- Archivos principales afectados:
  - `app/src/main/res/layout/activity_owner_rooms.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Ajuste visual del encabezado del piso en Workspace:
  - los tabs `Movimientos`, `Recordatorios` y `Gestion` pasan a estar junto al nombre del piso;
  - si el nombre es largo y no cabe, los tabs se recolocan autom�ticamente justo debajo del t�tulo.
- Limpieza t�cnica asociada:
  - se elimina la referencia residual a `saldosTabBtn` para evitar error de compilaci�n por id inexistente.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Homogeneizaci�n de copy en UI de pisos/habitaciones:
  - se reemplaza `Invitar` por `A�adir` en botones, di�logos y descripciones de alta de residentes;
  - tambi�n se ajusta la cadena equivalente en ingl�s (`Invite` -> `Add`).
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_groups.xml`
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/groups/OwnerRoomsActivity.java`
  - `app/src/main/res/values-en/strings.xml`
## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Habitaciones (solo propietario): reparto y posicion por inquilino tambi�n en creaci�n y edici�n.
  - al crear o editar una habitaci�n, se elige expl�citamente qui�n va en `Inquilino 1`, `Inquilino 2`, etc.;
  - si hay 2+ inquilinos, se puede elegir reparto `equitativo` o `porcentual`;
  - en reparto porcentual, el �ltimo inquilino queda autom�tico con el porcentaje restante.
- Persistencia en `rooms_groups`:
  - `memberEmails` (ordenado por plaza), `rentSplitMode`, `rentSplitPercentages`, `rentSplitOrder`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/OwnerRoomsActivity.java`
  - `MANUAL_USUARIO.md`
## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Ajuste visual en pantalla `Pisos`:
  - se centran los tres botones de acciones del detalle (`Editar`, `A�adir`, `Gestionar habitaciones`).
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
- Tipo: Avance reconstruido (sesi�n actual)
- Ajuste solicitado en `Movimientos`:
  - se oculta la opcion `Nuevo gasto` cuando el piso esta en `alquiler fijo`,
  - `Nuevo gasto` queda disponible solo para `alquiler variable`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Piso: mejora en `Editar piso` (solo propietario):
  - ahora permite cambiar el modelo de alquiler entre `fijo` y `variable`,
  - si se selecciona `variable`, permite elegir reparto `equitativo` o `porcentual`.
- Persistencia:
  - actualiza `groups.billingModel` y `groups.variableSplitMode` junto a nombre/descripci�n.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/groups/GroupsFragment.java`
  - `app/src/main/res/layout/dialog_edit_group.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-25
- Tipo: Avance reconstruido (sesi�n actual)
- Seeder admin: preset hardcodeado para demo r�pida solicitada:
  - nuevo preset `sergio-demo` con owner fijo `sergioaripe06@gmail.com`,
  - crea 1 piso demo con inquilinos inventados, habitaciones con distintos importes, gastos, pagos y recordatorios.
- Productivizaci�n:
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
- Tipo: Correcci�n de codificaci�n y textos
- Limpieza de mojibake en creaci�n/gesti�n de piso (`GroupsFragment`):
  - se corrigieron textos visibles corruptos (tildes, `�`, `N�`, nombres de provincias/ciudades y mensajes UI),
  - el archivo qued� en `UTF-8` sin BOM para evitar nuevos problemas de interpretaci�n.
- Verificaci�n:
  - b�squeda global sin coincidencias de patrones t�picos de mojibake.
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
  - el d�a actual, cuando no est� seleccionado, se muestra con n�mero en blanco (ya no gris).
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Mejora visual de componente
- Balance personal (`PieChartView`):
  - redise�o del gr�fico circular a estilo donut,
  - separaci�n visual entre porciones para mejorar legibilidad,
  - total acumulado en el centro del gr�fico.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/shared/widgets/PieChartView.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Calendario:
  - se unifica `calendar_day_text_selector` para que el texto de los d�as se mantenga claro tambi�n en estados `selected`, `pressed` y `activated`,
  - corrige el caso del d�a actual no seleccionado que se ve�a oscuro.
- Archivos principales afectados:
  - `app/src/main/res/color/calendar_day_text_selector.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste funcional en pagos
- `Movimientos` (`ExpensesFragment`):
  - pagos en estado `Pendiente` o `Pagado` quedan en solo lectura para acciones de modificaci�n/eliminaci�n,
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
  - se aument� el tama�o de los botones de tabs `Movimientos`, `Recordatorios` y `Gesti�n` (alto, texto y padding).
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Mejora UX de textos
- Recordatorios (`ExpensesFragment` y `CalendarFragment`):
  - se mejora la redacci�n en lista y detalle para que sea m�s legible y menos t�cnica,
  - se sustituye el estilo `X habitaci�n` / `X miembros` por `Por habitaci�n` / `Por miembros`,
  - subt�tulos de recordatorio con estructura m�s clara (`Para`, `Frecuencia`, `Desde/Hasta`).
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Mejora visual de di�logos
- Detalle de recordatorios:
  - se reemplaza el texto plano en bloque por filas visuales de informaci�n (etiqueta/valor) dentro de tarjetas,
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
  - `Dirigido a personas/habitaciones` con vi�etas por elemento (uno por l�nea),
  - `Frecuencia` en tarjeta propia,
  - `Desde` y `Hasta` en la misma l�nea, divididos en dos tarjetas.
- Infraestructura compartida:
  - nuevo helper reutilizable `DialogUtils.createReminderDetailView(...)`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/shared/ui/DialogUtils.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Mejora visual de di�logos
- Detalle de gasto en `Movimientos`:
  - se sustituye el texto corrido por tarjetas de datos (piso, pagado por, habitaci�n, categor�a, estado, vencimiento e importe),
  - se aplica tanto en solo lectura como en el di�logo con acciones `Borrar`/`Editar`.
- Archivos principales afectados:
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste visual de compatibilidad (calendario)
- Calendario (`CalendarView`):
  - se fuerza color de texto claro tambi�n en atributos de color primario/inverso del estilo de fecha,
  - se define fondo de semana seleccionada para mejorar contraste y evitar que el d�a actual se perciba oscuro en ciertos dispositivos.
- Archivos principales afectados:
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Correcci�n de color global (calendario)
- Se detecta dependencia del tema base en el color de d�as de `CalendarView`:
  - se crean colores dedicados `calendar_day_text` y `calendar_day_text_muted` en `values` y `values-night`,
  - se actualiza `calendar_day_text_selector` y `TextAppearance.FlatShare.CalendarDate` para usar esos colores dedicados,
  - se ajusta `CalendarFragment` para usar los nuevos colores al pintar d�as del mes enfocado/no enfocado.
- Resultado:
  - el d�a actual mantiene texto claro tambi�n cuando no est� seleccionado.
- Archivos principales afectados:
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values-night/colors.xml`
  - `app/src/main/res/color/calendar_day_text_selector.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/CalendarFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Men� inferior:
  - iconos y t�tulos del `BottomNavigationView` pasan a color blanco en estado seleccionado y no seleccionado (eliminando el gris).
- Archivos principales afectados:
  - `app/src/main/res/color/bottom_nav_item_colors.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- CTA principal de `Movimientos`:
  - se elimina el icono `+` del bot�n principal,
  - el texto de acci�n (`Nuevo gasto`, `Nuevo pago`, `Nuevo recordatorio`, `Gestion`) pasa a mostrarse dentro del propio bot�n,
  - se oculta la etiqueta inferior separada para simplificar el bloque CTA.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `app/src/main/java/com/sergio/flatshare/features/workspace/ExpensesFragment.java`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual
- Cabecera de Workspace:
  - nombre del piso centrado y ligeramente m�s grande para darle m�s importancia visual.
- Archivos principales afectados:
  - `app/src/main/res/layout/fragment_expenses.xml`
  - `MANUAL_USUARIO.md`

## 2026-05-26
- Tipo: Ajuste UI puntual de cabecera
- Workspace (`ExpensesFragment`):
  - nombre del piso con mayor presencia visual (tama�o ligeramente superior),
  - tabs `Movimientos`, `Recordatorios` y `Gesti�n` en modo expandido cuando van debajo del t�tulo (mismo ancho y ocupando todo el ancho del m�vil),
  - etiqueta `Habitaci�n:` con tama�o ligeramente superior.
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
- A�adido ajuste de ventana (djustResize) en dialogos largos para evitar bloqueo de desplazamiento en algunos dispositivos.
- Reforzada la apertura del selector de habitaciones en el formulario de gasto para evitar que se quede bloqueado en una unica opcion visible.


## 2026-05-26
- Tipo: Avance reconstruido
- Homogeneizado el comportamiento de dialogos tipo formulario en toda la app desde DialogUtils.show(...).
- Se fuerza djustResize para teclado y se a�ade cierre de foco/teclado al tocar fuera del campo para evitar bloqueos de scroll en pantallas equivalentes.


## 2026-05-26
- Tipo: Avance reconstruido
- Ajuste correctivo: se retira la captura global de toque en DialogUtils.show(...) porque interferia con selectores desplegables (Spinner) en formularios como Nuevo gasto.
- Se mantiene la mejora de scroll/teclado en formularios largos via ExpenseDialogs, evitando bloquear seleccion de habitacion y persona en reparto.


## 2026-05-26
- Tipo: Avance reconstruido
- Registro: se a�ade manejo explicito de colision de correo en Firebase Auth (FirebaseAuthUserCollisionException).
- Si el email ya existe, se muestra aviso claro al usuario: Ese correo ya tiene una cuenta registrada.
- Correccion de textos visibles del registro a UTF-8 correcto (tildes y e�e).


## 2026-05-26
- Tipo: Avance reconstruido
- Pantalla Pisos: el buscador de pisos se integra dentro de la tarjeta principal de listado, en la parte superior, para un bloque visual unico.
- Se ajusta espaciado interno para mantener separacion entre buscador y lista.


## 2026-05-26
- Tipo: Avance reconstruido
- Ventana de detalle del piso (long press en nombre): se sustituye el bloque de miembros por bloque de habitaciones con tarjetas clicables.
- Cada habitacion abre acciones: Ver informacion, Editar habitacion, Eliminar habitacion (editar/eliminar solo para propietario).
- Se anaden botones inferiores dentro del contenido: A�adir habitaci�n y Ver ubicaci�n.
- Se mantiene visible el Modelo de reparto dentro del resumen superior del piso.


## 2026-05-26
- Tipo: Avance reconstruido
- Correcci�n de textos corruptos (mojibake) en ExpensesFragment y ajustes de acentuaci�n en UI de Pisos.
- Se restauran tildes y caracteres en espa�ol (�, �, �, �, �, �, �) en textos visibles para usuario.


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
- Nuevo gasto/cobro: mejora en reparto por habitaci�n para casos con un �nico inquilino.
- Si la habitaci�n seleccionada tiene una sola persona, su l�nea de reparto se autocompleta con el importe total.
- Se a�ade indicador visual de reparto (splitRemainingTv):
  - rojo cuando falta importe por repartir o hay exceso,
  - verde cuando el reparto est� completo y listo para guardar.


## 2026-05-26
- Tipo: Avance reconstruido
- Pesta�a Movimientos: el CTA principal cambia su texto a Nueva acci�n (antes Nuevo gasto).


## 2026-05-27
- Tipo: Avance reconstruido
- Nuevo gasto/cobro: ajuste del reparto por habitaci�n para escenarios con un �nico residente seleccionado.
- Si solo hay una persona en la habitaci�n, se oculta el selector de persona y la fila pasa a modo autom�tico con esa persona fija.
- El importe de esa �nica persona se autocompleta con el total del gasto para evitar repartos manuales innecesarios.
- Para habitaciones con dos o m�s personas, se mantiene el flujo actual y se mejora el indicador de c�lculo bajo las filas:
  - muestra asignado/total,
  - avisa en rojo si falta importe o si se supera,
  - confirma en verde cuando el reparto es correcto.


## 2026-05-27
- Tipo: Avance reconstruido
- Informaci�n de piso: se elimina la duplicidad del nombre en el modal de detalle.
- El t�tulo del modal pasa a ser fijo (`Datos del piso`) y el nombre del piso se mantiene solo en la tarjeta principal del contenido.


## 2026-05-27
- Tipo: Avance reconstruido
- Seeder Firebase Admin: ampliado el sistema de presets para soportar m�ltiples escenarios de demo.
- A�adidos dos presets nuevos:
  - `sergio-owner-plus`: Sergio como propietario con m�s habitaciones y m�s volumen de datos.
  - `sergio-tenant-plus`: Sergio como inquilino en un piso con m�s compa�eros.
- Actualizado `tools/firebase-admin-seed/package.json` con scripts directos para ejecutar ambos presets.
- Actualizado `tools/firebase-admin-seed/README.md` con comandos y detalles de uso de los nuevos presets.


## 2026-05-27
- Tipo: Avance reconstruido
- Corregido scroll en el modal de `Registrar pago pendiente`.
- El formulario de pago ahora usa contenedor desplazable y ajuste de ventana para teclado, igual que el patr�n de `Nuevo gasto`.
- Se evita el bloqueo al deslizar hacia abajo en formularios largos de pago.


## 2026-05-27
- Tipo: Avance reconstruido
- Limpieza de mojibake y normalizaci�n de codificaci�n en archivos de UI y l�gica.
- Corregidos textos visibles con caracteres corruptos en:
  - `ExpensesFragment.java`
  - `GroupsFragment.java`
  - `fragment_groups.xml`
- Verificaci�n final: sin coincidencias de patrones de mojibake en archivos de texto del proyecto.


## 2026-05-27
- Tipo: Avance reconstruido
- Detalle de gastos: se a�ade el campo `Para` justo debajo de `Pagado por`.
- El campo `Para` se construye a partir del `customSplit` del gasto para mostrar a qui�n va el reparto.


## 2026-05-27
- Tipo: Avance reconstruido
- Flujo `Pagar pendiente`: bloqueo de edici�n del destinatario y del tipo de destino cuando el pago viene de una deuda pendiente.
- En `Registrar pago pendiente` ya no se pueden a�adir/quitar l�neas ni cambiar a qui�n se debe pagar.
- El guardado fuerza por c�digo el destino `Miembro` al acreedor del pendiente para evitar cambios manuales.


## 2026-05-27
- Tipo: Avance reconstruido
- Pagos pendientes: endurecimiento de validaciones y claridad de estados.
- OCR ya no sobreescribe el importe cuando el pago nace de una deuda pendiente bloqueada.
- Validaci�n estricta antes de guardar: una sola l�nea, mismo acreedor y mismo importe al c�ntimo.
- El estado `submitted` pasa a mostrarse como `En revisi�n` y deja de normalizarse como `Pendiente`.
- En detalle de pagos, el CTA cambia de `Editar` a `Cambiar estado` para evitar confusi�n de UX.
- Selector de pendientes ampliado: ahora lista todos y a�ade `A <acreedor>` en cada opci�n.
- Recordatorios: permisos alineados en UI y `firestore.rules` para que solo creador o propietario puedan actualizar/eliminar.


## 2026-05-27
- Tipo: Hotfix UX
- Nuevo gasto: corregido bloqueo de los desplegables de habitaciones e inquilinos en el modal.
- Causa: el wrapper scrollable del di�logo interceptaba toques globales y afectaba la apertura de `Spinner`.
- Soluci�n: retirada la interceptaci�n t�ctil global en `ExpenseDialogs.wrapFormForDialogScroll`.


## 2026-05-27
- Tipo: Ajuste visual de autenticaci�n
- Login y Registro: acciones principales (`Iniciar sesi�n` y `Crear cuenta`) pasan a estilo enlace azul subrayado, sin fondo tipo bot�n.
- Se unifica el estilo de enlaces con `�Olvidaste tu contrase�a?`.
- Fondo en modo claro para pantallas de acceso (`activity_login` y `activity_register`): blanco plano sin degradado.


## 2026-05-27
- Tipo: Localizaci�n de correos Auth
- Se a�ade `AuthEmailLocale` para aplicar idioma de Ajustes (`es`/`en`) a Firebase Auth antes de enviar emails.
- Cobertura en:
  - verificaci�n tras registro,
  - reenv�o de verificaci�n,
  - reset de contrase�a,
  - verificaci�n disparada desde login.


## 2026-05-27
- Tipo: Ajuste de registro
- Campo `Fecha de nacimiento` en `RegisterActivity`: se inicializa con la fecha actual en formato `YYYY-MM-DD`.


## 2026-05-27
- Tipo: UX de validaciones Auth
- Login y Registro: mensajes de error m�s claros en espa�ol para campos obligatorios (correo/usuario y contrase�a).
- Se a�ade mapeo de errores Firebase Auth a textos comprensibles en espa�ol (correo inv�lido, contrase�a incorrecta, usuario no encontrado, red, etc.).


## 2026-05-27
- Tipo: Simplificaci�n de identidad de usuario
- Se elimina el uso de `username`/apodo en autenticaci�n y registro.
- Registro: ahora solicita y guarda solo `nombre completo` como identidad visible (sin alias).
- Login: pasa a correo electr�nico + contrase�a (sin resoluci�n por colecci�n `usernames`).
- Sync de perfil: `users.displayName` y `users.fullName` quedan alineados con nombre completo.
- Ajustes de borrado de cuenta y reglas/documentaci�n actualizados para no depender de `usernames`.


## 2026-05-27
- Tipo: Propagaci�n de nombre de perfil
- Al editar el nombre completo en perfil, se a�ade sincronizaci�n transversal en Firestore.
- Se consultan colecciones por `uid` y/o `email` y se actualizan campos de nombre existentes (`displayName`, `fullName`, `name` y derivados) sin crear campos nuevos.


## 2026-05-27
- Tipo: Validaci�n de edici�n de perfil
- Se bloquea el guardado de `Editar perfil` cuando alg�n campo obligatorio queda vac�o (nombre, tel�fono o fecha de nacimiento).


## 2026-05-27
- Tipo: Ajuste UX de teclado en registro
- `RegisterActivity`: activado `windowSoftInputMode=adjustResize` y ajuste del contenedor del formulario a `wrap_content`.
- Resultado: al escribir la contrase�a en `Crear cuenta`, el teclado ya no tapa el campo y el scroll se comporta correctamente.


## 2026-05-27
- Tipo: Consistencia UX de teclado en autenticaci�n
- Se aplica `windowSoftInputMode=adjustResize` tambi�n en `LoginActivity` y `VerifyEmailActivity`.
- Objetivo: mismo comportamiento de visibilidad de campos con teclado en todas las pantallas de acceso.

## 2026-05-27
- Tipo: Nueva acci�n contextual en Pisos
- `GroupsFragment`: al mantener pulsado un piso se abre men� contextual por rol.
- Inquilino: acci�n `Salir del piso` con confirmaci�n, actualizando `members`, `memberEmails` y `roles`.
- Propietario: acci�n `Expulsar inquilino`, selecci�n del miembro y confirmaci�n antes de actualizar Firestore.
- Si el usuario sale del piso activo, se limpia `SessionStore` (`currentGroup/currentRoom`) para evitar referencias colgantes.

## 2026-05-27
- Tipo: Ajuste UI en detalle de piso
- Eliminado el bot�n `Gestionar habitaciones` del panel de detalle de `Pisos`.
- Motivo: evitar duplicidad, ya que la misma gesti�n existe en el acceso principal del workspace.

## 2026-05-27
- Tipo: Endurecimiento de ocupaci�n en pisos/habitaciones
- `GroupsFragment`: se bloquea el borrado de piso cuando alguna habitaci�n tiene residentes (`memberEmails` no vac�o).
- `OwnerRoomsActivity`: se bloquea borrar una habitaci�n ocupada y se obliga a vaciarla antes (quitar/cambiar inquilinos).
- `GroupsFragment`: al salir o expulsar un inquilino del piso, se limpia autom�ticamente su asignaci�n en `rooms_groups` del mismo piso (`memberEmails/memberCount`).
- `firestore.rules`: borrado en `rooms_groups` permitido solo para propietario y solo si la habitaci�n est� vac�a.

## 2026-05-27
- Tipo: Mejora de reparto en Nuevo gasto
- `dialog_expense.xml`: a�adido selector `Repartir por` con opciones `Personas/Habitaciones`.
- `ExpensesFragment`: nuevo modo de reparto por habitaci�n en el bloque de l�neas.
- En modo `Habitaciones`, cada l�nea representa una habitaci�n y su importe se divide autom�ticamente entre los inquilinos de esa habitaci�n al construir `customSplit`.
- El reparto sigue persistiendo por persona en Firestore, manteniendo compatibilidad con balances y vencimientos actuales.

## 2026-05-27
- Tipo: Mejora de reparto en Nuevo cobro/pago
- `ExpensesFragment`: las l�neas de destino de pago (`Miembro/Habitaci�n`) pasan a usar fila con selector + importe por l�nea.
- En `Miembro`, cada l�nea genera pago directo con el importe indicado.
- En `Habitaci�n`, cada l�nea reparte el importe entre residentes de la habitaci�n seg�n su porcentaje/configuraci�n de habitaci�n.
- Validaci�n previa de guardado: suma de l�neas = importe total del pago (excepto flujo bloqueado de `Pagar pendiente`).

## 2026-05-27
- Tipo: Saneamiento de codificaci�n UTF-8
- Eliminado BOM en archivos fuente y documentaci�n para evitar errores de compilaci�n (`\ufeff`).
- Corregido texto mojibake y caracteres de reemplazo (`?`) en Java y XML de UI.
- Validaci�n final: sin coincidencias de `�`, `�` o `?` en `app/src/main`.

## 2026-05-27
- Tipo: Ajuste UX cr�tico en Nuevo gasto
- Se elimina visualmente la secci�n superior de selecci�n de habitaciones en `Nuevo gasto` para evitar duplicidad.
- La selecci�n queda solo en el bloque inferior (`Repartir por` + l�neas con `+/-` e importe a la derecha).
- En modo `Habitaciones` ya no se cargan todas de golpe: arranca con una l�nea y se a�aden/eliminan manualmente.
- Se mantiene guardado de reparto por persona, dividiendo cada l�nea de habitaci�n entre sus residentes.


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
- Caso objetivo: evitar que avisos importantes se pierdan al escribir o navegar entre pesta�as.

## 2026-05-28
- Tipo: Ajuste visual de Login
- Se redise�a `activity_login.xml` para mejorar legibilidad y equilibrio visual en m�vil.
- Cambios: fondo ligado al tema, bloque de login centrado verticalmente, jerarqu�a tipogr�fica reforzada y bot�n principal de inicio de sesi�n en estilo destacado.
- Se corrigen textos visibles con acentos y signos en espa�ol en la pantalla de login.

## 2026-05-28
- Tipo: Ajuste UX en Nuevo gasto (reparto por personas)
- En `ExpensesFragment`, el reparto por personas arranca con una sola fila tanto en `Reparto personalizado` como en `Reparto equitativo`.
- En ambos modos se permite a�adir/quitar personas con los botones `+/-`.
- En `Reparto equitativo` se a�ade feedback de c�lculo en vivo (`n personas x importe = total`) y aviso si hay menos de 2 personas.
- Al a�adir filas de personas, se prioriza sugerir miembros no repetidos para evitar duplicados involuntarios.

## 2026-05-28
- Tipo: Correccion de compilacion Android
- Se restauro el package com.sergio.flatshare.features.auth; en LoginActivity, RegisterActivity y VerifyEmailActivity tras una incidencia de codificacion.
- Se repuso import android.widget.Toast; en las pantallas que aun usan Toast.makeText(...) para evitar errores cannot find symbol Toast.
- Se validaron archivos Java en UTF-8 sin BOM para evitar fallos de caracteres ilegales.

## 2026-05-28
- Tipo: Correccion de textos UTF-8 y UI de registro
- Se corrigen textos visibles corruptos en autenticacion (tildes y ñ) en ctivity_login.xml, ctivity_register.xml, LoginActivity, RegisterActivity y VerifyEmailActivity.
- La pantalla Crear cuenta adopta el mismo estilo visual oscuro de Iniciar sesión.
- Se mejora el comportamiento con teclado en Crear cuenta para evitar que tape campos (scroll y espaciado inferior).

## 2026-05-28
- Tipo: Limpieza global de mojibake en textos visibles
- Se corrigen textos corruptos en ExpensesFragment (ej: 'Nueva acción', 'Gestión', 'Habitación', 'línea', '¿Deseas continuar?').
- Se valida pp/src/main sin secuencias corruptas (�, Ã, Â, ï¿½) y en UTF-8 sin BOM.
