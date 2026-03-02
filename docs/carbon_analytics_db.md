# Carbon Expert DB Evolution (ML + Risk + Scenarios)

This document summarizes the extended schema for the Carbon Expert module.

## What’s new
- **Project profile** for baseline emissions and climate context.
- **Emission factors** for sector/activity calculations.
- **Carbon metrics** per project/evaluation over time.
- **Policy rules & outcomes** for regulatory logic.
- **Evaluation versioning** for audit trail and ML labels.
- **Scenarios & what‑if** runs with results.
- **Risk models & scores** for advanced analytics.
- **ML datasets** for training and inference.
- **API audit logs** for traceability.
- **Synthetic data tracking** for ML pipelines.

## Files
- Schema DDL: `schema_carbon_analytics.sql`

## Recommended rollout
1. Apply the new schema DDL.
2. Add optional ALTERs to existing tables if needed.
3. Seed emission factors and policy rules.
4. Generate synthetic data (optional).
5. Add services for scenario runs and risk scoring.

## Notes
- The schema is additive and should not break existing UI.
- Use JSON columns to keep ML features flexible.
- Indexes are included for common analytics filters.

