INSERT INTO "requisition"."available_requisition_columns"("id","canbechangedbyuser","canchangeorder","columntype","definition","indicator","isdisplayrequired","label","mandatory","name","supportstag")
VALUES
('d06a04f9-1a81-45df-9921-60adccd0a31e',FALSE,TRUE,'NUMERIC','Suggested quantity calculated by FC for final approval.','SQ',FALSE,'Suggested Quantity',FALSE,'suggestedQuantity',FALSE);

INSERT INTO "requisition"."available_requisition_column_sources"("columnid","value")
VALUES
('d06a04f9-1a81-45df-9921-60adccd0a31e','USER_INPUT');

INSERT INTO "requisition"."available_requisition_column_options"("id","optionlabel","optionname","columnid")
VALUES
('822fc359-6d78-4ba0-99fd-d7c776041c5e','requisitionConstants.displayTogetherWithApprovedQuantity','displayTogetherWithApprovedQuantity','d06a04f9-1a81-45df-9921-60adccd0a31e'),
('4b83501e-18cf-44c4-80d4-2dea7f24c1b7','requisitionConstants.notDisplayTogetherWithApprovedQuantity','notDisplayTogetherWithApprovedQuantity','d06a04f9-1a81-45df-9921-60adccd0a31e');
