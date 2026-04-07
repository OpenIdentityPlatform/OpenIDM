import { defineConfig } from "@playwright/test";

export default defineConfig({
    testDir: ".",
    testMatch: "**/*.spec.mjs",
    timeout: 180000,
    retries: 1,
    use: {
        headless: true,
        baseURL: process.env.OPENIDM_URL || "http://localhost:8080",
        ignoreHTTPSErrors: true,
        screenshot: "only-on-failure",
        trace: "retain-on-failure",
    },
    reporter: [["list"], ["html", { open: "never", outputFolder: "playwright-report" }]],
});
