# Manual de usuario y operacion - FlatShareApp

## 1. Identificacion del proyecto
- Nombre: FlatShare
- Plataforma: Android nativo
- Paquete: `com.sergio.flatshare`
- Objetivo: gestionar pisos compartidos (gastos, pagos, recordatorios, calendario y alquiler).

## 2. Stack tecnico y versiones

### 2.1 Lenguaje y toolchain
- Lenguaje principal: Java
- Version Java de compilacion: 17
- Android Gradle Plugin: 8.5.2
- Gradle Wrapper: 9.0.0
- Build Tools: 36.1.0
- Refactor tecnico incremental aplicado:
  - los fragments conservan capa UI,
  - la logica de negocio/datos se mueve progresivamente a servicios por dominio.
  - servicios ya aplicados en pagos/categorias, grupos/invitaciones y modulos de contratos/cobros.

### 2.2 Android SDK
- `compileSdk`: 35
- `targetSdk`: 35
- `minSdk`: 24

### 2.3 Dependencias principales
- AndroidX Core `1.13.1`
- AppCompat `1.7.0`
- Material Components `1.12.0`
- ConstraintLayout `2.1.4`
- RecyclerView `1.3.2`
- SwipeRefreshLayout `1.1.0`
- Firebase BoM `34.12.0`
- Firebase Auth
- Firebase Firestore
- Firebase Analytics
- ML Kit Text Recognition `16.0.0`
- ZXing Android Embedded `4.3.0`
- ZXing Core `3.5.3`

## 3. Requisitos de ejecucion
- Android Studio con soporte AGP 8.5.x.
- JDK 17.
- Proyecto Firebase configurado.
- Archivo `app/google-services.json` en local.

## 4. Configuracion Firebase

### 4.1 Servicios usados
- Authentication: Email/Password.
- Firestore (Native mode).

### 4.2 Reglas
- Archivo fuente: `firestore.rules`.
- Publicar en Firebase Console > Firestore > Rules.
- En gestion de alquiler:
  - `rent_collections`, `maintenance_tickets` y `group_documents` permiten crear/leer a miembros del grupo.
  - Solo el propietario del piso puede actualizar o eliminar en esas tres colecciones.

### 4.3 Colecciones principales
- `users`, `usernames`, `groups`, `group_codes`, `rooms_groups`
- `expenses`, `payments`, `payment_deadlines`, `reminders`, `activity_logs`, `invitations`
- Gestion alquiler: `rental_contracts`, `rent_collections`, `maintenance_tickets`, `group_documents`, `audit_events`, `rent_automations`, `event_reminder_rules`, `event_reminder_jobs`

## 5. Arranque y navegacion general

### 5.1 Inicio de app
- `SplashActivity`
  - Crea canal de notificaciones `flatshare_reminders`.
  - Solicita permiso `POST_NOTIFICATIONS` (Android 13+).
  - Redirige a login o main según sesión.
  - Si detecta sesión sin correo verificado, cierra sesión y redirige a login.

### 5.2 Navegacion inferior (MainActivity)
- `Pisos` (`menu_groups`)
- `Balance` (`menu_personal_balance`)
- `Calendario` (`menu_calendar`)
- `Perfil` (`menu_profile`)

## 6. Manual funcional por pantalla

## 6.1 Autenticación

### Login (`LoginActivity`)
Controles:
- Campo `Usuario o email`.
- Campo `Contraseña`.
- Botón `Iniciar sesión`.
- Boton debug `Entrar con cuenta seed` (solo DEBUG).
- Enlace `Crear cuenta`.
- Enlace `¿Olvidaste tu contraseña?`.

Comportamiento:
- Login por email directo.
- Login por username (resolución en `usernames`).
- Recuperación de contraseña solo por correo (Firebase Auth).
- Si el correo no está verificado, no se permite entrar en la app y se redirige a la pantalla de verificación.

### Registro (`RegisterActivity`)
Controles:
- `Nombre completo`, `username`, `email`, `teléfono`, `fecha nacimiento`, `contraseña`.
- Botón `Crear cuenta`.
- Enlace `Ya tengo cuenta` (vuelta a login).

Comportamiento:
- Crea usuario en Auth.
- Envía correo de verificación (`sendEmailVerification`).
- Abre `VerifyEmailActivity` para reenvío y confirmación.
- Solo tras verificar el correo se completa el alta en `users` y `usernames`.

## 6.2 Pisos (`GroupsFragment`)

### Zona superior
- KPI `Pisos`.
- KPI `Miembros totales`.
- Boton debug `Seed debug` (solo DEBUG).
- Buscador de pisos por nombre o direccion (filtrado en tiempo real).

### Lista de pisos
- Tap en un piso: abre tarjeta de detalle.
- Si no hay coincidencias con la busqueda, se muestra mensaje especifico.

### Botones principales
- `Crear piso`.
- `Unirse`.

### Flujo Crear piso
- Formulario por pasos:
  - Paso 1: datos basicos (nombre, direccion, provincia/ciudad).
  - Paso 2: configuracion (habitaciones, modelo de alquiler, modo variable).
- Alta inicial de habitaciones obligatoria.

### Flujo Unirse a piso
- Opciones:
  - Escribir codigo.
  - Escanear QR.

### Tarjeta detalle de piso
Elementos:
- Nombre del piso.
- Datos descriptivos.
- Miembros.
- Botones `Editar` (owner), `Anadir`, `Gestionar habitaciones` (owner), `Eliminar piso` (owner).
- Los botones `Editar`, `Anadir` y `Gestionar habitaciones` se muestran centrados en la tarjeta de detalle.
- En `Editar` (solo propietario) se puede cambiar:
  - nombre y descripción,
  - modelo de alquiler (`fijo`/`variable`),
  - reparto del alquiler variable (`equitativo`/`porcentual`).

Anadir:
- Por codigo.
- Por QR.
- Por email (coleccion `invitations`).
- El destinatario debe iniciar sesion con ese mismo email para ver/aceptar la invitacion.
- Al entrar en la app, si hay invitaciones pendientes, se muestra un dialogo con datos del piso (propietario, miembros, ubicacion, codigo e invitador) y accion `Unirme` o `Rechazar`.

## 6.3 Habitaciones del propietario (`OwnerRoomsActivity`)
Controles clave:
- `Crear habitacion`.
- `A?adir por codigo`.
- `A?adir por email`.
- `Terminar`.
- En creacion/edicion de inquilinos (solo propietario):
  - seleccion por plaza (`Inquilino 1`, `Inquilino 2`, etc.) para decidir quien ocupa cada posicion;
  - si hay 2 o mas inquilinos, reparto `equitativo` o `porcentual`;
  - en porcentual, el ultimo inquilino se calcula automaticamente con el porcentaje restante.

Operaciones por habitacion:
- Asignar inquilino.
- Renombrar.
- Eliminar.
- Editar inquilinos (solo propietario).
- Configurar reparto por habitaci?n en alquiler fijo: equitativo o porcentual.

## 6.4 Workspace del piso (`ExpensesFragment`)

Tabs visibles:
- `Movimientos`.
- `Recordatorios`.
- `Gestion`.
- Posicion adaptativa en cabecera:
  - si el nombre del piso cabe en una linea, los 3 tabs se muestran a la derecha del titulo;
  - si el nombre del piso es largo y se solapa, los tabs bajan automaticamente justo debajo del titulo.

CTA principal inferior:
- Cambia segun tab (nuevo gasto, nuevo recordatorio, etc.).

### 6.4.1 Movimientos
Controles:
- Buscador de movimientos.
- Boton `Filtros`.
- Boton de gestion de contexto/habitacion.

Operaciones:
- Crear/editar/eliminar gasto (`expenses`).
- Crear/editar/eliminar pago (`payments`).
- Confirmar/rechazar pagos pendientes.
- Generacion de vencimientos (`payment_deadlines`).
- Registro de actividad (`activity_logs`).
- OCR de ticket para importe.
- Exportacion PDF resumen mensual.
- `Nuevo gasto` solo aparece en pisos de `alquiler variable`.

Campos de gasto:
- Concepto, importe, categoria, prioridad, fecha limite.
- En `alquiler variable`, la categoria es editable (texto libre) con sugerencias historicas del propio piso.
- Reparto (`customSplit`) equitativo o por importes.
- Habitacion(es) destino.
- Estado visual del flujo:
  - `Solicitado` (rojo): recien creado y aun sin justificantes enviados.
  - `Pendiente` (naranja): hay justificantes enviados y falta validacion.
  - `Pagado` (verde): validado/confirmado.
- Edicion y borrado: solo permitidos en estado `Solicitado`.

Campos de pago:
- Importe, concepto, categoria, prioridad, fecha limite.
- En `alquiler variable`, la categoria es editable (texto libre) y reutiliza sugerencias historicas al registrar nuevos pagos/gastos.
- Destino (habitacion, miembro o todos).
- Si el destino es `Habitaci?n`, se pueden seleccionar una o varias habitaciones (a?adiendo/quitar l?neas).
- En `alquiler fijo`, el reparto se calcula dentro de cada habitaci?n seleccionada:
  - con 1 residente, ese residente asume el 100% de su habitaci?n;
  - con 2 o m?s residentes, puede ser equitativo o porcentual (configurable por habitaci?n).
- Si el destino es `Miembro`, se pueden anadir o quitar lineas de destinatario (minimo 1).
- Estado visual del pago:
  - `Solicitado` (rojo),
  - `Pendiente` (naranja),
  - `Pagado` (verde).
- Edicion y borrado: solo permitidos en estado `Solicitado`.

Flujo de inquilino con deuda (`alquiler fijo` y `alquiler variable`):
- En `Movimientos` se muestran sus pagos pendientes (`payment_deadlines`) como lista accionable.
- Cada item muestra concepto, importe, fecha limite, destinatario y estado pendiente.
- Al pulsar un pendiente, se abre `Registrar pago pendiente` con importe/concepto/fecha/destino precargados y bloqueados.
- `Adjuntar foto` del justificante es obligatorio para enviar el pago.
- El boton `Enviar pago` solo se habilita cuando hay justificante adjunto.
- Si no hay deuda, se muestra `No tienes pagos pendientes`.
- En el dialogo de acciones, para inquilino variable el acceso a pago general queda desactivado cuando no existen pendientes.

### 6.4.2 Recordatorios
Operaciones:
- Crear recordatorio (`reminders`).

Configuracion:
- Titulo/concepto.
- Fecha inicio.
- Frecuencia: unico, diario, semanal, mensual, personalizado.
- Fecha fin opcional.
- Destinatarios:
  - X miembros.
  - X habitaciones.
  - Todos los inquilinos.

Campos importantes guardados:
- `targetType`, `targetEmails`, `interval`, `intervalDays`, `startAt`, `endAt`, `reminderCode`.

### 6.4.3 Gestion (RentalManagementFragment + TenantsFragment)
Modulos:
- Contrato (`rental_contracts`).
- Cobros (`rent_collections`).
- Incidencias (`maintenance_tickets`).
- Documentos (`group_documents`).
- Auditoria (`audit_events`).
- Automatizacion (`rent_automations`).
- Reglas push (`event_reminder_rules`, `event_reminder_jobs`).

Operaciones:
- Altas, ediciones, estados, historicos y trazabilidad por modulo.

## 6.5 Balance personal (`PersonalBalanceFragment`)
- Selector de piso (o todos).
- Grafico circular por categorias de gasto/pago.
- Categorias dinamicas: si se crean nuevas (por ejemplo, `luz`, `agua`), aparecen automaticamente en el balance.
- Color estable por categoria: la leyenda y el grafico usan el mismo color para cada categoria.
- Barras mensuales de importe pagado.

## 6.6 Calendario (`CalendarFragment`)
- Calendario mensual.
- Lista de eventos en fecha seleccionada.
- Fuentes:
  - `payment_deadlines` (deudor actual).
  - `payments` emitidos/recibidos.
  - `reminders`.

## 6.7 Perfil (`ProfileFragment`)
Controles:
- Cambio de foto.
- Abrir ajustes.
- `Editar perfil`.

Campos perfil:
- Nombre completo, telefono, email, fecha nacimiento.

Persistencia:
- Firestore `users`.
- `UserProfileChangeRequest` en Auth para nombre/foto.

## 6.8 Ajustes (`SettingsFragment`)
Controles:
- Switch modo oscuro.
- Switch notificaciones.
- Selector idioma ES/EN.
- Boton `Borrar cuenta`.
- Boton `Cerrar sesion` al final de la pantalla.

Efecto:
- Persistencia en `SettingsStore`.
- Aplicacion de tema e idioma en runtime.
- Borrado de cuenta:
  - exige validación previa con correo y contraseña de la cuenta activa,
  - elimina `users/{uid}` y `usernames/{username}`,
  - elimina invitaciones donde el usuario invita o fue invitado,
  - saca al usuario de `groups.members`, `groups.memberEmails` y `groups.roles`,
  - si era propietario y hay otros miembros, transfiere `ownerId` al primer miembro restante,
  - no borra historicos de pagos/gastos existentes.
  - el boton de `Borrar cuenta` usa color rojo fijo en modo claro y oscuro.

### Ajuste visual del modo claro
- Se incrementa contraste general en fondos y superficies.
- La barra inferior deja de ser transparente y usa contenedor propio para mejorar legibilidad.
- Iconos y textos seleccionados de la barra inferior se muestran en color primario para evitar perdida de contraste.

### Sonidos personalizados de acciones
- Puedes anadir sonidos propios en `app/src/main/res/raw` con estos nombres:
  - `sfx_group_created` -> suena al crear un piso correctamente.
  - `sfx_expense_accepted` -> suena al marcar un pago/gasto como aceptado (estado `confirmed`).
- Formatos recomendados: `wav` o `mp3` cortos (100-500 ms) para respuesta limpia.
- Si no existe el archivo, la app usa un tono breve de confirmacion como respaldo.

### Ajuste UX en pagos (alquiler fijo)
- En `Registrar pago`, cuando el piso es de alquiler fijo:
  - la categoria ya no aparece como desplegable bloqueado,
  - se muestra como valor fijo de solo lectura (`Alquiler`) con texto explicativo.

## 7. Seeder de datos de prueba
Ruta: `tools/firebase-admin-seed`

Crea:
- Usuarios reales en Auth.
- Estructura Firestore de usuarios, pisos y habitaciones.
- Datos financieros de prueba (`expenses`, `payments`, `payment_deadlines`).
- Recordatorios (`reminders`).
- Incluye preset hardcodeado `sergio-demo` con owner fijo `sergioaripe06@gmail.com` y datos completos de demo.

## 8. Inventario de colecciones de negocio
Ver detalle completo en:
- `FIREBASE_SETUP.md`
- `firestore.rules`

## 8.1 Conectividad Android (requisito)
- La app necesita permisos de red en `AndroidManifest.xml`:
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
- Si faltan, pueden aparecer errores de networking como timeouts, fallos de Wi-Fi o imposibilidad de conectar con Firebase.

## 9. Limitaciones actuales conocidas
- Hay textos antiguos con codificacion danada en algunos archivos legacy; se corrigen progresivamente al tocar cada modulo.
- Advertencia actual de AGP con `compileSdk 35` en AGP `8.5.2` (no bloqueante).

## 10. Procedimiento de actualizacion del manual
En cada tarea nueva:
1. Anadir pantalla/flujo nuevo o cambio de comportamiento.
2. Actualizar stack/versiones si cambian Gradle o dependencias.
3. Actualizar Firebase si se crean campos/colecciones nuevas.
4. Registrar el cambio en `diary.md`.

## 11. Control de versiones (Git)
- El repositorio ignora caches y artefactos locales para mantener commits limpios.
- No deben versionarse archivos de entorno local como:
  - `.gradle-home/`
  - `.idea/`
  - `crash.txt` y logs temporales
- Si alguno de esos archivos ya estaba trackeado, debe retirarse del indice con `git rm --cached` sin borrarlo en local.
