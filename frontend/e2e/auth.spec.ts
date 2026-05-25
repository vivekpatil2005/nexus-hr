import { test, expect } from '@playwright/test';

test.describe('NexusHR Authentication E2E Flows', () => {

  test('should fail login with invalid credentials', async ({ page }) => {
    // Navigate to login
    await page.goto('/login');

    // Fill form
    await page.fill('input[type="text"], input[placeholder*="Username"]', 'admin');
    await page.fill('input[type="password"]', 'wrongpassword');

    // Click Sign In
    await page.click('button[type="submit"]');

    // Verify error notification is shown
    // In React Hot Toast, toast messages typically render in an alert role or list
    const toast = page.locator('text=Authentication failed');
    await expect(toast).toBeVisible();

    // Check we are still on login page
    await expect(page).toHaveURL(/.*login/);
  });

  test('should successfully login and load dashboard', async ({ page }) => {
    // Navigate to login
    await page.goto('/login');

    // Fill valid credentials
    await page.fill('input[type="text"], input[placeholder*="Username"]', 'admin');
    await page.fill('input[type="password"]', 'admin123');

    // Click Sign In
    await page.click('button[type="submit"]');

    // Verify redirect to dashboard
    await expect(page).toHaveURL('http://localhost:5173/');

    // Verify Dashboard sections are loaded
    await expect(page.locator('h1:has-text("Dashboard")')).toBeVisible();
    await expect(page.locator('text=Total Employees')).toBeVisible();
    await expect(page.locator('text=Recent Employees')).toBeVisible();

    // Perform logout
    await page.click('button:has-text("Logout"), button:has-text("Sign Out")');
    
    // Verify redirect back to login
    await expect(page).toHaveURL(/.*login/);
  });
});
