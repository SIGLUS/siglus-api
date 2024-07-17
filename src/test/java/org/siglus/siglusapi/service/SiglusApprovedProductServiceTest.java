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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.joda.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.service.RequisitionService;
import org.powermock.api.mockito.PowerMockito;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.dto.ProgramProductDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.repository.SiglusProgramOrderableRepository;
import org.siglus.siglusapi.util.SupportedProgramsHelper;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@RunWith(MockitoJUnitRunner.class)
public class SiglusApprovedProductServiceTest {

  @InjectMocks
  private SiglusApprovedProductService service;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Mock
  private SupportedProgramsHelper programsHelper;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private SiglusProgramOrderableRepository siglusProgramOrderableRepository;

  private final UUID supportProgramId1 = UUID.randomUUID();
  private final UUID supportProgramId2 = UUID.randomUUID();

  @Before
  public void setup() {
    PowerMockito.when(programsHelper.findHomeFacilitySupportedPrograms())
        .thenReturn(getSupportedPrograms());
  }

  @Test
  public void shouldUpdateUnitWhenGetApprovedProductsGivenOrderableHasExtension() {
    UUID facilityId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    List<ApprovedProductDto> productDtos = newArrayList(buildApprovedProductDto(orderableId, "Unit_1"));
    when(requisitionService.getApprovedProducts(facilityId, supportProgramId1)).thenReturn(productDtos);
    List<ProgramOrderablesExtension> extensions =
        newArrayList(buildProgramOrderablesExtension(orderableId, "Unit_2"));
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any())).thenReturn(extensions);

    List<ApprovedProductDto> result = service.getApprovedProducts(facilityId, supportProgramId1);

    assertEquals(1, result.size());
    assertEquals("Unit_2", result.get(0).getOrderable().getDispensable().getDisplayUnit());
  }

  @Test
  public void shouldNotUpdateUnitWhenGetApprovedProductsGivenOrderableHasNotExtension() {
    UUID facilityId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    List<ApprovedProductDto> productDtos = newArrayList(buildApprovedProductDto(orderableId, "Unit_1"));
    when(requisitionService.getApprovedProducts(facilityId, supportProgramId1)).thenReturn(productDtos);
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any())).thenReturn(newArrayList());

    List<ApprovedProductDto> result = service.getApprovedProducts(facilityId, supportProgramId1);

    assertEquals(1, result.size());
    assertEquals("Unit_1", result.get(0).getOrderable().getDispensable().getDisplayUnit());
  }

  @Test
  public void shouldReturnEmptyWhenGetApprovedProductsGivenNoProducts() {
    UUID facilityId = UUID.randomUUID();
    when(requisitionService.getApprovedProducts(facilityId, supportProgramId1)).thenReturn(newArrayList());

    List<ApprovedProductDto> result = service.getApprovedProducts(facilityId, supportProgramId1);

    assertEquals(0, result.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenGetApprovedProductsGivenProgramIdIsWrong() {
    UUID facilityId = UUID.randomUUID();
    service.getApprovedProducts(facilityId, UUID.randomUUID());
  }

  @Test
  public void shouldGetAllSupportProgramsWhenGetApprovedProductsGivenProgramIdIs0() {
    UUID facilityId = UUID.randomUUID();
    when(requisitionService.getApprovedProducts(any(), any())).thenReturn(newArrayList());
    UUID programId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    service.getApprovedProducts(facilityId, programId);

    verify(requisitionService, times(1)).getApprovedProducts(facilityId, supportProgramId1);
    verify(requisitionService, times(1)).getApprovedProducts(facilityId, supportProgramId2);
  }

  @Test
  public void shouldGetApprovedProductsForFacility() {
    UUID facilityId = UUID.randomUUID();
    when(archiveProductService.searchArchivedProductsByFacilityId(facilityId)).thenReturn(emptySet());
    UUID orderableId = UUID.randomUUID();
    ProgramOrderable programOrderable = buildProgramOrderable(orderableId, "Unit_1");
    when(siglusProgramOrderableRepository.findMaxVersionOrderableByProgramIds(any()))
        .thenReturn(Collections.singletonList(programOrderable));
    ProgramOrderablesExtension programOrderablesExtension = buildProgramOrderablesExtension(orderableId, "Unit_2");
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any()))
        .thenReturn(Collections.singletonList(programOrderablesExtension));

    List<ProgramProductDto> approvedProductsForFacility =
        service.getApprovedProductsForFacility(facilityId, supportProgramId1, false);

    assertEquals(1, approvedProductsForFacility.size());
  }

  private ApprovedProductDto buildApprovedProductDto(UUID orderableId, String unit) {
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setOrderable(buildOrderableDto(orderableId, unit));
    return approvedProductDto;
  }

  private OrderableDto buildOrderableDto(UUID id, String unit) {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(id);
    DispensableDto dispensable = new DispensableDto(unit, unit);
    orderableDto.setDispensable(dispensable);
    return orderableDto;
  }

  private ProgramOrderable buildProgramOrderable(UUID orderableId, String unit) {
    ProgramOrderable programOrderable = new ProgramOrderable();
    programOrderable.setId(UUID.randomUUID());
    Program program = new Program(supportProgramId1);
    program.setActive(true);
    program.setCode(new Code("VC"));
    program.setName("Via Clássica");
    programOrderable.setProgram(program);
    Orderable orderable = new Orderable(new Code("product code"),
        null, 1, 3, true, orderableId, 1L);
    programOrderable.setProduct(orderable);
    OrderableDisplayCategory category = OrderableDisplayCategory.createNew(new Code("category"));
    return ProgramOrderable.createNew(program, category, orderable, CurrencyUnit.USD);
  }

  private ProgramOrderablesExtension buildProgramOrderablesExtension(UUID orderableId, String unit) {
    ProgramOrderablesExtension extension = new ProgramOrderablesExtension();
    extension.setOrderableId(orderableId);
    extension.setUnit(unit);
    return extension;
  }

  private List<SupportedProgramDto> getSupportedPrograms() {
    SupportedProgramDto supportedProgram1 = SupportedProgramDto.builder()
        .id(supportProgramId1)
        .code("VC")
        .name("Via Clássica")
        .description("description")
        .programActive(true)
        .supportActive(true)
        .supportLocallyFulfilled(true)
        .supportStartDate(LocalDate.now())
        .build();
    SupportedProgramDto supportedProgram2 = SupportedProgramDto.builder()
        .id(supportProgramId2)
        .code("T")
        .name("TARV")
        .description("description")
        .programActive(true)
        .supportActive(true)
        .supportLocallyFulfilled(true)
        .supportStartDate(LocalDate.now())
        .build();
    return asList(supportedProgram1, supportedProgram2);
  }
}
