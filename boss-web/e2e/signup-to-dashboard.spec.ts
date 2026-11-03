import { expect, test } from '@playwright/test';

/**
 * The golden path on the two-screen app: sign up on /login (toggled, not a separate
 * route), land on /dashboard, upload data through the modal (not a page), see the
 * dashboard populate, ask the AI CFO a question, then sign out.
 */
test('signup -> upload via dialog -> populated dashboard -> AI chat -> sign out', async ({ page }) => {
  const unique = Date.now();
  const tenantSlug = `e2e-tenant-${unique}`;
  const email = `owner-${unique}@e2e.test`;

  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();

  await page.getByLabel('Company / tenant name').fill('E2E Test Co');
  await page.getByLabel('Workspace slug').fill(tenantSlug);
  await page.getByLabel('Your full name').fill('E2E Owner');
  await page.getByLabel('Your email').fill(email);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText('Upload your first file to get started')).toBeVisible();

  await page.getByRole('button', { name: 'Upload data' }).first().click();
  await expect(page.getByRole('heading', { name: 'Upload business data' })).toBeVisible();

  const csv = [
    'date,description,amount,type',
    '2026-01-05,Consulting invoice,15000.00,INCOME',
    '2026-01-10,Office rent,4000.00,EXPENSE',
    '2026-01-15,Software subscription,500.00,EXPENSE',
  ].join('\n');

  await page.locator('input[type="file"]').setInputFiles({
    name: 'transactions.csv',
    mimeType: 'text/csv',
    buffer: Buffer.from(csv, 'utf-8'),
  });
  await page.getByLabel('Starting cash balance (optional)').fill('10000');
  await page.getByRole('button', { name: 'Analyze' }).click();

  // Dialog auto-closes on success and the dashboard reloads with real data.
  await expect(page.getByRole('heading', { name: 'Upload business data' })).not.toBeVisible({ timeout: 5000 });

  // The metrics/chart/comparison panels live under Finance -> Position in the
  // sidebar; categories start collapsed, so open Finance first.
  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await page.getByRole('button', { name: 'Position', exact: true }).click();
  await expect(page.getByText('Business status')).toBeVisible();
  await expect(page.locator('.metric__value').first()).toHaveText('15,000.00'); // Revenue

  // Only one month of data - no month-over-month comparison yet.
  await expect(page.getByText('Upload at least two months of data to see a comparison.')).toBeVisible();

  // AI chat is real and grounded in the data just uploaded: this business is
  // cash-flow positive, so it honestly says runway isn't applicable rather than
  // fabricating a number.
  await page.getByRole('button', { name: 'What is our runway?' }).click();
  await expect(page.getByText(/isn't available yet/)).toBeVisible();

  await page.getByRole('button', { name: 'Why did profit decrease?' }).click();
  await expect(page.getByText('Profit was 10500.00 in 2026-01 (70.00% margin).')).toBeVisible();

  // Sign out returns to /login, and /dashboard is unreachable again.
  await page.getByRole('button', { name: /E2E Owner/ }).click();
  await page.getByRole('menuitem', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/\/login$/);

  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login$/);
});

/**
 * Net cash flow and the cash flow chart are built only from the cash balance
 * actually entered at each upload - a real observed change between two recorded
 * months, never derived from revenue/expenses (section: never fabricate certainty).
 */
test('cash flow metric and chart reflect the real cash balance entered across two uploads', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Cash Flow Co');
  await page.getByLabel('Workspace slug').fill(`cashflow-${unique}`);
  await page.getByLabel('Your full name').fill('Cash Flow Owner');
  await page.getByLabel('Your email').fill(`cashflow-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // Month 1: cash balance 10,000.
  await page.getByRole('button', { name: 'Upload data' }).first().click();
  const janCsv = ['date,description,amount,type', '2026-01-05,Sales,1000.00,INCOME', '2026-01-10,Rent,800.00,EXPENSE'].join('\n');
  await page.locator('input[type="file"]').setInputFiles({ name: 'jan.csv', mimeType: 'text/csv', buffer: Buffer.from(janCsv) });
  await page.getByLabel('Starting cash balance (optional)').fill('10000');
  await page.getByRole('button', { name: 'Analyze' }).click();
  await expect(page.getByRole('heading', { name: 'Upload business data' })).not.toBeVisible({ timeout: 5000 });

  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await page.getByRole('button', { name: 'Position', exact: true }).click();
  await expect(page.getByText('Net cash flow')).toBeVisible();
  // Only one recorded month so far - no prior balance to diff against.
  await expect(page.locator('mat-card.metric', { hasText: 'Net cash flow' }).getByText('N/A')).toBeVisible();

  // Month 2: cash balance rises to 12,500 - a real, recorded +2,500 change.
  await page.getByRole('button', { name: 'Upload data' }).first().click();
  const febCsv = ['date,description,amount,type', '2026-02-05,Sales,1100.00,INCOME', '2026-02-10,Rent,800.00,EXPENSE'].join('\n');
  await page.locator('input[type="file"]').setInputFiles({ name: 'feb.csv', mimeType: 'text/csv', buffer: Buffer.from(febCsv) });
  await page.getByLabel('Starting cash balance (optional)').fill('12500');
  await page.getByRole('button', { name: 'Analyze' }).click();
  await expect(page.getByRole('heading', { name: 'Upload business data' })).not.toBeVisible({ timeout: 5000 });

  await expect(page.locator('mat-card.metric', { hasText: 'Net cash flow' }).getByText('2,500.00')).toBeVisible();
  await expect(page.getByText('Cash flow trend')).toBeVisible();
  await expect(page.getByText('No cash balance has been recorded yet')).not.toBeVisible();
});

test('forecast panel and AI forecast question use real projected data once 3+ months exist', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Forecast Co');
  await page.getByLabel('Workspace slug').fill(`forecast-${unique}`);
  await page.getByLabel('Your full name').fill('Forecast Owner');
  await page.getByLabel('Your email').fill(`forecast-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // Perfectly linear 3-month history: revenue +100/mo, expenses flat -> the
  // regression forecast next month is exactly predictable by hand.
  const csv = [
    'date,description,amount,type',
    '2026-01-15,Sales,1000.00,INCOME',
    '2026-01-20,Rent,800.00,EXPENSE',
    '2026-02-15,Sales,1100.00,INCOME',
    '2026-02-20,Rent,800.00,EXPENSE',
    '2026-03-15,Sales,1200.00,INCOME',
    '2026-03-20,Rent,800.00,EXPENSE',
  ].join('\n');

  await page.getByRole('button', { name: 'Upload data' }).first().click();
  await page.locator('input[type="file"]').setInputFiles({ name: 'history.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
  await page.getByRole('button', { name: 'Analyze' }).click();
  await expect(page.getByRole('heading', { name: 'Upload business data' })).not.toBeVisible({ timeout: 5000 });

  // Forecast + scenario simulator live under Finance -> Forecast in the
  // sidebar; categories start collapsed, so open Finance first.
  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await page.getByRole('button', { name: 'Forecast', exact: true }).click();
  await expect(page.getByText('Next 12 months, linear trend over your history')).toBeVisible();
  const forecastPanel = page.locator('mat-card', { hasText: 'Forecast' });
  await expect(forecastPanel.getByText('Revenue (2026-04)')).toBeVisible();
  await expect(forecastPanel.getByText('1,300.00')).toBeVisible(); // 1000,1100,1200 -> 1300

  // The AI chat's forecast answer is grounded in that same projection.
  await page.getByRole('textbox', { name: 'Ask BOSS anything about your business...' }).fill('What is your revenue forecast for next month?');
  await page.getByRole('button').filter({ has: page.locator('mat-icon', { hasText: 'send' }) }).click();
  await expect(page.getByText(/projected revenue is 1300\.00/)).toBeVisible();

  // Scenario simulator: same 3-month history (revenue 1000/1100/1200, flat expenses
  // 800), so base next-month is revenue 1300 / expenses 800 / profit 500. A -10%
  // revenue scenario: revenue 1170 / expenses 800 (unchanged) / profit 370.
  const scenarioPanel = page.locator('mat-card', { hasText: 'Scenario simulator' });
  await scenarioPanel.getByLabel('Revenue change (%)').fill('-10');
  await scenarioPanel.getByLabel('Expense change (%)').fill('0');
  await scenarioPanel.getByRole('button', { name: 'Run scenario' }).click();
  await expect(scenarioPanel.getByText('500.00')).toBeVisible(); // base profit
  await expect(scenarioPanel.getByText('370.00')).toBeVisible(); // scenario profit
});

/**
 * Regression test: the backend omits null JSON fields (default-property-inclusion:
 * non_null), so an absent `monthsUntilExhausted`/`monthsFromNow` arrives as
 * `undefined`, not `null` - a strict `!== null` check in the template previously
 * rendered a blank "mo" / crashed the "not within horizon" branch instead of the
 * correct "Sufficient" / "Not within horizon" text. This pins that path down for
 * both the forecast panel and the scenario comparison.
 */
test('forecast and scenario panels show "Sufficient" runway and "Not within horizon" break-even correctly', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Sufficient Co');
  await page.getByLabel('Workspace slug').fill(`sufficient-${unique}`);
  await page.getByLabel('Your full name').fill('Sufficient Owner');
  await page.getByLabel('Your email').fill(`sufficient-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  // Growing, profitable business with a starting cash balance -> cash keeps
  // increasing every projected month, so runway never runs out within the horizon.
  const csv = [
    'date,description,amount,type',
    '2026-01-15,Sales,10000.00,INCOME',
    '2026-01-20,Rent,7000.00,EXPENSE',
    '2026-02-15,Sales,10500.00,INCOME',
    '2026-02-20,Rent,7200.00,EXPENSE',
    '2026-03-15,Sales,11000.00,INCOME',
    '2026-03-20,Rent,7400.00,EXPENSE',
  ].join('\n');

  await page.getByRole('button', { name: 'Upload data' }).first().click();
  await page.locator('input[type="file"]').setInputFiles({ name: 'history.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
  await page.getByLabel('Starting cash balance (optional)').fill('15000');
  await page.getByRole('button', { name: 'Analyze' }).click();
  await page.getByRole('heading', { name: 'Upload business data' }).waitFor({ state: 'hidden' });

  // Forecast + scenario simulator live under Finance -> Forecast in the
  // sidebar; categories start collapsed, so open Finance first.
  await page.getByRole('button', { name: 'Finance', exact: true }).click();
  await page.getByRole('button', { name: 'Forecast', exact: true }).click();

  const forecastPanel = page.locator('mat-card', { hasText: 'Forecast' });
  await expect(forecastPanel.getByText('Sufficient')).toBeVisible();

  // A harsh enough scenario (-15% revenue, +10% expenses, 2 hires @ 5000/mo) flips
  // the base's healthy trajectory into one that exhausts cash and never breaks even
  // within the 12-month horizon.
  const scenarioPanel = page.locator('mat-card', { hasText: 'Scenario simulator' });
  await scenarioPanel.getByLabel('Revenue change (%)').fill('-15');
  await scenarioPanel.getByLabel('Expense change (%)').fill('10');
  await scenarioPanel.getByLabel('Hiring (headcount)').fill('2');
  await scenarioPanel.getByRole('button', { name: 'Run scenario' }).click();

  await expect(scenarioPanel.getByText('Sufficient')).toBeVisible(); // base runway
  await expect(scenarioPanel.getByText('2 mo')).toBeVisible(); // scenario runway: a real number, not blank
  await expect(scenarioPanel.getByText('Now')).toBeVisible(); // base break-even
  await expect(scenarioPanel.getByText('Not within horizon')).toBeVisible(); // scenario break-even
});

test('generated recommendations are grounded in the same risk that appears in the risk panel', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Losing Co');
  await page.getByLabel('Workspace slug').fill(`losing-${unique}`);
  await page.getByLabel('Your full name').fill('Losing Owner');
  await page.getByLabel('Your email').fill(`losing-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  const csv = ['date,description,amount,type', '2026-01-05,Sales,1000.00,INCOME', '2026-01-10,Rent,1500.00,EXPENSE'].join('\n');
  await page.getByRole('button', { name: 'Upload data' }).first().click();
  await page.locator('input[type="file"]').setInputFiles({ name: 'loss.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
  await page.getByRole('button', { name: 'Analyze' }).click();
  await page.getByRole('heading', { name: 'Upload business data' }).waitFor({ state: 'hidden' });

  // Risk identification and the recommendations/actions that respond to it all live
  // together under Risk -> Mitigation in the sidebar; categories start collapsed.
  await page.getByRole('button', { name: 'Risk', exact: true }).click();
  await page.getByRole('button', { name: 'Mitigation', exact: true }).click();

  const riskPanel = page.locator('mat-card', { hasText: 'Risks & anomalies' });
  await expect(riskPanel.getByText('Operating at a loss')).toBeVisible();

  const recommendationPanel = page.locator('mat-card', { hasText: 'Recommendations' });
  await expect(recommendationPanel.getByText('No recommendations yet')).toBeVisible();

  await recommendationPanel.getByRole('button', { name: 'Generate' }).click();
  await expect(recommendationPanel.getByText('Operating at a loss')).toBeVisible();
  await expect(recommendationPanel.getByText('confidence 0.55')).toBeVisible();

  await recommendationPanel.getByText('Operating at a loss').click(); // expand
  await expect(recommendationPanel.getByText('Expenses exceeded revenue this period.')).toBeVisible();
  await expect(recommendationPanel.getByText(/Review the largest expense categories/)).toBeVisible();
});

test('full loop: propose an action from a recommendation, approve it, complete it, and see the outcome variance', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Create a workspace' }).click();
  await page.getByLabel('Company / tenant name').fill('Full Loop Co');
  await page.getByLabel('Workspace slug').fill(`fullloop-${unique}`);
  await page.getByLabel('Your full name').fill('Loop Owner');
  await page.getByLabel('Your email').fill(`fullloop-${unique}@e2e.test`);
  await page.getByLabel('Password').fill('SuperSecret123');
  await page.getByRole('button', { name: 'Create workspace' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  const csv = ['date,description,amount,type', '2026-01-05,Sales,1000.00,INCOME', '2026-01-10,Rent,1500.00,EXPENSE'].join('\n');
  await page.getByRole('button', { name: 'Upload data' }).first().click();
  await page.locator('input[type="file"]').setInputFiles({ name: 'loss.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
  await page.getByRole('button', { name: 'Analyze' }).click();
  await page.getByRole('heading', { name: 'Upload business data' }).waitFor({ state: 'hidden' });

  // Recommendations, actions and outcomes all live under Risk -> Mitigation in
  // the sidebar; categories start collapsed.
  await page.getByRole('button', { name: 'Risk', exact: true }).click();
  await page.getByRole('button', { name: 'Mitigation', exact: true }).click();

  const recommendationPanel = page.locator('mat-card', { hasText: 'Recommendations' });
  await recommendationPanel.getByRole('button', { name: 'Generate' }).click();
  await recommendationPanel.getByText('Operating at a loss').click(); // expand
  await recommendationPanel.getByRole('button', { name: 'Propose action' }).click();

  // Propose: description is prefilled from the recommendation; set a concrete expected outcome.
  await expect(page.getByRole('heading', { name: 'Propose an action' })).toBeVisible();
  await page.getByLabel('Expected change (e.g. +100000)').fill('100');
  await page.getByRole('button', { name: 'Propose', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Propose an action' })).not.toBeVisible();

  const actionPanel = page.locator('mat-card', { hasText: 'Actions' });
  await expect(actionPanel.getByText('PENDING APPROVAL')).toBeVisible();
  await expect(actionPanel.getByText('from: Operating at a loss')).toBeVisible();

  // Self-signup creates a TENANT_ADMIN, which is an approver role - approve is visible.
  await actionPanel.getByRole('button', { name: 'Approve' }).click();
  await expect(actionPanel.getByText('APPROVED')).toBeVisible();

  await actionPanel.getByRole('button', { name: 'Complete' }).click();
  const completeDialog = page.locator('mat-dialog-container', { hasText: 'Record the actual outcome' });
  await expect(completeDialog.getByRole('heading', { name: 'Record the actual outcome' })).toBeVisible();
  await expect(completeDialog.getByText('Expected: Monthly profit changes by 100')).toBeVisible();
  await completeDialog.getByLabel('Actual change observed').fill('72');
  await completeDialog.getByLabel('Reason / learning (optional)').fill('Expected savings were partially achieved.');
  await completeDialog.getByRole('button', { name: 'Record outcome' }).click();
  await expect(page.getByRole('heading', { name: 'Record the actual outcome' })).not.toBeVisible();

  await expect(actionPanel.getByText('COMPLETED')).toBeVisible();

  const outcomePanel = page.locator('mat-card', { hasText: 'Outcomes' });
  await expect(outcomePanel.getByText('Expected: Monthly profit 100.00')).toBeVisible();
  await expect(outcomePanel.getByText('Actual: 72.00')).toBeVisible();
  await expect(outcomePanel.getByText('Variance: -28.00')).toBeVisible();
  await expect(outcomePanel.getByText('Expected savings were partially achieved.')).toBeVisible();
});
