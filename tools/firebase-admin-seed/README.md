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

La cuenta propietaria (`owner`) queda como admin en todos los pisos.

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

- `--preset`: preset de demo. Disponible: `sergio-demo`.
- `--service-account`: ruta al JSON de service account.
- `--owner-email`: cuenta propietaria de todos los pisos (obligatorio).
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

## Preset hardcodeado: `sergio-demo`

Este preset crea un piso demo con:

- owner fijo: `sergioaripe06@gmail.com`,
- 4 inquilinos inventados (Juan, María, Lucía y Álvaro),
- 4 habitaciones con precios distintos,
- gastos, pagos, vencimientos y recordatorios de prueba.

Comando rápido:

```bash
npm run seed:sergio-demo
```

Si el owner no existe todavía en Firebase Auth, añade también `--owner-password`:

```bash
node seed.js --service-account ./service-account.json --preset sergio-demo --owner-password TuPasswordSegura123!
```

## Resultado

El script genera un archivo con credenciales en:

`tools/firebase-admin-seed/output/seed-users-YYYYMMDD-HHMMSS.json`

Guardalo para iniciar sesion con los usuarios de prueba.
