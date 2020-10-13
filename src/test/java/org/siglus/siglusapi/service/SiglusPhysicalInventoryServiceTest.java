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
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.common.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.service.StockmanagementPermissionService;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.service.client.PhysicalInventoryStockManagementService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusPhysicalInventoryServiceTest {

  @Captor
  private ArgumentCaptor<PhysicalInventoryDto> physicalInventoryDtoArgumentCaptor;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private PhysicalInventoryStockManagementService physicalInventoryStockManagementService;

  @Mock
  private SupportedProgramsHelper supportedVirtualProgramsHelper;

  @Mock
  private PhysicalInventoryService physicalInventoryService;

  @Mock
  private StockmanagementPermissionService permissionService;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Mock
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Mock
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  private UUID facilityId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID programIdOne = UUID.randomUUID();

  private UUID programIdTwo = UUID.randomUUID();

  private UUID inventoryOne = UUID.randomUUID();

  private UUID inventoryTwo = UUID.randomUUID();

  private UUID id = UUID.randomUUID();

  private String startDate = "startDate";

  private String endDate = "endDate";

  @Test
  public void shouldCallV3MultipleTimesWhenCreateNewDraftForAllProducts() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));
    when(physicalInventoryStockManagementService.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);

    // when
    siglusPhysicalInventoryService.createNewDraftForAllProducts(physicalInventoryDto);

    // then
    verify(physicalInventoryStockManagementService, times(2))
        .createEmptyPhysicalInventory(physicalInventoryDto);
  }

  @Test
  public void shouldCallV3MultipleTimesWhenSaveDraftForAllProducts() {
    // given
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .programId(programIdOne)
        .build();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .programId(programIdTwo)
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .facilityId(facilityId)
        .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
        .build();
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build()
        ));
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programIdTwo, facilityId,
        true))
        .thenReturn(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdTwo)
            .facilityId(facilityId)
            .build()
        ));

    // when
    siglusPhysicalInventoryService.saveDraftForAllProducts(physicalInventoryDto);

    // then
    verify(physicalInventoryStockManagementService, times(2))
        .savePhysicalInventory(any(), any());
  }

  @Test
  public void shouldCallV3MultipleTimesWhenDeletePhysicalInventoryForAllProducts() {
    // given
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdOne,
        facilityId, true))
        .thenReturn(inventoryOne.toString());
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdTwo,
        facilityId,true))
        .thenReturn(inventoryTwo.toString());

    // when
    siglusPhysicalInventoryService.deletePhysicalInventoryForAllProducts(facilityId);

    // then
    verify(physicalInventoryStockManagementService, times(2)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(1)).deleteByPhysicalInventoryIdIn(any());
  }

  @Test
  public void shouldCallV3WhenGetPhysicalInventory() {
    // when
    siglusPhysicalInventoryService.getPhysicalInventory(id);

    // then
    verify(physicalInventoryStockManagementService).getPhysicalInventory(id);
  }

  @Test
  public void shouldCallV3MultipleTimesWhenGetPhysicalInventoryForAllProducts() {
    // given
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build()
        ));
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programIdTwo, facilityId,
        true))
        .thenReturn(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdTwo)
            .facilityId(facilityId)
            .build()
        ));

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId);

    // then
    verify(physicalInventoryStockManagementService, times(2))
        .searchPhysicalInventory(any(), any(), any());
  }

  @Test
  public void shouldSaveExtensionTextWhenSaveDraftForAllProducts() {
    // given
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .reasonFreeText("saveText")
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .facilityId(facilityId)
        .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
        .build();
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build()
        ));
    PhysicalInventoryLineItemsExtension extensionOne = PhysicalInventoryLineItemsExtension.builder()
        .orderableId(orderableId)
        .build();
    PhysicalInventoryLineItemsExtension extensionTwo = PhysicalInventoryLineItemsExtension.builder()
        .orderableId(orderableIdTwo)
        .reasonFreeText("saveText")
        .build();
    when(lineItemsExtensionRepository.save(Arrays.asList(extensionOne, extensionTwo)))
        .thenReturn(Arrays.asList(extensionOne, extensionTwo));

    // when
    PhysicalInventoryDto dto = siglusPhysicalInventoryService
        .saveDraftForAllProducts(physicalInventoryDto);

    // then
    assertNull(dto.getLineItems().get(0).getReasonFreeText());
    assertEquals("saveText", dto.getLineItems().get(1).getReasonFreeText());
  }

  @Test
  public void shouldGetExtensionTextWhenGetPhysicalInventoryForAllProducts() {
    // given
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(programIdOne));
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .lotId(null)
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .lotId(null)
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .build();
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(newArrayList(PhysicalInventoryDto.builder()
            .id(inventoryOne)
            .programId(programIdOne)
            .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
            .facilityId(facilityId)
            .build()
        ));
    PhysicalInventoryLineItemsExtension extensionOne = PhysicalInventoryLineItemsExtension.builder()
        .lotId(null)
        .orderableId(orderableId)
        .reasonFreeText("extension")
        .build();
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(
        Arrays.asList(inventoryOne))).thenReturn(Arrays.asList(extensionOne));

    // when
    PhysicalInventoryDto dto = siglusPhysicalInventoryService
        .getPhysicalInventoryForAllProducts(facilityId);

    // then
    assertEquals("extension", dto.getLineItems().get(0).getReasonFreeText());
    assertNull(dto.getLineItems().get(1).getReasonFreeText());
  }

  @Test
  public void shouldThrowExceptionWhenGetDtosForAllProductsIfSupportedVirtualProgramsIsEmpty() {
    // then
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_PERMISSION_NOT_SUPPORTED));

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryDtosForAllProducts(facilityId, true, true);
  }

  @Test
  public void shouldCreateInitialInventoryDraftForAllProductsWhenInitialInventory() {
    // given
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(programIdOne));
    Map<String, String> extraData = newHashMap();
    extraData.put(IS_BASIC, "true");
    ApprovedProductDto approvedProductDto = new ApprovedProductDto(
        OrderableDto.builder()
            .id(orderableId)
            .extraData(extraData)
            .build());
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programIdOne, null))
        .thenReturn(new OrderablesAggregator(newArrayList(approvedProductDto)));
    when(physicalInventoryStockManagementService.createEmptyPhysicalInventory(any()))
        .thenReturn(PhysicalInventoryDto.builder().build());

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryDtosForAllProducts(facilityId, true, true);

    // then
    verify(physicalInventoryStockManagementService).createEmptyPhysicalInventory(
        physicalInventoryDtoArgumentCaptor.capture());
    PhysicalInventoryDto physicalInventoryDto = physicalInventoryDtoArgumentCaptor.getValue();
    assertEquals(1, physicalInventoryDto.getLineItems().size());
    assertEquals(orderableId, physicalInventoryDto.getLineItems().get(0).getOrderableId());
  }

  @Test
  public void shouldReturnDistinctOccurredDatesWhenFindPhysicalInventoryDates() {
    // given
    PhysicalInventory physicalInventory1 = new PhysicalInventory();
    physicalInventory1.setOccurredDate(LocalDate.of(2020, 6, 10));
    PhysicalInventory physicalInventory2 = new PhysicalInventory();
    physicalInventory2.setOccurredDate(LocalDate.of(2020, 6, 13));
    PhysicalInventory physicalInventory3 = new PhysicalInventory();
    physicalInventory3.setOccurredDate(LocalDate.of(2020, 6, 10));
    when(physicalInventoriesRepository.findByFacilityIdAndStartDateAndEndDate(facilityId, startDate,
        endDate))
        .thenReturn(newArrayList(physicalInventory1, physicalInventory2, physicalInventory3));

    // when
    Set<String> occurredDates = siglusPhysicalInventoryService.findPhysicalInventoryDates(
        facilityId, startDate, endDate);

    // then
    assertEquals(2, occurredDates.size());
    assertTrue(occurredDates.contains("2020-06-10"));
    assertTrue(occurredDates.contains("2020-06-13"));
  }

  @Test
  public void shouldReturnLatestPhysicalInventoryWhenFindLatestPhysicalInventory() {
    // given
    PhysicalInventory physicalInventory1 = new PhysicalInventory();
    physicalInventory1.setOccurredDate(LocalDate.of(2020, 6, 10));
    PhysicalInventory physicalInventory2 = new PhysicalInventory();
    physicalInventory2.setOccurredDate(LocalDate.of(2020, 6, 13));
    when(physicalInventoriesRepository.findByFacilityId(facilityId))
        .thenReturn(newArrayList(physicalInventory1, physicalInventory2));

    // when
    PhysicalInventoryDto physicalInventoryDto = siglusPhysicalInventoryService
        .findLatestPhysicalInventory(facilityId);

    // then
    assertEquals(LocalDate.of(2020, 6, 13), physicalInventoryDto.getOccurredDate());
  }
}
