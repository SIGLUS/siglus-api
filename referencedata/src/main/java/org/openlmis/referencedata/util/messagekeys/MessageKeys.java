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

package org.openlmis.referencedata.util.messagekeys;

import java.util.Arrays;

public abstract class MessageKeys {
  private static final String DELIMITER = ".";

  // General
  private static final String SERVICE = "referenceData";
  protected static final String SERVICE_ERROR = join(SERVICE, "error");

  protected static final String NON_EXISTENT = "nonExistent";
  protected static final String UNAUTHORIZED = "unauthorized";
  protected static final String CONSTRAINT = "constraint";
  protected static final String DUPLICATED = "duplicated";
  protected static final String VALIDATION = "validation";
  protected static final String NOT_FOUND = "notFound";
  protected static final String REQUIRED = "required";
  protected static final String EXTERNAL = "external";
  protected static final String DELETING = "deleting";
  protected static final String CREATED = "created";
  protected static final String GENERIC = "generic";
  protected static final String WITHOUT = "without";
  protected static final String SEARCH = "search";
  protected static final String FAILED = "failed";
  protected static final String SAVING = "saving";
  protected static final String SAVE = "save";
  protected static final String NUMBER = "number";
  protected static final String NUMERIC = "numeric";
  protected static final String EMPTY = "empty";
  protected static final String RESET = "reset";
  protected static final String WRONG = "wrong";
  protected static final String WITH = "with";
  protected static final String NULL = "null";
  protected static final String NAME = "name";
  protected static final String TYPE = "type";
  protected static final String CODE = "code";
  protected static final String AND = "and";
  protected static final String ID = "id";
  protected static final String EXTRA_DATA = "extraData";
  protected static final String IS_INVARIABLE = "isInvariable";
  protected static final String USERNAME = "username";
  protected static final String INVALID = "invalid";
  protected static final String LACKS_PARAMETERS = "lacksParameters";
  protected static final String MUST_BE_UNIQUE = "mustBeUnique";
  protected static final String MUST_BE_PROVIDED = "mustBeProvided";
  protected static final String MISMATCH = "mismatch";
  protected static final String INVALID_LENGTH = "invalidLength";
  protected static final String INVALID_PARAMS = "invalidParams";
  protected static final String FORMAT = "format";
  protected static final String UUID = "uuid";
  protected static final String DATE = "date";
  protected static final String BOOLEAN = "boolean";
  protected static final String ID_MISMATCH = "idMismatch";
  protected static final String JAVERS = "javers";
  protected static final String UNALLOWED_KEY = "unallowedKey";
  protected static final String MODIFIED_KEY = "modifiedKey";
  protected static final String FIELD_IS_INVARIANT = "fieldIsInvariant";
  protected static final String MISSING = "missing";
  protected static final String PROVIDED = "provided";
  protected static final String ASSIGNED = "isAlreadyAssigned";

  // Entities
  protected static final String LOT = "lot";
  protected static final String USER = "user";
  protected static final String ROLE = "role";
  protected static final String ROLE_ASSIGNMENT = "roleAssignment";
  protected static final String ROLE_TYPE = "roleType";
  protected static final String RIGHT = "right";
  protected static final String PROGRAM = "program";
  protected static final String PRODUCT = "product";
  protected static final String FACILITY = "facility";
  protected static final String ORDERABLE = "orderable";
  protected static final String ORDERABLE_FULFILL = "orderableFulFill";
  protected static final String TRADE_ITEM = "tradeItem";
  protected static final String SUPPLY_LINE = "supplyLine";
  protected static final String FACILITY_TYPE = "facilityType";
  protected static final String COMMODITY_TYPE = "commodityType";
  protected static final String GEOGRAPHIC_ZONE = "geographicZone";
  protected static final String GEOGRAPHIC_LEVEL = "geographicLevel";
  protected static final String SUPERVISORY_NODE = "supervisoryNode";
  protected static final String FACILITY_OPERATOR = "facilityOperator";
  protected static final String PROCESSING_PERIOD = "processingPeriod";
  protected static final String PROCESSING_SCHEDULE = "processingSchedule";
  protected static final String ORDERED_DISPLAY_VALUE = "orderedDisplayValue";
  protected static final String STOCK_ADJUSTMENT_REASON = "stockAdjustmentReason";
  protected static final String ORDERABLE_DISPLAY_CATEGORY = "orderableDisplayCategory";
  protected static final String FACILITY_TYPE_APPROVED_PRODUCT = "facilityTypeApprovedProduct";
  protected static final String SUPPORTED_PROGRAMS = "supportedPrograms";
  protected static final String SERVICE_ACCOUNT = "serviceAccount";
  protected static final String FHIR = "fhir";
  protected static final String SUPPLY_PARTNER = "supplyPartner";
  protected static final String SYSTEM_NOTIFICATION = "systemNotification";

  // Common to subclasses
  protected static final String EMAIL = "email";
  protected static final String BODY = "body";
  protected static final String ACCOUNT = "account";
  protected static final String SUBJECT = "subject";
  protected static final String PASSWORD = "password";
  protected static final String PROGRAM_REPOSITORY = "programRepository";
  protected static final String ORDERABLE_DISPLAY_CATEGORY_REPOSITORY =
      "orderableDisplayCategoryRepository";
  protected static final String FIRSTNAME = "firstName";
  protected static final String LASTNAME = "lastName";

  // Javers
  public static final String ERROR_JAVERS_EXISTING_ENTRY =
      join(SERVICE_ERROR, JAVERS, "entryAlreadyExists");

  public static final String ERROR_IO = SERVICE_ERROR + ".io";

  protected MessageKeys() {
    throw new UnsupportedOperationException();
  }

  protected static String join(String... params) {
    return String.join(DELIMITER, Arrays.asList(params));
  }
}
