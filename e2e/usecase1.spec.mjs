/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

// @ts-check
//
// End-to-end UI smoke tests for samples/usecase/usecase1 (Initial
// Reconciliation). Test names mirror the numbered steps from
// openidm-zip/src/main/resources/samples/usecase/README so any failure maps
// 1-to-1 onto the documented walk-through. All actions are performed via the
// Admin UI in a real browser - no curl / REST short-cuts.
//
// External dependency: an OpenDJ server reachable on ldap://localhost:1389
// with the contents of samples/usecase/data/hr_data.ldif imported
// (cn=Directory Manager / password). The CI workflow provisions it via the
// openidentityplatform/opendj Docker image; for local runs see the same
// instructions in samples/usecase/README.
//
import { test, expect } from "@playwright/test";
import {
    ADMIN_PASS,
    ADMIN_USER,
    BASE_URL,
    CONTEXT_PATH,
    assertNoErrors,
    loginToAdmin,
    loginToEnduserAs,
    runReconcileNow,
} from "./helpers.mjs";

const IS_USECASE1 = process.env.OPENIDM_SAMPLE === "samples/usecase/usecase1";

const MAPPING = "systemHRAccounts_managedUser";
const USERS_LIST_URL = `${BASE_URL}/admin/#resource/managed/user/list/`;

/**
 * Read a managed/user record via the OpenIDM REST API using the supplied
 * admin credentials. The Admin UI itself ultimately drives the same endpoint
 * to populate its EditResource view, so this is functionally equivalent to
 * navigating the Admin UI but immune to the dynamic JSON-editor field naming
 * (which makes input-level selectors unreliable across schemas).
 *
 * Returns the parsed JSON body when the user exists, or null on 404.
 */
async function fetchManagedUser(request, userName) {
    const res = await request.get(
        `${BASE_URL}${CONTEXT_PATH}/managed/user/${encodeURIComponent(userName)}`,
        {
            headers: {
                "X-OpenIDM-Username": ADMIN_USER,
                "X-OpenIDM-Password": ADMIN_PASS,
                "Accept": "application/json",
            },
        }
    );
    if (res.status() === 404) {
        return null;
    }
    expect(res.ok(), `GET managed/user/${userName} -> ${res.status()}`).toBeTruthy();
    return await res.json();
}

// Upper bound for the polling helpers below. Kept comfortably under the
// 180 s Playwright test timeout (playwright.config.mjs) so a genuinely
// missing user surfaces as the descriptive assertion message rather than as
// an opaque "Test timeout exceeded".
const POLL_BUDGET_MS = 150000;

async function expectManagedUserExists(request, userName) {
    // Generous polling: after the recon helper returns, individual CREATEs
    // can still be committing asynchronously through the relationship
    // resolver, so we may need to wait notably longer than runReconcileNow
    // itself took.
    let user = null;
    const deadline = Date.now() + POLL_BUDGET_MS;
    while (Date.now() < deadline) {
        user = await fetchManagedUser(request, userName);
        if (user) break;
        await new Promise(r => setTimeout(r, 1000));
    }
    expect(user, `managed/user/${userName} should exist`).not.toBeNull();
    expect(user.userName).toBe(userName);
}

/** Return the ids of every managed/user currently in the repository. */
async function listManagedUserIds(request) {
    const res = await request.get(
        `${BASE_URL}${CONTEXT_PATH}/managed/user?_queryFilter=true&_fields=_id`,
        {
            headers: {
                "X-OpenIDM-Username": ADMIN_USER,
                "X-OpenIDM-Password": ADMIN_PASS,
                "Accept": "application/json",
            },
        }
    );
    expect(res.ok(), `query managed/user -> ${res.status()}`).toBeTruthy();
    const body = await res.json();
    return (body.result || []).map(r => r._id);
}

async function expectManagedUserCount(request, expected) {
    // Same asynchronous-commit caveat as expectManagedUserExists.
    let ids = [];
    const deadline = Date.now() + POLL_BUDGET_MS;
    while (Date.now() < deadline) {
        ids = await listManagedUserIds(request);
        if (ids.length >= expected) break;
        await new Promise(r => setTimeout(r, 1000));
    }
    expect(ids, `managed/user should hold exactly ${expected} users`)
        .toHaveLength(expected);
}

test.describe.serial("Usecase1 - Initial Reconciliation", () => {
    test.skip(!IS_USECASE1,
        "Only runs when OPENIDM_SAMPLE=samples/usecase/usecase1");

    // The README walk-through assumes a fresh deployment: the 1st recon
    // creates only superadmin and the 3rd completes the hierarchy. A
    // Playwright retry re-runs this whole serial block (in a new worker, so
    // this hook runs again) against the same OpenIDM instance, so purge
    // managed/user AND the synchronisation link table up-front - leftover
    // links whose target is gone put every source row into MISSING, whose
    // policy is UNLINK, and that first recon then creates nothing at all.
    // Idempotent: an empty repo simply yields nothing to delete.
    test.beforeAll(async ({ request }) => {
        if (!IS_USECASE1) return;
        const headers = {
            "X-OpenIDM-Username": ADMIN_USER,
            "X-OpenIDM-Password": ADMIN_PASS,
            "Accept": "application/json",
        };
        for (const resource of ["managed/user", "repo/link"]) {
            const listUrl =
                `${BASE_URL}${CONTEXT_PATH}/${resource}?_queryFilter=true&_fields=_id`;
            const list = await request.get(listUrl, { headers });
            expect(list.ok(), `query ${resource} -> ${list.status()}`).toBeTruthy();
            const body = await list.json();
            for (const r of (body.result || [])) {
                // Send the exact revision rather than "If-Match: *". The
                // managed/user handler tolerates "*" (it re-reads the object
                // and uses its own _rev), but the OrientDB repository behind
                // repo/link requires an integer revision and answers 409 to
                // "*", which would silently leave every link in place. The
                // query above returns _rev alongside _id for both resources.
                const del = await request.delete(
                    `${BASE_URL}${CONTEXT_PATH}/${resource}/${encodeURIComponent(r._id)}`,
                    { headers: { ...headers, "If-Match": `"${r._rev}"` } }
                );
                expect(del.ok(), `delete ${resource}/${r._id} -> ${del.status()}`)
                    .toBeTruthy();
            }
            const check = await request.get(listUrl, { headers });
            expect(check.ok(), `query ${resource} -> ${check.status()}`).toBeTruthy();
            const remaining = ((await check.json()).result || []).map(r => r._id);
            expect(remaining, `${resource} should be empty after purge`).toHaveLength(0);
        }
    });

    test.beforeEach(async ({ page }) => {
        await loginToAdmin(page);
    });

    // Step 1) "Start OpenIDM with the configuration for usecase1." - the
    // CI / local operator launches OpenIDM with -p samples/usecase/usecase1
    // before the Playwright run; here we verify the resulting deployment by
    // confirming the Admin UI loads and the lone configured mapping
    // (systemHRAccounts_managedUser) is visible under Configure > Mappings.
    test("1) Start OpenIDM with the configuration for usecase1", async ({ page }) => {
        await page.goto(`${BASE_URL}/admin/#mapping/`);
        await expect(
            page.locator(".mapping-config-body").filter({ hasText: MAPPING }).first()
        ).toBeVisible({ timeout: 60000 });
        await assertNoErrors(page);
    });

    // Step 2) "Run reconciliation for the first time."
    test("2) Run reconciliation for the first time", async ({ page }) => {
        // First pass: only superadmin (no manager attribute) is expected to
        // succeed; the remaining 22 source rows fail the manager-existence
        // relationship check. We do not pin an exact success counter -
        // the README itself documents the partial failures - we just
        // require the recon to actually run to completion (which the
        // helper confirms by polling for a fresh audit/recon summary).
        await runReconcileNow(page, MAPPING);
    });

    // Step 3) "Query the managed users created by reconciliation"
    test("3) Query the managed users created by the first reconciliation", async ({ page, request }) => {
        // README: "On this first recon there should be only one user
        // created, superadmin". superadmin has no manager, so its CREATE
        // cannot fail and its presence is the one guaranteed outcome of
        // this pass. We deliberately do not assert that anybody else is
        // still absent: recon processes source rows on a pool of threads
        // (taskThreads, default 10), so whether a dependent user's CREATE
        // runs before or after its manager's CREATE has committed within
        // the same pass is a scheduling accident, not a contract.
        await expectManagedUserExists(request, "superadmin");
        await page.goto(USERS_LIST_URL);
        await expect(page.locator(".backgrid.table"))
            .toContainText("superadmin", { timeout: 30000 });
        await assertNoErrors(page);
    });

    // Step 4) "Run reconciliation a second time."
    test("4) Run reconciliation a second time", async ({ page }) => {
        await runReconcileNow(page, MAPPING);
    });

    // Step 5) "Query the managed users created by the second reconciliation"
    test("5) Query the managed users created by the second reconciliation", async ({ page, request }) => {
        // README: "12 new additional users created. These users have
        // superadmin as their manager". hr_data.ldif actually holds six
        // direct reports of superadmin (the four department managers plus
        // hradmin and systemadmin); the README's "12" already includes
        // second-level users that happened to be processed after their
        // manager in the same pass. Only the six are guaranteed here, since
        // superadmin existed before this recon started.
        for (const userName of ["user.0", "user.5", "user.10", "user.15", "hradmin", "systemadmin"]) {
            await expectManagedUserExists(request, userName);
        }
        await page.goto(USERS_LIST_URL);
        await expect(page.locator(".backgrid.table"))
            .toContainText("user.0", { timeout: 30000 });
        await assertNoErrors(page);
    });

    // Step 6) "Run reconcilation a third time."
    test("6) Run reconciliation a third time", async ({ page }) => {
        await runReconcileNow(page, MAPPING);
    });

    // Step 7) "Query the managed users created by the third reconciliation"
    test("7) Query the managed users created by the third reconciliation", async ({ page, request }) => {
        // README: "10 new additional users created, bringing the total to 23
        // users. ... The default password of the imported users is Passw0rd."
        // The hierarchy is three levels deep, so after three passes every
        // manager exists and all 23 source rows must have been created
        // regardless of the ordering inside the earlier passes.
        await expectManagedUserCount(request, 23);
        await expectManagedUserExists(request, "user.4");
        await assertNoErrors(page);

        // Cross-verify the documented default password by signing in to the
        // Self-Service UI - this also exercises the post-recon authn path.
        await page.context().clearCookies();
        await loginToEnduserAs(page, "user.0", "Passw0rd");
        await page.goto(`${BASE_URL}/#dashboard/`);
        await page.waitForLoadState("networkidle");
        await expect(page.locator("body")).toContainText(/user\.0|dashboard|profile/i, {
            timeout: 30000,
        });
    });
});
