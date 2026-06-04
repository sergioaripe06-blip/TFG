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
  - `maintenance_tickets` permite crear, leer, actualizar y eliminar a miembros del grupo.
  - `group_schedules` permite lectura a miembros del grupo, pero solo el propietario puede crear, editar y eliminar.
  - `house_rules` se lee por miembros y lo gestiona el propietario.

### 4.3 Colecciones principales
- `users`, `groups`, `group_codes`, `rooms_groups`
- `expenses`, `payments`, `payment_deadlines`, `reminders`, `activity_logs`, `invitations`
- Gestion del piso: `maintenance_tickets`, `house_rules`, `group_schedules`, `audit_events`

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
- Campo `Correo electrónico`.
- Campo `Contraseña`.
- Botón `Iniciar sesión`.
- Boton debug `Entrar con cuenta seed` (solo DEBUG).
- Enlace `Crear cuenta`.
- Enlace `¿Olvidaste tu contraseña`.

Comportamiento:
- Login por email directo.
- Login por correo electrónico (Firebase Auth).
- Recuperación de contraseña solo por correo (Firebase Auth).
- Si el correo no está verificado, no se permite entrar en la app y se redirige a la pantalla de verificación.
- Si `Recordarme` está desactivado y el usuario abandona la app, al volver deberá iniciar sesión de nuevo.
- Si `Recordarme` está activado, la sesión se mantiene entre aperturas de la app.

### Registro (`RegisterActivity`)
Controles:
- `Nombre completo`, `email`, `teléfono`, `fecha nacimiento`, `contraseña`.
- Botón `Crear cuenta`.
- Enlace `Ya tengo cuenta` (vuelta a login).
- Consulta y aceptación obligatoria de `Términos y condiciones de uso`.

Comportamiento:
- Crea usuario en Auth.
- Envía correo de verificación (`sendEmailVerification`).
- Abre `VerifyEmailActivity` para reenvío y confirmación.
- Solo tras verificar el correo se completa el alta en `users`.

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
- En acciones de personas (por ejemplo `Expulsar inquilino`), la app muestra `Nombre` y debajo `correo` para identificar bien a cada miembro.

## 6.3 Habitaciones del propietario (`OwnerRoomsActivity`)
Controles clave:
- `Crear habitacion`.
- `Aadir por codigo`.
- `Aadir por email`.
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
- Configurar reparto por habitacin en alquiler fijo: equitativo o porcentual.

## 6.4 Workspace del piso (`ExpensesFragment`)

Tabs visibles:
- `Movimientos`.
- `Recordatorios`.
- `Gestion`.
- Tamaño de botones de tabs aumentado para mejorar legibilidad y área táctil.
- Cuando los tabs bajan debajo del título, se expanden en 3 columnas iguales ocupando todo el ancho disponible.
- El nombre del piso en cabecera se muestra centrado y con tamaño ligeramente mayor para dar prioridad visual.
- Posicion adaptativa en cabecera:
  - si el nombre del piso cabe en una linea, los 3 tabs se muestran a la derecha del titulo;
  - si el nombre del piso es largo y se solapa, los tabs bajan automaticamente justo debajo del titulo;
  - cuando bajan debajo del titulo, se muestran centrados horizontalmente.

CTA principal inferior:
- Cambia segun tab (nuevo gasto, nuevo recordatorio, etc.).
- El botón principal muestra el texto de acción dentro del propio botón (sin icono `+` ni etiqueta inferior separada).

### 6.4.1 Movimientos
Controles:
- Buscador de movimientos.
- Boton `Filtros`.
- Boton de gestion de contexto/habitacion.

Operaciones:
- Crear/editar/eliminar gasto (`expenses`).
- Crear/editar/eliminar pago (`payments`).
- Confirmar justificantes pendientes y aceptar pagos o confirmaciones según su origen.
- Generacion de vencimientos (`payment_deadlines`).
- Registro de actividad (`activity_logs`).
- OCR de ticket para importe.
- Exportacion PDF resumen mensual.
- El PDF de resumen usa ahora una presentacion visual cuidada: cabecera centrada, tarjetas por movimiento, paginacion y colores diferenciados por estado.
- Se ha aumentado la tipografia y el espaciado interno del PDF para que el contenido sea mas legible sin perder el estilo visual.
- Exportacion PDF compatible:
  - Android 10+ (`API 29+`): guarda en Descargas del dispositivo.
  - Android 7-9 (`API 24-28`): guarda en almacenamiento externo privado de la app.
- `Nuevo gasto` aparece tanto en pisos de `alquiler variable` como de `alquiler fijo`.

Campos de gasto:
- Concepto, importe, categoria, prioridad, fecha limite.
- La categoria es editable (texto libre) y muestra sugerencias historicas del propio piso.
- Reparto (`customSplit`) equitativo o por importes.
- Habitacion(es) destino.
- Estado visual del flujo:
  - `Solicitado` (rojo): recien creado y aun sin justificantes enviados.
  - `Pendiente` (naranja): hay justificantes enviados y falta validacion.
  - `Pagado` (verde): validado/confirmado.
- Edicion y borrado: solo permitidos en estado `Solicitado`.
- El detalle de gasto se muestra en tarjetas de datos (etiqueta/valor), evitando texto corrido.

Campos de pago:
- Importe, concepto, categoria, prioridad, fecha limite.
- La categoria es editable (texto libre) en todos los modelos de alquiler.
- Mientras escribes aparecen sugerencias historicas del mismo piso, combinando movimientos del balance y categorias ya usadas.
- Destino (habitacion, miembro o todos).
- Si el destino es `Habitacin`, se pueden seleccionar una o varias habitaciones (aadiendo/quitar lneas).
- En `alquiler fijo`, el reparto se calcula dentro de cada habitacin seleccionada:
  - con 1 residente, ese residente asume el 100% de su habitacin;
  - con 2 o ms residentes, puede ser equitativo o porcentual (configurable por habitacin).
- Si el destino es `Miembro`, se pueden anadir o quitar lineas de destinatario (minimo 1).
- Estado visual del pago:
  - `Solicitado` (rojo),
  - `Pendiente` (naranja),
  - `Pagado` (verde).
- Edicion y borrado: solo permitidos en estado `Solicitado`.
- Cambio manual de estado del pago: solo permitido mientras está en `Solicitado`.
- Si un pago ya está en `Pendiente`, `En revisión` o `Pagado`, deja de poder borrarse o editarse.
- No se permite crear un gasto dirigido solo al propio pagador; debe haber al menos otro destinatario real en el reparto.
- Las categorías al crear gasto o pago se sugieren a partir del historial del mismo piso, no de otros pisos.
- Si vuelves a escribir la misma categoría (aunque cambien mayúsculas o espacios), se reutiliza la misma clave lógica para mantener el balance agrupado; si escribes una distinta, se crea una nueva categoría para ese piso.
- En cada gasto solicitado se indica de forma clara si, para tu usuario, `tienes que pagarlo tú`.
- Cuando un gasto solicitado te corresponde como deudor, la fila muestra el aviso `(Este gasto es para ti)`.

Flujo de inquilino con deuda (`alquiler fijo` y `alquiler variable`):
- En `Movimientos` se muestran sus pendientes por confirmar (`payment_deadlines`) como lista accionable.
- Cada item muestra concepto, importe, fecha limite, destinatario y estado pendiente.
- Si el pendiente nace de un `gasto`, ese gasto sigue siendo el origen y se crea una deuda individual por cada deudor según `customSplit`; no se duplica el gasto principal.
- Al pulsar un pendiente, se abre `Confirmar gasto pendiente` o `Registrar pago pendiente` con importe/concepto/fecha/destino precargados y bloqueados según el origen.
- Si eres propietario del piso pero ese gasto te ha sido dirigido a ti como deudor, la app prioriza igualmente el flujo de `Confirmar gasto pendiente`: no te deja editar ni borrar ese gasto desde su detalle.
- `Adjuntar foto` del justificante es obligatorio para enviar la confirmación o el pago.
- El botón `Enviar pago` o `Enviar confirmación` solo se habilita cuando hay justificante adjunto.
- Si no hay deuda, se muestra `No tienes pendientes por confirmar`.
- En el dialogo de acciones ya no existe un pago libre separado: el flujo principal parte siempre de `Nuevo gasto` y, si debes dinero, de `Confirmar pendiente`.
- La ventana de pago pendiente se simplifica ocultando controles que no aplican al caso y evita dobles envíos: al pulsar enviar, el botón se bloquea hasta terminar la operación.
- Si una deuda ya existe para ese gasto, la app la reutiliza en lugar de crear otra nueva al abrir la pantalla de justificante.
- Cuando el deudor envía el justificante, la deuda pasa a revisión (`submitted`) y el gasto padre pasa a `Pendiente`.
- La aceptación final del pago pendiente la puede hacer el propietario del piso o el acreedor del pago (`toEmail`), que en una deuda nacida desde gasto coincide con quien adelantó ese gasto.
- Al crear o editar un gasto, ya no es obligatorio que el piso tenga habitaciones creadas: el reparto sigue funcionando por personas y la habitación queda solo como dato opcional cuando existe.
- Si el gasto antiguo no tenía aún una deuda técnica creada, la app intenta reconstruirla antes de abrir el flujo de justificante.
- Si se vence la fecha límite de un gasto o de un pago, la app lanza una notificación local avisando del vencimiento para que se contacte con la otra parte y se resuelva el plazo.

### 6.4.2 Recordatorios
Operaciones:
- Crear recordatorio (`reminders`).
- Visual: en la lista se muestran en blanco neutro (sin código rojo/amarillo/verde por estado).
- Redacción mejorada en lista y detalle: se muestra destino con lenguaje natural (`Por miembro`, `Por miembros`, `Por habitación`, `Por habitaciones`).
- En detalle de recordatorio, los datos se presentan en filas visuales (etiqueta/valor) para mejorar lectura y distribución.
- Estructura del detalle:
  - `Nombre del piso`,
  - bloque `Dirigido a ...` con elementos en viñetas (uno por línea),
  - `Frecuencia`,
  - fila final con `Desde` y `Hasta` en la misma línea (dos tarjetas).

Configuracion:
- Titulo/concepto.
- Fecha inicio.
- Fecha final opcional para limitar la recurrencia.
- La fecha `Hasta` se respeta tambien en `Calendario`: un recordatorio diario con mismo inicio y fin solo aparece ese dia y no se arrastra a fechas posteriores.
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
- Incidencias (`maintenance_tickets`).
- Reglas (`house_rules`).
- Horarios (`group_schedules`).

Operaciones:
- Altas y ediciones desde la propia pestaña de gestión.
- `Incidencias`: título, zona, responsable, estado, costes y descripción.
- Los campos de coste en `Incidencias` admiten importes con coma o con punto, por ejemplo `17,66` o `17.66`.
- `Reglas`: norma para todo el piso, una persona concreta o una habitación.
- `Horarios`: reserva recurrente con frecuencia, rango de fechas, hora inicio y hora fin.
- `Horarios` normaliza y guarda las horas en formato `HH:MM`, por ejemplo `17:00`, `18:00` o `19:00`, aunque se escriban sin cero inicial o con separador alternativo.
- En `Horarios`, las fechas se muestran y se guardan en formato español `DD/MM/AAAA`; si editas datos antiguos, la app sigue aceptando valores heredados en `AAAA-MM-DD` y los normaliza al abrirlos.
- En `Horarios`, solo el propietario puede crear, editar o eliminar.
- El resto de miembros puede abrir el detalle y consultarlo, pero no modificarlo.
- Los selectores de persona muestran `Nombre` en primera línea y `correo` en segunda línea cuando el formulario lo usa.
- Las tarjetas de gestión separan título, contexto y detalle en bloques distintos, y muestran el dato lateral en formato pastilla para mejorar lectura.
- La zona superior y las filas de `Gestión` usan tarjetas propias con más jerarquía visual y mejor contraste en modo claro.

## 6.5 Balance personal (`PersonalBalanceFragment`)
- Selector de piso (o todos).
- Grafico circular por categorias de gasto/pago.
- El gráfico circular usa estilo donut con separaciones suaves entre categorías y total en el centro para lectura rápida.
- Categorias dinamicas: si se crean nuevas (por ejemplo, `luz`, `agua`), aparecen automaticamente en el balance.
- Color estable por categoria: la leyenda y el grafico usan el mismo color para cada categoria.
- Barras mensuales de importe pagado.

## 6.6 Calendario (`CalendarFragment`)
- Calendario mensual.
- Lista de eventos en fecha seleccionada.
- Números de los días con texto claro en calendario (incluido el día actual cuando no está seleccionado).
- Etiqueta de fecha seleccionada en formato `dd/MM/yyyy` (por ejemplo `26/05/2026`).
- Ajuste global de tema para asegurar legibilidad del día actual en `CalendarView` en distintos dispositivos.
- Colores del calendario desacoplados del tema general (`calendar_day_text` y `calendar_day_text_muted`) para evitar que el día actual herede tonos oscuros.
- Fuentes:
  - `payment_deadlines` (deudor actual).
  - `payments` emitidos/recibidos.
  - `reminders`.
  - `group_schedules` para horarios recurrentes del piso.
- Los horarios se muestran con un color propio para distinguirlos de pagos y recordatorios.

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
  - elimina `users/{uid}`,
  - elimina invitaciones donde el usuario invita o fue invitado,
  - saca al usuario de `groups.members`, `groups.memberEmails` y `groups.roles`,
  - si era propietario y hay otros miembros, transfiere `ownerId` al primer miembro restante,
  - no borra historicos de pagos/gastos existentes.
  - el boton de `Borrar cuenta` usa color rojo fijo en modo claro y oscuro.

### Ajuste visual del modo claro
- Se incrementa contraste general en fondos y superficies.
- La barra inferior deja de ser transparente y usa contenedor propio para mejorar legibilidad.
- El elemento activo del menu inferior mantiene alto contraste y los elementos inactivos usan un morado apagado mas legible sobre fondo crema.
- La paleta clara pasa a una base crema con acentos morados en botones y elementos activos.
- Inputs, selectores y tarjetas refuerzan bordes y rellenos claros para que no se pierdan en modo claro.
- Las tarjetas, chips y superficies auxiliares usan blancos y cremas suaves para dar mas volumen sin tocar el modo oscuro.

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
- Incluye presets de demo:
  - `sergio-demo` (base),
  - `sergio-owner-plus` (Sergio como propietario con más habitaciones),
  - `sergio-tenant-plus` (Sergio como inquilino con más compañeros),
  - `sergio-owner-homoerectus` (Sergio como propietario y `homoerectus079@gmail.com` como inquilino real).

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
- Se corrigió la codificación dañada en `GroupsFragment` (textos visibles de creación/gestión de piso).
- Si aparece texto legacy corrupto en otros módulos, debe corregirse al tocar ese archivo para mantener `UTF-8` limpio.
- Advertencia actual de AGP con `compileSdk 35` en AGP `8.5.2` (no bloqueante).

## 10. Procedimiento de actualizacion del manual
En cada tarea nueva:
1. Anadir pantalla/flujo nuevo o cambio de comportamiento.
2. Actualizar stack/versiones si cambian Gradle o dependencias.
3. Actualizar Firebase si se crean campos/colecciones nuevas.
4. Registrar el cambio en `diary.md`.

- Las portadas de los documentos HTML incluyen ya los datos del autor: Sergio Ariño Pérez, DAM Superior, Segundo año.

## 11. Control de versiones (Git)
- El repositorio ignora caches y artefactos locales para mantener commits limpios.
- No deben versionarse archivos de entorno local como:
  - `.gradle-home/`
  - `.idea/`
  - `crash.txt` y logs temporales
- Si alguno de esos archivos ya estaba trackeado, debe retirarse del indice con `git rm --cached` sin borrarlo en local.

### Ajuste visual (2026-05-26) - Movimientos
- El boton principal inferior (por ejemplo, Nuevo gasto) se posiciona mas cerca del menu inferior para mejorar la ergonomia.
- La lista de gastos/pagos respeta un inset inferior dinamico para que las ultimas filas no queden tapadas por el bloque de accion.


### Ajuste de usabilidad (2026-05-26) - Nuevo gasto
- En el formulario de Nuevo gasto, al cerrar el teclado el contenido vuelve a desplazarse correctamente.
- El selector de habitaciones en Nuevo gasto mantiene apertura estable del desplegable en dispositivos donde antes fallaba al tocarlo.


### Ajuste global (2026-05-26) - Dialogos con formulario
- Los dialogos de formulario de la app comparten ahora la misma gestion de teclado y redimensionado de ventana.
- Al tocar fuera de un campo, se cierra foco/teclado y se mantiene el desplazamiento vertical estable.


### Ajuste correctivo (2026-05-26) - Selectores en Nuevo gasto
- Corregida la seleccion de Habitacion(es) y de personas en Tipo de reparto dentro de Nuevo gasto cuando se usan desplegables.
- Se mantiene la mejora de desplazamiento del formulario tras cerrar teclado.


### Registro (2026-05-26) - Correo duplicado
- Si intentas crear una cuenta con un correo ya registrado, la app bloquea el alta y muestra el aviso: Ese correo ya tiene una cuenta registrada.


### Pisos (2026-05-26) - Buscador integrado
- El campo Buscar piso por nombre o direccion ahora aparece dentro de la tarjeta/listado de pisos, en la zona superior.


### Workspace (2026-05-26) - Ventana de piso ampliada
- Al mantener pulsado el nombre del piso, la ventana muestra: propietario, modelo de reparto y listado de habitaciones.
- En Habitaciones, cada tarjeta permite abrir acciones de: Ver informacion, Editar habitacion, Eliminar habitacion (segun permisos).
- En la parte inferior de la ventana aparecen los botones Añadir habitación y Ver ubicación.


### Calidad de texto (2026-05-26)
- Se corrigieron textos con codificación incorrecta en pantallas de Workspace y Pisos.
- Todos los mensajes visibles revisados mantienen acentos y eñe en UTF-8.


### Movimientos (2026-05-26) - Detalle de pagos
- En pagos (solicitado, pendiente, pagado), la opcion Ver detalle abre ahora una ventana de detalle en formato tarjeta/modal (no notificacion).


### Pagos (2026-05-26) - Apertura directa de detalle
- Al tocar un pago, se abre directamente su ventana de detalle.
- En estados Solicitado y Pendiente, aparecen botones abajo para Editar y Borrar (si tienes permiso).
- En estado Pagado, solo se permite visualizar el detalle.


### Nuevo gasto / cobro (2026-05-26) - Ayuda de reparto
- En Tipo de reparto, cuando la habitación elegida tiene un único inquilino, su importe se rellena automáticamente con el total.
- Debajo del reparto aparece un indicador de estado:
  - rojo: falta por repartir o te has pasado,
  - verde: reparto completo y listo para guardar.


### Nuevo gasto / cobro (2026-05-27) - Reparto por habitación refinado
- Si la habitación seleccionada tiene una sola persona:
  - no aparece el selector de persona,
  - se muestra la persona fija de esa habitación,
  - el importe queda autocompletado con el total del gasto.
- Si la habitación tiene dos o más personas:
  - se mantiene el comportamiento de reparto actual,
  - aparece un cálculo bajo las filas con formato `Asignado: X / Y EUR`,
  - en rojo se indica si falta importe o si se ha superado,
  - en verde se confirma cuando el reparto está perfecto para guardar.


### Movimientos (2026-05-26) - Texto CTA
- El botón principal inferior de Movimientos pasa a mostrarse como Nueva acción.


### Workspace (2026-05-27) - Modal de información del piso
- En la ventana de información del piso ya no se repite el nombre dos veces.
- El encabezado del modal muestra `Datos del piso` y el nombre del piso queda en la tarjeta principal del contenido.


### Pagos (2026-05-27) - Scroll en Registrar pago pendiente
- Corregido el desplazamiento vertical del formulario en `Registrar pago pendiente`.
- Ahora el modal de pago usa contenedor scrollable y ajuste de ventana para teclado, permitiendo deslizar hacia abajo en todo el formulario.

### Movimientos (2026-05-27) - Detalle de gastos con destinatarios
- En el detalle de un gasto se muestra ahora el campo `Para` justo debajo de `Pagado por`.
- `Para` enseña los destinatarios del gasto según el reparto (`customSplit`), para identificar claramente a quién va ese gasto.

### Pagos pendientes (2026-05-27) - Destino bloqueado
- Cuando registras un pago desde `Pagar pendiente`, el destinatario queda bloqueado al acreedor real de esa deuda.
- En ese flujo no se permite cambiar tipo de destino, ni añadir/quitar líneas de miembros/habitaciones.
- El importe que aparece corresponde a tu parte pendiente (no al total de otros inquilinos).

### Gastos dirigidos al propietario (2026-06-03)
- Si un gasto está asignado al propietario como deudor, ese propietario ve el flujo de confirmación con justificante igual que cualquier otro usuario afectado.
- En ese caso especial, el detalle del gasto deja de ofrecer `Editar` y `Borrar`, aunque el usuario siga siendo propietario del piso.
- Mientras su deuda esté pendiente, el modal del gasto muestra abajo el botón `Subir justificante`.
- Si el gasto antiguo todavía no tenía creada su deuda técnica en `payment_deadlines`, la app la reconstruye al pulsar ese botón para no bloquear el envío del justificante.
- En la pantalla de justificante, importe, concepto, destino, categoría, fecha y prioridad quedan en solo lectura: el único paso editable es adjuntar la foto.
- Al enviar la foto, su parte queda `En revisión` y la validación final corresponde a quien adelantó el gasto.
- Cuando ya existe una confirmación ligada a ese gasto para el usuario actual, `Movimientos` deja de mostrar además el gasto original para evitar duplicados visuales.
- El estado visible de un gasto con deudas asociadas se recalcula desde `payment_deadlines`, para que propietario y deudor vean la misma transición efectiva entre `Solicitado`, `Pendiente`, `En revisión` y `Pagado`.

### Pagos pendientes (2026-05-27) - Validación estricta y OCR seguro
- Si el pago viene de una deuda pendiente, el OCR no puede sobreescribir el importe bloqueado.
- Antes de guardar, el sistema valida coincidencia exacta (a céntimo) entre pendiente y pago generado.
- Si no cuadra importe o acreedor, no se envía el pago y se muestra error.

### Movimientos (2026-05-27) - Estados y acciones de pago más claros
- En el detalle de pago, el botón de acción pasa de `Editar` a `Cambiar estado`.
- El estado `submitted` ahora se muestra como `En revisión` (ya no se mezcla con `Pendiente`).

### Pagos pendientes (2026-05-27) - Picker más claro
- En la selección de pendientes se muestra también a quién se debe cada pago (`A <acreedor>`).
- Se muestra la lista completa de pendientes (sin recorte a 8 elementos).

### Recordatorios (2026-05-27) - Permisos alineados UI + reglas
- Eliminar/editar recordatorios queda limitado al creador o al propietario del piso.
- El resto de miembros solo puede ver recordatorios, no modificarlos ni eliminarlos.


### Calidad de texto (2026-05-27) - UTF-8 y mojibake
- Se realizó limpieza de texto corrupto (mojibake) en pantallas de Pisos y Workspace.
- Se normalizaron cadenas visibles a UTF-8 correcto, recuperando tildes y caracteres especiales en español.
- Archivos corregidos: `GroupsFragment.java`, `ExpensesFragment.java`, `fragment_groups.xml`.

### Nuevo gasto (2026-05-27) - Desplegables de habitación e inquilinos
- Corregido bloqueo de interacción en los desplegables dentro del modal `Nuevo gasto`.
- Ahora los `Spinner` de habitaciones e inquilinos vuelven a abrirse y seleccionarse correctamente.

### Acceso (2026-05-27) - Estilo de acciones principales
- En `Iniciar sesión` y `Crear cuenta`, las acciones principales pasan a estilo enlace (azul y subrayado), sin fondo de botón.
- El enlace `¿Olvidaste tu contraseña` se alinea al mismo estilo visual azul y subrayado.
- En modo claro, el fondo de `Login` y `Registro` pasa a blanco plano (sin degradado).

### Acceso (2026-05-27) - Idioma de correos de Auth
- Los correos de Firebase Auth (verificación y restablecimiento) se envían con el idioma seleccionado en Ajustes (`es`/`en`).
- La app aplica el idioma configurado antes de lanzar `sendEmailVerification` y `sendPasswordResetEmail`.

### Registro (2026-05-27) - Fecha de nacimiento por defecto
- El campo `Fecha de nacimiento` en alta de cuenta se rellena por defecto con la fecha actual (`YYYY-MM-DD`).

### Acceso (2026-05-27) - Errores en español más claros
- En login y registro, los mensajes por campos faltantes se muestran de forma específica (`Falta el correo`, `Falta la contraseña`).
- Errores habituales de Firebase Auth (correo inválido, contraseña incorrecta, usuario no encontrado, red, etc.) se traducen a mensajes comprensibles en español.

### Acceso (2026-05-27) - Eliminación de apodo/username
- Se retira el campo `username` del registro.
- La app usa `Nombre completo` como identidad visible del usuario.
- El login pasa a correo electrónico + contraseña (sin acceso por apodo).

### Perfil (2026-05-27) - Propagación de nombre editado
- Al editar `Nombre completo` en Perfil, se actualiza `users/{uid}` y se lanza propagación automática en colecciones de negocio donde existan campos de nombre visibles del mismo usuario.
- La propagación se hace por coincidencia de `uid` y/o `email`, actualizando únicamente campos de nombre que ya existan en cada documento.

### Perfil (2026-05-27) - Validación de campos obligatorios
- En `Editar perfil`, no se permite guardar si `Nombre completo`, `Teléfono` o `Fecha de nacimiento` están vacíos.

### Registro (2026-05-27) - Teclado y visibilidad de contraseña
- En `Crear cuenta`, con teclado abierto se ajusta la ventana para mantener visible el campo de contraseña mientras se escribe.
- Se corrige el contenedor del formulario para mejorar el desplazamiento vertical en pantallas pequeñas.
- Se extiende el mismo ajuste de teclado (`adjustResize`) a `Login` y `Verificación de correo` para comportamiento consistente.

### Pisos (2026-05-27) - Menú al mantener pulsado (salir/expulsar)
- En la lista de `Pisos`, al mantener pulsado sobre un piso aparece un menú contextual.
- Si eres inquilino: verás `Salir del piso` y podrás abandonarlo con confirmación.
- Si eres propietario: verás `Expulsar inquilino` para quitar miembros del piso (nunca al propietario).
- Al salir o expulsar, se actualizan en Firestore los campos `members`, `memberEmails` y `roles`.

### Pisos (2026-05-27) - Limpieza de acciones duplicadas
- Se elimina el botón `Gestionar habitaciones` del detalle de piso para evitar duplicidad.
- La gestión de habitaciones se mantiene en su acceso principal del workspace.

### Pisos y habitaciones (2026-05-27) - Reglas de borrado y ocupación
- No se puede eliminar un piso si existe al menos una habitación con inquilinos asignados.
- Para poder borrar el piso, primero hay que quitar o cambiar de habitación a esos inquilinos desde la gestión de habitaciones.
- Una habitación solo se puede eliminar si está vacía (sin residentes).
- Si un inquilino sale del piso o es expulsado por el propietario, también se limpia su email de las habitaciones del piso en Firestore.

### Nuevo gasto (2026-05-27) - Reparto por personas o habitaciones
- En el formulario de `Nuevo gasto` se añade un selector `Repartir por` debajo de `Tipo de reparto`, con opciones `Personas` y `Habitaciones`.
- Si eliges `Personas`, el comportamiento sigue como hasta ahora (líneas por persona + importe por línea).
- Si eliges `Habitaciones`, las líneas pasan a ser por habitación seleccionada y el importe de cada línea se reparte automáticamente entre los inquilinos de esa habitación.
- El resultado se guarda por persona en `customSplit`, para que cada inquilino de la habitación quede con su parte de deuda.

### Nuevo cobro/pago (2026-05-27) - Líneas con importe por persona/habitación
- En `Nuevo cobro` (registrar pago), las líneas de destino ahora incluyen importe editable por línea.
- `Miembro`: cada línea representa una persona y el importe de esa línea define cuánto se le paga.
- `Habitación`: cada línea representa una habitación y el importe de la línea se reparte entre los inquilinos de esa habitación según su reparto configurado.
- Validación: la suma de líneas debe coincidir con el importe total del pago.
- En `Pagar pendiente`, el flujo sigue bloqueado y guiado (sin edición libre de destinatario/importe).

### Calidad técnica (2026-05-27) - UTF-8 estricto
- Archivos de código y recursos guardados en UTF-8 sin BOM para evitar errores de compilación en Java.
- Se corrigen textos con codificación dañada en pantallas de gastos, pagos y pisos.


### Avisos persistentes (Ajustes)
- En el flujo de borrado de cuenta (`Ajustes`), los avisos ahora se muestran como ventana emergente con boton `Aceptar`.
- Se evita asi que mensajes importantes desaparezcan en 2 segundos mientras el usuario escribe.

### Login (ajuste visual)
- Se mejora la presentación de `Iniciar sesión` para evitar apariencia descompensada:
- Fondo adaptado al tema de la app.
- Tarjeta de acceso centrada y con mejor espaciado.
- Botón principal de inicio de sesión más claro y visible.
- Textos revisados en español con acentos correctos.

### Nuevo gasto: reparto por personas
- En `Reparto personalizado` y `Reparto equitativo`, el formulario empieza con 1 persona.
- Puedes añadir o quitar personas manualmente con `+` y `-` en ambos modos.
- En modo equitativo, la pantalla muestra cálculo en vivo por persona para validar que el reparto cuadra con el total.

### Correccion tecnica de compilacion (2026-05-28)
- Se corrige una incidencia interna de codificacion que impedia compilar la app en Android Studio.
- No cambia el flujo funcional para usuario final; mejora la estabilidad del build.

### Registro y textos (2026-05-28)
- Crear cuenta ahora mantiene el mismo estilo visual de Iniciar sesión (fondo y tipografia coherentes).
- Se corrigen textos de autenticacion con tildes y ñ en UTF-8.
- Mejora de uso con teclado en registro para que los campos no queden ocultos al escribir.

### Corrección de textos (2026-05-28)
- Arreglados textos visibles con tildes/ñ en el flujo de gastos y pagos.
- Eliminadas cadenas corruptas para que la interfaz muestre español correcto en todas las pantallas afectadas.

### Firebase/Reglas (2026-05-29) - Alineación con borrado de piso
- Se ajustan reglas para que el borrado en cascada del piso no falle por permisos.
- `payments`: el propietario puede borrar pagos del grupo (incluye estados no `requested` en contexto de limpieza).
- `audit_events` y `event_reminder_jobs`: se mantiene bloqueo de edición, pero el propietario puede borrarlos al eliminar piso.
- `group_codes`: la autorización ya depende del propietario actual del grupo y no del `ownerId` histórico del documento.

### Ajustes (2026-05-29) - Sincronización de idioma al cambiar
- Se corrige una desincronización en `Ajustes > Idioma` donde el spinner podía mostrar un idioma distinto al aplicado en la UI.
- Ahora el idioma seleccionado se guarda de forma síncrona antes de recrear la actividad.
- El selector de idioma toma como referencia el locale activo de `AppCompat`, evitando estados mezclados.

### Localización (2026-05-29) - Autenticación y balance
- Se elimina texto hardcoded en `Login`, `Registro` y `Verificación de correo`, moviendo títulos, labels, hints, diálogos, avisos y errores a `strings.xml`.
- Se añaden traducciones equivalentes en `values-en/strings.xml` para que el cambio ES/EN afecte también a mensajes dinámicos (no solo a la UI estática).
- `BalancesFragment` pasa mensajes de estado a recursos traducibles (`Selecciona grupo`, `Te deben`, `Debes`, etc.).

### Localizacion EN (2026-05-29) - Correccion de compilacion de recursos
- Se corrige el texto de terminos en ingles para evitar un fallo de compilacion en Android (Invalid unicode escape sequence en mergeDebugResources).
- Sin cambios funcionales para usuario; mejora de estabilidad del build.

### Formato de fechas (2026-05-31)
- Todas las fechas visibles y editables pasan a formato `DD/MM/AAAA` (día/mes/año).
- Aplica en:
  - Registro y perfil (fecha de nacimiento).
  - Gastos, pagos y recordatorios.
  - Calendario.
  - Gestión de alquiler (contratos, cobros, documentos, automatizaciones).
- Compatibilidad con datos antiguos:
  - Si existían fechas guardadas en `AAAA-MM-DD`, la app las sigue aceptando y las muestra normalizadas en `DD/MM/AAAA`.

### Gestión de alquiler (2026-05-31) - Vista simplificada
- El apartado `Gestión` se centra en tres módulos prácticos:
  - `Cobros`
  - `Incidencias`
  - `Documentos`
- Se retiran del selector visible para reducir complejidad:
  - `Contrato`
  - `Auditoría`
  - `Automatización`
  - `Reglas push`

### Cobros y soporte (2026-05-31) - Nomenclatura más clara
- El acceso principal antes mostrado como `Gestion` pasa a llamarse `Cobros y soporte`.
- En la cabecera del apartado se muestra `Cobros y soporte del alquiler`.
- El texto guía ahora explica de forma directa su uso: `Gestiona cobros, incidencias y documentos del piso`.
- Objetivo: que el usuario entienda en un vistazo para qué sirve este apartado.

### Perfil (2026-06-01) - Fecha de nacimiento en Editar perfil
- En `Editar perfil`, la `Fecha de nacimiento` se visualiza en formato `DD/MM/AAAA`.
- Si el dato venía en formato antiguo `AAAA-MM-DD`, se normaliza al abrir el perfil/diálogo.
- El ejemplo del campo se actualiza a `Ejemplo: 14/05/1999`.

### Textos de Workspace (2026-06-01) - Corrección de codificación
- Se corrigen textos con caracteres corruptos en los módulos de `Movimientos` y `Cobros y soporte`.
- La app vuelve a mostrar correctamente acentos y `ñ` en:
  - avisos,
  - títulos de diálogo,
  - etiquetas de formularios y estados.

### Workspace (2026-06-01) - Selector de habitación sin botón duplicado
- Se retira el botón `Gestionar` que estaba junto al selector `Habitación`.
- El selector de habitación queda a ancho completo y sin acción lateral duplicada.
- Para ver detalles/acciones del piso se mantiene el acceso existente de información contextual.

### Invitaciones y habitación (2026-06-01)
- Al unirte a un piso por invitación, código o QR, la app te permite elegir habitación en ese momento si hay plazas.
- Si ya estabas en el piso, la invitación pendiente se cierra automáticamente para evitar que reaparezca en bucle.
- Si no hay habitaciones libres, entras al piso sin asignación y podrás asignarte cuando haya hueco.

### Reparto por habitación según ocupación (2026-06-01)
- Si una habitación tiene un único residente, ese residente asume el 100% del coste mensual de la habitación.
- Cuando entra un segundo residente en una habitación con reparto equitativo, el coste se divide automáticamente entre ambos.
- En `Información habitación` se listan los inquilinos asignados (nombre y correo) además de capacidad y coste.

### Cobros en Gestión (2026-06-01) - Selector de habitación e inquilino
- En `Cobros y soporte > Cobros > Nuevo/Editar cobro`:
  - `Habitación` se elige desde un desplegable (ya no es texto libre).
  - El desplegable de `Inquilino` muestra cada opción con formato `Nombre - email`.
- Si no hay habitaciones configuradas, no se puede guardar el cobro hasta elegir una habitación válida.

### Documentos (2026-06-01) - Subir archivo desde el móvil
- En Cobros y soporte > Documentos > Nuevo documento ya no dependes solo de URL.
- Ahora puedes pulsar Adjuntar archivo del móvil para elegir PDF, imagen u otro documento del teléfono.
- Al guardar:
  - El archivo se sube automáticamente a Firebase Storage.
  - El documento queda registrado en el piso con su enlace de descarga.
- Si prefieres, puedes seguir usando Referencia/URL (opcional) sin adjuntar archivo.

### Textos (2026-06-01) - Correccion de mojibake
- Se corrigen textos corruptos en Cobros y soporte para que vuelvan a mostrarse con acentos y simbolos correctos.
- Ejemplos corregidos: Habitacion, Automatizacion, Titulo, valido y separadores visuales.

### Mantenimiento tecnico (2026-06-01) - Limpieza de residuos locales
- Se retiran archivos de log/crash antiguos y caches locales del proyecto para mantener el repositorio limpio.
- No cambia ningun flujo funcional de la app para el usuario final.

### Invitaciones (2026-06-01) - Solo codigo o QR
- Se elimina la invitacion por correo en la gestion de piso/habitaciones.
- Para anadir personas al piso, ahora solo se permite compartir codigo o QR.

### Union por codigo/QR (2026-06-01) - Seleccion de habitacion robusta
- Al unirte por codigo o QR, si hay habitaciones con plaza, la app mantiene el selector de habitacion de forma estable para evitar cierres accidentales.
- Si la habitacion elegida deja de estar disponible, se reabre la seleccion para escoger otra, en vez de entrar al piso sin asignacion.
- La reasignacion normal de habitaciones se realiza desde la gestion del propietario.

### Habitaciones (2026-06-01) - Permisos de cambio
- El inquilino puede autoasignarse en el alta por codigo/QR cuando hay plazas.
- El cambio o reasignacion posterior de habitacion queda reservado al propietario desde la gestion del piso.

### Pagos y gastos (2026-06-01) - Estabilidad de seleccion
- Se corrige un comportamiento que podia impedir seleccionar bien destinatarios en los desplegables de miembro/habitacion.
- La seleccion vuelve a mantenerse estable al editar lineas en Nuevo gasto y Registrar pago.

### Union a piso (2026-06-01) - Habitacion obligatoria para inquilino
- Cuando un inquilino se une por codigo o QR, debe quedar asignado a una habitacion antes de entrar al piso.
- Si el inquilino ya tenia habitacion, entra normalmente.
- Si no hay habitaciones creadas o no quedan plazas, la app informa la situacion y no deja avanzar al workspace hasta que el propietario lo gestione.
- El propietario sigue siendo quien puede reasignar habitaciones mas adelante.


### Union por QR/codigo (2026-06-01) - Duplicados bloqueados
- Si intentas escanear o usar el codigo de un piso al que ya perteneces, la app no vuelve a unirte.
- Se muestra un aviso de acceso denegado indicando que ya estas en ese piso.


### Pagos y recordatorios (2026-06-01) - Desplegables estables
- En Registrar pago y en Nuevo recordatorio, los desplegables de miembros/habitaciones vuelven a abrir y seleccionar con normalidad.
- Se evita el cierre automatico del selector mientras eliges destino.


### Pagos (2026-06-01) - Flujo de validacion y aceptacion
- El inquilino paga desde su deuda pendiente, adjuntando obligatoriamente justificante (imagen/archivo).
- Al enviar, el pago queda en estado Pendiente.
- El propietario o el acreedor del pago pueden aceptar el pago para pasarlo a Confirmado.
- Para evitar duplicados visuales, la vista de inquilino muestra solo sus pagos enviados y sus deudas pendientes.


### Pagos (2026-06-01) - Gestion compartida
- El registro manual de pago deja de exponerse como accion principal separada en la UI.
- El acreedor del pago (`toEmail`) y el propietario del piso pueden aceptar pagos pendientes; el creador del pago y el propietario pueden eliminarlos.
- El flujo de envío con justificante obligatorio se mantiene para el deudor cuando paga una deuda pendiente.


### Pagos (2026-06-01) - Nuevo pago para todos
- El flujo visible para todos pasa a ser `Nuevo gasto`, tanto en alquiler fijo como variable.
- Si hay deudas pendientes, se ofrece `Confirmar pendiente` como acceso guiado con justificante obligatorio.
- En pagos en estado Pendiente, la acción principal se muestra como `Aceptar pago` o `Aceptar confirmación` cuando el pendiente nace de un gasto.


### Nuevo gasto (2026-06-01) - Desplegables de reparto
- Los selectores de miembros y habitaciones en Nuevo gasto vuelven a abrir correctamente al tocar, incluso dentro del formulario desplazable.


### Union por codigo (2026-06-01) - Compatibilidad con reglas estrictas
- Si un usuario aun no es miembro y no tiene permiso de lectura del documento de grupo, la app aplica alta directa por codigo sin bloquear la union.


### Movimientos (2026-06-01) - Sin duplicados visuales
- Se corrige la recarga de datos para que el listado no muestre pagos/gastos repetidos por respuestas asincronas solapadas.


### Mantenimiento tecnico (2026-06-01) - Proyecto Firebase versionado
- Se incluyen firebase.json y firestore.indexes.json en la raiz para permitir despliegue de reglas desde terminal sin configuracion adicional.


### Calidad TFG (2026-06-01) - Refactor y validacion
- Se separa la logica de permisos de pagos en un servicio especifico para mejorar mantenibilidad.
- Se incorpora checklist de pruebas en TEST_CHECKLIST_TFG.md para validar flujos criticos antes de entrega.



### Calidad TFG (2026-06-01) - Refactor de mantenibilidad (fase 2)
- Se reorganiza logica interna de Pisos/Balance para reducir dependencia de fragments gigantes.
- No cambia el flujo funcional para usuario final: los cambios son de estructura tecnica para estabilidad y mantenimiento.
- Se amplia checklist de pruebas con regresion especifica para alta por provincia, etiquetas de miembros y textos de gestion.

### UI (2026-06-03) - Texto del boton en info del piso
- En la informacion del piso, el boton visible pasa a mostrarse como `Añadir inquilinos` sin cambiar la logica asociada.

### Confirmaciones (2026-06-03) - Vista del justificante
- Cuando una confirmacion o pago ya tiene justificante subido, su detalle muestra tambien la imagen para poder revisarla visualmente.

### Gastos compartidos (2026-06-03) - Un gasto por deudor
- Cuando repartes un gasto entre varias personas, la app ya no guarda un unico gasto con reparto interno.
- Ahora crea un gasto independiente por cada deudor, con su importe exacto y su propio flujo de confirmacion.
- Ejemplo: si el total era 53 EUR y el reparto era 20 EUR + 33 EUR, se generan dos gastos distintos: uno de 20 EUR y otro de 33 EUR.
- Esto evita duplicados raros, mezclas de estados y errores al aceptar justificantes.
- Si recibes una confirmacion de un gasto que tu adelantaste, `Movimientos` te la muestra aunque no seas el propietario del piso, para que puedas aceptarla desde tu propia cuenta.
- En `Reparto equitativo`, si hay varios deudores posibles, el formulario expande automaticamente el reparto para todos ellos y genera un gasto por cada persona.

### Balance (2026-06-03) - Renombre visual
- La pantalla de balance pasa a presentar el bloque principal como `Balance de gastos`.
- El grafico mensual se renombra a `Balance mensual` para darle el mismo peso visual que al bloque principal.
- Se oculta el detalle secundario por mes y miembro para dejar solo los bloques principales, sin cambiar calculos ni datos.

### Balance (2026-06-03) - Dos bloques principales y rango temporal
- La pantalla queda reducida a dos visualizaciones principales con el mismo peso: `Balance de gastos` y `Balance por tiempo`.
- Se elimina el tercer bloque secundario de balance para simplificar la lectura y evitar competir visualmente con los dos graficos principales.
- `Balance por tiempo` incorpora un desplegable entre el titulo y la grafica para cambiar el rango visible.
- El rango temporal permite ver al menos `Trimestre`, `Cuatrimestre`, `Semestre`, `Año completo` y `2 años`.
- Las barras muestran los meses con etiqueta de mes y año corto para que un rango largo no repita nombres ambiguos.
- Cuando el periodo incluye muchos meses, la grafica puede desplazarse en horizontal para evitar que las etiquetas se superpongan.

### Recordatorios y selectores (2026-06-03) - Etiquetas de inquilinos
- En los selectores de personas visibles de la app se muestra el formato `Nombre (correo)` en lugar de solo nombre o solo email.
- Se aplica al menos en recordatorios, pagos, reparto de gastos y selectores relacionados con residentes.
- La recarga visual de recordatorios se protege para no mostrar duplicados por respuestas solapadas del listado.

### Confirmaciones (2026-06-03) - Justificante y rendimiento
- El detalle de una confirmacion con justificante permite desplazarse verticalmente para ver toda la imagen.
- La vista previa del justificante se carga reducida y en segundo plano para evitar cierres o bloqueos al abrir gastos en revision.

### Recordatorios (2026-06-03) - Rango de fechas respetado
- Los recordatorios con recurrencia pasan a respetar la fecha `Hasta` tambien en `Calendario`.
- Si un recordatorio empieza y termina el mismo dia, aunque su frecuencia sea `Diario`, solo se muestra y programa para ese unico dia.

### Documentacion de entrega (2026-06-04) - Estilo sobrio
- Los HTML de `docs_entrega/` se han simplificado para que tengan apariencia de documento basico tipo Word.
- Se eliminan esquinas redondeadas, sombras y recursos visuales decorativos, priorizando la informacion y la facilidad de importacion a Word o Google Docs.

### Acceso (2026-06-04) - Recordarme al cerrar la app
- Si `Recordarme` esta activado, la sesion permanece iniciada al cerrar y volver a abrir la aplicacion.
- Si `Recordarme` esta desactivado, al mandar la app a segundo plano o cerrarla se cierra la sesion y al volver se exige login de nuevo.

### Tema visual (2026-06-04) - Sincronizacion entre acceso y app
- El modo claro/oscuro se aplica ahora de forma consistente desde `Splash`, `Login`, `Registro`, `Verificacion` y `Main`.
- Se evita el caso intermitente en el que la pantalla de acceso aparece con un tema y, al entrar en la app, el contenido se muestra con el contrario.

### Balance y habitaciones (2026-06-04) - Reparto real por inquilino
- El coste mensual de una habitacion ya no se trata como si lo asumiera una sola persona.
- Si una habitacion se reparte entre varios inquilinos, el balance descuenta a cada uno solo su parte segun el reparto configurado en la propia habitacion.
- En la vista de balance por categorias, el alquiler variable de habitaciones aparece ademas como categoria propia `Gasto habitación`.

### Nuevo gasto (2026-06-04) - Ticket obligatorio
- Al crear un gasto nuevo, adjuntar el ticket pasa a ser obligatorio.
- El boton `Guardar` del modal `Nuevo gasto` permanece desactivado mientras no haya ticket adjunto.
- Si se adjunta una imagen, la app mantiene el OCR para intentar detectar el importe y agilizar el alta.
- En `Editar gasto`, si el gasto ya tenia ticket, no hace falta volver a subirlo para actualizar otros datos.
- Los modales de detalle largos, como `Gasto habitación` o algunas confirmaciones con muchos campos, permiten desplazarse verticalmente para ver toda la información.
- El diálogo de `Términos y condiciones` del registro se muestra con secciones visuales, mejor separación y scroll, para que el texto legal sea más legible.
- Se corrigen textos visibles del acceso y recuperación de contraseña para mantener la puntuación completa en español.
- Se han saneado las cadenas base de la interfaz en español para eliminar textos corruptos de codificación en login, ajustes, registro, verificación y balance.

### Gestion (2026-06-04) - Horarios solo del propietario
- El modulo `Horarios` queda en modo solo lectura para inquilinos que no sean propietarios del piso.
- Solo el propietario puede crear, editar y eliminar horarios.
- Al tocar un horario como propietario aparecen acciones para ver detalle, editarlo o eliminarlo.

### Gestion (2026-06-04) - Deteccion de propietario corregida
- La pantalla de `Gestion` vuelve a reconocer correctamente al propietario tambien cuando el grupo usa rol `admin`.
- Con ello, `Reglas` y `Horarios` dejan de quedarse bloqueados por error para el propietario legitimo del piso.

### Filtro por habitacion (2026-06-04) - Alcance completo
- El selector de habitacion del piso ya no afecta solo a `Movimientos`.
- Ahora tambien filtra `Recordatorios` y `Gestion`.
- Si se elige una habitacion concreta, se ocultan recordatorios, incidencias, reglas y horarios ligados a otra habitacion.
- En elementos dirigidos a una persona, el filtro usa tambien los miembros reales de la habitacion seleccionada para decidir si deben verse.

### Habitaciones (2026-06-04) - Reparto mensual exacto
- El coste mensual de una habitacion se aplica al 100 % si solo vive una persona en ella.
- Si viven varias personas, se divide entre ellas segun el reparto configurado en esa habitacion.
- El reparto se redondea a centimos de forma consistente para que balance, resumen rapido y pagos por habitacion usen exactamente los mismos importes.
- Esto evita diferencias de calculo entre vistas cuando el alquiler de la habitacion cambia o cuando cambian sus residentes.

### Movimientos (2026-06-04) - Gasto de habitación visible
- La parte mensual que cada inquilino debe por su habitacion se guarda tambien como cobro mensual del piso.
- En `Movimientos` aparece como una fila mas con el nombre `Gasto habitación`.
- La solicitud se genera automaticamente al entrar en el piso, aunque quien abra primero no sea el propietario.
- Ese movimiento queda asociado al propietario del piso como solicitante.
- La fecha de inicio se fija en el mismo dia de generacion y el vencimiento se coloca 30 dias despues.
- Si cambia el coste de la habitacion o cambian sus residentes, el cobro mensual del mes actual se recalcula y se actualiza.
- La siguiente mensualidad pendiente de `Gasto habitación` tambien se resincroniza para que no conserve importes antiguos si cambia el reparto antes de vencer.
- Si una habitacion pasa de una persona a dos, el importe visible de quien ya estaba baja a su nueva parte y se crea tambien la fila de la nueva persona.
- Cuando un gasto normal va dirigido a ti, al abrirlo se prioriza el flujo de justificante y no la edición del gasto.
- Tras subir la foto, el estado pasa a `En revisión` para que quien adelantó el gasto pueda revisarla y aceptarla.
- El detalle de pagos y confirmaciones permite abrir el justificante en un visor más grande para verlo completo sin cargar la imagen a tamaño original.
### Dialogos (2026-06-04) - Altura adaptativa
- Las ventanas emergentes ajustan ahora mejor su tamaño al contenido que muestran.
- Los dialogos cortos, como `Nueva accion` o avisos de validacion, ya no ocupan una altura desproporcionada.
- Los dialogos largos siguen permitiendo desplazamiento vertical cuando el contenido lo necesita.

### Pagos (2026-06-04) - Ticket visible al validar
- En el detalle de `Pago solicitado` o de una confirmacion ya enviada puede mostrarse tambien el ticket original del gasto, no solo el justificante del pago.
- Esto permite a quien valida revisar mejor si el importe y el concepto del gasto coinciden con el ticket adjunto.
- Las imagenes siguen abriendose en visor ampliado para verlas completas sin cargar el archivo original a maxima resolucion.

### Movimientos y calendario (2026-06-04) - Limpieza del flujo antiguo de pagos
- La app deja de mostrar en `Pisos` y en `Calendario` los pagos genericos del flujo antiguo que no nacen de un gasto real.
- Se mantienen visibles las confirmaciones ligadas a gastos y sus pendientes asociados, que son el flujo vigente.
- Con ello desaparecen etiquetas residuales como `Pago pendiente` cuando no aportan nada al funcionamiento actual.

### Nuevo gasto y tarjetas (2026-06-04) - Informacion mas clara
- El reparto de `Nuevo gasto` se centra ahora en personas, no en lineas por habitaciones.
- En cada selector de persona se muestra tambien su habitacion para identificar mejor a quien corresponde cada parte del gasto.
- Las tarjetas de `Movimientos`, `Recordatorios` y `Gestion` pasan a destacar mejor la informacion principal, con textos como quien solicita, para quien va, fechas y frecuencia.

### Seguridad (2026-06-04) - Edicion y borrado de gastos
- A nivel de Firebase, los gastos solo pueden editarse o borrarse mientras siguen en estado `Solicitado`.
- Esa gestion queda reservada al pagador original del gasto o al propietario del piso.
- Cuando el gasto ya ha pasado a confirmacion o validacion, las reglas del servidor bloquean cambios manuales aunque alguien intente saltarse la interfaz.

### Gastos dirigidos (2026-06-04) - Ticket visible y justificante recuperado
- Cuando un gasto solicitado va dirigido a un inquilino, su detalle muestra tambien el ticket original del gasto para poder comprobarlo antes de pagar.
- Ese detalle puede desplazarse aunque incluya imagen, para que no se corte en pantallas pequenas.
- El flujo de `Subir justificante` vuelve a activarse tambien para deudas antiguas que todavia arrastraban estado `requested` en vez de `pending`.

### Gastos (2026-06-04) - Habitacion real y etiquetas limpias
- Los gastos creados por reparto de personas ya no arrastran por defecto la etiqueta `Todas las habitaciones`.
- La app intenta guardar y mostrar la habitacion real de las personas afectadas, o `Varias habitaciones` si participan varias.
- En `Movimientos` se eliminan restos visuales del texto `Pago solicitado` para el flujo actual de gastos y confirmaciones.

### Gastos y balance (2026-06-04) - Fechas y reparto más claros
- En `Nuevo gasto`, el botón de adjuntar ticket deja de mostrar la coletilla `OCR`, aunque la lectura automática del importe sigue funcionando al subir una imagen.
- En el reparto por personas ya no aparece el propio pagador dentro de los desplegables de deudores, evitando una opción inválida que luego quedaba bloqueada.
- Las tarjetas de `Movimientos` dan más protagonismo a `Solicitado por` y `Para`.
- Cada gasto muestra también la `Fecha del gasto` en la tarjeta y en su detalle.
- Todas las fechas visibles de este flujo pasan a mostrarse en `DD/MM/AAAA`, incluidos los meses de gastos de habitación y las etiquetas temporales del balance.
- En la vista rápida de `Movimientos`, los nombres se muestran de forma compacta para no repetir el correo entre paréntesis y mejorar la lectura de la tarjeta.
- El buscador rápido junto a `Filtros` desaparece y se reemplaza por tres accesos directos de estado: `Solicitado`, `En revisión` y `Pagados`.
- Cada botón de estado muestra solo los movimientos que correspondan a ese bloque; si vuelves a tocar el mismo botón activo, se quita ese filtro y vuelven a verse todos.
- El botón `Filtros` sigue disponible aparte para los filtros avanzados por categoría, persona o fecha.
