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

package org.siglus.siglusapi.service.client;

import java.util.UUID;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.springframework.stereotype.Service;

@Service
public class RequisitionTemplateRequisitionService
    extends BaseRequisitionService<RequisitionTemplateDto> {

  @Override
  protected String getUrl() {
    return "/api/requisitionTemplates/";
  }

  @Override
  protected Class<RequisitionTemplateDto> getResultClass() {
    return RequisitionTemplateDto.class;
  }

  @Override
  protected Class<RequisitionTemplateDto[]> getArrayResultClass() {
    return RequisitionTemplateDto[].class;
  }

  @Override
  protected String getServiceName() {
    return null;
  }

  public RequisitionTemplateDto findTemplate(UUID id) {
    return findOne(id);
  }

}
