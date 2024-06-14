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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class SiglusApprovedProductService {

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  public List<ApprovedProductDto> getApprovedProducts(UUID facilityId, UUID programId) {
    List<ApprovedProductDto> approvedProducts = requisitionService.getApprovedProducts(facilityId, programId);
    Set<UUID> orderableIds = approvedProducts.stream()
        .map(product -> product.getOrderable().getId()).collect(Collectors.toSet());
    if (!ObjectUtils.isEmpty(orderableIds)) {
      Map<UUID, ProgramOrderablesExtension> extensionMap =
          programOrderablesExtensionRepository.findAllByOrderableIdIn(orderableIds)
              .stream().collect(Collectors.toMap(ProgramOrderablesExtension::getOrderableId, item -> item));
      approvedProducts.forEach(
          product -> {
            ProgramOrderablesExtension extension = extensionMap.get(product.getOrderable().getId());
            if (!ObjectUtils.isEmpty(extension)) {
              product.getOrderable().setDispensable(new DispensableDto(extension.getUnit(), extension.getUnit()));
            }
          }
      );
    }
    return approvedProducts;
  }
}
