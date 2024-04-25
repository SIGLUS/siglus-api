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

package org.siglus.siglusapi.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ReportNameEnum {
  STOCK_STATUS("STOCK_STATUS"),
  SOH_BY_LOT("SOH_BY_LOT"),
  EXPIRING_PRODUCTS("EXPIRING_PRODUCTS"),
  EXPIRED_PRODUCTS("EXPIRED_PRODUCTS"),
  TRACER_DRUG("TRACER_DRUG"),
  HISTORICAL_DATA("HISTORICAL_DATA"),
  REQUISITIONS_MONTHLY("REQUISITIONS_MONTHLY"),
  REQUISITION_DATA("REQUISITION_DATA"),
  MMIA_REGIMENS("MMIA_REGIMENS"),
  MMTB_REGIMENS("MMTB_REGIMENS"),
  MALARIA_CONSUMPTION_DATA("MALARIA_CONSUMPTION_DATA"),
  RAPID_TEST_CONSUMPTION_DATA("RAPID_TEST_CONSUMPTION_DATA"),
  FULFILLMENT("FULFILLMENT"),
  SYSTEM_VERSION("SYSTEM_VERSION"),
  SYSTEM_UPDATE("SYSTEM_UPDATE"),
  USER("USER"),
  USER_ACCESS("USER_ACCESS"),
  EXPIRED_REMOVED_PRODUCTS("EXPIRED_REMOVED_PRODUCTS");

  private final String name;
}
