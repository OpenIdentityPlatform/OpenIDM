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
import { test, expect } from "@playwright/test";
import {
    BASE_URL,
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

async function openMappingsPage(page) {
    await clickDropdownItem(page, /configure/i, "#mapping/");
    await expect(page.locator(".mapping-config-body").first())
        .toBeVisible({ timeout: 30000 });
}

test.describe.serial("Workflow Sample - Provisioning UI smoke", () => {
    test.skip(!IS_WORKFLOW, "Only runs when OPENIDM_SAMPLE=samples/workflow");

    test.beforeEach(async ({ page }) => {
        await loginToAdmin(page);
    });

    // -----------------------------------------------------------------------
    // Mappings page
    // -----------------------------------------------------------------------
    test("Configure > Mappings lists all three workflow-sample mappings", async ({ page }) => {
        await openMappingsPage(page);
        for (const mapping of [MAPPING_ROLES, MAPPING_USERS_IN, MAPPING_USERS_OUT]) {
            await expect(
                page.locator(".mapping-config-body").filter({ hasText: mapping }).first()
            ).toBeVisible({ timeout: 30000 });
        }
        await assertNoErrors(page);
    });

    // -----------------------------------------------------------------------
    // Reconciliations
    // -----------------------------------------------------------------------
    test("Reconcile systemRolesFileRole_managedRole creates 2 managed roles", async ({ page }) => {
        await runReconcileNow(page, MAPPING_ROLES, 2);
        await assertNoErrors(page);
    });

    test("Reconcile systemXmlfileAccounts_managedUser (1st pass) creates top-level manager", async ({ page }) => {
        // First pass creates only top-level managers (manager1) because user1's
        // validSource requires manager1 to already exist. Counts can vary
        // between OpenIDM revisions, so we only assert "completed" + "success".
        await runReconcileNow(page, MAPPING_USERS_IN);
        await assertNoErrors(page);
    });

    test("Reconcile systemXmlfileAccounts_managedUser (2nd pass) creates remaining users", async ({ page }) => {
        await runReconcileNow(page, MAPPING_USERS_IN);
        await assertNoErrors(page);
    });

    // -----------------------------------------------------------------------
    // Manage > Role / User lists reflect reconciled data
    // -----------------------------------------------------------------------
    test("Manage > Role list contains 'employee' and 'manager'", async ({ page }) => {
        await page.goto(ROLES_LIST_URL);
        await expect(page.locator(".page-header h1")).toContainText(/role/i, { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("employee", { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("manager", { timeout: 30000 });
        await assertNoErrors(page);
    });

    test("Manage > User list contains 'user1' and 'manager1'", async ({ page }) => {
        await page.goto(USERS_LIST_URL);
        await expect(page.locator(".page-header h1")).toContainText(/user/i, { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("user1", { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText("manager1", { timeout: 30000 });
        await assertNoErrors(page);
    });

    // -----------------------------------------------------------------------
    // Processes page (Definitions tab) shows the Contractor onboarding workflow
    // -----------------------------------------------------------------------
    test("Manage > Processes > Definitions tab lists 'Contractor onboarding process'", async ({ page }) => {
        await page.goto(PROCESSES_URL);
        await expect(page.locator(".page-header h1")).toBeVisible({ timeout: 30000 });

        // Switch to the "Process Definitions" tab inside the Processes view.
        await page.locator('#processTabs a[href="#processDefinitions"]')
            .waitFor({ state: "visible", timeout: 30000 });
        await page.locator('#processTabs a[href="#processDefinitions"]').click();
        await expect(page.locator("#processDefinitions"))
            .toContainText(/Contractor onboarding process/i, { timeout: 60000 });
        await assertNoErrors(page);
    });
});

// ---------------------------------------------------------------------------
// Self-Service UI - process catalog visible to authorised users
// ---------------------------------------------------------------------------
test.describe.serial("Workflow Sample - Self-Service UI smoke", () => {
    test.skip(!IS_WORKFLOW, "Only runs when OPENIDM_SAMPLE=samples/workflow");

    test("user1 / Welcome1 sees 'Contractor onboarding process' on dashboard", async ({ page }) => {
        await loginToEnduserAs(page, "user1", "Welcome1");
        // Navigate explicitly to the dashboard in case the post-login page differs.
        await page.goto(`${BASE_URL}/#dashboard/`);
        await page.waitForLoadState("networkidle");
        // The Processes panel on the enduser dashboard renders process names
        // returned by the workflow service and filtered by process-access.json.
        await expect(page.locator("body"))
            .toContainText(/Contractor onboarding process/i, { timeout: 60000 });
        await assertNoErrors(page);
    });

    test("manager1 / Welcome1 logs in and reaches the dashboard without errors", async ({ page }) => {
        await loginToEnduserAs(page, "manager1", "Welcome1");
        await page.goto(`${BASE_URL}/#dashboard/`);
        await page.waitForLoadState("networkidle");
        // Manager has access to start the same workflow as well per process-access.json.
        await expect(page.locator("body"))
            .toContainText(/Contractor onboarding process/i, { timeout: 60000 });
        await assertNoErrors(page);
    });
});

