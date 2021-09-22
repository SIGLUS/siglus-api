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
  private SiglusPhysicalInventoryController siglusPhysicalInventoryController;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  private final UUID programId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final Boolean isDraft = true;

  private final Boolean canInitialInventory = true;

  private final String startDate = "startDate";

  private final String endDate = "endDate";

  @Test
  public void shouldCallGetForAllProductsWhenSearchIfProgramIsAllProducts() {
    siglusPhysicalInventoryController.searchPhysicalInventories(ALL_PRODUCTS_PROGRAM_ID, facilityId,
        isDraft, canInitialInventory);

    verify(siglusPhysicalInventoryService).getPhysicalInventoryDtosForAllProducts(facilityId,
        isDraft, canInitialInventory);
  }

  @Test
  public void shouldCallGetPhysicalInventoryDtosWhenSearchIfProgramIsNotAllProducts() {
    siglusPhysicalInventoryController.searchPhysicalInventories(programId, facilityId, isDraft,
        canInitialInventory);

    verify(siglusPhysicalInventoryService).getPhysicalInventoryDtos(programId, facilityId, isDraft);
  }

  @Test
  public void shouldCallGetForAllProductsWhenSearchByIdIfIdIsAllProducts() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    siglusPhysicalInventoryController.searchPhysicalInventory(ALL_PRODUCTS_UUID);

    verify(siglusPhysicalInventoryService).getPhysicalInventoryForAllProducts(facilityId);
  }

  @Test
  public void shouldCallGetPhysicalInventoryWhenSearchByIdIfIdIsNotAllProducts() {
    siglusPhysicalInventoryController.searchPhysicalInventory(id);

    verify(siglusPhysicalInventoryService).getPhysicalInventory(id);
  }

  @Test
  public void shouldCallCreateForAllProductsWhenCreateIfProgramIsAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();

    siglusPhysicalInventoryController.createEmptyPhysicalInventory(physicalInventoryDto);

    verify(siglusPhysicalInventoryService).createNewDraftForAllProducts(physicalInventoryDto);
  }

  @Test
  public void shouldCallCreateNewDraftWhenCreateIfProgramIsNotAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().programId(programId)
        .build();

    siglusPhysicalInventoryController.createEmptyPhysicalInventory(physicalInventoryDto);

    verify(siglusPhysicalInventoryService).createNewDraft(physicalInventoryDto);
  }

  @Test
  public void shouldCallSaveDraftForAllProductsWhenUpdateIfIdIsAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(ALL_PRODUCTS_UUID)
        .facilityId(facilityId)
        .build();

    siglusPhysicalInventoryController.updatePhysicalInventory(ALL_PRODUCTS_UUID,
        physicalInventoryDto);

    verify(siglusPhysicalInventoryService).checkDraftIsExist(physicalInventoryDto.getFacilityId());
    verify(siglusPhysicalInventoryService).saveDraftForAllProducts(physicalInventoryDto);
  }

  @Test
  public void shouldCallSaveDraftWhenUpdateIfIdIsNotAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(id).build();

    siglusPhysicalInventoryController.updatePhysicalInventory(id, physicalInventoryDto);

    verify(siglusPhysicalInventoryService).saveDraft(physicalInventoryDto, id);
  }

  @Test
  public void shouldCallDeleteForAllProductsWhenDeleteIfIdIsAllProducts() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    siglusPhysicalInventoryController.deletePhysicalInventory(ALL_PRODUCTS_UUID);

    verify(siglusPhysicalInventoryService).deletePhysicalInventoryForAllProducts(facilityId);
  }

  @Test
  public void shouldCallDeletePhysicalInventoryWhenDeleteIfIdIsNotAllProducts() {
    siglusPhysicalInventoryController.deletePhysicalInventory(id);

    verify(siglusPhysicalInventoryService).deletePhysicalInventory(id);
  }

  @Test
  public void shouldCallFindPhysicalInventoryDatesWhenSearchPhysicalInventoryDates() {
    siglusPhysicalInventoryController.searchPhysicalInventoryDates(facilityId, startDate, endDate);

    verify(siglusPhysicalInventoryService).findPhysicalInventoryDates(facilityId, startDate,
        endDate);
  }

  @Test
  public void shouldCallFindLatestPhysicalInventoryWhenSearchLatestPhysicalInventoryOccurDate() {
    siglusPhysicalInventoryController.searchLatestPhysicalInventoryOccurDate(facilityId);

    verify(siglusPhysicalInventoryService).findLatestPhysicalInventory(facilityId);
  }
}
