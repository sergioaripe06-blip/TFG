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
