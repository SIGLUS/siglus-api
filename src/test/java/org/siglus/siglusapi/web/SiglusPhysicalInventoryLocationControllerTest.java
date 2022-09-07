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
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftDto;
import org.siglus.siglusapi.dto.enums.LocationManagementOption;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.service.SiglusPhysicalInventorySubDraftService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusPhysicalInventoryLocationControllerTest {

  @InjectMocks
  private SiglusPhysicalInventoryLocationController controller;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private SiglusPhysicalInventorySubDraftService siglusPhysicalInventorySubDraftService;

  private final UUID programId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final Boolean isDraft = true;

  private final Boolean isByLocation = true;

  private final UUID subDraftId = UUID.randomUUID();

  private final String optionString = LocationManagementOption.BY_LOCATION.toString();

  @Test
  public void shouldCallGetForAllProductsWhenSearchIfProgramIsAllProducts() {
    // when
    controller.searchPhysicalInventories(ALL_PRODUCTS_PROGRAM_ID, facilityId,
        isDraft, isByLocation);
    // then
    verify(siglusPhysicalInventoryService).getPhysicalInventoryDtosForAllProducts(facilityId,
        isDraft, isByLocation);
  }

  @Test
  public void shouldCallGetPhysicalInventoryDtosWhenSearchIfProgramIsNotAllProducts() {
    // when
    controller.searchPhysicalInventories(programId, facilityId, isDraft, isByLocation);
    // then
    verify(siglusPhysicalInventoryService)
        .getPhysicalInventoryDtosForProductsInOneProgram(programId, facilityId, isDraft, isByLocation);
  }


  @Test
  public void shouldCallCreateWithLocationOptionForAllProductsWhenCreateIfProgramIsAllProducts() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    // when
    controller.createEmptyPhysicalInventoryWithLocationOption(
        physicalInventoryDto, 2, true, optionString, isByLocation);
    // then
    verify(siglusPhysicalInventoryService).createAndSplitNewDraftForAllProduct(
        physicalInventoryDto, 2, true, optionString, isByLocation);
  }

  @Test
  public void shouldCallCreateNewDraftWhenCreateIfProgramIsNotAllProducts() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().programId(programId)
        .build();
    // when
    controller.createEmptyPhysicalInventoryWithLocationOption(
        physicalInventoryDto, 2, false, optionString, isByLocation);
    // then
    verify(siglusPhysicalInventoryService).createAndSpiltNewDraftForOneProgram(
        physicalInventoryDto, 2, optionString, isByLocation);
  }

  @Test
  public void shouldCallUpdateSubDraftsBySubDraftIdsWhenUpdateSubDrafts() {
    // given
    PhysicalInventorySubDraftDto physicalInventoryDto = new PhysicalInventorySubDraftDto();
    // when
    controller.submitSubDrafts(physicalInventoryDto, isByLocation);
    // then
    verify(siglusPhysicalInventorySubDraftService).updateSubDrafts(null, physicalInventoryDto,
        PhysicalInventorySubDraftEnum.SUBMITTED, isByLocation);
  }

  @Test
  public void shouldCallSubmitSubDraftsBySubDraftIdsWhenSubmitSubDrafts() {
    // given
    PhysicalInventorySubDraftDto physicalInventoryDto = new PhysicalInventorySubDraftDto();
    // when
    controller.updateSubDrafts(physicalInventoryDto, isByLocation);
    // then
    verify(siglusPhysicalInventorySubDraftService).updateSubDrafts(null, physicalInventoryDto,
        PhysicalInventorySubDraftEnum.DRAFT, isByLocation);
  }

  @Test
  public void shouldCallGetSubDraftListForAllProductWhenSearchAllProductSubDraftList() {
    // when
    controller.searchSubDraftList(ALL_PRODUCTS_PROGRAM_ID, facilityId, isDraft);
    // then
    verify(siglusPhysicalInventoryService).getSubDraftListForAllProduct(facilityId, isDraft);
  }

  @Test
  public void shouldCallGetSubDraftListInOneProgramWhenSearchOneProgramSubDraftList() {
    // when
    controller.searchSubDraftList(programId, facilityId, isDraft);
    // then
    verify(siglusPhysicalInventoryService).getSubDraftListForOneProgram(programId, facilityId, isDraft);
  }

  @Test
  public void shouldCallGetSubPhysicalInventoryDtoBysubDraftIdWhenSearchSubDraftDetail() {
    // when
    controller.searchSubDraftList(programId, facilityId, isDraft);
    // then
    verify(siglusPhysicalInventoryService).getSubDraftListForOneProgram(programId, facilityId, isDraft);
  }

  @Test
  public void shouldCallDeleteSubDraftsBySubDraftIdsWhenDeleteSubDrafts() {
    // when
    controller.deleteSubDrafts(Boolean.FALSE, Lists.newArrayList(subDraftId), isByLocation);
    // then
    verify(siglusPhysicalInventorySubDraftService).deleteSubDrafts(
        Lists.newArrayList(subDraftId), Boolean.FALSE, isByLocation);
  }


}
