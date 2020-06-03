--
-- Name: status_changes; Type: TABLE; Schema: fulfillment; Owner: postgres; Tablespace:
--

CREATE TABLE status_changes (
    id uuid NOT NULL,
    authorid uuid,
    createddate timestamp with time zone,
    status character varying(255) NOT NULL,
    orderid uuid NOT NULL
);
