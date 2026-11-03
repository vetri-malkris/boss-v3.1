import { defineConfig, devices } from '@playwright/test';

/**
 * E2E config for the "important end-to-end flows" called out in section 29.
 * Runs against a server the caller starts (docker compose, or `npm start`) -
 * it deliberately does not manage a backend/DB lifecycle itself.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
