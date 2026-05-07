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
import { BASE_URL, assertNoErrors, clickDropdownItem, loginToAdmin } from "./helpers.mjs";

const IS_GETTING_STARTED = process.env.OPENIDM_SAMPLE === "samples/getting-started";
const MAPPING_NAME = "HumanResources_Engineering";
const MAPPING_PROPERTIES_URL = `${BASE_URL}/admin/#properties/${MAPPING_NAME}/`;
const ENGINEERING_LIST_URL = `${BASE_URL}/admin/#resource/system/engineering/account/list/`;

async function openMappingsPage(page) {
    await clickDropdownItem(page, /configure/i, "#mapping/");
    await expect(page.locator(".mapping-config-body").filter({ hasText: MAPPING_NAME }).first())
        .toBeVisible({ timeout: 30000 });
}

async function openMappingProperties(page) {
    await page.goto(MAPPING_PROPERTIES_URL);
    await expect(page.locator("h1")).toContainText(MAPPING_NAME, { timeout: 30000 });
    await page.locator("#propertiesTab").waitFor({ state: "visible", timeout: 30000 });
    await expect(page.locator("#propertiesTab")).toHaveClass(/active/, { timeout: 30000 });
    await expect(page.locator("#attributesGridHolder")).toBeVisible({ timeout: 30000 });
}

async function chooseJaneSanchezSample(page) {
    // Selectize replaces the original <select id="findSampleSource"> with a
    // generated text input named "<id>-selectized" that the user actually types into.
    const sampleSourceInput = page.locator("#findSampleSource-selectized");
    await sampleSourceInput.waitFor({ state: "visible", timeout: 30000 });
    await sampleSourceInput.click();
    await sampleSourceInput.fill("Sanchez");

    const janeOption = page.locator(".selectize-dropdown .option, .selectize-dropdown .fr-search-option")
        .filter({ hasText: /Jane[\s\S]*Sanchez/i })
        .first();
    await janeOption.waitFor({ state: "visible", timeout: 15000 });
    await janeOption.click();

    // After selecting the sample source the AttributesGrid re-renders with sample
    // values appended in parentheses next to the source/target property cells.
    await expect(page.locator("#attributesGridHolder")).toContainText("Jane", { timeout: 30000 });
    await expect(page.locator("#attributesGridHolder")).toContainText("Sanchez", { timeout: 30000 });
    await expect(page.locator("#attributesGridHolder")).toContainText("jsanchez@example.com", { timeout: 30000 });
}

test.describe.serial("Getting Started Sample - HumanResources_Engineering UI", () => {
    test.skip(!IS_GETTING_STARTED, "Only runs when OPENIDM_SAMPLE=samples/getting-started");

    test.beforeEach(async ({ page }) => {
        await loginToAdmin(page);
    });

    test("Configure > Mappings page lists HumanResources_Engineering", async ({ page }) => {
        await openMappingsPage(page);
        await assertNoErrors(page);
    });

    test("Open HumanResources_Engineering mapping > Properties tab shows expected attributes", async ({ page }) => {
        await openMappingsPage(page);
        await page.locator(".mapping-config-body").filter({ hasText: MAPPING_NAME }).first().click();

        await expect(page).toHaveURL(new RegExp(`/admin/#properties/${MAPPING_NAME}/?$`), { timeout: 30000 });
        await expect(page.locator("#propertiesTab")).toHaveClass(/active/, { timeout: 30000 });

        for (const attribute of ["firstname", "lastname", "email", "telephoneNumber"]) {
            await expect(page.locator("#attributesGridHolder")).toContainText(attribute, { timeout: 30000 });
        }

        await assertNoErrors(page);
    });

    test("Sample Source 'Sanchez' shows Jane Sanchez dropdown and selecting populates preview", async ({ page }) => {
        await openMappingProperties(page);
        await chooseJaneSanchezSample(page);
        await assertNoErrors(page);
    });

    test("Reconcile Now completes successfully with 3 entries", async ({ page }) => {
        await openMappingProperties(page);
        await chooseJaneSanchezSample(page);

        await page.evaluate(() => window.scrollTo(0, 0));
        await page.locator("#syncNowButton").click();

        // syncLabel switches to the "Last reconciled" / "Completed" translation when
        // the recon ends successfully (see MappingBaseView.setReconEnded).
        await expect(page.locator("#syncLabel")).toContainText(/completed/i, { timeout: 120000 });

        // Expand the sync details widget so the entry counters render.
        await page.locator("#syncStatus").click();
        await expect(page.locator("#syncStatusDetails")).toBeVisible({ timeout: 30000 });
        await expect(page.locator("#syncStatusDetails")).toContainText(/success/i, { timeout: 30000 });
        await expect(page.locator("#syncStatusDetails .success-display.display-number"))
            .toHaveText("3", { timeout: 30000 });

        await assertNoErrors(page);
    });

    test("Reconciled Jane Sanchez appears in Manage > engineering system list", async ({ page }) => {
        await page.goto(ENGINEERING_LIST_URL);
        await expect(page.locator(".page-header h1")).toContainText(/engineering/i, { timeout: 30000 });
        await expect(page.locator(".backgrid.table")).toContainText(/Sanchez/i, { timeout: 30000 });
        await expect(page.locator(".backgrid.table"))
            .toContainText(/Jane|jsanchez@example\.com/i, { timeout: 30000 });
        await assertNoErrors(page);
    });
});
