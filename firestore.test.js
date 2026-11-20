const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { createRequire } = require("node:module");
const r = createRequire(require.resolve("@firebase/rules-unit-testing"));
const firebase = r("firebase/compat/app").default;
r("firebase/compat/firestore");
const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();
const { test, before, after, beforeEach } = require("node:test");
const crypto = require("node:crypto");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256Hex(input) {
  return crypto.createHash("sha256").update(input, "utf8").digest("hex");
}

const ADMIN_SALT = "0123456789abcdef0123456789abcdef";
const ADMIN_USERNAME = "romel_admin";
const ADMIN_PASSWORD = "StrongPassword!2026";
const ADMIN_HASH = sha256Hex(`${ADMIN_SALT}:${ADMIN_USERNAME.toLowerCase()}:${ADMIN_PASSWORD}`);
const WRONG_HASH = sha256Hex(`${ADMIN_SALT}:${ADMIN_USERNAME.toLowerCase()}:WrongPassword`);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function setupAdmin(db) {
  const batch = db.batch();
  batch.set(db.collection("admin_config").doc("status"), {
    initialized: true,
    salt: ADMIN_SALT,
    updatedAt: serverTimestamp(),
  });
  batch.set(db.collection("admin_config").doc("credentials"), {
    credentialHash: ADMIN_HASH,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  await batch.commit();
}

test("Admin setup: initializes once, hides credential hash, and rejects overwrite or weak hashes", async () => {
  const db = testEnv.unauthenticatedContext().firestore();

  // Credentials cannot be created alone without status
  await assertFails(
    db.collection("admin_config").doc("credentials").set({
      credentialHash: ADMIN_HASH,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Valid atomic setup succeeds
  await assertSucceeds(setupAdmin(db));

  // Public client CAN read status (salt & initialized only)
  await assertSucceeds(db.collection("admin_config").doc("status").get());

  // Nobody can read credentials or proofs
  await assertFails(db.collection("admin_config").doc("credentials").get());
  await assertFails(db.collection("admin_auth_proofs").doc("batch_proof").get());
  await assertFails(db.collection("admin_auth_proofs").doc("login_check").get());

  // Re-initializing or overwriting admin credentials/status MUST fail
  await assertFails(
    db.collection("admin_config").doc("credentials").set({
      credentialHash: WRONG_HASH,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    db.collection("admin_config").doc("status").set({
      initialized: true,
      salt: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Admin login proof: succeeds with valid hash and fails with wrong hash", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await setupAdmin(db);

  await assertFails(
    db.collection("admin_auth_proofs").doc("login_check").set({
      proofHash: WRONG_HASH,
      op: "LOGIN",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    db.collection("admin_auth_proofs").doc("login_check").set({
      proofHash: ADMIN_HASH,
      op: "LOGIN",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Products & Categories: Customers read freely; only Admin can add, edit, update prices, manage availability, or delete", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await setupAdmin(db);

  // Customer cannot create category or product directly
  await assertFails(
    db.collection("categories").doc("cat_switches").set({
      name: "Switches",
      description: "Wall switches",
      sortOrder: 1,
      catalogStatus: "ACTIVE",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Admin creates category + 12 products in a batch
  const seedBatch = db.batch();
  seedBatch.set(db.collection("admin_auth_proofs").doc("batch_proof"), {
    proofHash: ADMIN_HASH,
    op: "SEED",
    updatedAt: serverTimestamp(),
  });
  seedBatch.set(db.collection("categories").doc("cat_switches"), {
    name: "Switches",
    description: "Wall switches",
    sortOrder: 1,
    catalogStatus: "ACTIVE",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  for (let i = 1; i <= 12; i++) {
    const code = `RL060${i}`;
    seedBatch.set(db.collection("products").doc(code), {
      code,
      name: `Switch Model ${i}`,
      category: "Switches",
      series: "Splendor RL06",
      description: "Flush mounted switch",
      specifications: "10A | 250V",
      catalogPage: 1,
      imageAsset: "catalog_images/rl0602.jpg",
      unit: "PCS",
      piecesPerBox: 10,
      piecesPerCarton: 100,
      lowStockThreshold: 10,
      needsReview: false,
      catalogStatus: "ACTIVE",
      availabilityStatus: "AVAILABLE",
      updatedBy: ADMIN_USERNAME,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
  await assertSucceeds(seedBatch.commit());

  // Customer can freely read and query active products & categories without login
  await assertSucceeds(db.collection("products").where("catalogStatus", "==", "ACTIVE").get());
  await assertSucceeds(db.collection("categories").where("catalogStatus", "==", "ACTIVE").get());
  await assertSucceeds(db.collection("products").doc("RL0601").get());

  // Customer cannot update prices, stock, availability, or delete even after admin proof exists (no replay)
  await assertFails(
    db.collection("products").doc("RL0601").update({
      sellingPrice: 1.0,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(db.collection("products").doc("RL0601").delete());
  await assertFails(db.collection("categories").doc("cat_switches").delete());

  // Admin updates price + availability + logs StockMovement atomically
  const priceBatch = db.batch();
  priceBatch.set(db.collection("admin_auth_proofs").doc("batch_proof"), {
    proofHash: ADMIN_HASH,
    op: "UPDATE",
    updatedAt: serverTimestamp(),
  });
  priceBatch.update(db.collection("products").doc("RL0601"), {
    sellingPrice: 185.0,
    availabilityStatus: "OUT_OF_STOCK",
    stockQuantity: 0,
    updatedBy: ADMIN_USERNAME,
    updatedAt: serverTimestamp(),
  });
  priceBatch.set(db.collection("stock_movements").doc("mov_1"), {
    productId: "RL0601",
    productCode: "RL0601",
    productName: "Switch Model 1",
    movementType: "PRICE_UPDATE",
    quantityDelta: 0,
    newPrice: 185.0,
    reason: "Admin price update",
    performedByName: ADMIN_USERNAME,
    createdAt: serverTimestamp(),
  });
  await assertSucceeds(priceBatch.commit());

  // Customer cannot delete or mutate stock_movements
  await assertFails(db.collection("stock_movements").doc("mov_1").delete());
  await assertFails(
    db.collection("stock_movements").doc("mov_2").set({
      productId: "RL0601",
      productCode: "RL0601",
      productName: "Hack",
      movementType: "PRICE_UPDATE",
      quantityDelta: 0,
      reason: "Unauthorized",
      performedByName: "Hacker",
      createdAt: serverTimestamp(),
    })
  );

  // Admin updates and deletes category
  const editCatBatch = db.batch();
  editCatBatch.set(db.collection("admin_auth_proofs").doc("batch_proof"), {
    proofHash: ADMIN_HASH,
    op: "UPDATE",
    updatedAt: serverTimestamp(),
  });
  editCatBatch.update(db.collection("categories").doc("cat_switches"), {
    name: "Switches & Dimmers",
    description: "Updated description",
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(editCatBatch.commit());

  const delCatBatch = db.batch();
  delCatBatch.set(db.collection("admin_auth_proofs").doc("batch_proof"), {
    proofHash: ADMIN_HASH,
    op: "DELETE",
    updatedAt: serverTimestamp(),
  });
  delCatBatch.delete(db.collection("categories").doc("cat_switches"));
  await assertSucceeds(delCatBatch.commit());

  // Admin deletes product
  const delProdBatch = db.batch();
  delProdBatch.set(db.collection("admin_auth_proofs").doc("batch_proof"), {
    proofHash: ADMIN_HASH,
    op: "DELETE",
    updatedAt: serverTimestamp(),
  });
  delProdBatch.delete(db.collection("products").doc("RL0601"));
  await assertSucceeds(delProdBatch.commit());
});
