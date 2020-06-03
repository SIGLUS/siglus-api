--
-- Data for Name: configuration_settings; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--


--
-- Data for Name: order_file_templates; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--

INSERT INTO order_file_templates (id, fileprefix, headerinfile) VALUES ('457ed5b0-80d7-4cb6-af54-e3f6138c8128', 'O', true);



--
-- Data for Name: order_file_columns; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--

INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('33b2d2e9-3167-46b0-95d4-1295be9afc21', 'Order number', 'fulfillment.header.order.number', NULL, true, 'orderCode', 'order', true, 1, NULL, NULL, '457ed5b0-80d7-4cb6-af54-e3f6138c8128');
INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('6b8d331b-a0dd-4a1f-aafb-40e6a72ab9f6', 'Facility code', 'fulfillment.header.facility.code', NULL, true, 'facilityId', 'order', true, 2, 'Facility', 'code', '457ed5b0-80d7-4cb6-af54-e3f6138c8128');
INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('752cda76-0db5-4b6e-bb79-0f531ab78e2e', 'Product code', 'fulfillment.header.product.code', NULL, true, 'orderableId', 'lineItem', true, 3, 'Orderable', 'productCode', '457ed5b0-80d7-4cb6-af54-e3f6138c8128');
INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('9e825396-269d-4873-baa4-89054e2722f5', 'Product name', 'fulfillment.header.product.name', NULL, true, 'orderableId', 'lineItem', true, 4, 'Orderable', 'name', '457ed5b0-80d7-4cb6-af54-e3f6138c8128');
INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('cd57f329-f549-4717-882e-ecbf98122c39', 'Approved quantity', 'fulfillment.header.approved.quantity', NULL, true, 'approvedQuantity', 'lineItem', true, 5, NULL, NULL, '457ed5b0-80d7-4cb6-af54-e3f6138c8128');
INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('d0e1aec7-1556-4dc1-8e21-d80a2d76b678', 'Period', 'fulfillment.header.period', 'MM/yy', true, 'processingPeriodId', 'order', true, 6, 'ProcessingPeriod', 'startDate', '457ed5b0-80d7-4cb6-af54-e3f6138c8128');
INSERT INTO order_file_columns (id, columnlabel, datafieldlabel, format, include, keypath, nested, openlmisfield, "position", related, relatedkeypath, orderfiletemplateid) VALUES ('dab6eec0-4cb4-4d4c-94b7-820308da73ff', 'Order date', 'fulfillment.header.order.date', 'dd/MM/yy', true, 'createdDate', 'order', true, 7, NULL, NULL, '457ed5b0-80d7-4cb6-af54-e3f6138c8128');



--
-- Data for Name: order_line_items; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: order_number_configurations; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--

INSERT INTO order_number_configurations (id, includeordernumberprefix, includeprogramcode, includetypesuffix, ordernumberprefix) VALUES ('70543032-b131-4219-b44d-7781d29db330', true, false, true, 'ORDER-');


--
-- Data for Name: orders; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: proof_of_deliveries; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: proof_of_delivery_line_items; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: status_messages; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: template_parameters; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: templates; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--



--
-- Data for Name: transfer_properties; Type: TABLE DATA; Schema: fulfillment; Owner: postgres
--
