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

import static org.openlmis.referencedata.domain.Code.code;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.openlmis.referencedata.domain.Code;


@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PeriodConstants {

  public static final Code MONTH_SCHEDULE_CODE = code("M1");

  public static final Code REPORT_MONTH_SCHEDULE_CODE = code("M2");
}
