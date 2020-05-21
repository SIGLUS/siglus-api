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

package org.openlmis.requisition.web;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequestParameters;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyLineReferenceDataService;
import org.openlmis.requisition.testutils.FacilityDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.openlmis.requisition.testutils.SupplyLineDtoDataBuilder;
import org.openlmis.requisition.utils.Pagination;
import org.springframework.data.domain.PageRequest;

@SuppressWarnings("PMD.UnusedPrivateField")
public class RequisitionForConvertBuilderTest {

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private BasicRequisitionDtoBuilder basicRequisitionDtoBuilder;

  @Mock
  private SupplyLineReferenceDataService supplyLineReferenceDataService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks
  private RequisitionForConvertBuilder requisitionForConvertBuilder =
      new RequisitionForConvertBuilder();

  private FacilityDto facility1;
  private FacilityDto facility2;
  private Requisition requisition1;
  private Requisition requisition2;
  private Requisition requisition3;
  private SupplyLineDto supplyLine1;
  private SupplyLineDto supplyLine2;
  private SupplyLineDto supplyLine3;
  private SupplyLineDto supplyLine4;
  private RequisitionDto requisitionDto;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    final FacilityDto facility = new FacilityDtoDataBuilder().buildAsDto();
    facility1 = new FacilityDtoDataBuilder().buildAsDto();
    facility2 = new FacilityDtoDataBuilder().buildAsDto();

    SupervisoryNodeDto supervisoryNode1 = new SupervisoryNodeDto();
    supervisoryNode1.setId(UUID.randomUUID());
    SupervisoryNodeDto supervisoryNode2 = new SupervisoryNodeDto();
    supervisoryNode2.setId(UUID.randomUUID());

    ProgramDto program1 = new ProgramDtoDataBuilder().buildAsDto();
    ProgramDto program2 = new ProgramDtoDataBuilder().buildAsDto();

    requisition1 = new RequisitionDataBuilder()
        .withProgramId(program1.getId())
        .withFacilityId(facility.getId())
        .withSupervisoryNodeId(supervisoryNode1.getId())
        .build();
    requisition2 = new RequisitionDataBuilder()
        .withProgramId(program1.getId())
        .withFacilityId(facility.getId())
        .withSupervisoryNodeId(supervisoryNode2.getId())
        .build();
    requisition3 = new RequisitionDataBuilder()
        .withProgramId(program2.getId())
        .withFacilityId(facility.getId())
        .withSupervisoryNodeId(supervisoryNode2.getId())
        .build();

    supplyLine1 = new SupplyLineDtoDataBuilder()
        .withProgram(program1)
        .withSupervisoryNode(supervisoryNode1)
        .withSupplyingFacility(facility1)
        .buildAsDto();
    supplyLine2 = new SupplyLineDtoDataBuilder()
        .withProgram(program1)
        .withSupervisoryNode(supervisoryNode2)
        .withSupplyingFacility(facility2)
        .buildAsDto();
    supplyLine3 = new SupplyLineDtoDataBuilder()
        .withProgram(program2)
        .withSupervisoryNode(supervisoryNode2)
        .withSupplyingFacility(facility2)
        .buildAsDto();
    supplyLine4 = new SupplyLineDtoDataBuilder()
        .withProgram(program1)
        .withSupervisoryNode(supervisoryNode1)
        .withSupplyingFacility(facility2)
        .buildAsDto();

    requisitionDto = new RequisitionDataBuilder()
            .withId(requisition1.getId())
            .withProgramId(program1.getId())
            .withSupervisoryNodeId(supervisoryNode1.getId())
            .buildAsDto();
    requisitionDto.setFacility(facility1);

    when(facilityReferenceDataService.search(
        asSet(facility.getId(), facility1.getId(), facility2.getId())))
        .thenReturn(asList(facility, facility1, facility2));
    when(programReferenceDataService.search(asSet(program1.getId())))
        .thenReturn(singletonList(program1));
    when(supplyLineReferenceDataService.getPage(any(RequestParameters.class)))
        .thenReturn(Pagination.getPage(asList(supplyLine1, supplyLine2, supplyLine3),
            new PageRequest(0, Integer.MAX_VALUE), 2));
  }

  @Test
  public void shouldBuildRequisitionsWithDepotsRespectingUserAccessibleDepots() {
    //when
    List<RequisitionWithSupplyingDepotsDto> result = requisitionForConvertBuilder.buildRequisitions(
        asList(requisition1, requisition2, requisition3),
        asSet(facility1.getId(), facility2.getId()),
        asList(supplyLine1, supplyLine2, supplyLine3));

    //then
    //we have 2 requisition representations
    assertEquals(3, result.size());

    //first requisition has 2 supplying depots where user has rights
    assertEquals(facility1, result.get(0).getSupplyingDepots().get(0));

    //second requisition has 1 supplying depot where user has right
    assertEquals(facility2, result.get(1).getSupplyingDepots().get(0));

    verifyNoMoreInteractions(supplyLineReferenceDataService);
  }

  @Test
  public void shouldSearchForSupplyingFacilitiesIfNullValueWasPassed() {
    requisitionForConvertBuilder.buildRequisitions(asList(requisition1, requisition2, requisition3),
        asSet(facility1.getId(), facility2.getId()), null);

    verify(supplyLineReferenceDataService).getPage(any(RequestParameters.class));
  }

  @Test
  public void shouldGetAvailableSupplyingDepots() {
    when(requisitionRepository.findOne(requisitionDto.getId()))
            .thenReturn(requisition1);
    when(supplyLineReferenceDataService
            .search(requisition1.getProgramId(), requisition1.getSupervisoryNodeId()))
            .thenReturn(asList(supplyLine1, supplyLine4));

    List<FacilityDto> result = requisitionForConvertBuilder
            .getAvailableSupplyingDepots(requisitionDto.getId());

    assertEquals(2, result.size());
    assertEquals(requisitionDto.getFacilityId(), result.get(0).getId());
  }
}
