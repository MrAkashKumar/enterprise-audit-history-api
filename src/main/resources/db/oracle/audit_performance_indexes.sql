-- Read-only inspection: check existing index columns before proposing new indexes.
SELECT index_name, column_name, column_position
FROM user_ind_columns
WHERE table_name = 'PMC_POSITION_BALANCE_AUD'
ORDER BY index_name, column_position;

-- Example only. Ask the DBA to review the real execution plan and existing indexes first.
-- CREATE INDEX IX_POSITION_BAL_AUD_ID_REV
--     ON PMC_POSITION_BALANCE_AUD (ID, REV);
