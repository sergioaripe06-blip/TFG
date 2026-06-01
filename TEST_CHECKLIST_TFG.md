# Test Checklist TFG (FlatShareApp)

## 1. Autenticacion y acceso

1. Registro con email valido.
2. Verificacion de correo obligatoria antes de entrar.
3. Login con credenciales correctas.
4. Login con contraseña incorrecta (mensaje de error claro).

## 2. Pisos y union

1. Crear piso nuevo con habitaciones iniciales.
2. Unirse por codigo con usuario no miembro.
3. Reintentar unirse por codigo al mismo piso (debe bloquear: ya estas en este piso).
4. Unirse por QR con usuario no miembro.
5. Al unirse, seleccion de habitacion obligatoria si hay plazas.
6. Si no hay plazas, no entrar directo al workspace.

## 3. Habitaciones

1. Propietario puede crear, editar y eliminar habitacion vacia.
2. No se puede eliminar habitacion con residentes.
3. Reasignar inquilino de habitacion desde propietario.
4. Ver informacion de habitacion con residentes listados.

## 4. Gastos

1. Crear gasto con reparto por personas.
2. Crear gasto con reparto por habitaciones.
3. Validacion: suma de reparto debe coincidir con importe total.
4. Verificar que desplegables de miembros/habitaciones abren correctamente en Nuevo gasto.

## 5. Pagos

1. Cualquier miembro puede abrir Registrar pago (alquiler fijo y variable).
2. Pago pendiente: deudor adjunta justificante obligatorio y envia.
3. Estado del pago tras envio: pending.
4. Aceptar pago pending desde propietario.
5. Aceptar pago pending desde creador del pago.
6. Eliminar pago desde propietario.
7. Eliminar pago desde creador del pago.
8. Verificar que no se duplican tarjetas en Movimientos tras recargas rapidas.

## 6. Recordatorios

1. Crear recordatorio por miembros seleccionados.
2. Crear recordatorio por habitaciones seleccionadas.
3. Verificar que desplegables abren y seleccionan correctamente.
4. Eliminar recordatorio como creador.
5. Eliminar recordatorio como propietario.

## 7. Reglas Firestore (seguridad)

1. Usuario no miembro no puede leer grupo privado.
2. Usuario no miembro puede unirse por codigo valido.
3. Miembro no autorizado no puede eliminar piso.
4. Pago pending solo puede confirmarse por creador o propietario.
5. Usuario ajeno al pago no puede confirmarlo ni borrarlo.

## 8. Calidad tecnica

1. Buscar BOM en archivos de texto (debe ser 0).
2. Buscar patrones de mojibake en app/src/main (debe ser 0).
3. Build debug en entorno con JDK 17+.
4. Prueba de smoke: abrir app, navegar por tabs, crear y listar datos.

## 9. Regresion por refactor (2026-06-01)

1. GroupsFragment: crear piso y validar selector de provincia (autocompletado y valor canonico).
2. GroupsFragment: union por codigo/QR sigue mostrando selector de habitacion al entrar por primera vez.
3. RentalManagementFragment: labels de miembro en desplegables muestran Nombre + email correctamente.
4. RentalManagementFragment: tipos de documento y eventos siguen mostrando textos legibles (Contrato, Factura, Vencimiento de renta, etc.).
5. ExpensesFragment: resumen de propietario/miembros y detalle de objetivos mantienen formato sin duplicados ni nombres vacios.

