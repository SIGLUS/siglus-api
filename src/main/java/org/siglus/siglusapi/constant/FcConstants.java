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

import java.util.Arrays;
import java.util.List;

public class FcConstants {

  private FcConstants() { }

  public static final String CMM_JOB = "CMM";
  public static final String CP_JOB = "CP";
  public static final String RECEIPT_PLAN_JOB = "RECEIPT_PLAN";
  public static final String ISSUE_VOUCHER_JOB = "ISSUE_VOUCHER";
  public static final String PROGRAM_JOB = "PROGRAM";
  public static final String REGIMEN_JOB = "REGIMEN";

  public static final String CMM_API = "/cmm/cmms";
  public static final String CP_API = "/cp/cps";
  public static final String RECEIPT_PLAN_API = "/receiptPlan/receiptplans";
  public static final String ISSUE_VOUCHER_API = "/issueVoucher/issuevouchers";
  public static final String PROGRAM_API = "/area/areas";
  public static final String REGIMEN_API = "/areaRegime/arearegimes";

  public static final String PROGRAM_STATUS_ACTIVE = "Activo";
  public static final String PROGRAM_STATUS_INACTIVE = "Inactivo";
  public static final String DUMMY = "DUMMY";

  public static List<String> getQueryByDateApiList() {
    return Arrays.asList(RECEIPT_PLAN_API, ISSUE_VOUCHER_API, PROGRAM_API, REGIMEN_API);
  }

}
