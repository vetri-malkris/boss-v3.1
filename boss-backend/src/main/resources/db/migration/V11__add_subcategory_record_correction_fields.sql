ALTER TABLE subcategory_data_records
    ADD COLUMN voided      BOOLEAN     NOT NULL DEFAULT false,
    ADD COLUMN voided_at   TIMESTAMPTZ NULL,
    ADD COLUMN voided_by   UUID        NULL REFERENCES users (id),
    ADD COLUMN void_reason VARCHAR(500) NULL;
