#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

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
    --members-per-group 4

Opciones:
  --service-account <ruta-json>   Ruta al JSON de service account (opcional si usas GOOGLE_APPLICATION_CREDENTIALS)
  --owner-email <email>           Email de la cuenta propietaria (obligatorio)
  --owner-password <password>     Password del owner si hay que crearlo
  --users <n>                     Numero de cuentas inquilino a generar (default: 12)
  --groups <n>                    Numero de pisos a crear (default: 4)
  --rooms <n>                     Habitaciones por piso (default: 3)
  --members-per-group <n>         Inquilinos por piso (sin contar owner, default: 4)
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

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help || args.h) {
    usage();
    return;
  }

  const ownerEmail = normalizeEmail(args["owner-email"]);
  const ownerPassword = args["owner-password"] ? String(args["owner-password"]) : "";
  const groupsCount = asInt(args.groups, 4);
  const roomsPerGroup = asInt(args.rooms, 3);
  const usersCount = asInt(args.users, 12);
  const membersPerGroup = asInt(args["members-per-group"], 4);
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
  if (groupsCount <= 0 || roomsPerGroup <= 0 || usersCount <= 0 || membersPerGroup <= 0) {
    throw new Error("Los valores numericos deben ser mayores que 0.");
  }
  if (groupsCount > 200 || roomsPerGroup > 50 || usersCount > 1000 || membersPerGroup > 200) {
    throw new Error("Limite de seguridad superado. Reduce el tamano del seed.");
  }

  initFirebase({ serviceAccountPath, projectId });
  const auth = admin.auth();
  const db = admin.firestore();

  console.log("Iniciando seed admin...");
  console.log(`Owner: ${ownerEmail}`);
  console.log(`Configuracion: users=${usersCount}, groups=${groupsCount}, rooms=${roomsPerGroup}, membersPerGroup=${membersPerGroup}`);
  if (dryRun) {
    console.log("Modo dry-run: no se escribira nada en Firebase.");
  }

  const owner = await ensureAuthUser(auth, ownerEmail, ownerPassword, "Owner Seed");
  console.log(`Owner ${owner.created ? "creado" : "encontrado"}: ${owner.uid}`);

  const generatedUsers = [];
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

  for (let g = 1; g <= groupsCount; g++) {
    const groupMembers = pickMembers(generatedUsers, g - 1, Math.min(membersPerGroup, generatedUsers.length));
    const memberUids = [owner.uid, ...groupMembers.map((m) => m.uid)];
    const memberEmails = [ownerEmail, ...groupMembers.map((m) => m.email)];
    const roles = { [owner.uid]: "admin" };
    for (const m of groupMembers) {
      roles[m.uid] = "member";
    }

    const groupId = dryRun ? `dry_group_${String(g).padStart(3, "0")}` : db.collection("groups").doc().id;
    const groupName = `Piso Seed Admin ${g}`;
    const shareCode = groupId.toUpperCase();

    if (dryRun) {
      console.log(`[dry-run] Crearia group ${groupId} (${groupName}) con ${groupMembers.length} inquilinos + owner`);
      continue;
    }

    const batch = db.batch();
    const groupRef = db.collection("groups").doc(groupId);
    batch.set(groupRef, {
      name: groupName,
      description: "Piso de pruebas generado por firebase-admin seed",
      location: {
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
      batch.set(roomRef, {
        groupId,
        roomNumber: r,
        name: `Habitacion ${r}`,
        capacity: 2,
        monthlyCost: 350 + r * 30,
        memberEmails: roomMemberEmails,
        memberCount: roomMemberEmails.length,
        createdByUid: owner.uid,
        updatedByUid: owner.uid,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    await batch.commit();
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
          membersPerGroup
        },
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
  console.log(`Credenciales guardadas en: ${outputJson}`);
}

main().catch((err) => {
  console.error("Error en seed admin:");
  console.error(err && err.stack ? err.stack : err);
  process.exit(1);
});
