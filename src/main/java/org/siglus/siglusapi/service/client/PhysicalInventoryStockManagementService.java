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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.constant.FieldConstants;
import org.springframework.stereotype.Service;

@Service
public class PhysicalInventoryStockManagementService
    extends BaseStockManagementService<PhysicalInventoryDto> {

  @Override
  protected String getUrl() {
    return "/api/physicalInventories/";
  }

  @Override
  protected Class<PhysicalInventoryDto> getResultClass() {
    return PhysicalInventoryDto.class;
  }

  @Override
  protected Class<PhysicalInventoryDto[]> getArrayResultClass() {
    return PhysicalInventoryDto[].class;
  }

  public List<PhysicalInventoryDto> searchPhysicalInventory(UUID programId, UUID facilityId,
      Boolean isDraft) {
    Map<String, Object> params = new HashMap<>();
    params.put(FieldConstants.PROGRAM, programId);
    params.put(FieldConstants.FACILITY, facilityId);
    params.put(FieldConstants.IS_DRAFT, isDraft);
    return new ArrayList<>(findAll("", params, Boolean.TRUE));
  }

  public PhysicalInventoryDto getPhysicalInventory(UUID id) {
    return findOne(id, Boolean.TRUE);
  }

  public PhysicalInventoryDto createEmptyPhysicalInventory(PhysicalInventoryDto dto) {
    return postResult("", dto, getResultClass(), Boolean.TRUE);
  }

  public void savePhysicalInventory(UUID id, PhysicalInventoryDto dto) {
    put(id.toString(), dto, Void.class, Boolean.TRUE);
  }

  public void deletePhysicalInventory(UUID id) {
    delete(id.toString(), Boolean.TRUE);
  }
}
