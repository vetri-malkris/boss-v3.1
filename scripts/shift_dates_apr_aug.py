# -*- coding: utf-8 -*-
"""Shifts every date-like and period-like cell value in the 6 category sample
files forward by 3 months (Jan-May 2026 -> Apr-Aug 2026), so a fresh tenant's
data genuinely spans April through August rather than January through May.
Pattern-matches on cell VALUES (DD-MM-YYYY full dates, YYYY-MM periods,
YYYY-Q# quarters), not on column semantics, so it's generic across every
subcategory without needing to know which column means what."""
import calendar
import re
import shutil
import openpyxl

FILES = [
    'sample-data/category-templates/finance-template.xlsx',
    'sample-data/category-templates/human_resource-template.xlsx',
    'sample-data/category-templates/accounting-template.xlsx',
    'sample-data/category-templates/auditing-template.xlsx',
    'sample-data/category-templates/compliance-template.xlsx',
    'sample-data/category-templates/risk-template.xlsx',
]

FULL_DATE = re.compile(r'^(\d{2})-(\d{2})-(\d{4})$')       # DD-MM-YYYY
PERIOD = re.compile(r'^(\d{4})-(\d{2})$')                   # YYYY-MM
QUARTER = re.compile(r'^(\d{4})-Q(\d)$')                    # YYYY-Q#

MONTHS_SHIFT = 3


def shift_month_year(year: int, month: int, delta: int) -> tuple[int, int]:
    total = (month - 1) + delta
    new_year = year + total // 12
    new_month = total % 12 + 1
    return new_year, new_month


def shift_value(value):
    if not isinstance(value, str):
        return value

    m = FULL_DATE.match(value)
    if m:
        day, month, year = int(m.group(1)), int(m.group(2)), int(m.group(3))
        new_year, new_month = shift_month_year(year, month, MONTHS_SHIFT)
        last_day = calendar.monthrange(new_year, new_month)[1]
        day = min(day, last_day)
        return f'{day:02d}-{new_month:02d}-{new_year:04d}'

    m = PERIOD.match(value)
    if m:
        year, month = int(m.group(1)), int(m.group(2))
        new_year, new_month = shift_month_year(year, month, MONTHS_SHIFT)
        return f'{new_year:04d}-{new_month:02d}'

    m = QUARTER.match(value)
    if m:
        # Quarters shift by whole quarters (3 months = 1 quarter) for consistency.
        year, quarter = int(m.group(1)), int(m.group(2))
        total_q = (quarter - 1) + 1
        new_year = year + total_q // 4
        new_quarter = total_q % 4 + 1
        return f'{new_year:04d}-Q{new_quarter}'

    return value


for path in FILES:
    wb = openpyxl.load_workbook(path)
    changed = 0
    for sheet_name in wb.sheetnames:
        if sheet_name == 'Field Guide':
            continue
        ws = wb[sheet_name]
        # Row 1 = header, row 2 = auto-generated example - leave both untouched;
        # only shift the real data rows (3+).
        for row in ws.iter_rows(min_row=3):
            for cell in row:
                new_value = shift_value(cell.value)
                if new_value != cell.value:
                    cell.value = new_value
                    changed += 1
    wb.save(path)
    print(f'{path}: {changed} cells shifted')
