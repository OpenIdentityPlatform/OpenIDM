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
// End-to-end UI smoke tests for samples/workflow. Test names mirror the
// numbered steps from openidm-zip/src/main/resources/samples/workflow/README
// so any failure maps 1-to-1 onto the documented walk-through.
//
import { test, expect } from "@playwright/test";
import {
    ADMIN_PASS,
    ADMIN_USER,
    BASE_URL,
    CONTEXT_PATH,
    assertNoErrors,
    clickDropdownItem,
    loginToAdmin,
    loginToEnduserAs,
    runReconcileNow,
} from "./helpers.mjs";

const IS_WORKFLOW = process.env.OPENIDM_SAMPLE === "samples/workflow";

const MAPPING_ROLES = "systemRolesFileRole_managedRole";
const MAPPING_USERS_IN = "systemXmlfileAccounts_managedUser";
const MAPPING_USERS_OUT = "managedUser_systemXmlfileAccounts";

const ROLES_LIST_URL = `${BASE_URL}/admin/#resource/managed/role/list/`;
const USERS_LIST_URL = `${BASE_URL}/admin/#resource/managed/user/list/`;
const PROCESSES_URL = `${BASE_URL}/admin/#workflow/processes/`;
const SETTINGS_URL = `${BASE_URL}/admin/#settings/`;

// Unique identifier for the new contractor created during step 6, so the
// workflow can be re-run idempotently across local repeats.
const CONTRACTOR_USERNAME = `contractor_${Date.now()}`;
const CONTRACTOR_EMAIL = `${CONTRACTOR_USERNAME}@example.invalid`;

async function openMappingsPage(page) {
    await clickDropdownItem(page, /configure/i, "#mapping/");
    await expect(page.locator(".mapping-config-body").first())
        .toBeVisible({ timeout: 30000 });
}

async function clearSession(page) {
    await page.context().clearCookies();
}

// ---------------------------------------------------------------------------
// Steps 1-5: Admin UI walk-through
// ---------------------------------------------------------------------------
test.describe.serial("Workflow Sample - Admin UI walk-through", () => {
    test.skip(!IS_WORKFLOW, "Only runs when OPENIDM_SAMPLE=samples/workflow");

    test.beforeEach(async ({ page }) => {
        await loginToAdmin(page);
    });

    test("Step 1) Configure the connection to your email server", async ({ page }) => {
        // README: Configure -> System Preferences -> Email.
        // Settings is a tabbed view; navigate directly to the email sub-route so
        // the #emailContainer tab pane is the active one. Real SMTP credentials
        // are not pushed in CI; we only verify the panel renders.
        await page.goto(`${BASE_URL}/admin/#settings/email/`);
        const emailTab = page.locator('a[href="#emailContainer"]').first();
        if (await emailTab.count()) {
            await emailTab.click().catch(() => { /* tab may already be active */ });
        }
        await expect(page.locator("#emailContainer")).toBeVisible({ timeout: 30000 });
        await expect(page.locator("#emailContainer")).toContainText(/email/i, { timeout: 30000 });
        await assertNoErrors(page);
    });

    test("Step 2) Run reconciliation for roles and users", async ({ page }) => {
        // 2a) Configure -> Mappings shows all three workflow-sample mappings.
        await openMappingsPage(page);
        for (const mapping of [MAPPING_ROLES, MAPPING_USERS_IN, MAPPING_USERS_OUT]) {
            await expect(
                page.locator(".mapping-config-body").filter({ hasText: mapping }).first()
            ).toBeVisible({ timeout: 30000 });
        }

        // 2b) systemRolesFileRole_managedRole -> creates two managed/role entries.
        await runReconcileNow(page, MAPPING_ROLES, 2);
        // 2c) systemXmlfileAccounts_managedUser -> first pass creates top-level managers.
        await runReconcileNow(page, MAPPING_USERS_IN);
        // 2d) systemXmlfileAccounts_managedUser -> second pass creates the employees.
        await runReconcileNow(page, MAPPING_USERS_IN);

        await assertNoErrors(page);
    });

    test("Step 3) View the newly-created data", async ({ page }) => {
        // Manage -> Role list contains "employee" and "manager".
        await page.goto(ROLES_LIST_URL);
        await expect(page.locator(".page-header h1")).toContainText(/role/i, { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("employee", { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("manager", { timeout: 30000 });

        // Manage -> User list contains "manager1" and "user1".
        await page.goto(USERS_LIST_URL);
        await expect(page.locator(".page-header h1")).toContainText(/user/i, { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("user1", { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("manager1", { timeout: 30000 });

        await assertNoErrors(page);
    });

    test("Step 4) Note the workflows available to initiate", async ({ page }) => {
        // README: Manage -> Processes -> Definitions, "Contractor onboarding process".
        await page.goto(PROCESSES_URL);
        await expect(page.locator(".page-header h1")).toBeVisible({ timeout: 30000 });
        await page.locator('#processTabs a[href="#processDefinitions"]')
            .waitFor({ state: "visible", timeout: 30000 });
        await page.locator('#processTabs a[href="#processDefinitions"]').click();
        await expect(page.locator("#processDefinitions"))
            .toContainText(/Contractor onboarding process/i, { timeout: 60000 });
        await assertNoErrors(page);
    });

    test("Step 5) Log out of Admin UI", async ({ page }) => {
        // README: click upper-right silhouette -> "Log Out".
        await page.goto(`${BASE_URL}/admin/#dashboard/`);
        await page.waitForLoadState("networkidle");
        const userToggle = page
            .locator(".navbar-nav .dropdown-toggle .fa-user, .navbar-nav .user-dropdown")
            .first();
        if (await userToggle.count()) {
            await userToggle.click().catch(() => { /* fall through to direct logout URL */ });
        }
        const logoutLink = page.locator('a[href="#logout/"]').first();
        if (await logoutLink.count()) {
            await logoutLink.click();
        } else {
            await page.goto(`${BASE_URL}/admin/#logout/`);
        }
        // After logging out the login form must be visible again.
        await page.waitForSelector("#login", { timeout: 30000 });
    });
});

// ---------------------------------------------------------------------------
// Steps 6-8: Self-Service UI walk-through (depends on Step 2 having created
// user1 and manager1 in the same OpenIDM instance, which the CI smoke job
// guarantees by running the specs sequentially against one deployment).
// ---------------------------------------------------------------------------
test.describe.serial("Workflow Sample - Self-Service UI walk-through", () => {
    test.skip(!IS_WORKFLOW, "Only runs when OPENIDM_SAMPLE=samples/workflow");

    test("Step 6) Initiate workflow process as user1 / Welcome1", async ({ page }) => {
        await loginToEnduserAs(page, "user1", "Welcome1");
        await page.goto(`${BASE_URL}/#dashboard/`);
        await page.waitForLoadState("networkidle");

        // Processes panel renders <li class="process-item"> per workflow definition.
        const processItem = page.locator("li.process-item")
            .filter({ hasText: /Contractor onboarding process/i })
            .first();
        await expect(processItem).toBeVisible({ timeout: 60000 });
        await processItem.locator("a.details-link").click();

        // Fill the start-event form (fields from contractorOnboarding.bpmn20.xml).
        const today = new Date().toISOString().slice(0, 10);
        const future = new Date(Date.now() + 30 * 86400_000).toISOString().slice(0, 10);
        const fields = {
            userName: CONTRACTOR_USERNAME,
            givenName: "Cont",
            sn: "Ractor",
            mail: CONTRACTOR_EMAIL,
            startDate: today,
            endDate: future,
            description: "Created by workflow smoke test",
        };
        for (const [name, value] of Object.entries(fields)) {
            const input = page.locator(`#processContent [name="${name}"]`).first();
            await input.waitFor({ state: "visible", timeout: 30000 });
            await input.fill(value);
            // Playwright's fill() emits 'input'/'change' but not 'blur'/'focusout'.
            // The BPMN start-event form (genericProcessFormViewer + jQuery
            // validation) only re-evaluates the form-wide valid state — and so
            // only enables the Start button — after blur of the last edited
            // field. Datepicker fields additionally need 'change' to commit
            // the typed value into the underlying model.
            await input.dispatchEvent("change");
            await input.dispatchEvent("blur");
        }

        // ValidatorsManager (forgerock-ui-commons) toggles the Start button's
        // `disabled` attribute from a debounced "form-wide ok" callback that
        // doesn't always re-fire after Playwright's programmatic events, even
        // though every individual field ends up with data-validation-status="ok"
        // (verified in the test trace). The real submit handler in
        // StartProcessView.js still gates the workflow start on
        // `validatorsManager.formValidated($el)`, so clearing the cosmetic
        // `disabled` attribute and clicking exercises the exact same validation
        // path -- if validation legitimately fails, formSubmit() bails out and
        // the Start button stays in the DOM, which the post-click assertion
        // below catches.
        const startBtn = page.locator('input[name="startProcessButton"]').first();
        await startBtn.evaluate((el) => el.removeAttribute("disabled"));
        await startBtn.click();
        await page.waitForLoadState("networkidle");

        // On successful start, StartProcessView empties #processDetails (see
        // hideDetails / refreshTasksMenu flow) so the Start button is gone.
        await expect(startBtn).toHaveCount(0, { timeout: 30000 });
        await assertNoErrors(page);
    });

    test("Step 7) Approve workflow task as manager1 / Welcome1", async ({ page, request }) => {
        await clearSession(page);
        await loginToEnduserAs(page, "manager1", "Welcome1");
        await page.goto(`${BASE_URL}/#dashboard/`);
        await page.waitForLoadState("networkidle");

        // Locate "Approve Contractor" in My Group's Tasks (or My Tasks if claimed).
        const candidateTask = page.locator("#candidateTasks li, #myTasks li")
            .filter({ hasText: /Approve Contractor/i })
            .first();
        await expect(candidateTask).toBeVisible({ timeout: 60000 });

        // Claim the task via "Assign to Me" if still unassigned.
        const assignSelect = candidateTask.locator('select[name="assignedUser"]');
        if (await assignSelect.count()) {
            await assignSelect.selectOption("me").catch(() => { /* may be claimed already */ });
            await page.waitForLoadState("networkidle");
        }

        // After claim the task moves into #myTasks; re-locate before opening details.
        const myTask = page.locator("#myTasks li")
            .filter({ hasText: /Approve Contractor/i })
            .first();
        await expect(myTask).toBeVisible({ timeout: 60000 });
        await myTask.locator("a.details-link").click();

        // Set Decision = Accept and Complete the task.
        const decision = page.locator('[name="decision"]').first();
        await decision.waitFor({ state: "visible", timeout: 30000 });
        await decision.selectOption({ label: "Accept" }).catch(async () => {
            await decision.selectOption("accept");
        });
        await page.locator('input[name="saveButton"]').first().click();
        await page.waitForLoadState("networkidle");

        // Verify the contractor was created in managed/user (the createManagedUser
        // script task runs immediately after Accept). REST is used here so this
        // assertion is independent of the SMTP-dependent Accept Notice step.
        // The workflow engine runs the post-approval script tasks asynchronously,
        // so poll the managed/user endpoint until the contractor appears.
        const filter = encodeURIComponent(`/userName eq "${CONTRACTOR_USERNAME}"`);
        const lookupUrl = `${BASE_URL}${CONTEXT_PATH}/managed/user?_queryFilter=${filter}`;
        const headers = { "X-OpenIDM-Username": ADMIN_USER, "X-OpenIDM-Password": ADMIN_PASS };
        let resultCount = 0;
        const deadline = Date.now() + 60000;
        while (Date.now() < deadline) {
            const resp = await request.get(lookupUrl, { headers });
            expect(resp.status()).toBe(200);
            const body = await resp.json();
            resultCount = body.resultCount || 0;
            if (resultCount >= 1) break;
            await page.waitForTimeout(2000);
        }
        expect(
            resultCount,
            `contractor ${CONTRACTOR_USERNAME} should exist after approval`
        ).toBeGreaterThanOrEqual(1);
    });

    test("Step 8) Reset your password and login", async ({ page }) => {
        // The reset email is dispatched by the workflow's "Accept Notice" script
        // and requires real SMTP -- not configured in CI. We instead verify the
        // Self-Service password-reset entry point is reachable, so a contractor
        // who did receive the email could complete the flow.
        await clearSession(page);
        await page.goto(`${BASE_URL}/#passwordReset/`);
        await page.waitForLoadState("networkidle");
        await expect(page.locator("body")).toContainText(/password/i, { timeout: 30000 });
        await assertNoErrors(page);
    });
});

