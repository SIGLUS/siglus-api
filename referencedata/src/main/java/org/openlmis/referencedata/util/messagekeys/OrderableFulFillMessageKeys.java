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

public abstract class OrderableFulFillMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, ORDERABLE_FULFILL);

  public static final String ERROR_INVALID_PARAMS = join(ERROR, SEARCH, INVALID_PARAMS);
  public static final String ERROR_PROVIDED_FACILITY_ID_WITHOUT_PROGRAM_ID =
      join(ERROR, SEARCH, "providedFacilityIdWithoutProgramId");
  public static final String ERROR_PROVIDED_PROGRAM_ID_WITHOUT_FACILITY_ID =
      join(ERROR, SEARCH, "providedProgramIdWithoutFacilityId");
  public static final String ERROR_IDS_CANNOT_BY_PROVIDED_TOGETHER_WITH_FACILITY_ID_AND_PROGRAM_ID =
      join(ERROR, SEARCH, "idsCannotBeProvidedTogetherWithFacilityIdAndProgramId");
}
