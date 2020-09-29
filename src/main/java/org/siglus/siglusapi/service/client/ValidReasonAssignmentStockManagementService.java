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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.siglusapi.constant.FieldConstants;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ValidReasonAssignmentStockManagementService extends
    BaseStockManagementService<ValidReasonAssignmentDto> {

  @Override
  protected String getUrl() {
    return "/api/validReasons/";
  }

  @Override
  protected Class<ValidReasonAssignmentDto> getResultClass() {
    return ValidReasonAssignmentDto.class;
  }

  @Override
  protected Class<ValidReasonAssignmentDto[]> getArrayResultClass() {
    return ValidReasonAssignmentDto[].class;
  }

  public Collection<ValidReasonAssignmentDto> getValidReasons(UUID programId, UUID facilityType,
      String reasonType, UUID reason) {
    Map<String, Object> params = new HashMap<>();
    params.put(FieldConstants.PROGRAM, programId);
    params.put(FieldConstants.FACILITY_TYPE, facilityType);
    params.put(FieldConstants.REASON_TYPE, reasonType);
    params.put(FieldConstants.REASON, reason);
    return findAll("", params);
  }

  @Async
  public ValidReasonAssignmentDto assignReason(ValidReasonAssignmentDto dto) {
    return postResult("", dto, getResultClass());
  }
}
