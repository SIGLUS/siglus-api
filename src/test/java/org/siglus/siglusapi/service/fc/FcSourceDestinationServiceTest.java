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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.AI;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.CS;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DDM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DPM;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.repository.NodeRepository;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class FcSourceDestinationServiceTest {

  @InjectMocks
  private FcSourceDestinationService fcSourceDestinationService;

  @Mock
  private NodeRepository nodeRepository;

  @Mock
  private SiglusFacilityTypeReferenceDataService facilityTypeReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;

  private final Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

  private final UUID hfFacilityTypeId = UUID.randomUUID();

  private final UUID ddmFacilityTypeId = UUID.randomUUID();

  private final UUID dpmFacilityTypeId = UUID.randomUUID();

  private final UUID warehouseFacilityTypeId = UUID.randomUUID();

  private final String arvProgramCode = "T";

  private final UUID arvProgramId = UUID.randomUUID();

  private final String mpProgramCode = "VC";

  private final UUID mpProgramId = UUID.randomUUID();

  private final String rapidTestProgramCode = "TR";

  private final UUID rapidTestProgramId = UUID.randomUUID();

  @Before
  public void prepare() {
    mockFacilityType();
    mockProgram();
  }

  @Test
  public void shouldCreateSourceAndDestinationForHfFacility() {
    // given
    List<FacilityDto> facilities = buildFacility(hfFacilityTypeId, "CS");

    // when
    fcSourceDestinationService.createSourceAndDestination(facilities);

    // then
    verify(nodeRepository).save(any(Node.class));
    verify(validSourceDestinationStockManagementService, times(6)).assignDestination(any());
  }

  @Test
  public void shouldCreateSourceAndDestinationForDdmFacility() {
    // given
    List<FacilityDto> facilities = buildFacility(ddmFacilityTypeId, "DDM");

    // when
    fcSourceDestinationService.createSourceAndDestination(facilities);

    // then
    verify(nodeRepository).save(any(Node.class));
    verify(validSourceDestinationStockManagementService).assignDestination(any());
    verify(validSourceDestinationStockManagementService, times(2)).assignSource(any());
  }

  @Test
  public void shouldCreateSourceAndDestinationForDpmFacility() {
    // given
    List<FacilityDto> facilities = buildFacility(dpmFacilityTypeId, "DPM");

    // when
    fcSourceDestinationService.createSourceAndDestination(facilities);

    // then
    verify(nodeRepository).save(any(Node.class));
    verify(validSourceDestinationStockManagementService, times(3)).assignDestination(any());
    verify(validSourceDestinationStockManagementService, times(26)).assignSource(any());
  }

  @Test
  public void shouldCreateSourceAndDestinationForWhFacility() {
    // given
    List<FacilityDto> facilities = buildFacility(warehouseFacilityTypeId, "AC");

    // when
    fcSourceDestinationService.createSourceAndDestination(facilities);

    // then
    verify(nodeRepository).save(any(Node.class));
    verify(validSourceDestinationStockManagementService, times(9)).assignSource(any());
  }

  private void mockFacilityType() {
    FacilityTypeDto hfFacilityType = new FacilityTypeDto();
    hfFacilityType.setCode(CS);
    hfFacilityType.setId(hfFacilityTypeId);
    FacilityTypeDto ddmFacilityType = new FacilityTypeDto();
    ddmFacilityType.setCode(DDM);
    ddmFacilityType.setId(ddmFacilityTypeId);
    FacilityTypeDto dpmFacilityType = new FacilityTypeDto();
    dpmFacilityType.setCode(DPM);
    dpmFacilityType.setId(dpmFacilityTypeId);
    FacilityTypeDto whFacilityType = new FacilityTypeDto();
    whFacilityType.setCode(AI);
    whFacilityType.setId(warehouseFacilityTypeId);
    when(facilityTypeReferenceDataService.getPage(any())).thenReturn(
        Pagination.getPage(newArrayList(hfFacilityType, ddmFacilityType, dpmFacilityType,
            whFacilityType), pageable, 4));
  }

  private void mockProgram() {
    ProgramDto arvProgram = new ProgramDto();
    arvProgram.setCode(arvProgramCode);
    arvProgram.setId(arvProgramId);
    ProgramDto mpProgram = new ProgramDto();
    mpProgram.setCode(mpProgramCode);
    mpProgram.setId(mpProgramId);
    ProgramDto rapidTestProgram = new ProgramDto();
    rapidTestProgram.setCode(rapidTestProgramCode);
    rapidTestProgram.setId(rapidTestProgramId);
    when(programReferenceDataService.findAll()).thenReturn(newArrayList(arvProgram, mpProgram, rapidTestProgram));
  }

  private List<FacilityDto> buildFacility(UUID facilityTypeId, String typeCode) {
    FacilityTypeDto facilityType = new FacilityTypeDto();
    facilityType.setId(facilityTypeId);
    facilityType.setCode(typeCode);
    FacilityDto facility = new FacilityDto();
    facility.setType(facilityType);
    return newArrayList(facility);
  }

}
