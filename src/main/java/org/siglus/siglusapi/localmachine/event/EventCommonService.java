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

package org.siglus.siglusapi.localmachine.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventCommonService {
  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final OrderableReferenceDataService orderableReferenceDataService;

  public UUID getReceiverId(UUID facilityId, UUID programId) {
    List<RequisitionGroupMembersDto> parentFacility =
        requisitionGroupMembersRepository.findParentFacilityByRequisitionGroup(
            facilityId, Collections.singleton(programId));
    if (CollectionUtils.isEmpty(parentFacility)) {
      throw new IllegalStateException(String.format("can't find event's receiver id, facilityId = %s, programId = %s",
          facilityId, programId));
    }
    return parentFacility.get(0).getFacilityId();
  }

  public String getGroupId(UUID requisitionId) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    return requisitionExtension.getRequisitionNumberPrefix() + requisitionExtension.getRequisitionNumber();
  }

  public Map<VersionIdentityDto, OrderableDto> getOrderableDtoMap(Requisition requisition) {
    if (CollectionUtils.isEmpty(requisition.getRequisitionLineItems())) {
      return new HashMap<>();
    }
    Set<VersionEntityReference> orderables = requisition.getRequisitionLineItems().stream()
        .map(RequisitionLineItem::getOrderable).collect(Collectors.toSet());
    return orderableReferenceDataService.findByIdentities(orderables).stream()
        .collect(Collectors.toMap(BasicOrderableDto::getIdentity, Function.identity()));
  }
}
