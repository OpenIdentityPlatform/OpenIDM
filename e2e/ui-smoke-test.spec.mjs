// @ts-check
import { test, expect } from "@playwright/test";

const BASE_URL = process.env.OPENIDM_URL || "http://localhost:8080";
const ADMIN_USER = process.env.OPENIDM_ADMIN_USER || "openidm-admin";
const ADMIN_PASS = process.env.OPENIDM_ADMIN_PASS || "openidm-admin";

/** Log in to the Admin UI and wait for the navigation bar to appear. */
async function loginToAdmin(page) {
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
async function loginToEnduser(page) {
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
async function assertNoErrors(page) {
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
async function clickDropdownItem(page, dropdownLabel, itemHref) {
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

test.describe("OpenIDM UI Smoke Tests", () => {

    test("Admin UI login page loads", async ({ page }) => {
        const response = await page.goto(`${BASE_URL}/admin/`);
        expect(response.status()).toBe(200);
        await page.waitForSelector("#login", { timeout: 30000 });
        await page.waitForSelector("#password", { timeout: 5000 });
    });

    test("Admin UI login with openidm-admin succeeds", async ({ page }) => {
        await loginToAdmin(page);
        // Instead of checking the login field is gone, assert navigation appeared
        const navToggle = page.locator(".navbar-nav a.dropdown-toggle").first();
        await expect(navToggle).toBeVisible({ timeout: 30000 });
    });

    test("Enduser UI login page loads", async ({ page }) => {
        const response = await page.goto(`${BASE_URL}/`);
        expect(response.status()).toBe(200);
        await page.waitForSelector("#login", { timeout: 30000 });
        await page.waitForSelector("#password", { timeout: 5000 });
    });

    test("REST API ping is accessible", async ({ request }) => {
        const response = await request.get(`${BASE_URL}/openidm/info/ping`, {
            headers: {
                "X-OpenIDM-Username": ADMIN_USER,
                "X-OpenIDM-Password": ADMIN_PASS,
            },
        });
        expect(response.status()).toBe(200);
        const body = await response.json();
        expect(body.state).toBe("ACTIVE_READY");
    });

    test("REST API config endpoint is accessible", async ({ request }) => {
        const response = await request.get(`${BASE_URL}/openidm/config/ui/configuration`, {
            headers: {
                "X-OpenIDM-Username": ADMIN_USER,
                "X-OpenIDM-Password": ADMIN_PASS,
            },
        });
        expect(response.status()).toBe(200);
        const body = await response.json();
        expect(body.configuration).toBeDefined();
    });

    test("Admin UI - Dashboard page loads after login", async ({ page }) => {
        await loginToAdmin(page);
        await page.goto(`${BASE_URL}/admin/#dashboard/`);
        await page.waitForLoadState("networkidle");
        await assertNoErrors(page);
    });

    test("No JavaScript console errors on Admin UI load", async ({ page }) => {
        const errors = [];
        page.on("pageerror", (err) => errors.push(err.message));

        await page.goto(`${BASE_URL}/admin/`);
        await page.waitForSelector("#login", { timeout: 30000 });

        await page.waitForLoadState("networkidle");

        const criticalErrors = errors.filter(
            (e) => !e.includes("favicon") && !e.includes("404")
        );
        expect(criticalErrors).toEqual([]);
    });
});

// ---------------------------------------------------------------------------
// Admin UI – Navigation Menu
// ---------------------------------------------------------------------------
test.describe("Admin UI - Navigation Menu", () => {
    test.beforeEach(async ({ page }) => {
        await loginToAdmin(page);
    });

    test("Dashboards dropdown opens and navigates to New Dashboard", async ({ page }) => {
        // Verify the Dashboards dropdown toggle is visible
        const toggle = page
            .locator(".navbar-nav a.dropdown-toggle")
            .filter({ hasText: /dashboards/i });
        await toggle.waitFor({ state: "visible", timeout: 10000 });
        await toggle.click();

        // The "New Dashboard" item is always present regardless of existing dashboards
        const newDashboardLink = page
            .locator(".dropdown-menu a[href=\"#newDashboard/\"]")
            .first();
        await newDashboardLink.waitFor({ state: "visible", timeout: 10000 });
        await newDashboardLink.click();
        await page.waitForLoadState("networkidle");
        await assertNoErrors(page);
        expect(page.url()).toContain("newDashboard");
    });

    test("Configure dropdown - Connectors sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#connectors/");
        await assertNoErrors(page);
        expect(page.url()).toContain("connectors");
    });

    test("Configure dropdown - Managed Objects sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#managed/");
        await assertNoErrors(page);
        expect(page.url()).toContain("managed");
    });

    test("Configure dropdown - Mapping sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#mapping/");
        await assertNoErrors(page);
        expect(page.url()).toContain("mapping");
    });

    test("Configure dropdown - Scheduler sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#scheduler/");
        await assertNoErrors(page);
        expect(page.url()).toContain("scheduler");
    });

    test("Configure dropdown - Authentication sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#authentication/");
        await assertNoErrors(page);
        expect(page.url()).toContain("authentication");
    });

    test("Configure dropdown - System Preferences sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#settings/");
        await assertNoErrors(page);
        expect(page.url()).toContain("settings");
    });

    test("Configure dropdown - User Registration sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#selfservice/userregistration/");
        await assertNoErrors(page);
        expect(page.url()).toContain("selfservice/userregistration");
    });

    test("Configure dropdown - Password Reset sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#selfservice/passwordreset/");
        await assertNoErrors(page);
        expect(page.url()).toContain("selfservice/passwordreset");
    });

    test("Configure dropdown - Forgotten Username sub-item navigates correctly", async ({ page }) => {
        await clickDropdownItem(page, /configure/i, "#selfservice/forgotUsername/");
        await assertNoErrors(page);
        expect(page.url()).toContain("selfservice/forgotUsername");
    });

    test("Manage dropdown opens and navigates to Users list", async ({ page }) => {
        // Verify the Manage dropdown toggle is visible
        const toggle = page
            .locator(".navbar-nav a.dropdown-toggle")
            .filter({ hasText: /manage/i });
        await toggle.waitFor({ state: "visible", timeout: 30000 });
        await toggle.click();

        // Wait for at least one managed-object link to appear.
        // The cssClass "navigation-managed-object" is applied to the <li>, not the <a>,
        // so use the descendant selector.
        const firstManagedItem = page.locator("li.navigation-managed-object a").first();
        await firstManagedItem.waitFor({ state: "visible", timeout: 15000 });

        // Click the Users list link (standard managed object in every OpenIDM installation)
        const usersLink = page.locator('a[href="#resource/managed/user/list/"]').first();
        await usersLink.waitFor({ state: "visible", timeout: 10000 });
        await usersLink.click();
        await page.waitForLoadState("networkidle");
        await assertNoErrors(page);
        expect(page.url()).toContain("managed/user/list");
    });
});

// ---------------------------------------------------------------------------
// Enduser UI – Navigation Menu
// ---------------------------------------------------------------------------
test.describe("Enduser UI - Navigation Menu", () => {
    test.beforeEach(async ({ page }) => {
        await loginToEnduser(page);
    });

    test("Dashboard menu item navigates correctly", async ({ page }) => {
        // The Dashboard nav link is a direct link (not a dropdown)
        const dashboardLink = page
            .locator(".navbar-nav a[href='#dashboard/']")
            .first();
        await dashboardLink.waitFor({ state: "visible", timeout: 10000 });
        await dashboardLink.click();
        await page.waitForLoadState("networkidle");
        await assertNoErrors(page);
        expect(page.url()).toContain("dashboard");
    });

    test("Profile menu item navigates correctly", async ({ page }) => {
        const profileLink = page
            .locator(".navbar-nav a[href='#profile/']")
            .first();
        await profileLink.waitFor({ state: "visible", timeout: 10000 });
        await profileLink.click();
        await page.waitForLoadState("networkidle");
        await assertNoErrors(page);
        expect(page.url()).toContain("profile");
    });
});
