// @ts-check
import { test, expect } from "@playwright/test";

const BASE_URL = process.env.OPENIDM_URL || "http://localhost:8080";
const ADMIN_USER = process.env.OPENIDM_ADMIN_USER || "openidm-admin";
const ADMIN_PASS = process.env.OPENIDM_ADMIN_PASS || "openidm-admin";

test.describe("OpenIDM UI Smoke Tests", () => {

    test("Admin UI login page loads", async ({ page }) => {
        const response = await page.goto(`${BASE_URL}/admin/`);
        expect(response.status()).toBe(200);
        await page.waitForSelector("#login", { timeout: 30000 });
        await page.waitForSelector("#password", { timeout: 5000 });
    });

    test("Admin UI login with openidm-admin succeeds", async ({ page }) => {
        await page.goto(`${BASE_URL}/admin/`);
        await page.waitForSelector("#login", { timeout: 30000 });

        await page.fill("#login", ADMIN_USER);
        await page.fill("#password", ADMIN_PASS);
        await page.click("[type=submit], .btn-primary");

        await page.waitForFunction(() => {
            return document.querySelector("#content") !== null
                || document.querySelector(".navbar") !== null
                || document.querySelector("#wrapper") !== null;
        }, { timeout: 30000 });

        const loginField = await page.$("#login");
        const isLoginVisible = loginField ? await loginField.isVisible() : false;
        expect(isLoginVisible).toBe(false);
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
        await page.goto(`${BASE_URL}/admin/`);
        await page.waitForSelector("#login", { timeout: 30000 });
        await page.fill("#login", ADMIN_USER);
        await page.fill("#password", ADMIN_PASS);
        await page.click("[type=submit], .btn-primary");

        await page.waitForFunction(() => {
            return document.querySelector("#content") !== null
                || document.querySelector(".navbar") !== null;
        }, { timeout: 30000 });

        await page.goto(`${BASE_URL}/admin/#dashboard/`);
        await page.waitForLoadState("networkidle");

        const alertDangerLocator = page.locator(".alert-danger");
        const count = await alertDangerLocator.count();
        let visibleErrors = 0;
        for (let i = 0; i < count; i++) {
            if (await alertDangerLocator.nth(i).isVisible()) {
                visibleErrors++;
            }
        }
        expect(visibleErrors).toBe(0);
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
