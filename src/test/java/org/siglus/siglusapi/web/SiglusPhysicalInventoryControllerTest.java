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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusPhysicalInventoryControllerTest {

  @InjectMocks
  private SiglusPhysicalInventoryController controller;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  private final UUID programId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final Boolean isDraft = true;

  @Test
  public void shouldCallGetForAllProductsWhenSearchIfProgramIsAllProducts() {
    controller.searchPhysicalInventories(ALL_PRODUCTS_PROGRAM_ID, facilityId, isDraft);

    verify(siglusPhysicalInventoryService).getPhysicalInventoryBriefDtos(facilityId, ALL_PRODUCTS_PROGRAM_ID, isDraft);
  }

  @Test
  public void shouldCallGetPhysicalInventoryDtosWhenSearchIfProgramIsNotAllProducts() {
    controller.searchPhysicalInventories(programId, facilityId, isDraft);

    verify(siglusPhysicalInventoryService).getPhysicalInventoryBriefDtos(facilityId, programId, isDraft);
  }

  @Test
  public void shouldCallGetForAllProductsWhenSearchByIdIfIdIsAllProducts() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    controller.searchPhysicalInventory(ALL_PRODUCTS_UUID);

    verify(siglusPhysicalInventoryService).getPhysicalInventoryForAllPrograms(facilityId);
  }

  @Test
  public void shouldCallGetPhysicalInventoryWhenSearchByIdIfIdIsNotAllProducts() {
    controller.searchPhysicalInventory(id);

    verify(siglusPhysicalInventoryService).getPhysicalInventory(id);
  }

  @Test
  public void shouldCallSaveDraftForAllProductsWhenUpdateIfIdIsAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(ALL_PRODUCTS_UUID)
        .facilityId(facilityId)
        .build();

    controller.updatePhysicalInventory(ALL_PRODUCTS_UUID, physicalInventoryDto);

    verify(siglusPhysicalInventoryService).checkDraftIsExist(physicalInventoryDto.getFacilityId());
    verify(siglusPhysicalInventoryService).saveDraftForAllPrograms(physicalInventoryDto);
  }

  @Test
  public void shouldCallSaveDraftWhenUpdateIfIdIsNotAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(id).build();

    controller.updatePhysicalInventory(id, physicalInventoryDto);

    verify(siglusPhysicalInventoryService).saveDraftForProductsForOneProgram(physicalInventoryDto);
  }

  @Test
  public void shouldCallDeleteForAllProductsWhenDeleteIfIdIsAllProducts() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    controller.deletePhysicalInventory(ALL_PRODUCTS_UUID);

    verify(siglusPhysicalInventoryService).deletePhysicalInventoryDraftForAllPrograms(facilityId);
  }

  @Test
  public void shouldCallDeletePhysicalInventoryWhenDeleteIfIdIsNotAllProducts() {
    controller.deletePhysicalInventory(id);

    verify(siglusPhysicalInventoryService).deletePhysicalInventoryDraft(id);
  }

  @Test
  public void shouldCallFindPhysicalInventoryDatesWhenSearchPhysicalInventoryDates() {
    String startDate = "startDate";
    String endDate = "endDate";
    controller.searchPhysicalInventoryDates(programId, facilityId, startDate, endDate);

    verify(siglusPhysicalInventoryService).findPhysicalInventoryDates(programId, facilityId, startDate,
        endDate);
  }

  @Test
  public void shouldCallFindLatestPhysicalInventoryWhenSearchLatestPhysicalInventoryOccurDate() {
    controller.searchLatestPhysicalInventoryOccurDate(facilityId, programId);

    verify(siglusPhysicalInventoryService).findLatestPhysicalInventory(facilityId, programId);
  }

  @Test
  public void shouldConflictIfOtherProgramHaveDraft() {
    controller.checkPhysicalInventoryConflict(ALL_PRODUCTS_PROGRAM_ID, facilityId, null);

    verify(siglusPhysicalInventoryService).checkConflictForAllPrograms(facilityId, null);
  }

  @Test
  public void shouldConflictWithAllProductsWhenAllProductsHaveDraft() {
    controller.checkPhysicalInventoryConflict(programId, facilityId, null);

    verify(siglusPhysicalInventoryService).checkConflictForOneProgram(facilityId, programId, null);
  }

  @Test
  public void shouldCallGetSubDraftListForAllProductWhenSearchAllProductSubDraftList() {
    controller.searchSubDraftList(ALL_PRODUCTS_PROGRAM_ID, facilityId, isDraft);

    verify(siglusPhysicalInventoryService).getSubDraftListForAllPrograms(facilityId, isDraft);
  }

  @Test
  public void shouldCallGetSubDraftListInOneProgramWhenSearchOneProgramSubDraftList() {
    controller.searchSubDraftList(programId, facilityId, isDraft);

    verify(siglusPhysicalInventoryService).getSubDraftListForOneProgram(programId, facilityId, isDraft);
  }

  @Test
  public void shouldCallGetSubPhysicalInventoryDtoBysubDraftIdWhenSearchSubDraftDetail() {
    controller.searchSubDraftList(programId, facilityId, isDraft);

    verify(siglusPhysicalInventoryService).getSubDraftListForOneProgram(programId, facilityId, isDraft);
  }
}
