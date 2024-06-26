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

package org.siglus.siglusapi.constant;

import java.util.UUID;

public class FieldConstants {

  private FieldConstants() {
  }

  public static final String PROGRAM = "program";
  public static final String PROGRAM_ID = "programId";
  public static final String PRODUCT_CODE = "productCode";
  public static final String FULL_PRODUCT_NAME = "fullProductName";
  public static final String FACILITY_ID = "facilityId";
  public static final String FACILITY_TYPE = "facilityType";
  public static final String ORDERABLE_ID = "orderableId";
  public static final String REASON_TYPE = "reasonType";
  public static final String REASON = "reason";
  public static final String CODE = "code";
  public static final String NAME = "name";
  public static final String ID = "id";
  public static final String EXPIRATION_DATE = "expirationDate";
  public static final String LOT_CODE = "lotCode";
  public static final String TRADE_ITEM = "tradeItem";
  public static final String RIGHT_NAME = "rightName";
  public static final String IS_BASIC = "isBasic";
  public static final String IS_TRACER = "isTracer";
  public static final String VM_STATUS = "vmStatus";
  public static final String STOCK_CARD_ID = "stockCardId";
  public static final String EXCLUDE_ARCHIVED = "excludeArchived";
  public static final String SITE = "SITE";
  public static final String DISTRICT = "DISTRICT";
  public static final String PROVINCE = "PROVINCE";
  public static final String NON_EMPTY_ONLY = "nonEmptyOnly";
  public static final String CONSUMED = "consumed";
  public static final String ACTIVE = "active";
  public static final String TOTAL = "total";
  public static final String DELETE = "delete";
  public static final String RECEIVE = "receive";
  public static final String RECEIVE_WITH_LOCATION = "receiveWithLocation";
  public static final String ISSUE = "issue";
  public static final String ISSUE_WITH_LOCATION = "issueWithLocation";
  public static final String CAPITAL_RECEIVE = "RECEIVE";
  public static final String CAPITAL_ISSUE = "ISSUE";
  public static final String CAPITAL_ADJUSTMENT = "ADJUSTMENT";
  public static final String ADJUSTMENT = "adjustment";
  public static final String INVENTORY = "INVENTORY";
  public static final String PHYSICAL_INVENTORY = "PHYSICAL_INVENTORY";
  public static final String ZONEID = "zoneId";
  public static final String SINGLE_PROGRAM = "SINGLE";
  public static final String ALL_PROGRAM = "ALL";
  public static final String EXCEL_CONTENT_TYPE = "application/vnd.ms-excel";
  public static final String TRACER_DRUG_INFORMATION = "Informações_sobre_medicamento_rastreadores_";
  public static final String SUBTITLE = "legenda";
  public static final String EMPTY_VALUE = "N/A";
  public static final String ATTACHMENT_FILENAME = "attachment;filename=";
  public static final String XLSX_SUFFIX = ".xlsx";
  public static final String UTF_8 = "UTF-8";
  public static final String STOCK_OUT_PORTUGUESE = "Roptura de stock";
  public static final String LOW_STOCK_PORTUGUESE = "Eminência de roptura";
  public static final String REGULAR_STOCK_PORTUGUESE = "Stock regular";
  public static final String OVER_STOCK_PORTUGUESE = "Stock acumulado";
  public static final String DRUG_CODE_PORTUGUESE = "Código do produto";
  public static final String PROGRAM_PORTUGUESE = "Programa";
  public static final String DRUG_NAME_PORTUGUESE = "Nome do medicamento";
  public static final String PROVINCE_PORTUGUESE = "Província";
  public static final String DISTRICT_PORTUGUESE = "Distrito";
  public static final String FACILITY_PORTUGUESE = "Instalação";
  public static final String CMM = "CMM";
  public static final int BASIC_ROW = 1;
  public static final int BASIC_COLUMN = 8;
  public static final int RED_MARK = 1;
  public static final int YELLOW_MARK = 2;
  public static final int GREEN_MARK = 3;
  public static final int PURPLE_MARK = 4;
  public static final String REPORT_GENERATED_FOR_PORTUGUESE = "Relatório gerado para";
  public static final String ALL_GEOGRAPHIC_ZONE = "0000-0000";
  public static final String PRODUCT = "product";
  public static final String LOCATION = "location";
  public static final String SEPARATOR = "&";
  public static final String DOT = ".";
  public static final String UNDERSCORE = "_";
  public static final String ORDER_ID = "orderId";
  public static final int MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_DAY_RANGE = 90;
  public static final int QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_DAY_RANGE = 180;
  public static final String METABASE_PARAM_TEMPLATE = "\"%s\": \"%s\"";
  public static final String METABASE_PAYLOAD_TEMPLATE = "{\"resource\": {\"dashboard\": %d},\"params\": {%s}}";
  public static final String METABASE_PART_URL = "/embed/dashboard/";
  public static final String METABASE_EXTENSION_URL =
      "#bordered=false&titled=true"
          + "&hide_parameters=facility_code,district_facility_code,province_facility_code,district";
  public static final String DISTRICT_LOWER_CASE = "district";
  public static final String JWT_TOKEN_HEADER_PARAM_NAME = "typ";
  public static final String JWT_TOKEN_HEADER_PARAM_VALUE = "JWT";
  public static final String UNPACK_KIT_TYPE = "UNPACK_KIT";
  public static final String UNPACK_KIT_REASON = "Unpack Kit";
  public static final String ISSUE_TYPE = "ISSUE";
  public static final String ADJUSTMENT_KEY = "stockConstants.adjustment";
  public static final String PHYSICAL_INVENTORY_KEY = "stockCard.physicalInventory";
  public static final String INITIAL_INVENTORY_KEY = "stockCard.inventory";
  public static final UUID LAST_SYNC_RECORD_ID = UUID.fromString("07997013-b33b-4159-bdf4-585182fb82fb");
  public static final String VALUE = "value";
  public static final UUID ALL_GEOGRAPHIC_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  public static final UUID REQUISITION_DRAFT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
}
