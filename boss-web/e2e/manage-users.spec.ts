import { expect, test } from '@playwright/test';

/**
 * Dynamic per-user menu access (section: sidebar categories/subcategories): a
 * TENANT_ADMIN manages other users' sidebar access from the "Manage users" dialog,
 * and a user only ever sees the categories/subcategories they were actually granted.
 */
test('a limited user only sees the categories/subcategories granted to them, never the full taxonomy', async ({ page }) => {
  const unique = Date.now();
  const tenantSlug = `access-${unique}`;
  const adminEmail = `admin-${unique}@e2e.test`;
  const memberEmail = `member-${unique}@e2e.test`;

  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Access Co');
  await page.getByLabel('Workspace slug').fill(tenantSlug);
  await page.getByLabel('Your full name').fill('Access Admin');
  await page.getByLabel('Your email').fill(adminEmail);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // The bootstrap admin sees the entire taxonomy, including categories with no
  // real panels behind them yet. Categories start collapsed - open each to check.
  await page.getByRole('button', { name: 'Human Resource', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Workforce', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Position', exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Manage users' }).click();
  await expect(page.getByRole('heading', { name: 'Manage users' })).toBeVisible();
  await page.getByRole('button', { name: 'Create user' }).click();

  await page.getByLabel('Email').fill(memberEmail);
  await page.getByLabel('Password').fill('MemberSecret123');
  await page.getByLabel('Full name').fill('Access Member');
  await page.getByLabel('Roles').click();
  await page.getByRole('option', { name: 'EMPLOYEE' }).click();
  await page.keyboard.press('Escape');

  // Grant only Finance -> Position and Profitability.
  const financeToggle = page.locator('mat-dialog-content').locator('.category', { hasText: 'Finance' }).getByRole('switch');
  await financeToggle.click();
  const financeCategory = page.locator('mat-dialog-content').locator('.category', { hasText: 'Finance' });
  await financeCategory.getByRole('checkbox', { name: 'Position' }).click();
  await financeCategory.getByRole('checkbox', { name: 'Profitability' }).click();

  await page.getByRole('button', { name: 'Create', exact: true }).click();
  await expect(page.getByText('Access Member')).toBeVisible();
  await page.keyboard.press('Escape'); // close the dialog

  await page.getByRole('button', { name: /Access Admin/ }).click();
  await page.getByRole('menuitem', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.getByLabel('Email').fill(memberEmail);
  await page.getByLabel('Password').fill('MemberSecret123');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // Only the granted category/subcategories appear; nothing else does, and there's
  // no "Manage users" button since this user isn't a TENANT_ADMIN. Finance is the
  // only category this member has, so open it to check its subcategories.
  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Position', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Profitability', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Workforce', exact: true })).not.toBeVisible();
  await expect(page.getByRole('button', { name: 'Liquidity', exact: true })).not.toBeVisible();
  await expect(page.getByText('Human Resource')).not.toBeVisible();
  await expect(page.getByRole('button', { name: 'Manage users' })).not.toBeVisible();
});

test('a category switched on with no subcategory picked blocks saving, and the picker explains why', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Validation Co');
  await page.getByLabel('Workspace slug').fill(`validation-${unique}`);
  await page.getByLabel('Your full name').fill('Validation Admin');
  await page.getByLabel('Your email').fill(`validation-admin-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('button', { name: 'Manage users' }).click();
  await page.getByRole('button', { name: 'Create user' }).click();

  await page.getByLabel('Email').fill(`validation-member-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('MemberSecret123');
  await page.getByLabel('Full name').fill('Validation Member');
  await page.getByLabel('Roles').click();
  await page.getByRole('option', { name: 'EMPLOYEE' }).click();
  await page.keyboard.press('Escape');

  const createButton = page.getByRole('button', { name: 'Create', exact: true });

  // No category enabled at all - a user with no access yet is a valid, saveable state.
  await expect(createButton).toBeEnabled();

  // Switching a category on without picking any of its subcategories is invalid.
  const hrToggle = page.locator('mat-dialog-content').locator('.category', { hasText: 'Human Resource' }).getByRole('switch');
  await hrToggle.click();
  await expect(page.getByText('Pick at least one, or turn this category off.')).toBeVisible();
  await expect(createButton).toBeDisabled();

  // Picking one subcategory clears the error and re-enables saving.
  await page.locator('mat-dialog-content').locator('.category', { hasText: 'Human Resource' }).getByRole('checkbox', { name: 'Hiring' }).click();
  await expect(page.getByText('Pick at least one, or turn this category off.')).not.toBeVisible();
  await expect(createButton).toBeEnabled();

  // Turning the category back off also clears the invalid state.
  await page.locator('mat-dialog-content').locator('.category', { hasText: 'Human Resource' }).getByRole('checkbox', { name: 'Hiring' }).click();
  await hrToggle.click();
  await expect(createButton).toBeEnabled();
});

test("a TENANT_ADMIN can edit an existing user's access after creation, and it takes effect on their next login", async ({ page }) => {
  const unique = Date.now();
  const adminEmail = `edit-admin-${unique}@e2e.test`;
  const memberEmail = `edit-member-${unique}@e2e.test`;

  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Edit Access Co');
  await page.getByLabel('Workspace slug').fill(`edit-access-${unique}`);
  await page.getByLabel('Your full name').fill('Edit Admin');
  await page.getByLabel('Your email').fill(adminEmail);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('button', { name: 'Manage users' }).click();
  await page.getByRole('button', { name: 'Create user' }).click();
  await page.getByLabel('Email').fill(memberEmail);
  await page.getByLabel('Password').fill('MemberSecret123');
  await page.getByLabel('Full name').fill('Edit Member');
  await page.getByLabel('Roles').click();
  await page.getByRole('option', { name: 'EMPLOYEE' }).click();
  await page.keyboard.press('Escape');

  // Grant nothing at first.
  await page.getByRole('button', { name: 'Create', exact: true }).click();
  await expect(page.getByText('Edit Member')).toBeVisible();

  // Now edit that user's access to grant Risk -> Mitigation.
  await page.getByRole('listitem').filter({ hasText: 'Edit Member' }).getByRole('button', { name: 'Edit access' }).click();
  await expect(page.getByRole('heading', { name: /Edit access/ })).toBeVisible();
  const riskToggle = page.locator('mat-dialog-content').locator('.category', { hasText: 'Risk' }).getByRole('switch');
  await riskToggle.click();
  await page.locator('mat-dialog-content').locator('.category', { hasText: 'Risk' }).getByRole('checkbox', { name: 'Mitigation' }).click();
  await page.getByRole('button', { name: 'Save access' }).click();
  await expect(page.getByRole('heading', { name: 'Manage users' })).toBeVisible();
  await page.keyboard.press('Escape'); // close the dialog

  await page.getByRole('button', { name: /Edit Admin/ }).click();
  await page.getByRole('menuitem', { name: 'Sign out' }).click();

  await page.getByLabel('Email').fill(memberEmail);
  await page.getByLabel('Password').fill('MemberSecret123');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // Risk is the only category granted; open it to reach Mitigation.
  await page.getByRole('button', { name: 'Risk', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Mitigation', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Position', exact: true })).not.toBeVisible();
});
