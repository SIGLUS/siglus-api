--
-- Name: configuration_settings; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE configuration_settings (
    "key" character varying(255) NOT NULL,
    "value" character varying(255) NOT NULL
);

--
-- Name: order_file_columns; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE order_file_columns (
    id uuid NOT NULL,
    columnlabel character varying(255),
    datafieldlabel character varying(255),
    format character varying(255),
    include boolean NOT NULL,
    keypath character varying(255),
    nested character varying(255),
    openlmisfield boolean NOT NULL,
    "position" integer NOT NULL,
    related character varying(255),
    relatedkeypath character varying(255),
    orderfiletemplateid uuid NOT NULL
);

--
-- Name: order_file_templates; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE order_file_templates (
    id uuid NOT NULL,
    fileprefix character varying(255) NOT NULL,
    headerinfile boolean NOT NULL
);

--
-- Name: order_line_items; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE order_line_items (
    id uuid NOT NULL,
    approvedquantity bigint NOT NULL,
    filledquantity bigint NOT NULL,
    orderableid uuid,
    orderedquantity bigint NOT NULL,
    packstoship bigint,
    orderid uuid NOT NULL
);

--
-- Name: order_number_configurations; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE order_number_configurations (
    id uuid NOT NULL,
    includeordernumberprefix boolean NOT NULL,
    includeprogramcode boolean NOT NULL,
    includetypesuffix boolean NOT NULL,
    ordernumberprefix character varying(255)
);

--
-- Name: orders; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE orders (
    id uuid NOT NULL,
    createdbyid uuid NOT NULL,
    createddate timestamp with time zone,
    emergency boolean NOT NULL,
    externalid uuid NOT NULL,
    facilityid uuid,
    ordercode text NOT NULL,
    processingperiodid uuid,
    programid uuid NOT NULL,
    quotedcost numeric(19,2) NOT NULL,
    receivingfacilityid uuid NOT NULL,
    requestingfacilityid uuid NOT NULL,
    status character varying(255) NOT NULL,
    supplyingfacilityid uuid NOT NULL
);

--
-- Name: proof_of_deliveries; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE proof_of_deliveries (
    id uuid NOT NULL,
    deliveredby text,
    receivedby text,
    receiveddate timestamp with time zone,
    orderid uuid NOT NULL
);

--
-- Name: proof_of_delivery_line_items; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE proof_of_delivery_line_items (
    id uuid NOT NULL,
    notes text,
    quantityreceived bigint,
    quantityreturned bigint,
    quantityshipped bigint,
    replacedproductcode text,
    orderlineitemid uuid NOT NULL,
    proofofdeliveryid uuid NOT NULL
);

--
-- Name: status_messages; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE status_messages (
    id uuid NOT NULL,
    authorid uuid,
    body character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    orderid uuid NOT NULL
);

--
-- Name: template_parameters; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE template_parameters (
    id uuid NOT NULL,
    datatype text,
    defaultvalue text,
    description text,
    displayname text,
    name text,
    selectsql text,
    templateid uuid NOT NULL
);

--
-- Name: templates; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE templates (
    id uuid NOT NULL,
    data bytea,
    description text,
    name text NOT NULL,
    type text
);

--
-- Name: transfer_properties; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE transfer_properties (
    type character varying(31) NOT NULL,
    id uuid NOT NULL,
    facilityid uuid NOT NULL,
    localdirectory text,
    passivemode boolean,
    password text,
    protocol text,
    remotedirectory text,
    serverhost text,
    serverport integer,
    username text,
    path text
);

--
-- Name: configuration_settings_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY configuration_settings
    ADD CONSTRAINT configuration_settings_pkey PRIMARY KEY (key);


--
-- Name: order_file_columns_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY order_file_columns
    ADD CONSTRAINT order_file_columns_pkey PRIMARY KEY (id);


--
-- Name: order_file_templates_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY order_file_templates
    ADD CONSTRAINT order_file_templates_pkey PRIMARY KEY (id);


--
-- Name: order_line_items_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY order_line_items
    ADD CONSTRAINT order_line_items_pkey PRIMARY KEY (id);


--
-- Name: order_number_configurations_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY order_number_configurations
    ADD CONSTRAINT order_number_configurations_pkey PRIMARY KEY (id);


--
-- Name: orders_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: proof_of_deliveries_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY proof_of_deliveries
    ADD CONSTRAINT proof_of_deliveries_pkey PRIMARY KEY (id);


--
-- Name: proof_of_delivery_line_items_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY proof_of_delivery_line_items
    ADD CONSTRAINT proof_of_delivery_line_items_pkey PRIMARY KEY (id);


--
-- Name: status_messages_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY status_messages
    ADD CONSTRAINT status_messages_pkey PRIMARY KEY (id);


--
-- Name: template_parameters_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY template_parameters
    ADD CONSTRAINT template_parameters_pkey PRIMARY KEY (id);


--
-- Name: templates_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY templates
    ADD CONSTRAINT templates_pkey PRIMARY KEY (id);


--
-- Name: transfer_properties_pkey; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY transfer_properties
    ADD CONSTRAINT transfer_properties_pkey PRIMARY KEY (id);


--
-- Name: uk_1nah70jfu9ck93htxiwym9c3b; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY templates
    ADD CONSTRAINT uk_1nah70jfu9ck93htxiwym9c3b UNIQUE (name);


--
-- Name: uk_21y81ilpcwtxc459g3l41nbli; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY orders
    ADD CONSTRAINT uk_21y81ilpcwtxc459g3l41nbli UNIQUE (ordercode);


--
-- Name: uk_sprkvmtubsjd58jc0afdycmiy; Type: CONSTRAINT; Schema: fulfillment; Owner: postgres; Tablespace:
--

ALTER TABLE ONLY transfer_properties
    ADD CONSTRAINT uk_sprkvmtubsjd58jc0afdycmiy UNIQUE (facilityid);


--
-- Name: fk6p8u7j1vauhmjf0q8axqj3aj4; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY template_parameters
    ADD CONSTRAINT fk6p8u7j1vauhmjf0q8axqj3aj4 FOREIGN KEY (templateid) REFERENCES templates(id);


--
-- Name: fkc31fti7wlfim9ly9fu394pfj9; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY proof_of_delivery_line_items
    ADD CONSTRAINT fkc31fti7wlfim9ly9fu394pfj9 FOREIGN KEY (orderlineitemid) REFERENCES order_line_items(id);


--
-- Name: fkou618chqnxfgjmouj7nlemadc; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY status_messages
    ADD CONSTRAINT fkou618chqnxfgjmouj7nlemadc FOREIGN KEY (orderid) REFERENCES orders(id);


--
-- Name: fkq9ce0i4053ykpvnb1fhmsxo3y; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY proof_of_delivery_line_items
    ADD CONSTRAINT fkq9ce0i4053ykpvnb1fhmsxo3y FOREIGN KEY (proofofdeliveryid) REFERENCES proof_of_deliveries(id);


--
-- Name: fkqomo9559lvt6qgkgfrmjw49ag; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY order_file_columns
    ADD CONSTRAINT fkqomo9559lvt6qgkgfrmjw49ag FOREIGN KEY (orderfiletemplateid) REFERENCES order_file_templates(id);


--
-- Name: fkt87opovuclkhewnkb7qpg8bpe; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY order_line_items
    ADD CONSTRAINT fkt87opovuclkhewnkb7qpg8bpe FOREIGN KEY (orderid) REFERENCES orders(id);


--
-- Name: fkx8ej15oxtmq8sqt7dw8t99x4; Type: FK CONSTRAINT; Schema: fulfillment; Owner: postgres
--

ALTER TABLE ONLY proof_of_deliveries
    ADD CONSTRAINT fkx8ej15oxtmq8sqt7dw8t99x4 FOREIGN KEY (orderid) REFERENCES orders(id);
