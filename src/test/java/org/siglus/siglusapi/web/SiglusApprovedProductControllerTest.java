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

package org.siglus.siglusapi.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.siglus.siglusapi.service.SiglusApprovedProductService;
import org.siglus.siglusapi.web.response.ApprovedProductResponse;

@RunWith(MockitoJUnitRunner.class)
public class SiglusApprovedProductControllerTest {

  @InjectMocks
  private SiglusApprovedProductController controller;

  @Mock
  private SiglusApprovedProductService service;

  @Test
  public void shouldGetApprovedProducts() {
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    controller.approvedProductDtos(facilityId, programId);

    verify(service).getApprovedProducts(facilityId, programId);
  }

  @Test
  public void shouldGetApprovedProductsBrif() {
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    ApprovedProductDto approvedProductDto = buildProduct();
    when(service.getApprovedProducts(facilityId, programId)).thenReturn(Collections.singletonList(approvedProductDto));

    List<ApprovedProductResponse> approvedProductResponses = controller.approvedProductResponse(facilityId, programId);

    assertEquals(1, approvedProductResponses.size());
    assertEquals(approvedProductDto.getId(), approvedProductResponses.get(0).getId());
  }

  private ApprovedProductDto buildProduct() {
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    MetadataDto metadataDto = new MetadataDto();
    metadataDto.setVersionNumber(1L);
    productDto.setMeta(metadataDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(UUID.randomUUID());
    programDto.setCode("VC");
    programDto.setActive(true);
    programDto.setName("via");
    productDto.setProgram(programDto);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(UUID.randomUUID());
    orderableDto.setFullProductName("full product name");
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("default");
    orderableDto.setPrograms(Collections.singleton(programOrderableDto));
    productDto.setOrderable(orderableDto);
    return productDto;
  }
}
