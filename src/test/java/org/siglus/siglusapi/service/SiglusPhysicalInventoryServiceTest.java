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
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PermissionService;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.DraftListDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.PhysicalInventoryLineItemExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftLineItemsExtensionDto;
import org.siglus.siglusapi.dto.SubDraftDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.service.client.PhysicalInventoryStockManagementService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField", "PMD.AvoidDuplicateLiterals"})
public class SiglusPhysicalInventoryServiceTest {

  public static final String FREE_TEXT = "freeText";
  @Captor
  private ArgumentCaptor<PhysicalInventoryDto> physicalInventoryDtoArgumentCaptor;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private PhysicalInventoryStockManagementService physicalInventoryStockManagementService;

  @Mock
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private PhysicalInventoryService physicalInventoryService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Mock
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Mock
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  @Mock
  private PhysicalInventoryController inventoryController;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID programIdOne = UUID.randomUUID();

  private final UUID programIdTwo = UUID.randomUUID();

  private final UUID inventoryOne = UUID.randomUUID();

  private final UUID inventoryTwo = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final UUID physicalInventoryId = UUID.randomUUID();

  private final UUID subDraftIdOne = UUID.randomUUID();

  private final UUID subDraftIdTwo = UUID.randomUUID();

  private final String startDate = "startDate";

  private final String endDate = "endDate";

  @Test
  public void shouldCallV3MultipleTimesWhenCreateNewDraftForAllProducts() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(physicalInventoryStockManagementService.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());

    // when
    siglusPhysicalInventoryService.createNewDraftForAllProducts(physicalInventoryDto);

    // then
    verify(physicalInventoryStockManagementService, times(2)).createEmptyPhysicalInventory(physicalInventoryDto);
  }

  @Test
  public void shouldCallV3MultipleTimesWhenCreateNewDraftForAllProductsDirectly() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));
    when(inventoryController.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);

    // when
    siglusPhysicalInventoryService.createNewDraftForAllProductsDirectly(physicalInventoryDto);

    // then
    verify(inventoryController, times(2)).createEmptyPhysicalInventory(physicalInventoryDto);
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
  public void shouldSplicingDataWhenSaveDraftForProductsInOneProgram() {
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .reasonFreeText("freeText")
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(programIdOne)
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
        .reasonFreeText("freeText")
        .build();
    when(lineItemsExtensionRepository.save(Arrays.asList(extensionOne, extensionTwo)))
        .thenReturn(Arrays.asList(extensionOne, extensionTwo));

    // when
    PhysicalInventoryDto dto = siglusPhysicalInventoryService
        .saveDraftForProductsForOneProgram(physicalInventoryDto);

    // then
    assertNull(dto.getLineItems().get(0).getReasonFreeText());
    assertEquals(FREE_TEXT, dto.getLineItems().get(1).getReasonFreeText());
  }


  @Test
  public void shouldSplicingDataWithExtensionWhenSaveDraftForProductsInOneProgram() {
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .reasonFreeText("freeText")
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(programIdOne)
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
        .reasonFreeText("freeText")
        .build();
    when(lineItemsExtensionRepository.save(Arrays.asList(extensionOne, extensionTwo)))
        .thenReturn(Arrays.asList(extensionOne, extensionTwo));

    PhysicalInventoryLineItemExtensionDto physicalInventoryExtendDto = new PhysicalInventoryLineItemExtensionDto();
    BeanUtils.copyProperties(physicalInventoryDto, physicalInventoryExtendDto);
    List<PhysicalInventorySubDraftLineItemsExtensionDto> draftLineItemsExtensionDtos = new ArrayList<>();
    draftLineItemsExtensionDtos.add(PhysicalInventorySubDraftLineItemsExtensionDto.builder()
        .orderableId(orderableId).subDraftId(UUID.randomUUID()).build());
    draftLineItemsExtensionDtos.add(PhysicalInventorySubDraftLineItemsExtensionDto.builder()
        .orderableId(orderableIdTwo).subDraftId(UUID.randomUUID()).build());
    physicalInventoryExtendDto.setLineItemsExtensions(draftLineItemsExtensionDtos);

    // when
    PhysicalInventoryDto dto = siglusPhysicalInventoryService
        .saveDraftForProductsForOneProgramWithExtension(physicalInventoryExtendDto);

    // then
    assertNull(dto.getLineItems().get(0).getReasonFreeText());
  }

  @Test
  public void shouldCallV3OneTimesWhenDeletePhysicalInventoryProductInOneProgramDirectly() {
    // given
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdOne,
        facilityId, true))
        .thenReturn(inventoryOne.toString());

    // when
    siglusPhysicalInventoryService.deletePhysicalInventoryForProductInOneProgramDirectly(facilityId, programIdOne);

    // then
    verify(inventoryController, times(1)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(1)).deleteByPhysicalInventoryIdIn(any());
  }

  @Test
  public void shouldCallV3OneTimesWhenDeletePhysicalInventoryProductInOneProgram() {
    // given
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdOne,
        facilityId, true))
        .thenReturn(inventoryOne.toString());

    // when
    siglusPhysicalInventoryService.deletePhysicalInventoryForProductInOneProgram(facilityId, programIdOne);

    // then
    verify(physicalInventoryStockManagementService, times(1)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(1)).deleteByPhysicalInventoryIdIn(any());
  }

  @Test
  public void shouldCallV3MultipleTimesWhenDeletePhysicalInventoryForAllProducts() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdOne,
        facilityId, true))
        .thenReturn(inventoryOne.toString());
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdTwo,
        facilityId, true))
        .thenReturn(inventoryTwo.toString());

    // when
    siglusPhysicalInventoryService.deletePhysicalInventoryForAllProducts(facilityId);

    // then
    verify(physicalInventoryStockManagementService, times(2)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(2)).deleteByPhysicalInventoryIdIn(any());
  }

  @Test
  public void shouldCallV3MultipleTimesWhenDeletePhysicalInventoryForAllProductsDirectly() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdOne,
        facilityId, true))
        .thenReturn(inventoryOne.toString());
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programIdTwo,
        facilityId, true))
        .thenReturn(inventoryTwo.toString());

    // when
    siglusPhysicalInventoryService.deletePhysicalInventoryForAllProductsDirectly(facilityId);

    // then
    verify(inventoryController, times(2)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(2)).deleteByPhysicalInventoryIdIn(any());
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
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
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
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
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
  public void shouldSplicingDataWhenGetPhysicalInventoryDtosForProductsInOneProgramAndThereIsMappingInventories() {
    // given
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = Collections.singletonList(
        PhysicalInventoryLineItemDto.builder().build());
    List<PhysicalInventoryDto> inventories = Collections.singletonList(
        PhysicalInventoryDto.builder().id(id).lineItems(physicalInventoryLineItemDtos).build());
    List<PhysicalInventoryLineItemsExtension> extensions = Collections.singletonList(
        PhysicalInventoryLineItemsExtension.builder().reasonFreeText("hello world").build());
    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(physicalInventoryStockManagementService.searchPhysicalInventory(programId, facilityId, true))
        .thenReturn(inventories);
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(any())).thenReturn(extensions);

    // when
    List<PhysicalInventoryDto> resultInventory = siglusPhysicalInventoryService
        .getPhysicalInventoryDtosForProductsInOneProgram(programId, facilityId, true);

    // then
    assertEquals("hello world", resultInventory.get(0).getLineItems().get(0).getReasonFreeText());
  }

  @Test
  public void shouldThrowExceptionWhenGetDtosForAllProductsIfsupportedProgramsIsEmpty() {
    // then
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_PERMISSION_NOT_SUPPORTED));

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryDtosForAllProducts(facilityId, true, true);
  }

  @Test
  public void shouldCreateInitialInventoryDraftForAllProductsWhenInitialInventory() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne));
    Map<String, String> extraData = newHashMap();
    extraData.put(IS_BASIC, "true");
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setExtraData(extraData);
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setOrderable(orderableDto);
    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    when(facilityReferenceDataService.findOne(facilityId))
        .thenReturn(facilityDto);
    when(approvedProductReferenceDataService
        .getApprovedProducts(facilityId, programIdOne, emptyList()))
        .thenReturn(Collections.singletonList(approvedProductDto));
    when(physicalInventoryStockManagementService.createEmptyPhysicalInventory(any()))
        .thenReturn(PhysicalInventoryDto.builder().build());
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());

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
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndStartDateAndEndDate(programId,
        facilityId, startDate, endDate))
        .thenReturn(newArrayList(physicalInventory1, physicalInventory2, physicalInventory3));

    // when
    Set<String> occurredDates = siglusPhysicalInventoryService.findPhysicalInventoryDates(
        programId, facilityId, startDate, endDate);

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
    when(physicalInventoriesRepository
        .findTopByProgramIdAndFacilityIdAndIsDraftOrderByOccurredDateDesc(programId, facilityId, false))
        .thenReturn(physicalInventory2);

    // when
    PhysicalInventoryDto physicalInventoryDto = siglusPhysicalInventoryService
        .findLatestPhysicalInventory(facilityId, programId);

    // then
    assertEquals(LocalDate.of(2020, 6, 13), physicalInventoryDto.getOccurredDate());
  }

  @Test
  public void shouldThrowExceptionWhenCheckDraftIsExistIfDraftExisted() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString("stockmanagement.error.physicalInventory.isSubmitted"));

    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));
    when(inventoryController.searchPhysicalInventory(any(), any(), any()))
        .thenReturn(new ResponseEntity(Collections.emptyList(), HttpStatus.OK));

    // when
    siglusPhysicalInventoryService.checkDraftIsExist(facilityId);
  }

  @Test
  public void shouldGetSubDraftListInOneProgramWhenSubDraftExists() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(id).build();
    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .num(1)
        .status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
        .build();
    physicalInventorySubDraft.setId(subDraftIdOne);
    List<SubDraftDto> expectedSubDraftDtoList = Collections.singletonList(
        SubDraftDto.builder().groupNum(1).status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
            .subDraftId(Collections.singletonList(subDraftIdOne)).build());
    when(physicalInventoryStockManagementService
        .searchPhysicalInventory(programId, facilityId, true))
        .thenReturn(Collections.singletonList(physicalInventoryDto));
    DraftListDto expectedDraftList = DraftListDto.builder()
        .physicalInventoryId(id)
        .subDrafts(expectedSubDraftDtoList)
        .build();
    when(physicalInventorySubDraftRepository
        .findByPhysicalInventoryId(
            id)).thenReturn(Collections.singletonList(physicalInventorySubDraft));
    when(authenticationHelper
        .isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(false);
    // when
    DraftListDto subDraftListInOneProgram = siglusPhysicalInventoryService.getSubDraftListForOneProgram(programId,
        facilityId, true);
    // then
    assertEquals(expectedDraftList, subDraftListInOneProgram);
  }

  @Test
  public void shouldThrowExceptionWhenThereIsSubDraftExistAlready() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("Has already begun the physical inventory program"));

    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryId)
        .programId(programId).facilityId(facilityId).lineItems(Collections.emptyList()).build();
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.set("facilityId", String.valueOf(physicalInventoryDto.getFacilityId()));
    parameters.set("programId", String.valueOf(physicalInventoryDto.getProgramId()));
    parameters.set("rightName", "STOCK_INVENTORIES_EDIT");
    List<StockCardSummaryV2Dto> summaryV2Dtos = Collections.emptyList();
    when(physicalInventoryStockManagementService.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);
    when(siglusStockCardSummariesService.findSiglusStockCard(
        parameters, Collections.emptyList(), new PageRequest(0, Integer.MAX_VALUE)))
        .thenReturn(new PageImpl<>(summaryV2Dtos, new PageRequest(0, Integer.MAX_VALUE), 0));
    when(physicalInventorySubDraftRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Collections.singletonList(PhysicalInventorySubDraft.builder().build()));
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());

    // when
    siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(physicalInventoryDto, 3);
  }

  @Test
  public void shouldThrowExceptionWhenThereIsDraftExistAlready() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("there is not draft exists for program"));
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryId)
        .programId(ALL_PRODUCTS_PROGRAM_ID).facilityId(facilityId).lineItems(Collections.emptyList()).build();
    when(physicalInventoryStockManagementService.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(ALL_PRODUCTS_PROGRAM_ID));
    doNothing().when(physicalInventoryService).checkPermission(ALL_PRODUCTS_PROGRAM_ID, facilityId);
    when(physicalInventoryStockManagementService
        .searchPhysicalInventory(ALL_PRODUCTS_PROGRAM_ID, facilityId, true))
        .thenReturn(Collections.emptyList());
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());

    // when
    siglusPhysicalInventoryService.createAndSplitNewDraftForAllProduct(physicalInventoryDto, 3);
  }

  @Test
  public void shouldThrowExceptionWhenSubDraftIdsIsEmpty() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("empty subDraftIds"));

    //given
    List<UUID> subDraftIds = Collections.emptyList();

    // when
    siglusPhysicalInventoryService.getSubPhysicalInventoryDtoBySubDraftId(subDraftIds);
  }


  @Test
  public void shouldThrowExceptionWhenThereIsNoSubDraftListForAnyProgram() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("there is no subDraft for any record"));

    // given
    Set<UUID> supportedPrograms = new HashSet<>();
    supportedPrograms.add(programIdOne);
    supportedPrograms.add(programIdTwo);
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(supportedPrograms);
    doNothing().when(physicalInventoryService).checkPermission(programIdOne, facilityId);
    doNothing().when(physicalInventoryService).checkPermission(programIdTwo, facilityId);
    when(physicalInventoryStockManagementService
        .searchPhysicalInventory(programIdOne, facilityId, true)).thenReturn(Collections.emptyList());
    when(physicalInventoryStockManagementService
        .searchPhysicalInventory(programIdTwo, facilityId, true)).thenReturn(Collections.emptyList());

    // when
    siglusPhysicalInventoryService.getSubDraftListForAllProduct(facilityId, true);
  }

}
