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
import { expect } from "@playwright/test";

export const BASE_URL = process.env.OPENIDM_URL || "http://localhost:8080";
export const CONTEXT_PATH = process.env.OPENIDM_CONTEXT_PATH || "/openidm";
export const ADMIN_USER = process.env.OPENIDM_ADMIN_USER || "openidm-admin";
export const ADMIN_PASS = process.env.OPENIDM_ADMIN_PASS || "openidm-admin";

/** Log in to the Admin UI and wait for the navigation bar to appear. */
export async function loginToAdmin(page) {
    await page.goto(`${BASE_URL}/admin/`);
    await page.waitForSelector("#login", { timeout: 30000 });
    await page.fill("#login", ADMIN_USER);
    await page.fill("#password", ADMIN_PASS);
    await page.click("[type=submit], .btn-primary");
    // Wait for the first dropdown toggle to appear (signals post-login render started).
    await page.waitForSelector(".navbar-nav a.dropdown-toggle", {
        state: "visible",
        timeout: 60000,
    });
    // Configure and Manage toggles are populated by additional async REST calls and
    // may render significantly later than Dashboards in slow CI environments.
    await Promise.all([
        page.locator(".navbar-nav a.dropdown-toggle").filter({ hasText: /configure/i })
            .waitFor({ state: "visible", timeout: 90000 }),
        page.locator(".navbar-nav a.dropdown-toggle").filter({ hasText: /manage/i })
            .waitFor({ state: "visible", timeout: 90000 }),
    ]);
}

/** Log in to the Enduser UI and wait for the navigation bar to appear. */
export async function loginToEnduser(page) {
    await page.goto(`${BASE_URL}/`);
    await page.waitForSelector("#login", { timeout: 30000 });
    await page.fill("#login", ADMIN_USER);
    await page.fill("#password", ADMIN_PASS);
    await page.click("[type=submit], .btn-primary");
    await page.waitForFunction(
        () => document.querySelector("#content") !== null || document.querySelector(".navbar") !== null,
        { timeout: 30000 }
    );
}

/** Assert that no visible .alert-danger elements are present on the page. */
export async function assertNoErrors(page) {
    // Allow up to 3 s for transient notification banners to auto-dismiss before
    // we check. Bootstrap alert-danger elements rendered by the Messages module
    // are hidden via display:none (causing offsetParent to be null) when gone.
    await page.waitForFunction(
        () => [...document.querySelectorAll(".alert-danger")]
                  .every(el => !el.offsetParent),
        { timeout: 3000 }
    ).catch(() => { /* persistent alerts will be caught by the check below */ });

    const alertDangerLocator = page.locator(".alert-danger");
    const count = await alertDangerLocator.count();
    let visibleErrors = 0;
    for (let i = 0; i < count; i++) {
        if (await alertDangerLocator.nth(i).isVisible()) {
            visibleErrors++;
        }
    }
    expect(visibleErrors).toBe(0);
}

/**
 * Open a navbar dropdown by its visible text label and then click a sub-item
 * identified by its href attribute. Waits for the sub-item to become visible
 * before clicking so the dropdown animation has completed.
 */
export async function clickDropdownItem(page, dropdownLabel, itemHref) {
    const toggle = page
        .locator(".navbar-nav a.dropdown-toggle")
        .filter({ hasText: dropdownLabel });
    await toggle.waitFor({ state: "visible", timeout: 30000 });
    await toggle.click();
    const item = page.locator(`.dropdown-menu a[href="${itemHref}"]`).first();
    await item.waitFor({ state: "visible", timeout: 15000 });
    await item.click();
    await page.waitForLoadState("networkidle");
}
