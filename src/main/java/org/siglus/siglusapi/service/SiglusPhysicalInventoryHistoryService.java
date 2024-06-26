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

package org.siglus.siglusapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.PhysicalInventoryHistory;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryHistoryDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.PhysicalInventoryHistoryRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class SiglusPhysicalInventoryHistoryService {

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  private PhysicalInventoryHistoryRepository physicalInventoryHistoryRepository;

  public List<SiglusPhysicalInventoryHistoryDto> searchPhysicalInventoryHistories() {
    UserDto currentUser = authenticationHelper.getCurrentUser();
    if (ObjectUtils.isEmpty(currentUser) || ObjectUtils.isEmpty(currentUser.getHomeFacilityId())) {
      return new ArrayList<>();
    }
    UUID facilityId = currentUser.getHomeFacilityId();
    return physicalInventoryHistoryRepository.queryPhysicalInventoryHistories(facilityId);
  }

  public String searchPhysicalInventoryHistoryData(UUID id) {
    UserDto currentUser = authenticationHelper.getCurrentUser();
    if (ObjectUtils.isEmpty(currentUser)
        || ObjectUtils.isEmpty(currentUser.getHomeFacilityId())
        || ObjectUtils.isEmpty(id)) {
      return "";
    }
    Optional<PhysicalInventoryHistory> historyOptional = physicalInventoryHistoryRepository.findById(id);
    return historyOptional.orElseGet(PhysicalInventoryHistory::new).getHistoryData();
  }
}
