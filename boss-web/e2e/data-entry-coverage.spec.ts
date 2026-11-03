import { expect, test } from '@playwright/test';

/**
 * Exhaustive coverage: exercises every subcategory data-entry form that isn't
 * already covered by data-entry.spec.ts (Hiring, Capital, Reporting, Findings,
 * Tax/GST, Risk Exposure). For each one: fill every required field with minimal
 * valid data, submit, and confirm it actually saved (not just that the button
 * looked clickable). Grouped one test per top-level category to keep signups few.
 */
async function signUp(page: import('@playwright/test').Page, name: string) {
  const unique = Date.now() + Math.floor(Math.random() * 1000);
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill(`${name} Co`);
  await page.getByLabel('Workspace slug').fill(`${name.toLowerCase()}-${unique}`);
  await page.getByLabel('Your full name').fill(`${name} Admin`);
  await page.getByLabel('Your email').fill(`${name.toLowerCase()}-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

test('Human Resource: Workforce, People Cost, Productivity, Attrition, Capability all save real records', async ({ page }) => {
  await signUp(page, 'HrCoverage');
  await page.getByRole('button', { name: 'Human Resource', exact: true }).click();

  await page.getByRole('button', { name: 'Workforce', exact: true }).click();
  await page.getByLabel('Record date *').fill('2026-01-01');
  await page.getByLabel('Department *').fill('Engineering');
  await page.getByLabel('Active headcount *').fill('42');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: '42' })).toBeVisible();

  await page.getByRole('button', { name: 'People Cost', exact: true }).click();
  await page.getByLabel('Period (e.g. 2026-03) *').fill('2026-03');
  await page.getByLabel('Department *').fill('Engineering');
  await page.getByLabel('Gross salary cost *').fill('185000');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: '185000' })).toBeVisible();

  await page.getByRole('button', { name: 'Productivity', exact: true }).click();
  await page.getByLabel('Period (e.g. 2026-03) *').fill('2026-03');
  await page.getByLabel('Department *').fill('Sales');
  await page.getByLabel('Output metric name *').fill('Revenue per rep');
  await page.getByLabel('Output value *').fill('50000');
  await page.getByLabel('FTE count *').fill('8');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Revenue per rep' })).toBeVisible();

  await page.getByRole('button', { name: 'Attrition', exact: true }).click();
  await page.getByLabel('Employee name/ID *').fill('E-4821');
  await page.getByLabel('Department *').fill('Support');
  await page.getByLabel('Separation date *').fill('2026-02-15');
  await page.getByLabel('Separation type *').click();
  await page.getByRole('option', { name: 'VOLUNTARY', exact: true }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'E-4821' })).toBeVisible();

  await page.getByRole('button', { name: 'Capability', exact: true }).click();
  await page.getByLabel('Employee name/ID *').fill('E-1190');
  await page.getByLabel('Department *').fill('Engineering');
  await page.getByLabel('Skill / certification name *').fill('AWS Solutions Architect');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'IN_PROGRESS' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'AWS Solutions Architect' })).toBeVisible();
});

test('Accounting: Working Capital, Assets/Liabilities, Reconciliation, Controls all save real records', async ({ page }) => {
  await signUp(page, 'AcctCoverage');
  await page.getByRole('button', { name: 'Accounting', exact: true }).click();

  await page.getByRole('button', { name: 'Working Capital', exact: true }).click();
  await page.getByLabel('As-of date *').fill('2026-03-31');
  await page.getByLabel('Current assets *').fill('500000');
  await page.getByLabel('Current liabilities *').fill('320000');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: '500000' })).toBeVisible();

  await page.getByRole('button', { name: 'Assets/Liabilities', exact: true }).click();
  await page.getByLabel('As-of date *').fill('2026-03-31');
  await page.getByLabel('Item name *').fill('Office equipment');
  await page.getByLabel('Type *').click();
  await page.getByRole('option', { name: 'ASSET' }).click();
  await page.getByLabel('Category *').click();
  await page.getByRole('option', { name: 'FIXED_ASSET' }).click();
  await page.getByLabel('Value *').fill('75000');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Office equipment' })).toBeVisible();

  await page.getByRole('button', { name: 'Reconciliation', exact: true }).click();
  await page.getByLabel('Account name *').fill('Operating account');
  await page.getByLabel('Period (e.g. 2026-03)', { exact: false }).fill('2026-03');
  await page.getByLabel('Book balance *').fill('120000');
  await page.getByLabel('Statement balance *').fill('120000');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'RECONCILED', exact: true }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Operating account' })).toBeVisible();

  await page.getByRole('button', { name: 'Controls', exact: true }).click();
  await page.getByLabel('Control name *').fill('Month-end close checklist');
  await page.getByLabel('Owner *').fill('Controller');
  await page.getByLabel('Frequency *').click();
  await page.getByRole('option', { name: 'MONTHLY' }).click();
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'EFFECTIVE', exact: true }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Month-end close checklist' })).toBeVisible();
});

test('Auditing: Audit Status, Controls, Exceptions, Actions all save real records', async ({ page }) => {
  await signUp(page, 'AuditCoverage');
  await page.getByRole('button', { name: 'Auditing', exact: true }).click();

  await page.getByRole('button', { name: 'Audit Status', exact: true }).click();
  await page.getByLabel('Audit name *').fill('FY26 SOX Audit');
  await page.getByLabel('Audit type *').click();
  await page.getByRole('option', { name: 'INTERNAL' }).click();
  await page.getByLabel('Start date *').fill('2026-01-10');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'IN_PROGRESS' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'FY26 SOX Audit' })).toBeVisible();

  await page.getByRole('button', { name: 'Controls', exact: true }).click();
  await page.getByLabel('Control name *').fill('Access review');
  await page.getByLabel('Owner *').fill('IT Security');
  await page.getByLabel('Test frequency *').click();
  await page.getByRole('option', { name: 'QUARTERLY' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Access review' })).toBeVisible();

  await page.getByRole('button', { name: 'Exceptions', exact: true }).click();
  await page.getByLabel('Exception description *').fill('Unreviewed vendor payment');
  await page.getByLabel('Identified date *').fill('2026-02-20');
  await page.getByLabel('Risk level *').click();
  await page.getByRole('option', { name: 'MEDIUM' }).click();
  await page.getByLabel('Resolution status *').click();
  await page.getByRole('option', { name: 'OPEN' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Unreviewed vendor payment' })).toBeVisible();

  await page.getByRole('button', { name: 'Actions', exact: true }).click();
  await page.getByLabel('Description *').fill('Implement quarterly access recertification');
  await page.getByLabel('Owner *').fill('IT Security');
  await page.getByLabel('Due date *').fill('2026-04-30');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'NOT_STARTED' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Implement quarterly access recertification' })).toBeVisible();
});

test('Compliance: Compliance Status, Regulatory, Filings, Exceptions all save real records', async ({ page }) => {
  await signUp(page, 'CompCoverage');
  await page.getByRole('button', { name: 'Compliance', exact: true }).click();

  await page.getByRole('button', { name: 'Compliance Status', exact: true }).click();
  await page.getByLabel('Requirement name *').fill('Data residency policy');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'COMPLIANT', exact: true }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Data residency policy' })).toBeVisible();

  await page.getByRole('button', { name: 'Regulatory', exact: true }).click();
  await page.getByLabel('Regulation name *').fill('GDPR');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'COMPLIANT', exact: true }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'GDPR' })).toBeVisible();

  await page.getByRole('button', { name: 'Filings', exact: true }).click();
  await page.getByLabel('Filing name *').fill('Annual return');
  await page.getByLabel('Due date *').fill('2026-05-31');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'PENDING' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Annual return' })).toBeVisible();

  await page.getByRole('button', { name: 'Exceptions', exact: true }).click();
  await page.getByLabel('Description *').fill('Late supplier onboarding check');
  await page.getByLabel('Risk level *').click();
  await page.getByRole('option', { name: 'LOW' }).click();
  await page.getByLabel('Identified date *').fill('2026-02-05');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'OPEN' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Late supplier onboarding check' })).toBeVisible();
});

test('Risk register: Financial, Operational, Compliance, Technology all save real records', async ({ page }) => {
  await signUp(page, 'RiskCoverage');
  await page.getByRole('button', { name: 'Risk', exact: true }).click();

  await page.getByRole('button', { name: 'Financial', exact: true }).click();
  await page.getByLabel('Risk title *').fill('FX exposure on USD receivables');
  await page.getByLabel('Financial area *').click();
  await page.getByRole('option', { name: 'MARKET' }).click();
  await page.getByLabel('Likelihood *').click();
  await page.getByRole('option', { name: 'MEDIUM' }).click();
  await page.getByLabel('Impact *').click();
  await page.getByRole('option', { name: 'MEDIUM' }).click();
  await page.getByLabel('Identified date *').fill('2026-01-20');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'FX exposure on USD receivables' })).toBeVisible();

  await page.getByRole('button', { name: 'Operational', exact: true }).click();
  await page.getByLabel('Risk title *').fill('Single point of failure in fulfilment');
  await page.getByLabel('Likelihood *').click();
  await page.getByRole('option', { name: 'LOW' }).click();
  await page.getByLabel('Impact *').click();
  await page.getByRole('option', { name: 'HIGH' }).click();
  await page.getByLabel('Identified date *').fill('2026-02-01');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Single point of failure in fulfilment' })).toBeVisible();

  // "Compliance" is ambiguous here - it's both the top-level Compliance category
  // header and this Risk subcategory. Scoping to the open accordion region (only
  // Risk's subcategories, correctly excluding other collapsed categories' hidden
  // content) resolves it.
  await page.getByRole('region', { name: 'Risk' }).getByRole('button', { name: 'Compliance', exact: true }).click();
  await page.getByLabel('Risk title *').fill('Upcoming data protection rule change');
  await page.getByLabel('Likelihood *').click();
  await page.getByRole('option', { name: 'HIGH' }).click();
  await page.getByLabel('Impact *').click();
  await page.getByRole('option', { name: 'MEDIUM' }).click();
  await page.getByLabel('Identified date *').fill('2026-02-12');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Upcoming data protection rule change' })).toBeVisible();

  await page.getByRole('button', { name: 'Technology', exact: true }).click();
  await page.getByLabel('Risk title *').fill('Legacy billing system end-of-life');
  await page.getByLabel('Likelihood *').click();
  await page.getByRole('option', { name: 'MEDIUM' }).click();
  await page.getByLabel('Impact *').click();
  await page.getByRole('option', { name: 'HIGH' }).click();
  await page.getByLabel('Identified date *').fill('2026-01-05');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Legacy billing system end-of-life' })).toBeVisible();
});
