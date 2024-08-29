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

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcConstants {

  public static final String CMM_API = "/cmm/cmms";
  public static final String CP_API = "/cp/cps";
  public static final String RECEIPT_PLAN_API = "/receiptPlan/receiptplans";
  public static final String ISSUE_VOUCHER_API = "/issueVoucher/issuevouchers";
  public static final String PROGRAM_API = "/area/areas";
  public static final String PRODUCT_API = "/product/products";
  public static final String REGIMEN_API = "/areaRegime/arearegimes";
  public static final String FACILITY_API = "/client/clients";
  public static final String FACILITY_TYPE_API = "/clientType/clientstypes";
  public static final String GEOGRAPHIC_ZONE_API = "/region/regions";

  public static final String STATUS_ACTIVE = "Activo";
  public static final String DEFAULT_REGIMEN_CATEGORY_NAME = "Default";

  public static final String DATE_FORMAT = "yyyyMMdd";
  public static final String MONTH_FORMAT = "MM-yyyy";

  public static final String REGION_API = "/regions";
  public static final String PROVINCE_API = "/provinces";
  public static final String DISTRICT_API = "/districts";

  public static List<String> getCmmAndCpApis() {
    return Arrays.asList(CMM_API, CP_API);
  }

  public static Set<String> getFcNewApis() {
    return Sets.newHashSet(REGION_API, PROVINCE_API, DISTRICT_API);
  }
}
