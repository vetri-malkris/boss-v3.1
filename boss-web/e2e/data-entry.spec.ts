import { expect, test } from '@playwright/test';

/**
 * Subcategory data entry (section: 33 distinct domain-specific forms, starting with
 * Human Resource as the pilot category): each subcategory with a schema gets a real
 * form (fields distinct to that domain) plus a bulk CSV/Excel upload, both validated
 * against the same schema so a single form entry and a bulk row are held to the same
 * rules.
 */
test('Human Resource -> Hiring: single-record entry and bulk upload both validate against the same schema', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Entry Co');
  await page.getByLabel('Workspace slug').fill(`entry-${unique}`);
  await page.getByLabel('Your full name').fill('Entry Admin');
  await page.getByLabel('Your email').fill(`entry-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('button', { name: 'Human Resource', exact: true }).click();
  await page.getByRole('button', { name: 'Hiring', exact: true }).click();

  // The form's fields are Hiring-specific, not a generic label/value list.
  await expect(page.getByLabel('Requisition ID *')).toBeVisible();
  await expect(page.getByLabel('Position title *')).toBeVisible();

  await page.getByLabel('Requisition ID *').fill('REQ-100');
  await page.getByLabel('Position title *').fill('Backend Engineer');
  await page.getByLabel('Department *').fill('Engineering');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'OPEN' }).click();
  await page.getByLabel('Opened date *').fill('2026-03-01');
  await page.getByRole('button', { name: 'Add record' }).click();

  await expect(page.getByText('Saved.')).toBeVisible();
  const recordsTable = page.locator('table');
  await expect(recordsTable.getByRole('cell', { name: 'REQ-100' })).toBeVisible();
  await expect(recordsTable.getByRole('cell', { name: 'Backend Engineer' })).toBeVisible();

  // Bulk upload: one good row, one row missing a required field.
  await page.getByRole('button', { name: 'Bulk upload' }).click();
  const bulkDialog = page.locator('mat-dialog-container', { hasText: 'Bulk upload' });
  await expect(bulkDialog.getByRole('heading', { name: 'Bulk upload' })).toBeVisible();

  const csv = [
    'requisitionId,positionTitle,department,status,openedDate',
    'REQ-101,Frontend Engineer,Engineering,OPEN,2026-03-05',
    'REQ-102,,Engineering,OPEN,2026-03-06',
  ].join('\n');
  await page.locator('input[type="file"]').setInputFiles({ name: 'hiring.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
  await bulkDialog.getByRole('button', { name: 'Upload', exact: true }).click();

  await expect(bulkDialog.getByText('1 row(s) created, 1 skipped.')).toBeVisible();
  await expect(bulkDialog.getByText(/Row 3:.*Position title.*required/)).toBeVisible();

  await bulkDialog.getByRole('button', { name: 'Close' }).click();
  await expect(recordsTable.getByRole('cell', { name: 'REQ-101' })).toBeVisible();
  await expect(recordsTable.getByRole('cell', { name: 'REQ-102' })).not.toBeVisible();
});

test('the same pattern generalizes to Finance, Accounting, Auditing and Compliance', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Spread Co');
  await page.getByLabel('Workspace slug').fill(`spread-${unique}`);
  await page.getByLabel('Your full name').fill('Spread Admin');
  await page.getByLabel('Your email').fill(`spread-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // Finance -> Capital
  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await page.getByRole('button', { name: 'Capital', exact: true }).click();
  await page.getByLabel('Record date *').fill('2026-01-15');
  await page.getByLabel('Capital type *').click();
  await page.getByRole('option', { name: 'EQUITY' }).click();
  await page.getByLabel('Amount *').fill('250000');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();

  // Accounting -> Reporting
  await page.getByRole('button', { name: 'Accounting', exact: true }).click();
  await page.getByRole('button', { name: 'Reporting', exact: true }).click();
  await page.getByLabel('Report period (e.g. 2026-03) *').fill('2026-03');
  await page.getByLabel('Report type *').click();
  await page.getByRole('option', { name: 'BALANCE_SHEET' }).click();
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'DRAFT' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();

  // Auditing -> Findings
  await page.getByRole('button', { name: 'Auditing', exact: true }).click();
  await page.getByRole('button', { name: 'Findings', exact: true }).click();
  await page.getByLabel('Audit name *').fill('FY26 Internal Audit');
  await page.getByLabel('Finding title *').fill('Segregation of duties gap');
  await page.getByLabel('Severity *').click();
  await page.getByRole('option', { name: 'HIGH' }).click();
  await page.getByLabel('Identified date *').fill('2026-02-01');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'OPEN' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Segregation of duties gap' })).toBeVisible();

  // Compliance -> Tax/GST
  await page.getByRole('button', { name: 'Compliance', exact: true }).click();
  await page.getByRole('button', { name: 'Tax/GST', exact: true }).click();
  await page.getByLabel('Filing period (e.g. 2026-03) *').fill('2026-03');
  await page.getByLabel('Tax type *').click();
  await page.getByRole('option', { name: 'GST' }).click();
  await page.getByLabel('Amount due *').fill('4500');
  await page.getByLabel('Due date *').fill('2026-04-20');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'PENDING' }).click();
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: '4500' })).toBeVisible();
});

test('Risk -> Exposure shows the manual risk register alongside (not instead of) the auto-detected risk panel', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Dual Co');
  await page.getByLabel('Workspace slug').fill(`dual-${unique}`);
  await page.getByLabel('Your full name').fill('Dual Admin');
  await page.getByLabel('Your email').fill(`dual-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('button', { name: 'Risk', exact: true }).click();
  await page.getByRole('button', { name: 'Exposure', exact: true }).click();

  // No financial data yet, but the manual risk register is still usable.
  await expect(page.getByLabel('Risk title *')).toBeVisible();
  await expect(page.getByText('Upload your first file to get started')).toBeVisible();

  await page.getByLabel('Risk title *').fill('Key customer concentration');
  await page.getByLabel('Category *').click();
  await page.getByRole('option', { name: 'STRATEGIC' }).click();
  await page.getByLabel('Likelihood *').click();
  await page.getByRole('option', { name: 'MEDIUM' }).click();
  await page.getByLabel('Impact *').click();
  await page.getByRole('option', { name: 'HIGH' }).click();
  await page.getByLabel('Identified date *').fill('2026-02-10');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Key customer concentration' })).toBeVisible();

  // Uploading data now surfaces the real auto-detected risk panel too, without
  // hiding the manual register entry just saved.
  const csv = ['date,description,amount,type', '2026-01-05,Sales,1000.00,INCOME', '2026-01-10,Rent,1500.00,EXPENSE'].join('\n');
  await page.getByRole('button', { name: 'Upload data' }).first().click();
  await page.locator('input[type="file"]').setInputFiles({ name: 'loss.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
  await page.getByRole('button', { name: 'Analyze' }).click();
  await page.getByRole('heading', { name: 'Upload business data' }).waitFor({ state: 'hidden' });

  await expect(page.getByText('Upload your first file to get started')).not.toBeVisible();
  await expect(page.locator('mat-card', { hasText: 'Risks & anomalies' }).getByText('Operating at a loss')).toBeVisible();
  await expect(page.getByLabel('Risk title *')).toBeVisible();
  await expect(page.locator('table').getByRole('cell', { name: 'Key customer concentration' })).toBeVisible();
});

/**
 * A mistaken entry must never be a dead end (section: corrections must remain
 * traceable). Every data-entry record can be corrected in place or voided with a
 * reason - neither is a silent hard delete.
 */
test('Human Resource -> Hiring: a record can be corrected in place or voided with a reason', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Correction Co');
  await page.getByLabel('Workspace slug').fill(`correction-${unique}`);
  await page.getByLabel('Your full name').fill('Correction Admin');
  await page.getByLabel('Your email').fill(`correction-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('button', { name: 'Human Resource', exact: true }).click();
  await page.getByRole('button', { name: 'Hiring', exact: true }).click();

  await page.getByLabel('Requisition ID *').fill('REQ-200');
  await page.getByLabel('Position title *').fill('QA Engineer');
  await page.getByLabel('Department *').fill('Engineering');
  await page.getByLabel('Status *').click();
  await page.getByRole('option', { name: 'OPEN' }).click();
  await page.getByLabel('Opened date *').fill('2026-03-10');
  await page.getByRole('button', { name: 'Add record' }).click();
  await expect(page.getByText('Saved.')).toBeVisible();

  const recordsTable = page.locator('table');
  const row = recordsTable.getByRole('row', { name: /REQ-200/ });
  await expect(row.getByRole('cell', { name: 'QA Engineer' })).toBeVisible();

  // Correct a mistaken value - the prior value is not silently discarded, it moves
  // into the audit trail, and the visible record reflects the correction.
  await row.getByRole('button', { name: 'Edit record' }).click();
  const editDialog = page.locator('mat-dialog-container', { hasText: 'Edit record' });
  await expect(editDialog.getByRole('heading', { name: 'Edit record' })).toBeVisible();
  await expect(editDialog.getByLabel('Position title *')).toHaveValue('QA Engineer');
  await editDialog.getByLabel('Position title *').fill('Senior QA Engineer');
  await editDialog.getByRole('button', { name: 'Save changes' }).click();
  await expect(editDialog).not.toBeVisible();

  await expect(recordsTable.getByRole('cell', { name: 'Senior QA Engineer' })).toBeVisible();
  await expect(recordsTable.getByRole('cell', { name: 'QA Engineer', exact: true })).not.toBeVisible();

  // Voiding requires a reason and removes the record from the everyday list
  // without pretending it never happened.
  await row.getByRole('button', { name: 'Void record' }).click();
  const voidDialog = page.locator('mat-dialog-container', { hasText: 'Void record' });
  await expect(voidDialog.getByRole('heading', { name: 'Void record' })).toBeVisible();

  await voidDialog.getByLabel('Reason').fill('Duplicate requisition, superseded by REQ-201');
  await voidDialog.getByRole('button', { name: 'Void record' }).click();
  await expect(voidDialog).not.toBeVisible();

  await expect(recordsTable.getByRole('cell', { name: 'Senior QA Engineer' })).not.toBeVisible();
});
