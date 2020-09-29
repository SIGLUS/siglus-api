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

package org.siglus.siglusapi.service.fc;

import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.domain.sourcedestination.SourceDestinationAssignment;
import org.openlmis.stockmanagement.repository.NodeRepository;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.util.RequestParameters;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FcSourceDestinationService {

  @Autowired
  private NodeRepository nodeRepository;

  @Autowired
  private SiglusFacilityTypeReferenceDataService facilityTypeReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;

  @Value("${hf.facilityTypeCode}")
  private String hfFacilityTypeCode;

  private UUID hfFacilityTypeId;

  @Value("${ddm.facilityTypeCode}")
  private String ddmFacilityTypeCode;

  private UUID ddmFacilityTypeId;

  @Value("${dpm.facilityTypeCode}")
  private String dpmFacilityTypeCode;

  private UUID dpmFacilityTypeId;

  @Value("${warehouse.facilityTypeCode}")
  private String warehouseFacilityTypeCode;

  private UUID warehouseFacilityTypeId;

  @Value("${arv.programCode}")
  private String arvProgramCode;

  private UUID arvProgramId;

  @Value("${mp.programCode}")
  private String mpProgramCode;

  private UUID mpProgramId;

  @Value("${rapidTest.programCode}")
  private String rapidTestProgramCode;

  private UUID rapidTestProgramId;

  public void createSourceAndDestination(List<FacilityDto> facilities) {
    fetchAndCacheMasterData();
    facilities.forEach(facility -> {
      Node node = new Node();
      node.setRefDataFacility(true);
      node.setReferenceId(facility.getId());
      node = nodeRepository.save(node);
      UUID facilityTypeId = facility.getType().getId();
      if (facilityTypeId.equals(hfFacilityTypeId)) {
        createForHf(node);
      } else if (facilityTypeId.equals(ddmFacilityTypeId)) {
        createForDdm(node);
      } else if (facilityTypeId.equals(dpmFacilityTypeId)) {
        createForDpm(node);
      } else if (facilityTypeId.equals(warehouseFacilityTypeId)) {
        createForWarehouse(node);
      }
    });
  }

  private void createForHf(Node node) {
    assignDestination(node, ddmFacilityTypeId, mpProgramId);
    assignDestination(node, dpmFacilityTypeId, rapidTestProgramId);
    assignDestination(node, dpmFacilityTypeId, arvProgramId);
  }

  private void createForDdm(Node node) {
    assignDestination(node, dpmFacilityTypeId, mpProgramId);
    assignSource(node, hfFacilityTypeId, mpProgramId);
  }

  private void createForDpm(Node node) {
    assignDestination(node, warehouseFacilityTypeId, mpProgramId);
    assignDestination(node, warehouseFacilityTypeId, rapidTestProgramId);
    assignDestination(node, warehouseFacilityTypeId, arvProgramId);
    assignSource(node, hfFacilityTypeId, rapidTestProgramId);
    assignSource(node, hfFacilityTypeId, arvProgramId);
    assignSource(node, ddmFacilityTypeId, mpProgramId);
  }

  private void createForWarehouse(Node node) {
    assignSource(node, dpmFacilityTypeId, mpProgramId);
    assignSource(node, dpmFacilityTypeId, rapidTestProgramId);
    assignSource(node, dpmFacilityTypeId, arvProgramId);
  }

  private void assignDestination(Node node, UUID facilityTypeId, UUID programId) {
    SourceDestinationAssignment assignment = new SourceDestinationAssignment();
    assignment.setNode(node);
    assignment.setFacilityTypeId(facilityTypeId);
    assignment.setProgramId(programId);
    validSourceDestinationStockManagementService.assignDestination(assignment);
  }

  private void assignSource(Node node, UUID facilityTypeId, UUID programId) {
    SourceDestinationAssignment assignment = new SourceDestinationAssignment();
    assignment.setNode(node);
    assignment.setFacilityTypeId(facilityTypeId);
    assignment.setProgramId(programId);
    validSourceDestinationStockManagementService.assignSource(assignment);
  }

  private void fetchAndCacheMasterData() {
    RequestParameters parameters = RequestParameters.init().set(ACTIVE, true);
    List<FacilityTypeDto> facilityTypes = facilityTypeReferenceDataService.getPage(parameters)
        .getContent();
    Map<String, UUID> facilityTypeCodeToIdMap = facilityTypes.stream()
        .collect(Collectors.toMap(FacilityTypeDto::getCode, FacilityTypeDto::getId));
    hfFacilityTypeId = facilityTypeCodeToIdMap.get(hfFacilityTypeCode);
    ddmFacilityTypeId = facilityTypeCodeToIdMap.get(ddmFacilityTypeCode);
    dpmFacilityTypeId = facilityTypeCodeToIdMap.get(dpmFacilityTypeCode);
    warehouseFacilityTypeId = facilityTypeCodeToIdMap.get(warehouseFacilityTypeCode);
    Map<String, UUID> programCodeToIdMap = programReferenceDataService.findAll().stream()
        .collect(Collectors.toMap(BasicProgramDto::getCode, BaseDto::getId));
    mpProgramId = programCodeToIdMap.get(mpProgramCode);
    rapidTestProgramId = programCodeToIdMap.get(rapidTestProgramCode);
    arvProgramId = programCodeToIdMap.get(arvProgramCode);
  }
}
