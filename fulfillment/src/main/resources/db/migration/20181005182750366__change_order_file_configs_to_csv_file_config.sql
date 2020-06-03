ALTER TABLE order_file_templates
  RENAME TO file_templates;

ALTER TABLE order_file_columns
  RENAME TO file_columns;


ALTER TABLE file_columns
  RENAME COLUMN orderFileTemplateId TO fileTemplateId;

ALTER TABLE file_templates
  ADD templateType VARCHAR(50);

UPDATE file_templates
  SET templateType = 'ORDER';

ALTER TABLE file_templates
    ALTER COLUMN templateType SET NOT NULL;


-- Insert the shipment file template

INSERT INTO file_templates
    (id, fileprefix, headerinfile, templateType)
  VALUES ('0e795ac8-9822-44f2-9314-9ca3c386be0b', 'S', true, 'SHIPMENT');


INSERT INTO file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, fileTemplateId)
  VALUES ('39f17ffe-097d-41e3-bfb2-cf5cbe0d6d2c', 'Order number', 'fulfillment.header.order.number', NULL, true, 'orderCode', 'order', true, 0, NULL, NULL, '0e795ac8-9822-44f2-9314-9ca3c386be0b');
INSERT INTO file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, fileTemplateId)
  VALUES ('2c515ecc-53de-46b8-ad6b-e5172066a24d', 'Facility code', 'fulfillment.header.facility.code', NULL, true, 'facilityId', 'order', true, 1, 'Facility', 'code', '0e795ac8-9822-44f2-9314-9ca3c386be0b');
INSERT INTO file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, fileTemplateId)
  VALUES ('aea50901-5ad5-49f2-89ce-43ee55624e38', 'Product code', 'fulfillment.header.product.code', NULL, true, 'orderableId', 'lineItem', true, 2, 'Orderable', 'productCode', '0e795ac8-9822-44f2-9314-9ca3c386be0b');
INSERT INTO file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, fileTemplateId)
  VALUES ('665c82bb-f126-4eb6-a8ee-c46cb32b4cae', 'Quantity Shipped', 'fulfillment.header.shipped.quantity', NULL, true, 'quantityShipped', 'lineItem', true, 3, NULL, NULL, '0e795ac8-9822-44f2-9314-9ca3c386be0b');
