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

package org.openlmis.requisition.service.referencedata;

import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.springframework.stereotype.Service;

@Service
public class RequisitionGroupReferenceDataService
    extends BaseReferenceDataService<RequisitionGroupDto> {

  @Override
  protected String getUrl() {
    return "/api/requisitionGroups/";
  }

  @Override
  protected Class<RequisitionGroupDto> getResultClass() {
    return RequisitionGroupDto.class;
  }

  @Override
  protected Class<RequisitionGroupDto[]> getArrayResultClass() {
    return RequisitionGroupDto[].class;
  }

}
