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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusApprovedProductServiceTest {

  @InjectMocks
  private SiglusApprovedProductService service;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Test
  public void shouldUpdateUnitWhenGetApprovedProductsGivenOrderableHasExtension() {
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    List<ApprovedProductDto> productDtos = newArrayList(buildApprovedProductDto(orderableId, "Unit_1"));
    when(requisitionService.getApprovedProducts(facilityId, programId)).thenReturn(productDtos);
    List<ProgramOrderablesExtension> extensions =
        newArrayList(buildProgramOrderablesExtension(orderableId, "Unit_2"));
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any())).thenReturn(extensions);

    List<ApprovedProductDto> result = service.getApprovedProducts(facilityId, programId);

    assertEquals(1, result.size());
    assertEquals("Unit_2", result.get(0).getOrderable().getDispensable().getDisplayUnit());
  }

  @Test
  public void shouldNotUpdateUnitWhenGetApprovedProductsGivenOrderableHasNotExtension() {
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID orderableId = UUID.randomUUID();
    List<ApprovedProductDto> productDtos = newArrayList(buildApprovedProductDto(orderableId, "Unit_1"));
    when(requisitionService.getApprovedProducts(facilityId, programId)).thenReturn(productDtos);
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any())).thenReturn(newArrayList());

    List<ApprovedProductDto> result = service.getApprovedProducts(facilityId, programId);

    assertEquals(1, result.size());
    assertEquals("Unit_1", result.get(0).getOrderable().getDispensable().getDisplayUnit());
  }

  @Test
  public void shouldReturnEmptyWhenGetApprovedProductsGivenNoProducts() {
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    when(requisitionService.getApprovedProducts(facilityId, programId)).thenReturn(newArrayList());

    List<ApprovedProductDto> result = service.getApprovedProducts(facilityId, programId);

    assertEquals(0, result.size());
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

  private ProgramOrderablesExtension buildProgramOrderablesExtension(UUID orderableId, String unit) {
    ProgramOrderablesExtension extension = new ProgramOrderablesExtension();
    extension.setOrderableId(orderableId);
    extension.setUnit(unit);
    return extension;
  }
}
