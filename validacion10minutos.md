# Validacion en 10 Minutos (TFG FlatShareApp)

## Objetivo
Validar en una sola pasada los flujos criticos antes de entregar.

## Preparacion (30 segundos)
- Usa 2 cuentas reales en 2 moviles o 1 movil + emulador.
- Cuenta A: propietario del piso.
- Cuenta B: inquilino.

## Minuto 0:00 - 1:00 | Build e inicio
1. Instala la ultima `debug` y abre app en ambos dispositivos.
2. Inicia sesion con A y B.

OK: ambos entran a la app sin crash.
KO: cierre inesperado o pantalla en blanco.

## Minuto 1:00 - 3:00 | Union por codigo/QR + habitacion obligatoria
1. En A, abre un piso con habitaciones disponibles.
2. En B, unete por codigo o QR.
3. Verifica que B debe elegir habitacion antes de entrar.
4. Intenta volver a unirte al mismo piso con B.

OK:
- Sale selector de habitacion y no entra directo.
- Si ya pertenece al piso, muestra denegacion (ya estas en este piso).
KO:
- Entra sin elegir habitacion.
- Deja unirse de nuevo al mismo piso.

## Minuto 3:00 - 5:00 | Nuevo gasto (desplegables)
1. En A o B, entra a Nuevo gasto.
2. Abre desplegable de miembros.
3. Abre desplegable de habitaciones.
4. Cambia tipo de reparto y confirma que no se bloquea.

OK: todos los desplegables abren, permiten seleccionar y no se cierran mal.
KO: no despliega miembros/habitaciones o queda bloqueado.

## Minuto 5:00 - 7:30 | Flujo de pago pendiente
1. Genera una deuda/pago solicitado para B.
2. Desde B, pulsa pagar y adjunta justificante (foto o archivo).
3. Envia el pago.
4. En A, revisa que aparece como pendiente y aceptalo.

OK:
- Justificante obligatorio.
- Estado pasa a pendiente al enviar.
- A (propietario) o creador puede aceptar.
KO:
- Permite enviar sin justificante.
- No cambia estado o no permite aceptar.

## Minuto 7:30 - 9:00 | Duplicados y refresco
1. En lista de movimientos/pagos, recarga varias veces rapido.
2. Cambia de tab y vuelve.

OK: no aparecen tarjetas duplicadas.
KO: items repetidos tras recarga/navegacion.

## Minuto 9:00 - 10:00 | Cierre tecnico rapido
1. Verifica que la app no muestra textos corruptos visibles.
2. Revisa 1 habitacion en info y confirma inquilinos asignados.
3. Confirma que Registrar pago y Recordatorio muestran miembros correctamente.

OK final:
- Sin crashes.
- Sin bloqueos en desplegables.
- Union y pagos con logica correcta.
- Sin duplicados visibles.

Si todo esta en OK, estado recomendado: LISTA PARA ENTREGA TFG.
