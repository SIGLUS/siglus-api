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

package org.siglus.siglusapi.web.withoutlocation;

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
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.service.SiglusPhysicalInventorySubDraftService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusPhysicalInventoryWithoutLocationControllerTest {

  @InjectMocks
  private SiglusPhysicalInventoryWithoutLocationController controller;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private SiglusPhysicalInventorySubDraftService siglusPhysicalInventorySubDraftService;

  private final UUID programId = UUID.randomUUID();

  private final UUID subDraftId = UUID.randomUUID();

  @Test
  public void shouldCallCreateWithLocationOptionForAllProductsWhenCreateIfProgramIsAllProducts()
          throws InterruptedException {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
            .programId(ALL_PRODUCTS_PROGRAM_ID).build();

    controller.createEmptyPhysicalInventory(physicalInventoryDto, 2, true);

    verify(siglusPhysicalInventoryService).createAndSplitNewDraftForAllPrograms(
            physicalInventoryDto, 2, true, null, false);
  }

  @Test
  public void shouldCallCreateForAllProductsWhenCreateIfProgramIsAllProducts() throws InterruptedException {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();

    controller.createEmptyPhysicalInventory(physicalInventoryDto, 2, true);

    verify(siglusPhysicalInventoryService).createAndSplitNewDraftForAllPrograms(
        physicalInventoryDto, 2, true, null, false);
  }

  @Test
  public void shouldCallCreateNewDraftWhenCreateIfProgramIsNotAllProducts() {
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().programId(programId).build();

    controller.createEmptyPhysicalInventory(physicalInventoryDto, 2, false);

    verify(siglusPhysicalInventoryService).createAndSpiltNewDraftForOneProgram(physicalInventoryDto, 2, null, false);
  }

  @Test
  public void shouldCallDeleteSubDraftsBySubDraftIdsWhenDeleteSubDrafts() {
    controller.deleteSubDrafts(Boolean.FALSE, Lists.newArrayList(subDraftId));

    verify(siglusPhysicalInventorySubDraftService).deleteSubDrafts(
        Lists.newArrayList(subDraftId), Boolean.FALSE, false);
  }

  @Test
  public void shouldCallUpdateSubDraftsBySubDraftIdsWhenUpdateSubDrafts() {
    PhysicalInventorySubDraftDto physicalInventoryDto = new PhysicalInventorySubDraftDto();

    controller.submitSubDrafts(physicalInventoryDto);

    verify(siglusPhysicalInventorySubDraftService).updateSubDrafts(null, physicalInventoryDto,
        PhysicalInventorySubDraftEnum.SUBMITTED, false);
  }

  @Test
  public void shouldCallSubmitSubDraftsBySubDraftIdsWhenSubmitSubDrafts() {
    PhysicalInventorySubDraftDto physicalInventoryDto = new PhysicalInventorySubDraftDto();

    controller.updateSubDrafts(physicalInventoryDto);

    verify(siglusPhysicalInventorySubDraftService).updateSubDrafts(null, physicalInventoryDto,
        PhysicalInventorySubDraftEnum.DRAFT, false);
  }

}
