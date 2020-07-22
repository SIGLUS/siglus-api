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

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusRequisitionExtensionService {

  @Autowired
  private RequisitionExtensionRepository requisitionExtensionRepository;

  public RequisitionExtension createRequisitionExtension(UUID requisitionId, Boolean emergency,
      String facilityCode) {
    RequisitionExtension requisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionId)
        .requisitionNumberPrefix(Boolean.TRUE.equals(emergency) ? "EM" : "NO" + facilityCode)
        .build();
    log.info("save requisition extension by requisition id: {}", requisitionId);
    return requisitionExtensionRepository.save(requisitionExtension);
  }

  public String getRequisitionNumber(RequisitionExtension requisitionExtension) {
    if (null == requisitionExtension) {
      return null;
    }
    return String.format("%s%07d", requisitionExtension.getRequisitionNumberPrefix(),
        requisitionExtension.getRequisitionNumber());
  }

  public String getRequisitionNumber(UUID requisitionId) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository
        .findByRequisitionId(requisitionId);
    return getRequisitionNumber(requisitionExtension);
  }

  public void deleteRequisitionExtension(UUID requisitionId) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository
        .findByRequisitionId(requisitionId);
    log.info("delete requisition extension by requisition id: {}", requisitionId);
    requisitionExtensionRepository.delete(requisitionExtension);
  }

}
