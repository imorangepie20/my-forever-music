import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
    testDir: './tests/e2e',
    timeout: 30_000,
    expect: {
        timeout: 5_000,
    },
    use: {
        baseURL: 'http://localhost:5173',
        trace: 'retain-on-failure',
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
    ],
    webServer: {
        command: 'npm run dev -- --host 127.0.0.1',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
        timeout: 120_000,
        env: {
            PLAYWRIGHT_E2E: 'true',
            VITE_API_BASE_URL: '',
            VITE_PUBLIC_HOST: 'localhost',
            VITE_HMR_PROTOCOL: '',
            VITE_HMR_CLIENT_PORT: '',
        },
    },
})
