#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const SERGIO_DEMO_PRESET = {
  key: "sergio-demo",
  ownerEmail: "sergioaripe06@gmail.com",
  ownerDisplayName: "Sergio Aripe",
  groupName: "Piso Demo Sergio",
  groupDescription: "Piso demo para validar flujo completo de alquiler variable.",
  location: {
    street: "Calle Alcalá 123",
    portal: "Portal A",
    postalCode: "28009",
    city: "Madrid",
    province: "Madrid"
  },
  roomTemplates: [
    { name: "Suite exterior", monthlyCost: 520 },
    { name: "Habitación balcón", monthlyCost: 470 },
    { name: "Habitación interior", monthlyCost: 420 },
    { name: "Habitación estudio", monthlyCost: 390 }
  ],
  tenants: [
    { email: "juan.demo@seed.flatshare.local", username: "juan_demo", displayName: "Juan Pérez", passwordSuffix: "juan01" },
    { email: "maria.demo@seed.flatshare.local", username: "maria_demo", displayName: "María García", passwordSuffix: "maria02" },
    { email: "lucia.demo@seed.flatshare.local", username: "lucia_demo", displayName: "Lucía Torres", passwordSuffix: "lucia03" },
    { email: "alvaro.demo@seed.flatshare.local", username: "alvaro_demo", displayName: "Álvaro Ruiz", passwordSuffix: "alvaro04" }
  ],
  config: {
    groupsCount: 1,
    roomsPerGroup: 4,
    usersCount: 4,
    membersPerGroup: 4,
    expensesPerGroup: 10,
    paymentsPerGroup: 8,
    remindersPerGroup: 4
  }
};

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i++) {
    const token = argv[i];
    if (!token.startsWith("--")) continue;
    const key = token.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith("--")) {
      args[key] = true;
      continue;
    }
    args[key] = next;
    i++;
  }
  return args;
}

function asInt(value, fallback) {
  if (value === undefined || value === null || value === "") return fallback;
  const parsed = Number.parseInt(String(value), 10);
  if (!Number.isFinite(parsed)) return fallback;
  return parsed;
}

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function safeNowStamp() {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  const h = String(d.getHours()).padStart(2, "0");
  const min = String(d.getMinutes()).padStart(2, "0");
  const s = String(d.getSeconds()).padStart(2, "0");
  return `${y}${m}${day}-${h}${min}${s}`;
}

function chunk(arr, size) {
  const out = [];
  for (let i = 0; i < arr.length; i += size) {
    out.push(arr.slice(i, i + size));
  }
  return out;
}

function usage() {
  console.log(`
Uso:
  node seed.js \\
    --owner-email owner@tu-dominio.com \\
    --owner-password TuPasswordSegura123! \\
    --users 12 \\
    --groups 4 \\
    --rooms 3 \\
    --members-per-group 4 \\
    --expenses-per-group 8 \\
    --payments-per-group 6 \\
    --reminders-per-group 4

 Opciones:
  --preset <id>                  Preset de demo. Disponible: sergio-demo
  --service-account <ruta-json>   Ruta al JSON de service account (opcional si usas GOOGLE_APPLICATION_CREDENTIALS)
  --owner-email <email>           Email de la cuenta propietaria (obligatorio)
  --owner-password <password>     Password del owner si hay que crearlo
  --users <n>                     Numero de cuentas inquilino a generar (default: 12)
  --groups <n>                    Numero de pisos a crear (default: 4)
  --rooms <n>                     Habitaciones por piso (default: 3)
  --members-per-group <n>         Inquilinos por piso (sin contar owner, default: 4)
  --expenses-per-group <n>        Gastos seed por piso (default: 8)
  --payments-per-group <n>        Pagos seed por piso (default: 6)
  --reminders-per-group <n>       Recordatorios seed por piso (default: 4)
  --password-prefix <txt>         Prefijo de password para inquilinos (default: FlatShareSeed!)
  --email-prefix <txt>            Prefijo de email para inquilinos (default: seeduser)
  --email-domain <txt>            Dominio de email para inquilinos (default: seed.flatshare.local)
  --project-id <id>               Project ID Firebase (opcional)
  --dry-run                       No escribe en Firebase; solo muestra plan
`);
}

function initFirebase({ serviceAccountPath, projectId }) {
  if (admin.apps.length > 0) return admin.app();
  if (serviceAccountPath) {
    const abs = path.resolve(serviceAccountPath);
    const raw = fs.readFileSync(abs, "utf8");
    const serviceAccount = JSON.parse(raw);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: projectId || serviceAccount.project_id
    });
    return admin.app();
  }
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    ...(projectId ? { projectId } : {})
  });
  return admin.app();
}

async function ensureAuthUser(auth, email, password, displayName) {
  try {
    const existing = await auth.getUserByEmail(email);
    return { uid: existing.uid, email: existing.email || email, created: false };
  } catch (err) {
    if (err && err.code !== "auth/user-not-found") throw err;
    if (!password) {
      throw new Error(`No existe la cuenta ${email} y no se ha pasado password para crearla.`);
    }
    const created = await auth.createUser({
      email,
      password,
      displayName
    });
    return { uid: created.uid, email: created.email || email, created: true };
  }
}

function pickMembers(users, startIndex, count) {
  if (users.length === 0 || count <= 0) return [];
  const selected = [];
  for (let i = 0; i < count; i++) {
    selected.push(users[(startIndex + i) % users.length]);
  }
  return selected;
}

function toIsoDate(value) {
  const date = value instanceof Date ? value : new Date(value);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function dateFromToday(daysOffset) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() + daysOffset);
  return date;
}

function round2(value) {
  return Math.round((Number(value) + Number.EPSILON) * 100) / 100;
}

function buildEqualCustomSplit(memberEmails) {
  if (!Array.isArray(memberEmails) || memberEmails.length === 0) return "";
  const pointsTotal = 10000;
  const base = Math.floor(pointsTotal / memberEmails.length);
  let remainder = pointsTotal - base * memberEmails.length;
  const entries = [];
  for (const email of memberEmails) {
    const points = base + (remainder > 0 ? 1 : 0);
    if (remainder > 0) remainder--;
    entries.push(`${email}:${(points / 100).toFixed(2)}`);
  }
  return entries.join(", ");
}

function buildWeightedCustomSplit(memberEmails, seed) {
  if (!Array.isArray(memberEmails) || memberEmails.length === 0) return "";
  const weights = memberEmails.map((_, idx) => ((seed + idx) % 4) + 1);
  const total = weights.reduce((acc, v) => acc + v, 0);
  let used = 0;
  const entries = [];
  for (let i = 0; i < memberEmails.length; i++) {
    let percent;
    if (i === memberEmails.length - 1) {
      percent = round2(Math.max(0, 100 - used));
    } else {
      percent = round2((weights[i] * 100) / total);
      used = round2(used + percent);
    }
    entries.push(`${memberEmails[i]}:${percent.toFixed(2)}`);
  }
  return entries.join(", ");
}

function parseCustomSplit(splitText) {
  const out = [];
  if (!splitText || typeof splitText !== "string") return out;
  const entries = splitText.split(",");
  for (const raw of entries) {
    const kv = raw.trim().split(":");
    if (kv.length !== 2) continue;
    const email = kv[0].trim().toLowerCase();
    const percent = Number.parseFloat(kv[1].trim());
    if (!email || !Number.isFinite(percent) || percent < 0) continue;
    out.push({ email, percent });
  }
  return out;
}

function pickExpenseCategory(index) {
  const categories = ["agua", "electricidad", "internet", "alquiler", "comida", "otros"];
  return categories[index % categories.length];
}

function pickPriority(index) {
  const priorities = ["baja", "media", "alta"];
  return priorities[index % priorities.length];
}

function chooseRoomSelection(rooms, index) {
  if (!Array.isArray(rooms) || rooms.length === 0) {
    return {
      roomIds: [],
      roomNames: [],
      roomId: "",
      roomName: ""
    };
  }
  const mode = index % 3;
  if (mode === 0) {
    return {
      roomIds: rooms.map((r) => r.id),
      roomNames: rooms.map((r) => r.name),
      roomId: "all",
      roomName: "Todas las habitaciones"
    };
  }
  if (mode === 1 || rooms.length === 1) {
    const single = rooms[index % rooms.length];
    return {
      roomIds: [single.id],
      roomNames: [single.name],
      roomId: single.id,
      roomName: single.name
    };
  }
  const first = rooms[index % rooms.length];
  const second = rooms[(index + 1) % rooms.length];
  return {
    roomIds: [first.id, second.id],
    roomNames: [first.name, second.name],
    roomId: "multi",
    roomName: "Varias habitaciones"
  };
}

function addDays(baseDate, days) {
  const date = new Date(baseDate.getTime());
  date.setDate(date.getDate() + days);
  return date;
}

function javaStringHash(value) {
  let hash = 0;
  const text = String(value || "");
  for (let i = 0; i < text.length; i++) {
    hash = ((hash * 31) + text.charCodeAt(i)) | 0;
  }
  return hash;
}

function buildReminderTargets(members, rooms, index) {
  const normalizedMembers = members
    .map((m) => String(m.email || "").toLowerCase())
    .filter(Boolean);
  const uniqueMembers = Array.from(new Set(normalizedMembers));
  const uniqueRooms = Array.isArray(rooms) ? rooms : [];
  const mode = index % 3;

  if (mode === 0) {
    const subset = uniqueMembers.slice(0, Math.min(2, uniqueMembers.length));
    return {
      targetType: "x_miembro",
      targetEmails: subset.length > 0 ? subset : uniqueMembers,
      targetMemberEmail: subset.length > 0 ? subset[0] : "",
      roomId: "",
      roomName: "",
      roomIds: [],
      roomNames: []
    };
  }

  if (mode === 1 && uniqueRooms.length > 0) {
    const r1 = uniqueRooms[index % uniqueRooms.length];
    const r2 = uniqueRooms[(index + 1) % uniqueRooms.length];
    const pickedRooms = r1.id === r2.id ? [r1] : [r1, r2];
    const targetEmails = new Set();
    for (const room of pickedRooms) {
      const emails = Array.isArray(room.memberEmails) ? room.memberEmails : [];
      for (const email of emails) {
        if (email) targetEmails.add(String(email).toLowerCase());
      }
    }
    if (targetEmails.size === 0) {
      for (const email of uniqueMembers) targetEmails.add(email);
    }
    return {
      targetType: "x_habitacion",
      targetEmails: Array.from(targetEmails),
      targetMemberEmail: "",
      roomId: pickedRooms[0] ? pickedRooms[0].id : "",
      roomName: pickedRooms[0] ? pickedRooms[0].name : "",
      roomIds: pickedRooms.map((r) => r.id),
      roomNames: pickedRooms.map((r) => r.name)
    };
  }

  return {
    targetType: "todos_inquilinos",
    targetEmails: uniqueMembers,
    targetMemberEmail: "",
    roomId: "",
    roomName: "",
    roomIds: [],
    roomNames: []
  };
}

async function seedFinancialDataForGroup({
  db,
  groupId,
  groupName,
  groupIndex,
  members,
  rooms,
  expensesPerGroup,
  paymentsPerGroup,
  uidByEmail
}) {
  if (!Array.isArray(members) || members.length < 2) {
    return { expenses: 0, payments: 0, deadlines: 0 };
  }

  let deadlinesCount = 0;
  let batch = db.batch();
  let batchOps = 0;

  async function flushBatch() {
    if (batchOps === 0) return;
    await batch.commit();
    batch = db.batch();
    batchOps = 0;
  }

  async function queueSet(ref, data) {
    if (batchOps >= 450) {
      await flushBatch();
    }
    batch.set(ref, data);
    batchOps++;
  }

  for (let e = 0; e < expensesPerGroup; e++) {
    const payer = members[e % members.length];
    const amount = round2(45 + ((groupIndex * 11 + e * 7) % 10) * 12.75);
    const dueOffset = e % 4 === 0 ? -2 : 5 + (e % 9);
    const dueAt = dateFromToday(dueOffset);
    const dueDateText = toIsoDate(dueAt);
    const useWeightedSplit = e % 2 === 1;
    const customSplit = useWeightedSplit
      ? buildWeightedCustomSplit(
          members.map((m) => m.email),
          groupIndex * 13 + e * 5
        )
      : buildEqualCustomSplit(members.map((m) => m.email));
    const roomSelection = chooseRoomSelection(rooms, e + groupIndex);
    const expenseRef = db.collection("expenses").doc();
    const expenseData = {
      groupId,
      concept: `Gasto seed ${groupIndex}-${e + 1}`,
      amount,
      payerId: uidByEmail.get(payer.email) || "",
      payerEmail: payer.email,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      customSplit,
      category: pickExpenseCategory(e + groupIndex),
      priority: pickPriority(e + groupIndex),
      ticketUri: "",
      dueAt,
      dueDateText,
      roomIds: roomSelection.roomIds,
      roomNames: roomSelection.roomNames,
      roomId: roomSelection.roomId,
      roomName: roomSelection.roomName
    };
    await queueSet(expenseRef, expenseData);

    const splitEntries = parseCustomSplit(customSplit);
    for (const splitEntry of splitEntries) {
      if (splitEntry.email === payer.email) continue;
      const deadlineRef = db.collection("payment_deadlines").doc();
      await queueSet(deadlineRef, {
        groupId,
        groupName,
        sourceType: "expense",
        sourceId: expenseRef.id,
        concept: expenseData.concept,
        amount: round2((amount * splitEntry.percent) / 100),
        debtorEmail: splitEntry.email,
        creditorEmail: payer.email,
        dueAt,
        priority: expenseData.priority,
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
      deadlinesCount++;
    }
  }

  for (let p = 0; p < paymentsPerGroup; p++) {
    const from = members[p % members.length];
    const to = members[(p + 1) % members.length];
    if (from.email === to.email) continue;
    const amount = round2(30 + ((groupIndex * 17 + p * 9) % 8) * 10.4);
    const statusCycle = ["pending", "confirmed", "confirmed", "pending", "rejected"];
    const status = statusCycle[p % statusCycle.length];
    const dueOffset = status === "pending" ? (p % 3 === 0 ? -1 : 4 + p) : -(3 + p);
    const dueAt = dateFromToday(dueOffset);
    const dueDateText = toIsoDate(dueAt);
    const roomSelection = chooseRoomSelection(rooms, p + groupIndex + 1);

    const paymentRef = db.collection("payments").doc();
    const paymentData = {
      groupId,
      amount,
      fromEmail: from.email,
      toEmail: to.email,
      category: pickExpenseCategory(p + groupIndex + 2),
      priority: pickPriority(p + groupIndex + 1),
      status,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      dueAt,
      dueDateText,
      concept: `Pago seed ${groupIndex}-${p + 1}`,
      targetType: "miembro",
      roomId: roomSelection.roomId,
      roomName: roomSelection.roomName
    };
    await queueSet(paymentRef, paymentData);

    const paymentDeadlineRef = db.collection("payment_deadlines").doc();
    await queueSet(paymentDeadlineRef, {
      groupId,
      groupName,
      sourceType: "payment",
      sourceId: paymentRef.id,
      concept: paymentData.concept,
      amount,
      debtorEmail: from.email,
      creditorEmail: to.email,
      dueAt,
      priority: paymentData.priority,
      status,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    deadlinesCount++;
  }

  await flushBatch();
  return {
    expenses: expensesPerGroup,
    payments: paymentsPerGroup,
    deadlines: deadlinesCount
  };
}

async function seedRemindersForGroup({
  db,
  groupId,
  groupName,
  ownerUid,
  ownerEmail,
  members,
  rooms,
  remindersPerGroup,
  groupIndex
}) {
  if (remindersPerGroup <= 0) return 0;
  const intervalCycle = [
    { key: "unico", intervalDays: 0, recurring: false },
    { key: "diario", intervalDays: 1, recurring: true },
    { key: "semanal", intervalDays: 7, recurring: true },
    { key: "mensual", intervalDays: 30, recurring: true },
    { key: "personalizado", intervalDays: 10, recurring: true }
  ];

  const batch = db.batch();
  for (let i = 0; i < remindersPerGroup; i++) {
    const interval = intervalCycle[(groupIndex + i) % intervalCycle.length];
    const startAt = dateFromToday(1 + (i % 9));
    startAt.setHours(10, 0, 0, 0);
    const endAt = interval.recurring ? addDays(startAt, interval.intervalDays * (2 + (i % 3))) : null;
    const target = buildReminderTargets(members, rooms, groupIndex + i);
    const reminderRef = db.collection("reminders").doc();
    const reminderCode = Math.abs(javaStringHash(`manual_${reminderRef.id}`));

    batch.set(reminderRef, {
      title: `Recordatorio seed ${groupIndex}-${i + 1}`,
      interval: interval.key,
      intervalDays: interval.intervalDays,
      startAt,
      startDateText: toIsoDate(startAt),
      endAt,
      endDateText: endAt ? toIsoDate(endAt) : "",
      targetType: target.targetType,
      targetEmails: target.targetEmails,
      targetMemberEmail: target.targetMemberEmail,
      roomId: target.roomId,
      roomName: target.roomName,
      roomIds: target.roomIds,
      roomNames: target.roomNames,
      ownerUid,
      ownerEmail,
      groupId,
      groupName,
      reminderCode,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  }
  await batch.commit();
  return remindersPerGroup;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help || args.h) {
    usage();
    return;
  }

  const presetKey = String(args.preset || "").trim().toLowerCase();
  const useSergioDemoPreset = presetKey === SERGIO_DEMO_PRESET.key;
  if (presetKey && !useSergioDemoPreset) {
    throw new Error(`Preset no soportado: ${presetKey}. Usa "${SERGIO_DEMO_PRESET.key}".`);
  }

  const ownerEmail = normalizeEmail(useSergioDemoPreset ? SERGIO_DEMO_PRESET.ownerEmail : args["owner-email"]);
  const ownerPassword = args["owner-password"] ? String(args["owner-password"]) : "";
  const groupsCount = asInt(
    args.groups,
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.groupsCount : 4
  );
  const roomsPerGroup = asInt(
    args.rooms,
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.roomsPerGroup : 3
  );
  const usersCount = asInt(
    args.users,
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.usersCount : 12
  );
  const membersPerGroup = asInt(
    args["members-per-group"],
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.membersPerGroup : 4
  );
  const expensesPerGroup = asInt(
    args["expenses-per-group"],
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.expensesPerGroup : 8
  );
  const paymentsPerGroup = asInt(
    args["payments-per-group"],
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.paymentsPerGroup : 6
  );
  const remindersPerGroup = asInt(
    args["reminders-per-group"],
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.config.remindersPerGroup : 4
  );
  const passwordPrefix = String(args["password-prefix"] || "FlatShareSeed!");
  const emailPrefix = String(args["email-prefix"] || "seeduser");
  const emailDomain = String(args["email-domain"] || "seed.flatshare.local");
  const dryRun = Boolean(args["dry-run"]);
  const serviceAccountPath = args["service-account"] ? String(args["service-account"]) : "";
  const projectId = args["project-id"] ? String(args["project-id"]) : "";

  if (!ownerEmail) {
    usage();
    throw new Error("Falta --owner-email");
  }
  if (
    groupsCount <= 0 ||
    roomsPerGroup <= 0 ||
    usersCount <= 0 ||
    membersPerGroup <= 0 ||
    expensesPerGroup < 0 ||
    paymentsPerGroup < 0 ||
    remindersPerGroup < 0
  ) {
    throw new Error("Valores invalidos: users/groups/rooms/members > 0 y expenses/payments/reminders >= 0.");
  }
  if (
    groupsCount > 200 ||
    roomsPerGroup > 50 ||
    usersCount > 1000 ||
    membersPerGroup > 200 ||
    expensesPerGroup > 100 ||
    paymentsPerGroup > 100 ||
    remindersPerGroup > 100
  ) {
    throw new Error("Limite de seguridad superado. Reduce el tamano del seed.");
  }

  initFirebase({ serviceAccountPath, projectId });
  const auth = admin.auth();
  const db = admin.firestore();

  console.log("Iniciando seed admin...");
  if (useSergioDemoPreset) {
    console.log(`Preset activo: ${SERGIO_DEMO_PRESET.key}`);
  }
  console.log(`Owner: ${ownerEmail}`);
  console.log(
    `Configuracion: users=${usersCount}, groups=${groupsCount}, rooms=${roomsPerGroup}, membersPerGroup=${membersPerGroup}, expensesPerGroup=${expensesPerGroup}, paymentsPerGroup=${paymentsPerGroup}, remindersPerGroup=${remindersPerGroup}`
  );
  if (dryRun) {
    console.log("Modo dry-run: no se escribira nada en Firebase.");
  }

  const owner = await ensureAuthUser(
    auth,
    ownerEmail,
    ownerPassword,
    useSergioDemoPreset ? SERGIO_DEMO_PRESET.ownerDisplayName : "Owner Seed"
  );
  console.log(`Owner ${owner.created ? "creado" : "encontrado"}: ${owner.uid}`);

  const generatedUsers = [];
  if (useSergioDemoPreset) {
    const selectedTenants = SERGIO_DEMO_PRESET.tenants.slice(0, usersCount);
    for (let i = 0; i < selectedTenants.length; i++) {
      const tenant = selectedTenants[i];
      const suffix = String(i + 1).padStart(3, "0");
      const email = normalizeEmail(tenant.email);
      const password = `${passwordPrefix}${tenant.passwordSuffix}`;
      if (!dryRun) {
        const authUser = await ensureAuthUser(auth, email, password, tenant.displayName);
        generatedUsers.push({
          uid: authUser.uid,
          email: authUser.email,
          password,
          username: tenant.username,
          displayName: tenant.displayName
        });
      } else {
        generatedUsers.push({
          uid: `dry_uid_${suffix}`,
          email,
          password,
          username: tenant.username,
          displayName: tenant.displayName
        });
      }
    }
  } else {
    for (let i = 1; i <= usersCount; i++) {
      const suffix = String(i).padStart(3, "0");
      const email = `${emailPrefix}${suffix}@${emailDomain}`.toLowerCase();
      const password = `${passwordPrefix}${suffix}`;
      const displayName = `Inquilino Seed ${i}`;
      if (!dryRun) {
        const authUser = await ensureAuthUser(auth, email, password, displayName);
        generatedUsers.push({
          uid: authUser.uid,
          email: authUser.email,
          password,
          username: `${emailPrefix}${suffix}`,
          displayName
        });
      } else {
        generatedUsers.push({
          uid: `dry_uid_${suffix}`,
          email,
          password,
          username: `${emailPrefix}${suffix}`,
          displayName
        });
      }
    }
  }

  if (!dryRun) {
    const userDocChunks = chunk(generatedUsers, 400);
    for (const docs of userDocChunks) {
      const batch = db.batch();
      for (const u of docs) {
        const userRef = db.collection("users").doc(u.uid);
        batch.set(
          userRef,
          {
            uid: u.uid,
            email: u.email,
            displayName: u.displayName,
            fullName: u.displayName,
            username: u.username,
            phone: "",
            birthDate: "",
            profileCompleted: true
          },
          { merge: true }
        );
        const usernameRef = db.collection("usernames").doc(u.username);
        batch.set(
          usernameRef,
          {
            uid: u.uid,
            email: u.email,
            displayName: u.displayName
          },
          { merge: true }
        );
      }
      await batch.commit();
    }
  }

  const uidByEmail = new Map();
  uidByEmail.set(ownerEmail, owner.uid);
  for (const user of generatedUsers) {
    uidByEmail.set(String(user.email || "").toLowerCase(), user.uid);
  }

  const seedStats = {
    groups: 0,
    rooms: 0,
    expenses: 0,
    payments: 0,
    deadlines: 0,
    reminders: 0
  };

  for (let g = 1; g <= groupsCount; g++) {
    const groupMembers = pickMembers(generatedUsers, g - 1, Math.min(membersPerGroup, generatedUsers.length));
    const memberUids = [owner.uid, ...groupMembers.map((m) => m.uid)];
    const memberEmails = [ownerEmail, ...groupMembers.map((m) => m.email)];
    const financialMembers = [{ uid: owner.uid, email: ownerEmail }, ...groupMembers.map((m) => ({ uid: m.uid, email: m.email }))];
    const roles = { [owner.uid]: "admin" };
    for (const m of groupMembers) {
      roles[m.uid] = "member";
    }

    const groupId = dryRun ? `dry_group_${String(g).padStart(3, "0")}` : db.collection("groups").doc().id;
    const groupName = useSergioDemoPreset && g === 1 ? SERGIO_DEMO_PRESET.groupName : `Piso Seed Admin ${g}`;
    const shareCode = groupId.toUpperCase();

    if (dryRun) {
      console.log(
        `[dry-run] Crearia group ${groupId} (${groupName}) con ${groupMembers.length} inquilinos + owner, ${roomsPerGroup} habitaciones, ${expensesPerGroup} gastos, ${paymentsPerGroup} pagos y ${remindersPerGroup} recordatorios`
      );
      continue;
    }

    const batch = db.batch();
    const groupRef = db.collection("groups").doc(groupId);
    const seededRooms = [];
    batch.set(groupRef, {
      name: groupName,
      description: useSergioDemoPreset && g === 1
        ? SERGIO_DEMO_PRESET.groupDescription
        : "Piso de pruebas generado por firebase-admin seed",
      location: useSergioDemoPreset && g === 1
        ? { ...SERGIO_DEMO_PRESET.location }
        : {
            street: `Calle Seed ${g}`,
            portal: `Portal ${g}`,
            postalCode: `28${String(g).padStart(3, "0")}`,
            city: "Madrid",
            province: "Madrid"
          },
      ownerId: owner.uid,
      roles,
      members: memberUids,
      memberEmails,
      shareCode,
      roomCount: roomsPerGroup,
      billingModel: "variable",
      variableSplitMode: "equal",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    const codeRef = db.collection("group_codes").doc(shareCode);
    batch.set(codeRef, {
      groupId,
      ownerId: owner.uid,
      name: groupName
    });

    for (let r = 1; r <= roomsPerGroup; r++) {
      const roomMember = groupMembers[(r - 1) % groupMembers.length];
      const roomMemberEmails = roomMember ? [roomMember.email] : [];
      const roomRef = db.collection("rooms_groups").doc();
      const roomTemplate = useSergioDemoPreset && g === 1
        ? SERGIO_DEMO_PRESET.roomTemplates[(r - 1) % SERGIO_DEMO_PRESET.roomTemplates.length]
        : null;
      const roomName = roomTemplate ? roomTemplate.name : `Habitacion ${r}`;
      const monthlyCost = roomTemplate ? roomTemplate.monthlyCost : 350 + r * 30;
      batch.set(roomRef, {
        groupId,
        roomNumber: r,
        name: roomName,
        capacity: 2,
        monthlyCost,
        memberEmails: roomMemberEmails,
        memberCount: roomMemberEmails.length,
        createdByUid: owner.uid,
        updatedByUid: owner.uid,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      seededRooms.push({
        id: roomRef.id,
        name: roomName,
        memberEmails: roomMemberEmails
      });
    }

    await batch.commit();
    seedStats.groups += 1;
    seedStats.rooms += seededRooms.length;

    const financialStats = await seedFinancialDataForGroup({
      db,
      groupId,
      groupName,
      groupIndex: g,
      members: financialMembers,
      rooms: seededRooms,
      expensesPerGroup,
      paymentsPerGroup,
      uidByEmail
    });
    seedStats.expenses += financialStats.expenses;
    seedStats.payments += financialStats.payments;
    seedStats.deadlines += financialStats.deadlines;

    const remindersCreated = await seedRemindersForGroup({
      db,
      groupId,
      groupName,
      ownerUid: owner.uid,
      ownerEmail,
      members: financialMembers,
      rooms: seededRooms,
      remindersPerGroup,
      groupIndex: g
    });
    seedStats.reminders += remindersCreated;
  }

  const outputDir = path.join(__dirname, "output");
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  const stamp = safeNowStamp();
  const outputJson = path.join(outputDir, `seed-users-${stamp}.json`);
  fs.writeFileSync(
    outputJson,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        owner: { email: ownerEmail, uid: owner.uid },
        config: {
          groupsCount,
          roomsPerGroup,
          usersCount,
          membersPerGroup,
          expensesPerGroup,
          paymentsPerGroup,
          remindersPerGroup
        },
        seededTotals: seedStats,
        users: generatedUsers.map((u) => ({
          uid: u.uid,
          email: u.email,
          password: u.password,
          username: u.username,
          displayName: u.displayName
        }))
      },
      null,
      2
    ),
    "utf8"
  );

  console.log("Seed completado.");
  if (!dryRun) {
    console.log(
      `Resumen: groups=${seedStats.groups}, rooms=${seedStats.rooms}, expenses=${seedStats.expenses}, payments=${seedStats.payments}, deadlines=${seedStats.deadlines}, reminders=${seedStats.reminders}`
    );
  }
  console.log(`Credenciales guardadas en: ${outputJson}`);
}

main().catch((err) => {
  console.error("Error en seed admin:");
  console.error(err && err.stack ? err.stack : err);
  process.exit(1);
});
