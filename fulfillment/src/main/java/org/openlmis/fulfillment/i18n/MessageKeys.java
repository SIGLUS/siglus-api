/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.fulfillment.i18n;

import java.util.Arrays;

public abstract class MessageKeys {
  private static final String DELIMITER = ".";

  private static final String SERVICE_PREFIX = "fulfillment";
  private static final String ERROR_PREFIX = join(SERVICE_PREFIX, "error");
  private static final String VALIDATION_ERROR = join(SERVICE_PREFIX, "validationError");

  private static final String AUTHENTICATION = "authentication";
  private static final String USER = "user";
  private static final String ORDER = "order";
  private static final String ORDER_RETRY = "orderRetry";
  private static final String ORDER_FILE_TEMPLATE = "fileTemplate";
  private static final String REFERENCE_DATA = "referenceData";
  private static final String REPORTING = "reporting";
  private static final String TRANSFER_PROPERTIES = "transferProperties";
  private static final String PROOF_OF_DELIVERY = "proofOfDelivery";
  private static final String SHIPMENT = "shipment";
  private static final String SHIPMENT_DRAFT = "shipmentDraft";
  private static final String STOCK_EVENT = "stockEvent";

  private static final String CLASS = "class";
  private static final String ENCODING = "encoding";
  private static final String JASPER = "jasper";
  private static final String PERMISSION = "permission";
  private static final String PERMISSIONS = PERMISSION + "s";
  private static final String FILE = "file";
  private static final String PARAMETER = "parameter";
  private static final String TEMPLATE = "template";
  private static final String LINE_ITEMS = "lineItems";
  private static final String JAVERS = "javers";

  private static final String REQUIRED = "required";
  private static final String NOT_FOUND = "notFound";
  private static final String NOT_SUPPORTED = "notSupported";
  private static final String MISSING = "missing";
  private static final String INVALID_STATUS = "invalidStatus";
  private static final String CREATION = "creation";
  private static final String EMPTY = "empty";
  private static final String INVALID = "invalid";
  private static final String INCORRECT_TYPE = "incorrectType";
  private static final String EXISTS = "exists";
  private static final String DUPLICATE = "duplicate";
  private static final String COLUMN = "column";
  private static final String FOUND = "found";

  public static final String USER_NOT_FOUND =
      join(ERROR_PREFIX, AUTHENTICATION, USER, NOT_FOUND);

  public static final String CLASS_NOT_FOUND = join(ERROR_PREFIX, CLASS, NOT_FOUND);
  public static final String DATA_INTEGRITY_VIOLATION =
      join(ERROR_PREFIX, "dataIntegrityViolation");
  public static final String CONSTRAINT_VIOLATION =
      join(ERROR_PREFIX, "constraintViolation");
  public static final String ERROR_IO = join(ERROR_PREFIX, "io");
  public static final String ERROR_ENCODING =
      join(ERROR_PREFIX, ENCODING, NOT_SUPPORTED);

  public static final String ERROR_JASPER = join(ERROR_PREFIX, JASPER);
  public static final String ERROR_JASPER_FILE_CREATION =
      join(ERROR_JASPER, "fileCreation");
  public static final String ERROR_JASPER_REPORT_CREATION_WITH_MESSAGE =
      join(ERROR_JASPER, "reportCreationWithMessage");

  public static final String PERMISSION_MISSING = join(ERROR_PREFIX, PERMISSION, MISSING);
  public static final String PERMISSIONS_MISSING = join(ERROR_PREFIX, PERMISSIONS, MISSING);

  public static final String ORDER_NOT_FOUND = join(ERROR_PREFIX, ORDER, NOT_FOUND);
  public static final String ORDER_INVALID_STATUS = join(ERROR_PREFIX, ORDER, INVALID_STATUS);

  public static final String ORDER_RETRY_INVALID_STATUS =
      join(ERROR_PREFIX, ORDER_RETRY, INVALID_STATUS);

  public static final String ERROR_ORDER_FILE_TEMPLATE_CREATION =
      join(ERROR_PREFIX, ORDER_FILE_TEMPLATE, CREATION);
  public static final String ERROR_MISSING_REQUIRED_COLUMN =
      join(ERROR_PREFIX, ORDER_FILE_TEMPLATE, REQUIRED, COLUMN, MISSING);

  public static final String ERROR_DUPLICATE_COLUMN_FOUND =
      join(ERROR_PREFIX, ORDER_FILE_TEMPLATE, DUPLICATE, COLUMN, FOUND);


  public static final String ERROR_REFERENCE_DATA_RETRIEVE =
      join(ERROR_PREFIX, REFERENCE_DATA, "retrieve");

  public static final String ERROR_REPORTING_CREATION = join(ERROR_PREFIX, REPORTING, CREATION);
  public static final String REPORTING_EXTRA_PROPERTIES =
      join(ERROR_PREFIX, REPORTING, "extraProperties");
  public static final String REPORTING_FILE_EMPTY =
      join(ERROR_PREFIX, REPORTING, FILE, EMPTY);
  public static final String REPORTING_FILE_INCORRECT_TYPE =
      join(ERROR_PREFIX, REPORTING, FILE, INCORRECT_TYPE);
  public static final String REPORTING_FILE_INVALID =
      join(ERROR_PREFIX, REPORTING, FILE, INVALID);
  public static final String REPORTING_FILE_MISSING =
      join(ERROR_PREFIX, REPORTING, FILE, MISSING);
  public static final String REPORTING_PARAMETER_INCORRECT_TYPE =
      join(ERROR_PREFIX, REPORTING, PARAMETER, INCORRECT_TYPE);
  public static final String REPORTING_PARAMETER_MISSING =
      join(ERROR_PREFIX, REPORTING, PARAMETER, MISSING);
  public static final String REPORTING_TEMPLATE_EXISTS =
      join(ERROR_PREFIX, REPORTING, TEMPLATE, EXISTS);
  public static final String REPORTING_TEMPLATE_NOT_FOUND =
      join(ERROR_PREFIX, REPORTING, TEMPLATE, NOT_FOUND);

  public static final String TRANSFER_PROPERTIES_DUPLICATE =
      join(ERROR_PREFIX, TRANSFER_PROPERTIES, DUPLICATE);
  public static final String TRANSFER_PROPERTIES_INCORRECT =
      join(ERROR_PREFIX, TRANSFER_PROPERTIES, "incorrect");

  public static final String PROOF_OF_DELIVERY_LINE_ITEMS_REQUIRED =
      join(ERROR_PREFIX, PROOF_OF_DELIVERY, LINE_ITEMS, REQUIRED);
  public static final String PROOF_OF_DELIVERY_NOT_FOUND =
      join(ERROR_PREFIX, PROOF_OF_DELIVERY, NOT_FOUND);
  public static final String PROOF_OF_DELIVERY_ALREADY_CONFIRMED =
      join(ERROR_PREFIX, PROOF_OF_DELIVERY, "alreadyConfirmed");
  public static final String ERROR_INCORRECT_VVM_STATUS =
      join(ERROR_PREFIX, PROOF_OF_DELIVERY, "vvmStatus", "incorrectValue");
  public static final String ERROR_MISSING_REASON =
      join(ERROR_PREFIX, PROOF_OF_DELIVERY, "reason", MISSING);
  public static final String ERROR_INCORRECT_QUANTITIES =
      join(ERROR_PREFIX, PROOF_OF_DELIVERY, "incorrectSumOfAcceptedAndRejected");

  public static final String MUST_CONTAIN_VALUE =
      join(VALIDATION_ERROR, "mustContainValue");
  public static final String MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO =
      join(VALIDATION_ERROR, "mustBeGreaterThanOrEqualToZero");

  public static final String FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT
      = "fulfillment.email.orderCreation.subject";
  public static final String FULFILLMENT_EMAIL_ORDER_CREATION_BODY
      = "fulfillment.email.orderCreation.body";

  public static final String FULFILLMENT_EMAIL_POD_CONFIRMED_SUBJECT
      = "fulfillment.email.podConfirmed.subject";
  public static final String FULFILLMENT_EMAIL_POD_CONFIRMED_BODY
      = "fulfillment.email.podConfirmed.body";

  private static final String ERROR_DTO_EXPANSION = join(ERROR_PREFIX, "dtoExpansion");
  public static final String ERROR_DTO_EXPANSION_CAST = join(ERROR_DTO_EXPANSION, "cast");
  public static final String ERROR_DTO_EXPANSION_HREF = join(ERROR_DTO_EXPANSION, "href");
  public static final String ERROR_DTO_EXPANSION_ASSIGNMENT =
      join(ERROR_DTO_EXPANSION, "assignment");

  public static final String SHIPMENT_NOT_FOUND =
      join(ERROR_PREFIX, SHIPMENT, NOT_FOUND);
  public static final String SHIPMENT_ORDERLESS_NOT_SUPPORTED =
      join(ERROR_PREFIX, SHIPMENT, "orderless", NOT_SUPPORTED);
  public static final String SHIPMENT_LINE_ITEMS_REQUIRED =
      join(ERROR_PREFIX, SHIPMENT, LINE_ITEMS, REQUIRED);
  public static final String SHIPMENT_ORDER_DUPLICATE =
      join(ERROR_PREFIX, SHIPMENT, ORDER, DUPLICATE);
  public static final String SHIPMENT_ORDER_REQUIRED =
      join(ERROR_PREFIX, SHIPMENT, "orderId", REQUIRED);
  public static final String SHIPMENT_ORDER_STATUS_INVALID =
      join(ERROR_PREFIX, SHIPMENT, ORDER, "status", INVALID);

  public static final String SHIPMENT_DRAFT_ORDER_REQUIRED =
      join(ERROR_PREFIX, SHIPMENT_DRAFT, "orderId", REQUIRED);
  public static final String SHIPMENT_DRAFT_ORDER_NOT_FOUND =
      join(ERROR_PREFIX, SHIPMENT_DRAFT, ORDER, NOT_FOUND);
  public static final String SHIPMENT_DRAFT_ID_MISMATCH =
      join(ERROR_PREFIX, SHIPMENT_DRAFT, "id", "mismatch");
  public static final String SHIPMENT_DRAT_ORDER_DUPLICATE =
      join(ERROR_PREFIX, SHIPMENT_DRAFT, ORDER, DUPLICATE);
  public static final String CANNOT_CREATE_SHIPMENT_DRAFT_FOR_ORDER_WITH_WRONG_STATUS =
      join(ERROR_PREFIX, SHIPMENT_DRAFT, "create", "orderWithWrongStatus");

  public static final String EVENT_MISSING_SOURCE_DESTINATION =
      join(ERROR_PREFIX, STOCK_EVENT, "missingSourceDestination");

  public static final String ERROR_JAVERS_EXISTING_ENTRY =
      join(ERROR_PREFIX, JAVERS, "entryAlreadyExists");

  public static final String ERROR_SIZE_NULL =
      join(ERROR_PREFIX,"pageable.size.null");
  public static final String ERROR_SIZE_NOT_POSITIVE =
      join(ERROR_PREFIX,"pageable.size.notPositive");

  private static String join(String... params) {
    return String.join(DELIMITER, Arrays.asList(params));
  }

  private MessageKeys() {
    throw new UnsupportedOperationException();
  }

}
