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

package org.siglus.siglusapi.i18n;

import java.util.Arrays;

public abstract class MessageKeys {
  private static final String DELIMITER = ".";
  private static final String SERVICE_PREFIX = "siglusapi";
  private static final String ERROR = "error";
  private static final String WIDGET = "widget";
  private static final String ADDITIONAL_ORDERABLE = "additionalorderable";
  private static final String CODE = "code";
  protected static final String SEARCH = "search";
  private static final String SHIPMENT = "shipment";
  private static final String ORDER = "order";
  protected static final String INVALID = "invalid";
  private static final String DUPLICATED = "duplicated";
  protected static final String INVALID_PARAMS = "invalidParams";

  protected static final String ERROR_PREFIX = join(SERVICE_PREFIX, ERROR);
  private static final String EVENT_ERROR_PREFIX = ERROR_PREFIX + ".event";
  private static final String STOCK_MANAGEMENT_ERROR_PREFIX = ERROR_PREFIX + ".stockManagement";
  public static final String ERROR_PERMISSION_NOT_SUPPORTED = ERROR_PREFIX + ".authorization.permission.not.supported";
  public static final String ERROR_USER_NOT_FOUND = ERROR_PREFIX + ".authorization.user.notFound";
  public static final String ERROR_WIDGET_CODE_DUPLICATED = join(ERROR_PREFIX, WIDGET, CODE, DUPLICATED);
  public static final String ERROR_ADDITIONAL_ORDERABLE_DUPLICATED =
      join(ERROR_PREFIX, ADDITIONAL_ORDERABLE, DUPLICATED);

  public static final String ERROR_NOT_ACCEPTABLE = ERROR_PREFIX + ".not.acceptable";
  public static final String ERROR_SUB_ORDER_LINE_ITEM = ERROR_PREFIX + ".suborder.line.item.not.exist";
  public static final String ERROR_LOT_CODE_IS_EMPTY = ERROR_PREFIX + ".lotcode.is.empty";
  public static final String ERROR_TRADE_ITEM_IS_EMPTY = ERROR_PREFIX + ".tradeItem.is.empty";
  public static final String ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY = ERROR_PREFIX + ".lotinfo.should.empty";
  public static final String ERROR_EVENT_FACILITY_INVALID = EVENT_ERROR_PREFIX + ".facilityId.invalid";
  public static final String ERROR_FACILITY_ID_MISSING = ERROR_PREFIX + ".facility.id.missing";
  public static final String ERROR_PROGRAM_MISSING = ERROR_PREFIX + ".program.missing";
  public static final String ERROR_PROGRAM_ID_MISSING = ERROR_PREFIX + ".program.id.missing";
  public static final String ERROR_USER_ID_MISSING = ERROR_PREFIX + ".user.id.missing";
  public static final String ERROR_IS_DRAFT_MISSING = ERROR_PREFIX + ".is.draft.missing";
  public static final String ERROR_DRAFT_TYPE_MISSING = ERROR_PREFIX + ".draft.type.missing";
  public static final String ERROR_NOT_EXPECTED_DRAFT_TYPE_ERROR = ERROR_PREFIX + ".not.expected.draft.type.error";
  public static final String ERROR_NOT_EXPECTED_USER_DRAFT = ERROR_PREFIX + ".not.expected.user.draft";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND = STOCK_MANAGEMENT_ERROR_PREFIX + ".id.notFound";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".draft.exists";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ID_MISMATCH = STOCK_MANAGEMENT_ERROR_PREFIX + ".id.mismatch";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED = STOCK_MANAGEMENT_ERROR_PREFIX + ".isSubmitted";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_DISABLED_VVM =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".orderable.disabled.vvm";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_LINE_ITEMS_MISSING =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".lineItems.missing";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_MISSING =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".orderable.missing";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ID_SHOULD_NULL =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".draft.id.should.null";
  public static final String ERROR_INVENTORY_CONFLICT_SUB_DRAFT = ERROR_PREFIX + ".inventory.conflict.subDraft";

  public static final String ERROR_SUBMIT_START_DATE_BEFORE_START_DATE = ERROR_PREFIX
      + ".submitStartDate.before.startDate";
  public static final String ERROR_SUBMIT_END_DATE_BEFORE_SUBMIT_START_DATE = ERROR_PREFIX
      + ".submitEndDate.before.submitStartDate";
  public static final String ERROR_SUBMIT_START_DATE_IS_BEFORE_LAST_SUBMIT_END_DATE = ERROR_PREFIX
      + ".submitStartDate.before.last.submitEndDate";

  public static final String SHIPMENT_ORDER_STATUS_INVALID =
      join(ERROR_PREFIX, SHIPMENT, ORDER, "status", INVALID);

  public static final String ERROR_SIZE_NULL = ERROR_PREFIX + ".pageable.size.null";
  public static final String ERROR_SIZE_NOT_POSITIVE = ERROR_PREFIX + ".pageable.size.notPositive";

  public static final String ERROR_VALIDATION_FAIL = "siglusapi.error.validationFail";

  public static final String SERVICE_ERROR = join(SERVICE_PREFIX, ERROR);
  public static final String FORMAT = "format";
  public static final String UUID = "uuid";
  public static final String DATE = "date";
  public static final String BOOLEAN = "boolean";
  public static final String ID_MISMATCH = "idMismatch";
  public static final String MISSING = "missing";
  public static final String ASSIGNED = "isAlreadyAssigned";
  public static final String PROCESSING_PERIOD = "processingPeriod";
  public static final String ID = "id";
  public static final String FACILITY = "facility";

  protected MessageKeys() {
    throw new UnsupportedOperationException();
  }

  protected static String join(String... params) {
    return String.join(DELIMITER, Arrays.asList(params));
  }
}
