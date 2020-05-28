UPDATE "requisition"."available_requisition_column_sources" SET value = 'CALCULATED' WHERE columnid = 'd06a04f9-1a81-45df-9921-60adccd0a31e';

DELETE FROM "requisition"."available_requisition_column_options" where columnid = 'd06a04f9-1a81-45df-9921-60adccd0a31e';
