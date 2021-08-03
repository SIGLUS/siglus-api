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
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.util.RequestParameters;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${cs.facilityTypeCode}")
  private String csFacilityTypeCode;

  @Value("${ps.facilityTypeCode}")
  private String psFacilityTypeCode;

  @Value("${hg.facilityTypeCode}")
  private String hgFacilityTypeCode;

  @Value("${hp.facilityTypeCode}")
  private String hpFacilityTypeCode;

  @Value("${hr.facilityTypeCode}")
  private String hrFacilityTypeCode;

  @Value("${hd.facilityTypeCode}")
  private String hdFacilityTypeCode;

  @Value("${outros.facilityTypeCode}")
  private String outrosFacilityTypeCode;

  @Value("${hpsiq.facilityTypeCode}")
  private String hpsiqFacilityTypeCode;

  @Value("${hm.facilityTypeCode}")
  private String hmFacilityTypeCode;

  @Value("${ddm.facilityTypeCode}")
  private String ddmFacilityTypeCode;

  @Value("${dpm.facilityTypeCode}")
  private String dpmFacilityTypeCode;

  @Value("${hc.facilityTypeCode}")
  private String hcFacilityTypeCode;

  @Value("${ai.facilityTypeCode}")
  private String aiFacilityTypeCode;

  @Value("${warehouse.facilityTypeCode}")
  private String warehouseFacilityTypeCode;

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
      List<String> csAndPsInCountryLevel = Lists.newArrayList(csFacilityTypeCode,
          psFacilityTypeCode);
      List<String> hgAndHpAndHrAndHdAndOutRosAndHpSiqAndHmInCountryLevel =
          Lists.newArrayList(hgFacilityTypeCode, hpFacilityTypeCode, hrFacilityTypeCode,
              hdFacilityTypeCode, outrosFacilityTypeCode, hpsiqFacilityTypeCode,
              hmFacilityTypeCode);
      List<String> dpmAndAiInProvinceLevel = Lists.newArrayList(dpmFacilityTypeCode,
          aiFacilityTypeCode);
      if (csAndPsInCountryLevel.contains(code)) {
        createForHfOrPsInCountryLevel(node);
      } else if (hgAndHpAndHrAndHdAndOutRosAndHpSiqAndHmInCountryLevel.contains(code)) {
        createForHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevel(node);
      } else if (code.equalsIgnoreCase(ddmFacilityTypeCode)) {
        createForDdmInDistrictLevel(node);
      } else if (dpmAndAiInProvinceLevel.contains(code)) {
        createForDpmOrAiInProvinceLevel(node, code);
      } else if (code.equals(hcFacilityTypeCode)) {
        createForHcInSpecificProvinceLevel(node);
      } else if (code.equalsIgnoreCase(warehouseFacilityTypeCode)) {
        createForWarehouse(node);
      }
    });
  }

  private void createForHfOrPsInCountryLevel(Node node) {
    UUID ddmFacilityTypeId = facilityTypeCodeToIdMap.get(ddmFacilityTypeCode);
    UUID dpmFacilityTypeId = facilityTypeCodeToIdMap.get(dpmFacilityTypeCode);
    assignDestination(node, ddmFacilityTypeId, mpProgramId);
    assignDestination(node, dpmFacilityTypeId, rapidTestProgramId);
    assignDestination(node, dpmFacilityTypeId, arvProgramId);
    assignAiTypeDestinationForAllProgram(node);
  }

  private void createForHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevel(Node node) {
    UUID dpmFacilityTypeId = facilityTypeCodeToIdMap.get(dpmFacilityTypeCode);
    assignDestinationForAllProgram(node, dpmFacilityTypeId);
    assignAiTypeDestinationForAllProgram(node);
  }

  private void createForDdmInDistrictLevel(Node node) {
    assignDestination(node, facilityTypeCodeToIdMap.get(dpmFacilityTypeCode), mpProgramId);
    assignSource(node, facilityTypeCodeToIdMap.get(csFacilityTypeCode), mpProgramId);
    assignSource(node, facilityTypeCodeToIdMap.get(psFacilityTypeCode), mpProgramId);
  }

  private void createForDpmOrAiInProvinceLevel(Node node, String code) {
    assignWarehouseTypeDestinationForAllProgram(node);
    UUID csFacilityTypeId = facilityTypeCodeToIdMap.get(csFacilityTypeCode);
    UUID psFacilityTypeId = facilityTypeCodeToIdMap.get(psFacilityTypeCode);
    UUID ddmFacilityTypeId = facilityTypeCodeToIdMap.get(ddmFacilityTypeCode);
    assignSource(node, csFacilityTypeId, rapidTestProgramId);
    assignSource(node, csFacilityTypeId, arvProgramId);
    assignSource(node, psFacilityTypeId, rapidTestProgramId);
    assignSource(node, psFacilityTypeId, arvProgramId);
    if (dpmFacilityTypeCode.equalsIgnoreCase(code)) {
      assignSource(node, ddmFacilityTypeId, mpProgramId);
    } else if (aiFacilityTypeCode.equalsIgnoreCase(code)) {
      assignSource(node, csFacilityTypeId, mpProgramId);
      assignSource(node, psFacilityTypeId, mpProgramId);
    }
    assignHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevelType(node);
  }

  private void createForHcInSpecificProvinceLevel(Node node) {
    assignWarehouseTypeDestinationForAllProgram(node);
  }

  private void createForWarehouse(Node node) {
    UUID dpmFacilityTypeId = facilityTypeCodeToIdMap.get(dpmFacilityTypeCode);
    UUID aiFacilityTypeId = facilityTypeCodeToIdMap.get(aiFacilityTypeCode);
    UUID hcFacilityTypeId = facilityTypeCodeToIdMap.get(hcFacilityTypeCode);
    assignSourceForAllProgram(node, dpmFacilityTypeId);
    assignSourceForAllProgram(node, aiFacilityTypeId);
    assignSourceForAllProgram(node, hcFacilityTypeId);
  }

  private void assignAiTypeDestinationForAllProgram(Node node) {
    UUID aiFacilityTypeId = facilityTypeCodeToIdMap.get(aiFacilityTypeCode);
    assignDestinationForAllProgram(node, aiFacilityTypeId);
  }

  private void assignWarehouseTypeDestinationForAllProgram(Node node) {
    UUID warehouseFacilityTypeId = facilityTypeCodeToIdMap.get(warehouseFacilityTypeCode);
    assignDestinationForAllProgram(node, warehouseFacilityTypeId);
  }

  private void assignDestinationForAllProgram(Node node, UUID facilityTypeId) {
    assignDestination(node, facilityTypeId, mpProgramId);
    assignDestination(node, facilityTypeId, rapidTestProgramId);
    assignDestination(node, facilityTypeId, arvProgramId);
  }

  private void assignHgAndHpAndHrAndHdAndOutrosAndHpsiqAndHmInCountryLevelType(Node node) {
    UUID hgFacilityTypeId = facilityTypeCodeToIdMap.get(hgFacilityTypeCode);
    assignSourceForAllProgram(node, hgFacilityTypeId);
    UUID hpFacilityTypeId = facilityTypeCodeToIdMap.get(hpFacilityTypeCode);
    assignSourceForAllProgram(node, hpFacilityTypeId);
    UUID hrFacilityTypeId = facilityTypeCodeToIdMap.get(hrFacilityTypeCode);
    assignSourceForAllProgram(node, hrFacilityTypeId);
    UUID hdFacilityTypeId = facilityTypeCodeToIdMap.get(hdFacilityTypeCode);
    assignSourceForAllProgram(node, hdFacilityTypeId);
    UUID outRosFacilityTypeId = facilityTypeCodeToIdMap.get(outrosFacilityTypeCode);
    assignSourceForAllProgram(node, outRosFacilityTypeId);
    UUID hpSiqFacilityTypeId = facilityTypeCodeToIdMap.get(hpsiqFacilityTypeCode);
    assignSourceForAllProgram(node, hpSiqFacilityTypeId);
    UUID hmFacilityTypeId = facilityTypeCodeToIdMap.get(hmFacilityTypeCode);
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
    mpProgramId = programCodeToIdMap.get(ProgramConstants.VIA_PROGRAM_NAME);
    rapidTestProgramId = programCodeToIdMap.get(ProgramConstants.RAPIDTEST_PROGRAM_NAME);
    arvProgramId = programCodeToIdMap.get(ProgramConstants.MMIA_PROGRAM_NAME);
  }
}
