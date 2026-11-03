-- Receivables/Payables/GST liability/People cost are now sourced from the real
-- Finance subcategory forms (Position, Tax-GST, People Cost) via
-- FinanceKpiService/GstService/WorkforceService - these four tables backed
-- entities with REST endpoints but no frontend path to ever create a row, so
-- they were always empty in practice. Dropped rather than left dormant.
DROP TABLE IF EXISTS receivables;
DROP TABLE IF EXISTS payables;
DROP TABLE IF EXISTS gst_periods;
DROP TABLE IF EXISTS employee_profiles;
