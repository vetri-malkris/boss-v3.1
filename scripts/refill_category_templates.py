# -*- coding: utf-8 -*-
"""Re-fills the 6 freshly-downloaded category templates with the canonical 5-row
sample data used throughout this session, every DATE field in dd-MM-yyyy."""
import openpyxl

def fill(path, sheet_name, rows):
    wb = openpyxl.load_workbook(path)
    ws = wb[sheet_name]
    start = ws.max_row + 1
    for row in rows:
        for col_idx, value in enumerate(row, start=1):
            ws.cell(row=start, column=col_idx, value=value)
        start += 1
    wb.save(path)

# ---------------- FINANCE ----------------
f = 'sample-data/category-templates/finance-template.xlsx'
fill(f, 'Position', [
    ['31-01-2026', 545000, 312000, 128000, 980000, 215000, 95000, 1255000],
    ['28-02-2026', 560000, 298000, 134000, 975000, 198000, 90000, 1281000],
    ['31-03-2026', 601000, 340000, 140000, 970000, 220000, 88000, 1343000],
    ['30-04-2026', 589000, 355000, 125000, 965000, 205000, 92000, 1337000],
    ['31-05-2026', 622000, 310000, 118000, 960000, 190000, 85000, 1335000],
])
fill(f, 'Liquidity', [
    [185000, '10-02-2026', 42000, '05-02-2026', 'Vendor payment - IT infra', '15-02-2026', 300000],
    [96000, '20-02-2026', 210000, '28-02-2026', 'Payroll run', '28-02-2026', 300000],
    [150000, '10-03-2026', 65000, '12-03-2026', 'Office lease renewal', '15-03-2026', 350000],
    [220000, '25-03-2026', 210000, '28-03-2026', 'Payroll run', '28-03-2026', 350000],
    [175000, '10-04-2026', 48000, '12-04-2026', 'Equipment lease', '18-04-2026', 400000],
])
fill(f, 'Profitability', [
    ['2026-01', 450000, 180000, 95000, 12000, 20000],
    ['2026-02', 470000, 190000, 98000, 11500, 21000],
    ['2026-03', 510000, 205000, 102000, 13000, 23000],
    ['2026-04', 495000, 198000, 99000, 12500, 22000],
    ['2026-05', 530000, 212000, 105000, 13500, 24000],
])
fill(f, 'Budget & Forecast', [
    ['2026-01', 460000, 200000, 450000, 195000, 440000, 190000, 50000, 120000, 'Base case'],
    ['2026-02', 465000, 202000, 470000, 198000, 455000, 193000, 30000, 100000, 'Base case'],
    ['2026-03', 500000, 210000, 510000, 205000, 495000, 200000, 80000, 150000, 'Optimistic'],
    ['2026-04', 490000, 205000, 495000, 198000, 480000, 195000, 40000, 110000, 'Base case'],
    ['2026-05', 520000, 215000, 530000, 212000, 510000, 205000, 60000, 130000, 'Base case'],
])
fill(f, 'Cost', [
    ['Cloud infrastructure', 'Engineering', 'Fixed', 18500, 'BUD-ENG-2026-01', 'Within budget'],
    ['Sales commissions', 'Sales', 'Variable', 32000, 'BUD-SAL-2026-01', 'Higher revenue than planned'],
    ['Office rent', 'Operations', 'Fixed', 42000, 'BUD-OPS-2026-01', 'Within budget'],
    ['Marketing campaigns', 'Marketing', 'Variable', 27500, 'BUD-MKT-2026-01', 'Overspend on paid ads'],
    ['Employee training', 'HR', 'Discretionary', 9500, 'BUD-HR-2026-01', 'Within budget'],
])
fill(f, 'Capital', [
    ['EQUITY', 2000000, 'Founders & angel investors', 0, 0, '31-12-2030', '01-01-2026', 'Initial working capital'],
    ['DEBT', 500000, 'HDFC Bank term loan', 420000, 9.5, '30-06-2029', '01-02-2026', 'Equipment purchase'],
    ['GRANT', 150000, 'State MSME growth grant', 0, 0, '31-12-2026', '01-03-2026', 'R&D expansion'],
    ['DEBT', 300000, 'Working capital line of credit', 180000, 11.0, '31-03-2027', '01-04-2026', 'Seasonal inventory'],
    ['OTHER', 100000, 'Founder bridge loan', 100000, 6.0, '31-12-2026', '01-05-2026', 'Short-term cash gap'],
])

# ---------------- HUMAN_RESOURCE ----------------
f = 'sample-data/category-templates/human_resource-template.xlsx'
fill(f, 'Workforce', [
    ['31-01-2026', 'Engineering', 'Bengaluru', 48, 42, 4, 2],
    ['31-01-2026', 'Sales', 'Mumbai', 22, 20, 0, 2],
    ['28-02-2026', 'Engineering', 'Bengaluru', 50, 44, 4, 2],
    ['28-02-2026', 'Operations', 'Chennai', 15, 14, 1, 0],
    ['31-03-2026', 'Sales', 'Mumbai', 24, 22, 0, 2],
])
fill(f, 'People Cost', [
    ['2026-01', 'Engineering', 3850000, 462000, 231000],
    ['2026-01', 'Sales', 1760000, 211000, 105600],
    ['2026-02', 'Engineering', 4020000, 482000, 241200],
    ['2026-02', 'Operations', 980000, 117600, 58800],
    ['2026-03', 'Sales', 1920000, 230400, 115200],
])
fill(f, 'Productivity', [
    ['2026-01', 'Sales', 'Revenue per FTE', 80000, 22],
    ['2026-01', 'Engineering', 'Story points delivered', 540, 48],
    ['2026-02', 'Sales', 'Revenue per FTE', 83500, 24],
    ['2026-02', 'Support', 'Tickets resolved per FTE', 310, 10],
    ['2026-03', 'Engineering', 'Story points delivered', 565, 50],
])
fill(f, 'Hiring', [
    ['REQ-2026-001', 'Senior Backend Engineer', 'Engineering', 'FILLED', '05-01-2026', '15-02-2026', '10-02-2026'],
    ['REQ-2026-002', 'Account Executive', 'Sales', 'INTERVIEWING', '12-01-2026', '20-02-2026', None],
    ['REQ-2026-003', 'HR Business Partner', 'Human Resources', 'OPEN', '01-02-2026', '15-03-2026', None],
    ['REQ-2026-004', 'DevOps Engineer', 'Engineering', 'OFFER', '10-02-2026', '10-03-2026', None],
    ['REQ-2026-005', 'Support Specialist', 'Operations', 'CANCELLED', '20-01-2026', '28-02-2026', None],
])
fill(f, 'Attrition', [
    ['Rahul Mehta', 'Engineering', '18-01-2026', 'VOLUNTARY', 'Better offer elsewhere', 28],
    ['Priya Nair', 'Sales', '02-02-2026', 'VOLUNTARY', 'Relocation', 14],
    ['Suresh Kumar', 'Operations', '14-02-2026', 'INVOLUNTARY', 'Performance', 9],
    ['Anita Rao', 'Engineering', '01-03-2026', 'RETIREMENT', 'Retirement', 96],
    ['Vikram Singh', 'Support', '10-03-2026', 'VOLUNTARY', 'Career change', 20],
])
fill(f, 'Capability', [
    ['Rahul Mehta', 'Engineering', 'AWS Certified Solutions Architect', 'COMPLETED', '25-01-2026', 15000],
    ['Priya Nair', 'Sales', 'Advanced Negotiation Training', 'COMPLETED', '10-02-2026', 8000],
    ['Anita Rao', 'Engineering', 'Kubernetes Administration', 'IN_PROGRESS', None, 12000],
    ['Vikram Singh', 'Support', 'ITIL Foundation', 'NOT_STARTED', None, 6000],
    ['Suresh Kumar', 'Operations', 'Lean Six Sigma Green Belt', 'IN_PROGRESS', None, 10000],
])

# ---------------- ACCOUNTING ----------------
f = 'sample-data/category-templates/accounting-template.xlsx'
fill(f, 'Reporting', [
    ['2026-01', 'PL', 'FINALIZED', 'Meera Iyer', '05-02-2026'],
    ['2026-01', 'BALANCE_SHEET', 'FINALIZED', 'Meera Iyer', '05-02-2026'],
    ['2026-02', 'PL', 'DRAFT', 'Meera Iyer', None],
    ['2026-02', 'CASH_FLOW', 'FINALIZED', 'Arjun Das', '04-03-2026'],
    ['2026-03', 'PL', 'FILED', 'Arjun Das', '02-04-2026'],
])
fill(f, 'Working Capital', [
    ['31-01-2026', 985000, 310000, 'Healthy buffer maintained'],
    ['28-02-2026', 1012000, 288000, 'Receivables collection improved'],
    ['31-03-2026', 1081000, 335000, 'Inventory build-up for Q2'],
    ['30-04-2026', 1069000, 322000, 'Stable'],
    ['31-05-2026', 1120000, 295000, 'Strong liquidity position'],
])
fill(f, 'Assets-Liabilities', [
    ['31-01-2026', 'Office building - Bengaluru', 'ASSET', 'FIXED_ASSET', 7500000, 'Owned outright'],
    ['31-01-2026', 'Accounts receivable - trade', 'ASSET', 'CURRENT_ASSET', 312000, 'Net of allowance'],
    ['31-01-2026', 'Term loan - HDFC Bank', 'LIABILITY', 'LONG_TERM_LIABILITY', 420000, '9.5% interest'],
    ['31-01-2026', 'Accounts payable - trade', 'LIABILITY', 'CURRENT_LIABILITY', 215000, '30-day terms'],
    ['31-01-2026', 'Office equipment & furniture', 'ASSET', 'FIXED_ASSET', 185000, 'Depreciated over 5 years'],
])
fill(f, 'Reconciliation', [
    ['HDFC Bank - Current A/C', '2026-01', 545000, 545000, 'RECONCILED', 'No discrepancies'],
    ['ICICI Bank - Savings A/C', '2026-01', 128000, 126500, 'UNRECONCILED', 'Pending bank charge entry'],
    ['Petty Cash', '2026-01', 15000, 15000, 'RECONCILED', 'Verified against vouchers'],
    ['HDFC Bank - Current A/C', '2026-02', 560000, 558000, 'IN_PROGRESS', 'Awaiting Feb statement'],
    ['Accounts Receivable Ledger', '2026-02', 298000, 298000, 'RECONCILED', 'Matches subledger'],
])
fill(f, 'Controls', [
    ['Bank reconciliation review', 'Meera Iyer', 'MONTHLY', '31-01-2026', 'EFFECTIVE'],
    ['Journal entry approval', 'Arjun Das', 'DAILY', '15-02-2026', 'EFFECTIVE'],
    ['Vendor master data review', 'Meera Iyer', 'QUARTERLY', '15-01-2026', 'NEEDS_REVIEW'],
    ['Expense report approval', 'Arjun Das', 'WEEKLY', '20-02-2026', 'EFFECTIVE'],
    ['Fixed asset register review', 'Meera Iyer', 'QUARTERLY', '30-12-2025', 'NEEDS_REVIEW'],
])

# ---------------- AUDITING ----------------
f = 'sample-data/category-templates/auditing-template.xlsx'
fill(f, 'Audit Status', [
    ['FY2025 Statutory Audit', 'EXTERNAL', 'Deloitte & Co.', '10-01-2026', '20-02-2026', 'COMPLETED'],
    ['Q1 2026 Internal Controls Review', 'INTERNAL', 'Internal Audit Team', '01-02-2026', '15-02-2026', 'COMPLETED'],
    ['GST Compliance Audit', 'REGULATORY', 'State GST Department', '01-03-2026', None, 'IN_PROGRESS'],
    ['IT Security Audit', 'EXTERNAL', 'CyberSafe Consultants', '15-03-2026', None, 'PLANNED'],
    ['Payroll Process Audit', 'INTERNAL', 'Internal Audit Team', '01-04-2026', None, 'PLANNED'],
])
fill(f, 'Findings', [
    ['FY2025 Statutory Audit', 'Delayed vendor invoice approvals', 'MEDIUM', 'Approval cycle exceeds policy SLA', '05-02-2026', 'OPEN'],
    ['FY2025 Statutory Audit', 'Missing fixed asset tags', 'LOW', '12 assets lack physical tags', '08-02-2026', 'REMEDIATED'],
    ['Q1 2026 Internal Controls Review', 'Segregation of duties gap', 'HIGH', 'Same user can create and approve journal entries', '12-02-2026', 'OPEN'],
    ['Q1 2026 Internal Controls Review', 'Outdated access list', 'MEDIUM', 'Two former employees retained system access', '14-02-2026', 'REMEDIATED'],
    ['GST Compliance Audit', 'Late filing in December', 'CRITICAL', 'GSTR-3B filed 5 days after due date', '05-03-2026', 'ACCEPTED_RISK'],
])
fill(f, 'Controls', [
    ['Journal entry segregation', 'Finance', 'Arjun Das', 'MONTHLY', '12-02-2026', 'FAIL'],
    ['User access review', 'IT', 'Internal Audit Team', 'QUARTERLY', '14-02-2026', 'PARTIAL'],
    ['Fixed asset tagging', 'Operations', 'Meera Iyer', 'QUARTERLY', '08-02-2026', 'PASS'],
    ['Vendor invoice approval SLA', 'Finance', 'Arjun Das', 'WEEKLY', '05-02-2026', 'FAIL'],
    ['GST filing calendar adherence', 'Finance', 'Arjun Das', 'MONTHLY', '05-03-2026', 'FAIL'],
])
fill(f, 'Exceptions', [
    ['Segregation of duties gap', 'Single user holds create+approve rights in ERP', '12-02-2026', 'HIGH', 'OPEN', None],
    ['Late filing in December', 'GSTR-3B filed late, penalty risk', '05-03-2026', 'HIGH', 'OPEN', None],
    ['Missing fixed asset tags', '12 untagged assets found in warehouse', '08-02-2026', 'LOW', 'RESOLVED', '20-02-2026'],
    ['Outdated access list', 'Former employees retained ERP access', '14-02-2026', 'MEDIUM', 'RESOLVED', '18-02-2026'],
    ['Delayed vendor invoice approvals', 'Approval cycle breaches 5-day SLA', '05-02-2026', 'MEDIUM', 'OPEN', None],
])
fill(f, 'Actions', [
    ['Segregation of duties gap', 'Split create and approve roles in ERP', 'Arjun Das', '15-03-2026', 'IN_PROGRESS', None],
    ['Late filing in December', 'Set up automated GST filing reminders', 'Meera Iyer', '20-03-2026', 'NOT_STARTED', None],
    ['Missing fixed asset tags', 'Tag all untagged assets', 'Operations Team', '20-02-2026', 'COMPLETED', '19-02-2026'],
    ['Outdated access list', 'Revoke access for former employees', 'IT Team', '18-02-2026', 'COMPLETED', '18-02-2026'],
    ['Delayed vendor invoice approvals', 'Introduce 3-day SLA alert in workflow tool', 'Arjun Das', '25-03-2026', 'OVERDUE', None],
])

# ---------------- COMPLIANCE ----------------
f = 'sample-data/category-templates/compliance-template.xlsx'
fill(f, 'Compliance Status', [
    ['GST Registration Renewal', 'GST Act', 'COMPLIANT', '15-01-2026', 'Meera Iyer'],
    ['POSH Policy Implementation', 'POSH Act 2013', 'COMPLIANT', '20-01-2026', 'HR Team'],
    ['Annual ROC Filing', 'Companies Act 2013', 'IN_PROGRESS', '10-02-2026', 'Arjun Das'],
    ['Fire Safety NOC', 'State Fire Safety Regulations', 'NON_COMPLIANT', '01-02-2026', 'Operations Team'],
    ['Data Privacy Policy Review', 'DPDP Act 2023', 'NOT_APPLICABLE', '15-12-2025', 'Legal Team'],
])
fill(f, 'Tax-GST', [
    ['2026-01', 'GST', 95000, 50000, 45000, 45000, '20-02-2026', '18-02-2026', 'PAID'],
    ['2026-02', 'GST', 110000, 58000, 52000, 52000, '20-03-2026', '19-03-2026', 'PAID'],
    ['2026-03', 'GST', 102000, 53500, 48500, 0, '20-04-2026', None, 'PENDING'],
    ['2025-Q4', 'INCOME_TAX', 180000, 0, 180000, 180000, '15-01-2026', '14-01-2026', 'PAID'],
    ['2026-02', 'VAT', 30000, 18000, 12000, 0, '15-03-2026', None, 'OVERDUE'],
])
fill(f, 'Regulatory', [
    ['Shops & Establishment Act Registration', 'Karnataka', 'Annual renewal of trade license', 'COMPLIANT', '10-01-2026', 'Operations Team'],
    ['Provident Fund Compliance', 'India - EPFO', 'Monthly PF contribution filing', 'COMPLIANT', '01-02-2026', 'HR Team'],
    ['Fire Safety NOC Renewal', 'Karnataka', 'Annual fire safety certification', 'NON_COMPLIANT', '01-02-2026', 'Operations Team'],
    ['ESI Registration Compliance', 'India - ESIC', 'Employee state insurance compliance', 'COMPLIANT', '25-01-2026', 'HR Team'],
    ['Professional Tax Filing', 'Karnataka', 'Monthly professional tax remittance', 'PENDING', '01-03-2026', 'Arjun Das'],
])
fill(f, 'Filings', [
    ['GSTR-3B January', 'GST Return', 'GST Department', '20-02-2026', '18-02-2026', 'FILED', 'GSTR-REF-001'],
    ['GSTR-3B February', 'GST Return', 'GST Department', '20-03-2026', '19-03-2026', 'FILED', 'GSTR-REF-002'],
    ['Annual ROC Filing (MGT-7)', 'Annual Return', 'Registrar of Companies', '31-03-2026', None, 'PENDING', None],
    ['TDS Return Q4', 'TDS Return', 'Income Tax Department', '31-05-2026', None, 'PENDING', None],
    ['PF Monthly Return - January', 'PF Return', 'EPFO', '15-02-2026', '10-02-2026', 'FILED', 'PF-REF-2026-01'],
])
fill(f, 'Exceptions', [
    ['Fire Safety NOC', 'NOC expired and not yet renewed', 'HIGH', '01-02-2026', 'OPEN'],
    ['Professional Tax Filing', 'Filing delayed due to data mismatch', 'MEDIUM', '01-03-2026', 'OPEN'],
    ['GST VAT Carryover', 'Legacy VAT liability from pre-GST period unresolved', 'LOW', '05-01-2026', 'ACCEPTED'],
    ['TDS Return Q3', 'Minor mismatch in TAN details', 'LOW', '20-01-2026', 'RESOLVED'],
    ['Data Privacy Policy Review', 'Policy review overdue per DPDP Act', 'MEDIUM', '15-02-2026', 'OPEN'],
])

# ---------------- RISK ----------------
f = 'sample-data/category-templates/risk-template.xlsx'
fill(f, 'Exposure', [
    ['Key customer concentration', 'MARKET', 'Top 2 customers account for 48% of revenue', 'MEDIUM', 'HIGH', 'Arjun Das', '10-01-2026'],
    ['Foreign exchange exposure', 'MARKET', 'USD-denominated vendor contracts', 'LOW', 'MEDIUM', 'Meera Iyer', '18-01-2026'],
    ['Customer credit default', 'CREDIT', 'Rising DSO on 3 large accounts', 'MEDIUM', 'HIGH', 'Arjun Das', '05-02-2026'],
    ['Short-term funding gap', 'LIQUIDITY', 'Seasonal cash crunch in Q1', 'LOW', 'MEDIUM', 'Arjun Das', '12-02-2026'],
    ['New market entry delay', 'STRATEGIC', 'Regulatory approval pending for expansion', 'MEDIUM', 'MEDIUM', 'CEO Office', '20-02-2026'],
])
fill(f, 'Financial', [
    ['Cash runway pressure', 'CASH_FLOW', 'Burn rate increased 15% QoQ', 'MEDIUM', 'HIGH', 'Arjun Das', '15-01-2026'],
    ['Customer payment delays', 'CREDIT', 'Average DSO rose from 35 to 52 days', 'MEDIUM', 'HIGH', 'Arjun Das', '01-02-2026'],
    ['Interest rate exposure', 'MARKET', 'Floating rate term loan outstanding', 'LOW', 'MEDIUM', 'Meera Iyer', '10-02-2026'],
    ['Undercapitalization for expansion', 'CAPITAL', 'Planned capex exceeds current funding', 'MEDIUM', 'HIGH', 'CEO Office', '18-02-2026'],
    ['Vendor advance payment risk', 'OTHER', 'Large advance paid to unverified new vendor', 'LOW', 'MEDIUM', 'Meera Iyer', '01-03-2026'],
])
fill(f, 'Operational', [
    ['Single point of failure - IT admin', 'IT Operations', 'Only one employee holds admin credentials', 'LOW', 'HIGH', 'IT Team', '20-01-2026'],
    ['Key supplier dependency', 'Procurement', '70% of raw material from one supplier', 'MEDIUM', 'HIGH', 'Operations Team', '02-02-2026'],
    ['Manual payroll processing errors', 'HR Operations', 'No automated payroll validation checks', 'MEDIUM', 'MEDIUM', 'HR Team', '08-02-2026'],
    ['Warehouse capacity constraint', 'Logistics', 'Inventory nearing storage capacity limit', 'LOW', 'MEDIUM', 'Operations Team', '25-02-2026'],
    ['Customer support backlog', 'Customer Service', 'Ticket resolution time trending up', 'MEDIUM', 'LOW', 'Support Lead', '05-03-2026'],
])
fill(f, 'Compliance', [
    ['Delayed GST filing penalty risk', 'GST Act', 'Repeated late filings in Q4', 'MEDIUM', 'MEDIUM', 'Meera Iyer', '12-01-2026'],
    ['Data privacy non-compliance', 'DPDP Act 2023', 'Customer data retention policy not finalized', 'LOW', 'HIGH', 'Legal Team', '22-01-2026'],
    ['Labour law non-adherence', 'Shops & Establishment Act', 'Overtime records incomplete for 2 branches', 'LOW', 'MEDIUM', 'HR Team', '05-02-2026'],
    ['Fire safety certification lapse', 'State Fire Safety Regulations', 'NOC renewal overdue', 'HIGH', 'HIGH', 'Operations Team', '15-02-2026'],
    ['Anti-bribery policy gap', 'Prevention of Corruption Act', 'No formal gifts & hospitality policy', 'LOW', 'LOW', 'Legal Team', '01-03-2026'],
])
fill(f, 'Technology', [
    ['Outdated ERP version', 'Finance ERP', 'Running a version 2 major releases behind', 'MEDIUM', 'MEDIUM', 'IT Team', '15-01-2026'],
    ['No disaster recovery site', 'Core infrastructure', 'Single data center, no DR failover', 'LOW', 'HIGH', 'IT Team', '25-01-2026'],
    ['Weak password policy enforcement', 'Identity & Access', 'MFA not enforced for all admin accounts', 'MEDIUM', 'HIGH', 'IT Team', '05-02-2026'],
    ['Unpatched server vulnerabilities', 'Production servers', 'Critical patches pending on 4 servers', 'MEDIUM', 'HIGH', 'IT Team', '18-02-2026'],
    ['Shadow IT - unapproved SaaS tools', 'Employee devices', 'Multiple unsanctioned tools in use', 'LOW', 'LOW', 'IT Team', '02-03-2026'],
])

print('All 6 templates refilled (Jan-May baseline).')
