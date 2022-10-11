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
  private static final String ADDITIONAL_ORDERABLE = "additionalorderable";
  protected static final String SEARCH = "search";
  private static final String SHIPMENT = "shipment";
  private static final String ORDER = "order";
  protected static final String INVALID = "invalid";
  private static final String DUPLICATED = "duplicated";
  protected static final String INVALID_PARAMS = "invalidParams";

  protected static final String ERROR_PREFIX = join(SERVICE_PREFIX, ERROR);
  private static final String EVENT_ERROR_PREFIX = ERROR_PREFIX + ".event";
  private static final String STOCK_MANAGEMENT_ERROR_PREFIX = ERROR_PREFIX + ".stockManagement";
  private static final String PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX = ERROR_PREFIX + ".stockMovement";
  private static final String POD_ERROR_PREFIX = ERROR_PREFIX + ".proofOfDelivery";
  private static final String LOCAL_ISSUE_VOUCHER_PREFIX = ERROR_PREFIX + ".localIssueVoucher";
  private static final String STOCK_CARD_ERROR_PREFIX = ERROR_PREFIX + ".stockCard";
  private static final String PERIOD_ERROR_PREFIX = ERROR_PREFIX + ".processingperiod";

  // authorization
  public static final String ERROR_PERMISSION_NOT_SUPPORTED = ERROR_PREFIX + ".authorization.permission.not.supported";
  public static final String ERROR_USER_NOT_FOUND = ERROR_PREFIX + ".authorization.user.notFound";
  public static final String ERROR_ADDITIONAL_ORDERABLE_DUPLICATED = join(ERROR_PREFIX, ADDITIONAL_ORDERABLE,
      DUPLICATED);
  public static final String ERROR_NOT_ACCEPTABLE = ERROR_PREFIX + ".not.acceptable";

  // order
  public static final String ERROR_ORDER_NOT_EXIST = ERROR_PREFIX + ".order.not.exist";
  public static final String ERROR_SUB_ORDER_LINE_ITEM = ERROR_PREFIX + ".suborder.line.item.not.exist";
  public static final String ERROR_TRADE_ITEM_IS_EMPTY = ERROR_PREFIX + ".tradeItem.is.empty";
  public static final String ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY = ERROR_PREFIX + ".lotinfo.should.empty";

  // shipment
  public static final String SHIPMENT_ORDER_STATUS_INVALID = join(ERROR_PREFIX, SHIPMENT, ORDER, "status", INVALID);

  // facility
  public static final String ERROR_EVENT_FACILITY_INVALID = EVENT_ERROR_PREFIX + ".facilityId.invalid";
  public static final String ERROR_EVENT_INITIAL_DRAFT_ID_INVALID = EVENT_ERROR_PREFIX + ".initial.draft.id.invalid";
  public static final String ERROR_FACILITY_ID_MISSING = ERROR_PREFIX + ".facility.id.missing";
  public static final String ERROR_PROGRAM_MISSING = ERROR_PREFIX + ".program.missing";
  public static final String ERROR_PROGRAM_ID_MISSING = ERROR_PREFIX + ".program.id.missing";
  public static final String ERROR_USER_ID_MISSING = ERROR_PREFIX + ".user.id.missing";
  public static final String ERROR_IS_DRAFT_MISSING = ERROR_PREFIX + ".is.draft.missing";
  public static final String ERROR_DRAFT_TYPE_MISSING = ERROR_PREFIX + ".draft.type.missing";
  public static final String ERROR_DRAFT_DOCUMENT_NUMBER_MISSING = ERROR_PREFIX + ".draft.document.number.missing";
  public static final String ERROR_NOT_EXPECTED_DRAFT_TYPE_ERROR = ERROR_PREFIX + ".not.expected.draft.type.error";
  public static final String ERROR_NOT_EXPECTED_USER_DRAFT = ERROR_PREFIX + ".not.expected.user.draft";
  public static final String ERROR_FACILITY_CHANGE_TO_WEB = ERROR_PREFIX + ".facility.change.to.web.error";
  public static final String ERROR_FACILITY_CHANGE_TO_ANDROID = ERROR_PREFIX + ".facility.change.to.android.error";
  public static final String ERROR_FACILITY_CHANGE_TO_LOCALMACHINE =
      ERROR_PREFIX + ".facility.change.to.localmachine.error";


  // stock management
  public static final String ERROR_VALIDATE_STOCK_MOVEMENT_DATE =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".movement.date.invalid";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND = STOCK_MANAGEMENT_ERROR_PREFIX + ".id.notFound";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND = STOCK_MANAGEMENT_ERROR_PREFIX + ".draft.notFound";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".draft.exists";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFT_SAME_ORDERABLE_ID_WITH_LOT_CODE =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".stock.management.subDraft.same.orderable.id.with.lot.code";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFT_EMPTY =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".subDrafts.empty";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_QUANTITY_NOT_MATCH =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".subDrafts.quantity.not.match";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFT_NOT_ALL_SUBMITTED =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".subDrafts.not.all.submitted";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFT_ALREADY_SUBMITTED =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".subDrafts.already.submitted";
  public static final String ERROR_STOCK_MANAGEMENT_INITIAL_DRAFT_EXISTS =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".initial.draft.exists";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFTS_MORE_THAN_TEN =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".drafts.more.than.ten";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ID_MISMATCH =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".id.mismatch";
  public static final String ERROR_STOCK_MANAGEMENT_SUB_DRAFT_TYPE_MISSING =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".draft.type.missing";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_IS_SUBMITTED =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".isSubmitted";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_DISABLED_VVM =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".orderable.disabled.vvm";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_LINE_ITEMS_MISSING =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".lineItems.missing";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_MISSING =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".orderable.missing";
  public static final String ERROR_STOCK_MANAGEMENT_DRAFT_ID_SHOULD_NULL =
      STOCK_MANAGEMENT_ERROR_PREFIX + ".draft.id.should.null";
  public static final String ERROR_INVENTORY_CONFLICT_SUB_DRAFT =
      ERROR_PREFIX + ".inventory.conflict.subDraft";
  public static final String ERROR_ISSUE_CONFLICT_SUB_DRAFT =
      ERROR_PREFIX + ".issue.conflict.subDraft";
  public static final String ERROR_INVENTORY_CONFLICT_DRAFT =
      ERROR_PREFIX + ".inventory.conflict.Draft";

  //product location movement
  public static final String ERROR_STOCK_CARD_NOT_FOUND =
      STOCK_CARD_ERROR_PREFIX + ".stock.card.notFound";
  public static final String ERROR_MOVEMENT_DRAFT_NOT_FOUND =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.draft.notFound";
  public static final String ERROR_MOVEMENT_DRAFT_EXISTS =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.draft.exists";
  public static final String ERROR_MOVEMENT_DRAFT_ID_MISMATCH =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.id.mismatch";
  public static final String ERROR_MOVEMENT_LINE_ITEMS_MISSING =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.lineItems.missing";
  public static final String ERROR_MOVEMENT_DRAFT_ORDERABLE_MISSING =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.orderable.missing";
  public static final String ERROR_MOVEMENT_DRAFT_ID_SHOULD_NULL =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.draft.id.should.null";
  public static final String ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.quantity.more.than.stock.on.hand";
  public static final String ERROR_MOVEMENT_QUANTITY_LESS_THAN_STOCK_ON_HAND =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".movement.quantity.less.than.stock.on.hand";

  //convert to order
  public static final String ERROR_REQUISITION_EXPIRED = ERROR_PREFIX + ".requisition.expired";
  public static final String ERROR_PERIOD_NOT_FOUND = ERROR_PREFIX + ".period.not.found";

  //location
  public static final String ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND =
      ERROR_PREFIX + ".locations.by.facility.not.found";

  public static final String ERROR_ADJUSTMENT_LOCATION_IS_RESTRICTED =
      PRODUCT_LOCATION_MOVEMENT_ERROR_PREFIX + ".positive.adjustment.has.restricted.location";
  // submit
  public static final String ERROR_SUBMIT_START_DATE_BEFORE_START_DATE = ERROR_PREFIX
      + ".submitStartDate.before.startDate";
  public static final String ERROR_SUBMIT_END_DATE_BEFORE_SUBMIT_START_DATE = ERROR_PREFIX
      + ".submitEndDate.before.submitStartDate";
  public static final String ERROR_SUBMIT_START_DATE_IS_BEFORE_LAST_SUBMIT_END_DATE = ERROR_PREFIX
      + ".submitStartDate.before.last.submitEndDate";

  //local issue voucher
  public static final String ERROR_ORDER_CODE_EXISTS =
      LOCAL_ISSUE_VOUCHER_PREFIX + ".order.code.already.exists";
  public static final String ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID =
      LOCAL_ISSUE_VOUCHER_PREFIX + ".local.issue.voucher.id.invalid";
  public static final String ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN =
      LOCAL_ISSUE_VOUCHER_PREFIX + ".subDrafts.more.than.ten";
  public static final String ERROR_ID_NOT_MATCH_SUB_DRAFT_ID =
      LOCAL_ISSUE_VOUCHER_PREFIX + ".local.issue.voucher.id.not.match.subdraft.id";
  public static final String ERROR_NO_LOCAL_ISSUE_VOUCHER_SUB_DRAFT_FOUND = LOCAL_ISSUE_VOUCHER_PREFIX
      + ".no.local.issue.voucher.sub.draft.found";
  public static final String ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFT_EMPTY =
      LOCAL_ISSUE_VOUCHER_PREFIX + ".subDrafts.empty";
  public static final String ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFT_NOT_ALL_SUBMITTED =
      LOCAL_ISSUE_VOUCHER_PREFIX + ".subDrafts.not.all.submitted";

  // localmachine
  public static final String ERROR_INVALID_ACTIVATION_CODE = "localmachine.activationCode.invalid";
  public static final String ERROR_ACTIVATION_CODE_USED_ALREADY = "localmachine.activationCode.usedAlready";
  public static final String ERROR_FACILITY_NOT_FOUND = "localmachine.facility.notFound";
  public static final String ERROR_NOT_ACTIVATED_YET = "localmachine.notActivated";
  public static final String ERROR_FACILITY_CHANGED = "localmachine.facilityChanged";

  // android
  public static final String ERROR_NOT_WEB_USER = "siglusapi.error.notWebUser";

  // proof of delivery
  public static final String ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND = POD_ERROR_PREFIX
      + ".no.pod.or.pod.line.item.found";
  public static final String ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH = POD_ERROR_PREFIX
      + ".pod.id.and.draft.id.not.match";
  public static final String ERROR_NO_POD_SUB_DRAFT_FOUND = POD_ERROR_PREFIX
      + ".no.pod.sub.draft.found";
  public static final String ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED = POD_ERROR_PREFIX
      + ".cannot.operate.when.sub.draft.submitted";
  public static final String ERROR_NOT_ALL_SUB_DRAFTS_SUBMITTED = POD_ERROR_PREFIX
      + ".not.all.sub.drafts.submitted";
  public static final String ERROR_SUB_DRAFTS_ALREADY_EXISTED = POD_ERROR_PREFIX
      + ".sub.drafts.already.existed";

  // period
  public static final String ERROR_NO_PERIOD_MATCH = PERIOD_ERROR_PREFIX + ".no.period.match";

  // common
  public static final String ERROR_SIZE_NULL = ERROR_PREFIX + ".pageable.size.null";
  public static final String ERROR_SIZE_NOT_POSITIVE = ERROR_PREFIX + ".pageable.size.notPositive";
  public static final String ERROR_VALIDATION_FAIL = "siglusapi.error.validationFail";
  public static final String ERROR_SPLIT_NUM_TOO_LARGE = ERROR_PREFIX + ".draft.number.greater.than.preset.products";

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

  // s3
  public static final String ERROR_FILE_NOT_FOUND = ERROR_PREFIX + ".fileNotFound";

  protected MessageKeys() {
    throw new UnsupportedOperationException();
  }

  protected static String join(String... params) {
    return String.join(DELIMITER, Arrays.asList(params));
  }
}
