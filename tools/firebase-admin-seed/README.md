# Seeder Firebase Admin (cuentas reales)

Este script crea:

- cuentas reales en `Firebase Authentication` (email/password),
- perfiles en `users`,
- entradas en `usernames`,
- pisos en `groups`,
- codigos en `group_codes`,
- habitaciones en `rooms_groups`,
- gastos en `expenses`,
- pagos en `payments`,
- vencimientos en `payment_deadlines`,
- recordatorios en `reminders`.

La cuenta propietaria (`owner`) queda como admin en todos los pisos que crea el seed.

## Requisitos

1. Node.js 18+.
2. Service account JSON de Firebase.
3. Proyecto Firebase con `Authentication (Email/Password)` y `Firestore`.

## Instalacion

```bash
cd tools/firebase-admin-seed
npm install
```

## Ejecucion basica

```bash
node seed.js \
  --service-account ./service-account.json \
  --owner-email tu_owner@email.com \
  --owner-password TuPasswordSegura123! \
  --users 12 \
  --groups 4 \
  --rooms 3 \
  --members-per-group 4 \
  --expenses-per-group 8 \
  --payments-per-group 6 \
  --reminders-per-group 4
```

## Opciones

- `--preset`: preset de demo. Disponibles: `sergio-demo`, `sergio-owner-plus`, `sergio-tenant-plus`, `sergio-owner-homoerectus`.
- `--service-account`: ruta al JSON de service account.
- `--owner-email`: cuenta propietaria de todos los pisos (obligatorio si no usas preset).
- `--owner-password`: solo se usa si el owner no existe y hay que crearlo.
- `--users`: numero de cuentas inquilino a generar.
- `--groups`: numero de pisos a crear.
- `--rooms`: habitaciones por piso.
- `--members-per-group`: inquilinos por piso (sin contar owner).
- `--expenses-per-group`: gastos seed por piso.
- `--payments-per-group`: pagos seed por piso.
- `--reminders-per-group`: recordatorios seed por piso.
- `--password-prefix`: prefijo de password de inquilinos.
- `--email-prefix`: prefijo de email de inquilinos.
- `--email-domain`: dominio de email de inquilinos.
- `--project-id`: project id de Firebase (opcional).
- `--dry-run`: no escribe nada; solo muestra el plan.

## Presets disponibles

### `sergio-demo`

Preset original de demo con:

- owner: `sergioaripe06@gmail.com`,
- 4 inquilinos de prueba,
- 4 habitaciones,
- gastos, pagos, vencimientos y recordatorios.

Comando:

```bash
npm run seed:sergio-demo
```

### `sergio-owner-plus`

Nuevo preset con Sergio como propietario y un piso mas grande:

- owner: `sergioaripe06@gmail.com`,
- 5 inquilinos de prueba,
- 5 habitaciones distintas,
- mayor volumen de gastos/pagos/recordatorios.

Comando:

```bash
npm run seed:sergio-owner-plus
```

### `sergio-tenant-plus`

Nuevo preset donde Sergio es inquilino (no propietario):

- owner: `laura.owner@seed.flatshare.local`,
- Sergio (`sergioaripe06@gmail.com`) como inquilino,
- varios inquilinos adicionales,
- 5 habitaciones con datos financieros de prueba.

Comando:

```bash
npm run seed:sergio-tenant-plus
```

### `sergio-owner-homoerectus`

Preset con Sergio como propietario y `homoerectus079@gmail.com` dentro del piso:

- owner: `sergioaripe06@gmail.com`,
- inquilino real: `homoerectus079@gmail.com`,
- 3 inquilinos seed adicionales,
- 4 habitaciones con gastos, pagos, vencimientos y recordatorios.

Comando:

```bash
npm run seed:sergio-owner-homoerectus
```

## Nota sobre passwords

Si una cuenta ya existe en Firebase Auth, el script no cambia su password.
Si no existe, la crea usando el `password-prefix` + `passwordSuffix` del preset.

## Resultado

El script genera un archivo con credenciales en:

`tools/firebase-admin-seed/output/seed-users-YYYYMMDD-HHMMSS.json`

Guarda ese archivo para iniciar sesion con los usuarios de prueba.
