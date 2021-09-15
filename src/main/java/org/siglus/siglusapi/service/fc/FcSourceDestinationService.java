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

import static org.siglus.siglusapi.constant.FacilityTypeConstants.AC;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.AI;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.CS;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DDM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DPM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HC;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HD;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HG;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HP;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HPSIQ;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.HR;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.OUTROS;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.PS;
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;

import com.google.common.collect.Lists;
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
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.common.util.RequestParameters;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings({"PMD.TooManyMethods"})
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

  private UUID arvProgramId;

  private UUID mpProgramId;

  private UUID rapidTestProgramId;

  private Map<String, UUID> facilityTypeCodeToIdMap;

  public void createSourceAndDestination(List<FacilityDto> facilities) {
    fetchAndCacheMasterData();
    facilities.forEach(facility -> {
      Node node = new Node();
      node.setRefDataFacility(true);
      node.setReferenceId(facility.getId());
      node = nodeRepository.save(node);
      String code = facility.getType().getCode();
      List<String> csAndPsInCountryLevel = Lists.newArrayList(CS, PS);
      List<String> hgAndHpAndHrAndHdAndOutRosAndHpSiqAndHmInCountryLevel =
          Lists.newArrayList(HG, HP, HR, HD, OUTROS, HPSIQ, HM);
      List<String> dpmAndAiInProvinceLevel = Lists.newArrayList(DPM, AI);
      if (csAndPsInCountryLevel.contains(code)) {
        createForHfOrPsInCountryLevel(node);
      } else if (hgAndHpAndHrAndHdAndOutRosAndHpSiqAndHmInCountryLevel.contains(code)) {
        createForHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevel(node);
      } else if (code.equalsIgnoreCase(DDM)) {
        createForDdmInDistrictLevel(node);
      } else if (dpmAndAiInProvinceLevel.contains(code)) {
        createForDpmOrAiInProvinceLevel(node, code);
      } else if (code.equals(HC)) {
        createForHcInSpecificProvinceLevel(node);
      } else if (code.equalsIgnoreCase(AC)) {
        createForWarehouse(node);
      }
    });
  }

  private void createForHfOrPsInCountryLevel(Node node) {
    UUID ddmFacilityTypeId = facilityTypeCodeToIdMap.get(DDM);
    UUID dpmFacilityTypeId = facilityTypeCodeToIdMap.get(DPM);
    assignDestination(node, ddmFacilityTypeId, mpProgramId);
    assignDestination(node, dpmFacilityTypeId, rapidTestProgramId);
    assignDestination(node, dpmFacilityTypeId, arvProgramId);
    assignAiTypeDestinationForAllProgram(node);
  }

  private void createForHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevel(Node node) {
    UUID dpmFacilityTypeId = facilityTypeCodeToIdMap.get(DPM);
    assignDestinationForAllProgram(node, dpmFacilityTypeId);
    assignAiTypeDestinationForAllProgram(node);
  }

  private void createForDdmInDistrictLevel(Node node) {
    assignDestination(node, facilityTypeCodeToIdMap.get(DPM), mpProgramId);
    assignSource(node, facilityTypeCodeToIdMap.get(CS), mpProgramId);
    assignSource(node, facilityTypeCodeToIdMap.get(PS), mpProgramId);
  }

  private void createForDpmOrAiInProvinceLevel(Node node, String code) {
    assignWarehouseTypeDestinationForAllProgram(node);
    UUID csFacilityTypeId = facilityTypeCodeToIdMap.get(CS);
    UUID psFacilityTypeId = facilityTypeCodeToIdMap.get(PS);
    UUID ddmFacilityTypeId = facilityTypeCodeToIdMap.get(DDM);
    assignSource(node, csFacilityTypeId, rapidTestProgramId);
    assignSource(node, csFacilityTypeId, arvProgramId);
    assignSource(node, psFacilityTypeId, rapidTestProgramId);
    assignSource(node, psFacilityTypeId, arvProgramId);
    if (DPM.equalsIgnoreCase(code)) {
      assignSource(node, ddmFacilityTypeId, mpProgramId);
    } else if (AI.equalsIgnoreCase(code)) {
      assignSource(node, csFacilityTypeId, mpProgramId);
      assignSource(node, psFacilityTypeId, mpProgramId);
    }
    assignHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevelType(node);
  }

  private void createForHcInSpecificProvinceLevel(Node node) {
    assignWarehouseTypeDestinationForAllProgram(node);
  }

  private void createForWarehouse(Node node) {
    UUID dpmFacilityTypeId = facilityTypeCodeToIdMap.get(DPM);
    UUID aiFacilityTypeId = facilityTypeCodeToIdMap.get(AI);
    UUID hcFacilityTypeId = facilityTypeCodeToIdMap.get(HC);
    assignSourceForAllProgram(node, dpmFacilityTypeId);
    assignSourceForAllProgram(node, aiFacilityTypeId);
    assignSourceForAllProgram(node, hcFacilityTypeId);
  }

  private void assignAiTypeDestinationForAllProgram(Node node) {
    UUID aiFacilityTypeId = facilityTypeCodeToIdMap.get(AI);
    assignDestinationForAllProgram(node, aiFacilityTypeId);
  }

  private void assignWarehouseTypeDestinationForAllProgram(Node node) {
    UUID warehouseFacilityTypeId = facilityTypeCodeToIdMap.get(AC);
    assignDestinationForAllProgram(node, warehouseFacilityTypeId);
  }

  private void assignDestinationForAllProgram(Node node, UUID facilityTypeId) {
    assignDestination(node, facilityTypeId, mpProgramId);
    assignDestination(node, facilityTypeId, rapidTestProgramId);
    assignDestination(node, facilityTypeId, arvProgramId);
  }

  private void assignHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevelType(Node node) {
    UUID hgFacilityTypeId = facilityTypeCodeToIdMap.get(HG);
    assignSourceForAllProgram(node, hgFacilityTypeId);
    UUID hpFacilityTypeId = facilityTypeCodeToIdMap.get(HP);
    assignSourceForAllProgram(node, hpFacilityTypeId);
    UUID hrFacilityTypeId = facilityTypeCodeToIdMap.get(HR);
    assignSourceForAllProgram(node, hrFacilityTypeId);
    UUID hdFacilityTypeId = facilityTypeCodeToIdMap.get(HD);
    assignSourceForAllProgram(node, hdFacilityTypeId);
    UUID outRosFacilityTypeId = facilityTypeCodeToIdMap.get(OUTROS);
    assignSourceForAllProgram(node, outRosFacilityTypeId);
    UUID hpSiqFacilityTypeId = facilityTypeCodeToIdMap.get(HPSIQ);
    assignSourceForAllProgram(node, hpSiqFacilityTypeId);
    UUID hmFacilityTypeId = facilityTypeCodeToIdMap.get(HM);
    assignSourceForAllProgram(node, hmFacilityTypeId);
  }

  private void assignSourceForAllProgram(Node node, UUID facilityTypeId) {
    assignSource(node, facilityTypeId, mpProgramId);
    assignSource(node, facilityTypeId, rapidTestProgramId);
    assignSource(node, facilityTypeId, arvProgramId);
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
    facilityTypeCodeToIdMap = facilityTypes.stream()
        .collect(Collectors.toMap(FacilityTypeDto::getCode, FacilityTypeDto::getId));
    Map<String, UUID> programCodeToIdMap = programReferenceDataService.findAll().stream()
        .collect(Collectors.toMap(BasicProgramDto::getCode, BaseDto::getId));
    mpProgramId = programCodeToIdMap.get(ProgramConstants.VIA_PROGRAM_CODE);
    rapidTestProgramId = programCodeToIdMap.get(ProgramConstants.RAPIDTEST_PROGRAM_CODE);
    arvProgramId = programCodeToIdMap.get(ProgramConstants.TARV_PROGRAM_CODE);
  }
}
