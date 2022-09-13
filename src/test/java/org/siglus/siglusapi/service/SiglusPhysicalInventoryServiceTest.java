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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;
import static org.siglus.siglusapi.constant.FieldConstants.ALL_PROGRAM;
import static org.siglus.siglusapi.constant.FieldConstants.EXCLUDE_ARCHIVED;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.SINGLE_PROGRAM;
import static org.siglus.siglusapi.constant.FieldConstants.STOCK_CARD_ID;
import static org.siglus.siglusapi.constant.FieldConstants.VM_STATUS;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PermissionService;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.domain.PhysicalInventoryExtension;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.DraftListDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.PhysicalInventoryLineItemExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftLineItemsExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventoryValidationDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryDto;
import org.siglus.siglusapi.dto.SubDraftDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.LocationManagementOption;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  private PhysicalInventoryController inventoryController;

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
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Mock
  private PhysicalInventoryExtensionRepository physicalInventoryExtensionRepository;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private OrderableRepository orderableRepository;

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

  private final UUID operatorId = UUID.randomUUID();

  private final UUID stockCardId = UUID.randomUUID();

  private final String startDate = "startDate";

  private final String endDate = "endDate";

  private final UUID lotId = UUID.randomUUID();

  private final List<PhysicalInventory> programIsDraft = new ArrayList<>();

  @Test
  public void shouldCallV3MultipleTimesWhenCreateNewDraftForAllProducts() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(inventoryController.createEmptyPhysicalInventory(any())).thenAnswer(i -> i.getArguments()[0]);
    when(inventoryController.searchPhysicalInventory(any(), any(),
        anyBoolean()))
        .thenReturn(makeResponseEntity(Collections.emptyList()));
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
            .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    PhysicalInventoryDto actualCalled1 = PhysicalInventoryDto.builder()
            .programId(programIdOne).build();
    PhysicalInventoryDto actualCalled2 = PhysicalInventoryDto.builder()
            .programId(programIdTwo).build();

    // when
    siglusPhysicalInventoryService.createNewDraftForAllProducts(physicalInventoryDto, null);

    // then
    verify(inventoryController, times(1)).createEmptyPhysicalInventory(actualCalled1);
    verify(inventoryController, times(1)).createEmptyPhysicalInventory(actualCalled2);
  }

  @Test
  public void shouldCallV3MultipleTimesWhenCreateNewDraftForAllProductsDirectly() {
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(ALL_PRODUCTS_PROGRAM_ID).build();
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(inventoryController.createEmptyPhysicalInventory(any())).thenAnswer(i -> i.getArguments()[0]);
    PhysicalInventoryDto actualCalled1 = PhysicalInventoryDto.builder()
            .programId(programIdOne).build();
    PhysicalInventoryDto actualCalled2 = PhysicalInventoryDto.builder()
            .programId(programIdTwo).build();
    // when
    siglusPhysicalInventoryService.createNewDraftForAllProductsDirectly(physicalInventoryDto, null);

    // then
    verify(inventoryController, times(1)).createEmptyPhysicalInventory(actualCalled1);
    verify(inventoryController, times(1)).createEmptyPhysicalInventory(actualCalled2);
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
    when(inventoryController.searchPhysicalInventory(any(), any(),
        anyBoolean()))
        .thenReturn(makeResponseEntity(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build())));

    // when
    siglusPhysicalInventoryService.saveDraftForAllProducts(physicalInventoryDto);

    // then
    verify(inventoryController, times(2))
        .savePhysicalInventory(any(), any());
  }

  private <E> ResponseEntity<E> makeResponseEntity(E body) {
    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  @Test
  public void shouldSplicingDataWhenSaveDraftForProductsInOneProgram() {
    UUID lineItemId1 = UUID.randomUUID();
    UUID lineItemId2 = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .id(lineItemId1)
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .id(lineItemId2)
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .reasonFreeText("freeText")
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(inventoryOne)
        .programId(programIdOne)
        .facilityId(facilityId)
        .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
        .build();
    when(inventoryController.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(makeResponseEntity(newArrayList(physicalInventoryDto)));
    when(inventoryController.getPhysicalInventory(inventoryOne)).thenReturn(physicalInventoryDto);
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
    UUID lineItemId1 = UUID.randomUUID();
    UUID lineItemId2 = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .id(lineItemId1)
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .id(lineItemId2)
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .reasonFreeText("freeText")
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryId)
        .programId(programIdOne)
        .facilityId(facilityId)
        .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
        .build();
    when(inventoryController.getPhysicalInventory(physicalInventoryId)).thenReturn(physicalInventoryDto);
    when(inventoryController.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(makeResponseEntity(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build()
        )));
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
    siglusPhysicalInventoryService.deletePhysicalInventoryDraftForOneProgram(facilityId, programIdOne);

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
    siglusPhysicalInventoryService.deletePhysicalInventoryDraftForOneProgramWithSubDraft(facilityId, programIdOne);

    // then
    verify(inventoryController, times(1)).deletePhysicalInventory(any());
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
    siglusPhysicalInventoryService.deletePhysicalInventoryDraftForAllProgramsWithSubDraft(facilityId);

    // then
    verify(inventoryController, times(2)).deletePhysicalInventory(any());
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
    siglusPhysicalInventoryService.deletePhysicalInventoryDraftForAllPrograms(facilityId);

    // then
    verify(inventoryController, times(2)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(2)).deleteByPhysicalInventoryIdIn(any());
  }

  @Test
  public void shouldCallV3WhenGetPhysicalInventory() {
    // when
    siglusPhysicalInventoryService.getPhysicalInventory(id);

    // then
    verify(inventoryController).getPhysicalInventory(id);
  }

  @Test
  public void shouldCallV3MultipleTimesWhenGetPhysicalInventoryForAllProducts() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(programIdOne, programIdTwo));
    when(inventoryController.searchPhysicalInventory(any(), any(),
        anyBoolean()))
        .thenReturn(makeResponseEntity(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build()
        )));

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId);

    // then
    verify(inventoryController, times(2))
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
    when(inventoryController.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(makeResponseEntity(newArrayList(PhysicalInventoryDto.builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .build()
        )));
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
    UUID lineItemId1 = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .id(lineItemId1)
        .lotId(null)
        .orderableId(orderableId)
        .programId(programIdOne)
        .build();
    UUID lineItemId2 = UUID.randomUUID();
    UUID orderableIdTwo = UUID.randomUUID();
    PhysicalInventoryLineItemDto lineItemDtoTwo = PhysicalInventoryLineItemDto.builder()
        .id(lineItemId2)
        .lotId(null)
        .orderableId(orderableIdTwo)
        .programId(programIdOne)
        .build();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
            .id(inventoryOne)
            .programId(programIdOne)
            .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
            .facilityId(facilityId)
            .build();
    when(inventoryController.searchPhysicalInventory(programIdOne, facilityId,
        true))
        .thenReturn(makeResponseEntity(newArrayList(physicalInventoryDto)));
    when(siglusPhysicalInventoryService.getPhysicalInventory(inventoryOne)).thenReturn(physicalInventoryDto);
    PhysicalInventoryLineItemsExtension extensionOne = PhysicalInventoryLineItemsExtension.builder()
        .physicalInventoryLineItemId(lineItemId1)
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
    when(inventoryController.searchPhysicalInventory(programId, facilityId, true))
        .thenReturn(makeResponseEntity(inventories));
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(any())).thenReturn(extensions);

    // when
    List<PhysicalInventoryDto> resultInventory = siglusPhysicalInventoryService
        .getPhysicalInventoryDtosForProductsForOneProgram(programId, facilityId, true, false);

    // then
    assertEquals("hello world", resultInventory.get(0).getLineItems().get(0).getReasonFreeText());
  }

  @Test
  public void shouldThrowExceptionWhenGetDtosForAllProductsIfsupportedProgramsIsEmpty() {
    // then
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_PERMISSION_NOT_SUPPORTED));

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryDtosForAllProducts(facilityId, true, false);
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
  public void shouldGetSubDraftListWhenThereIsSubDraft() {
    // given
    doNothing().when(physicalInventoryService).checkPermission(programIdOne, facilityId);
    when(inventoryController
        .searchPhysicalInventory(programIdOne, facilityId, true))
        .thenReturn(makeResponseEntity(Collections.singletonList(PhysicalInventoryDto
            .builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .isDraft(true)
            .id(physicalInventoryId)
            .build())));

    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .num(1)
        .physicalInventoryId(physicalInventoryId)
        .status(PhysicalInventorySubDraftEnum.DRAFT)
        .operatorId(operatorId)
        .build();
    physicalInventorySubDraft.setId(subDraftIdOne);
    when(physicalInventorySubDraftRepository
        .findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Collections.singletonList(physicalInventorySubDraft));
    when(authenticationHelper.getUserNameByUserId(operatorId)).thenReturn("saver");
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(true);

    DraftListDto expectedDraftListDto = DraftListDto.builder().physicalInventoryId(physicalInventoryId).subDrafts(
            Collections.singletonList(SubDraftDto.builder()
                .groupNum(1)
                .subDraftId(Collections.singletonList(subDraftIdOne))
                .status(PhysicalInventorySubDraftEnum.DRAFT)
                .saver("saver")
                .build()))
        .canSubmitDrafts(false)
        .canMergeOrDeleteDrafts(true)
        .build();

    // when
    DraftListDto subDraftList = siglusPhysicalInventoryService.getSubDraftListForOneProgram(programIdOne,
        facilityId, true);

    //then
    assertEquals(expectedDraftListDto, subDraftList);
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
    when(inventoryController
        .searchPhysicalInventory(programId, facilityId, true))
        .thenReturn(makeResponseEntity(Collections.singletonList(physicalInventoryDto)));
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
    exception.expectMessage(containsString("Has already begun the physical inventory : "));

    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryId)
        .programId(programId).facilityId(facilityId).lineItems(Collections.emptyList()).build();
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.set(FACILITY_ID, String.valueOf(physicalInventoryDto.getFacilityId()));
    parameters.set(PROGRAM_ID, String.valueOf(physicalInventoryDto.getProgramId()));
    parameters.set(RIGHT_NAME, STOCK_INVENTORIES_EDIT);
    parameters.set(EXCLUDE_ARCHIVED, Boolean.TRUE.toString());
    List<StockCardSummaryV2Dto> summaryV2Dtos = Collections.emptyList();
    when(inventoryController.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);
    when(inventoryController.searchPhysicalInventory(any(), any(),
        anyBoolean()))
        .thenReturn(makeResponseEntity(Collections.emptyList()));
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
    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);

    // when
    siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(physicalInventoryDto, 3, null, false);
  }

  @Test
  public void shouldThrowExceptionWhenThereIsDraftExistAlready() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("there is not draft exists for program"));
    // given
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryId)
        .programId(ALL_PRODUCTS_PROGRAM_ID).facilityId(facilityId).lineItems(Collections.emptyList()).build();
    when(inventoryController.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(physicalInventoryDto);
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(ALL_PRODUCTS_PROGRAM_ID));
    doNothing().when(physicalInventoryService).checkPermission(ALL_PRODUCTS_PROGRAM_ID, facilityId);
    when(inventoryController
        .searchPhysicalInventory(ALL_PRODUCTS_PROGRAM_ID, facilityId, true))
        .thenReturn(makeResponseEntity(Collections.emptyList()));
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());

    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);

    // when
    siglusPhysicalInventoryService.createAndSplitNewDraftForAllProduct(
        physicalInventoryDto, 3, false, null, false);
  }

  @Test
  public void shouldReturnPhysicalInventoryDtoWhenCreateAndSpiltNewDraftForOneProgram() {

    // given
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
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(facilityReferenceDataService.findOne(facilityId))
        .thenReturn(facilityDto);
    when(approvedProductReferenceDataService
        .getApprovedProducts(facilityId, programIdOne, emptyList()))
        .thenReturn(Collections.singletonList(approvedProductDto));

    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(inventoryController
        .searchPhysicalInventory(programId, facilityId, true)).thenReturn(makeResponseEntity(emptyList()));

    ObjectReferenceDto orderableReferenceDto = new ObjectReferenceDto();
    orderableReferenceDto.setId(orderableId);
    ObjectReferenceDto stockCradReferenceDto = new ObjectReferenceDto();
    stockCradReferenceDto.setId(stockCardId);
    ObjectReferenceDto lotReferenceDto = new ObjectReferenceDto();
    lotReferenceDto.setId(lotId);
    CanFulfillForMeEntryDto canFulfillForMeEntryDtos = new CanFulfillForMeEntryDto();
    canFulfillForMeEntryDtos.setOrderable(orderableReferenceDto);
    canFulfillForMeEntryDtos.setStockCard(stockCradReferenceDto);
    canFulfillForMeEntryDtos.setLot(lotReferenceDto);
    StockCardSummaryV2Dto stockCardSummaryV2Dto = new StockCardSummaryV2Dto();
    stockCardSummaryV2Dto.setCanFulfillForMe(Collections.singleton(canFulfillForMeEntryDtos));
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .facilityId(facilityId)
        .programId(programId).build();
    parameters.set(FACILITY_ID, String.valueOf(physicalInventoryDto.getFacilityId()));
    parameters.set(PROGRAM_ID, String.valueOf(physicalInventoryDto.getProgramId()));
    parameters.set(RIGHT_NAME, STOCK_INVENTORIES_EDIT);
    parameters.set(EXCLUDE_ARCHIVED, Boolean.TRUE.toString());
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
    when(siglusStockCardSummariesService.findSiglusStockCard(
        parameters, emptyList(), pageable)).thenReturn(
        (new PageImpl<>(Collections.singletonList(stockCardSummaryV2Dto), new PageRequest(0, Integer.MAX_VALUE), 0)));

    PhysicalInventoryDto createdPhysicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryId)
        .facilityId(facilityId).programId(programId).build();
    when(inventoryController.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(createdPhysicalInventoryDto);
    when(physicalInventorySubDraftRepository.findByPhysicalInventoryId(physicalInventoryId)).thenReturn(
        emptyList());

    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder().physicalInventoryId(physicalInventoryId).build();
    physicalInventorySubDraft.setId(subDraftIdOne);
    physicalInventorySubDraft.setNum(1);
    when(physicalInventorySubDraftRepository
        .findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryId))).thenReturn(
        Collections.singletonList(physicalInventorySubDraft));

    Orderable orderable = new Orderable();
    orderable.setFullProductName("fakeProductName");
    when(orderableRepository.findLatestById(orderableId)).thenReturn(Optional.of(orderable));

    when(lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryId))).thenReturn(
        Collections.singletonList(
            PhysicalInventoryLineItemsExtension.builder().physicalInventoryId(physicalInventoryId).lotId(lotId)
                .orderableId(orderableId).build()));
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programId,
        facilityId, true)).thenReturn(String.valueOf(physicalInventoryId));

    Map<String, String> expectedExtraData = newHashMap();
    expectedExtraData.put(VM_STATUS, null);
    expectedExtraData.put(STOCK_CARD_ID, String.valueOf(stockCardId));
    PhysicalInventoryLineItemDto expectedPhysicalInventoryLineItem = PhysicalInventoryLineItemDto
        .builder()
        .programId(programId)
        .orderableId(orderableId)
        .lotId(lotId)
        .extraData(expectedExtraData)
        .stockAdjustments(emptyList())
        .build();
    PhysicalInventoryDto expectedPhysicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(programId)
        .id(physicalInventoryId)
        .facilityId(facilityId)
        .lineItems(Collections.singletonList(expectedPhysicalInventoryLineItem))
        .build();
    when(inventoryController.getPhysicalInventory(physicalInventoryId)).thenReturn(expectedPhysicalInventoryDto);
    // when
    PhysicalInventoryDto returnedPhysicalInventoryDto = siglusPhysicalInventoryService
        .createAndSpiltNewDraftForOneProgram(physicalInventoryDto, 1, null, false);

    // then
    assertEquals(expectedPhysicalInventoryDto, returnedPhysicalInventoryDto);

  }

  @Test
  public void shouldThrowExceptionWhenUserCanNotInitialInventories() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString("not.acceptable"));

    // given
    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(facilityReferenceDataService.findOne(facilityId))
        .thenReturn(facilityDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(100);
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().facilityId(facilityId)
        .programId(programId).build();

    // when
    siglusPhysicalInventoryService.createAndSplitNewDraftForAllProduct(physicalInventoryDto, 3, true, null, false);
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
  public void shouldGetCorrespondingSubLocationPhysicalInventoryDtoWithLegalSubDraftIds() {
    // given
    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .physicalInventoryId(physicalInventoryId)
        .num(1)
        .build();

    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = Collections.singletonList(
        PhysicalInventoryLineItemDto
            .builder()
            .orderableId(orderableId)
            .lotId(lotId)
            .build());

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryId)
        .programId(programId)
        .facilityId(facilityId)
        .isDraft(true)
        .lineItems(physicalInventoryLineItemDtos)
        .build();
    SiglusPhysicalInventoryDto siglusPhysicalInventoryDto = new SiglusPhysicalInventoryDto();
    BeanUtils.copyProperties(physicalInventoryDto, siglusPhysicalInventoryDto);
    siglusPhysicalInventoryDto.setLocationOption(LocationManagementOption.BY_PRODUCT.getValue());
    PhysicalInventoryLineItemsExtension physicalInventoryLineItemsExtension = PhysicalInventoryLineItemsExtension
        .builder()
        .subDraftId(subDraftIdOne)
        .lotId(lotId)
        .orderableId(orderableId)
        .subDraftId(subDraftIdOne)
        .build();
    when(physicalInventorySubDraftRepository.findFirstById(subDraftIdOne)).thenReturn(physicalInventorySubDraft);
    when(inventoryController.getPhysicalInventory(physicalInventoryId))
        .thenReturn(physicalInventoryDto);
    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(inventoryController
        .searchPhysicalInventory(programId, facilityId, true)).thenReturn(makeResponseEntity(Collections.singletonList(
        physicalInventoryDto
    )));
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryId)))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(orderableRepository.findLatestById(orderableId)).thenReturn(Optional.of(new Orderable()));
    List<UUID> subDraftIds = Collections.singletonList(subDraftIdOne);
    PhysicalInventoryExtension extension = PhysicalInventoryExtension.builder()
            .locationOption(LocationManagementOption.BY_PRODUCT).build();
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
            .thenReturn(Arrays.asList(extension));
    // when
    SiglusPhysicalInventoryDto actualPhysicalInventoryDto = siglusPhysicalInventoryService
        .getSubLocationPhysicalInventoryDtoBySubDraftId(subDraftIds, false);

    // then
    assertEquals(siglusPhysicalInventoryDto, actualPhysicalInventoryDto);

  }

  @Test
  public void shouldGetCorrespondingSubPhysicalInventoryDtoWithLegalSubDraftIds() {
    // given
    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .physicalInventoryId(physicalInventoryId)
        .num(1)
        .build();

    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = Collections.singletonList(
        PhysicalInventoryLineItemDto
            .builder()
            .orderableId(orderableId)
            .lotId(lotId)
            .build());

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryId)
        .programId(programId)
        .facilityId(facilityId)
        .isDraft(true)
        .lineItems(physicalInventoryLineItemDtos)
        .build();
    PhysicalInventoryLineItemsExtension physicalInventoryLineItemsExtension = PhysicalInventoryLineItemsExtension
        .builder()
        .subDraftId(subDraftIdOne)
        .lotId(lotId)
        .orderableId(orderableId)
        .build();
    when(physicalInventorySubDraftRepository.findFirstById(subDraftIdOne)).thenReturn(physicalInventorySubDraft);
    when(inventoryController.getPhysicalInventory(physicalInventoryId))
        .thenReturn(physicalInventoryDto);
    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(inventoryController
        .searchPhysicalInventory(programId, facilityId, true)).thenReturn(makeResponseEntity(Collections.singletonList(
        physicalInventoryDto
    )));
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryId)))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(orderableRepository.findLatestById(orderableId)).thenReturn(Optional.of(new Orderable()));
    List<UUID> subDraftIds = Collections.singletonList(subDraftIdOne);
    // when
    PhysicalInventoryDto actualPhysicalInventoryDto = siglusPhysicalInventoryService
        .getSubPhysicalInventoryDtoBySubDraftId(subDraftIds);

    // then
    assertEquals(physicalInventoryDto, actualPhysicalInventoryDto);
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
    when(inventoryController.searchPhysicalInventory(any(), any(),
        anyBoolean()))
        .thenReturn(makeResponseEntity(Collections.emptyList()));

    // when
    siglusPhysicalInventoryService.getSubDraftListForAllProduct(facilityId, true);
  }

  @Test
  public void shouldGetExceptionWhenConflictForAllProduct() {
    exception.expect(PermissionMessageException.class);
    exception.expectMessage("stockmanagement.error.authorization.program.not.supported");
    siglusPhysicalInventoryService.checkConflictForAllProduct(facilityId);
  }

  @Test
  public void shouldGetExceptionWhenConflictForOneProgram() {
    exception.expect(PermissionMessageException.class);
    exception.expectMessage("stockmanagement.error.authorization.program.not.supported");
    siglusPhysicalInventoryService.checkConflictForOneProgram(facilityId);
  }

  @Test
  public void shouldGetConflictWhenClickAllProductWhileOneProgramHaveDraft() {
    // given
    PhysicalInventory physicalInventory = mockPhysicalInventory();
    programIsDraft.add(physicalInventory);
    PhysicalInventoryExtension physicalInventoryExtension = mockPhysicalInventoryExtensionSingleProgram();
    HashSet<UUID> supportedPrograms = Sets.newHashSet(programIdOne);
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(supportedPrograms);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(
        programIdOne, facilityId, true)).thenReturn(programIsDraft);
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Lists.newArrayList(physicalInventoryExtension));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForAllProduct(facilityId);
    // then
    assertFalse(physicalInventoryValidationDto.isCanStartInventory());
  }

  @Test
  public void shouldNotGetConflictWhenClickAllProductWhileNoProgramHaveDraft() {
    // given
    PhysicalInventory physicalInventory = mockPhysicalInventory();
    programIsDraft.add(physicalInventory);
    PhysicalInventoryExtension physicalInventoryExtension = mockPhysicalInventoryExtensionAllProduct();
    HashSet<UUID> supportedPrograms = Sets.newHashSet(programIdOne);
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(supportedPrograms);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(
        programIdOne, facilityId, true)).thenReturn(programIsDraft);
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Lists.newArrayList(physicalInventoryExtension));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForAllProduct(facilityId);

    // then
    assertTrue(physicalInventoryValidationDto.isCanStartInventory());
  }

  @Test
  public void shouldGetConflictWhenClickOneProgramWhileAllProductHaveDraft() {
    // given
    PhysicalInventory physicalInventory = mockPhysicalInventory();
    programIsDraft.add(physicalInventory);
    HashSet<UUID> supportedPrograms = Sets.newHashSet(programIdOne);
    PhysicalInventoryExtension physicalInventoryExtension = mockPhysicalInventoryExtensionAllProduct();
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(
        supportedPrograms);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(
        programIdOne, facilityId, true)).thenReturn(programIsDraft);
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Lists.newArrayList(physicalInventoryExtension));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForOneProgram(facilityId);

    // then
    assertFalse(physicalInventoryValidationDto.isCanStartInventory());
  }

  @Test
  public void shouldNotGetConflictWhenClickOneProgramWhileAllProductDontHaveDraft() {
    // given
    PhysicalInventory physicalInventory = mockPhysicalInventory();
    programIsDraft.add(physicalInventory);
    HashSet<UUID> supportedPrograms = Sets.newHashSet(programIdOne);
    PhysicalInventoryExtension physicalInventoryExtension = mockPhysicalInventoryExtensionSingleProgram();
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(
        supportedPrograms);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(
        programIdOne, facilityId, true)).thenReturn(programIsDraft);
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryId))
        .thenReturn(Lists.newArrayList(physicalInventoryExtension));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForOneProgram(facilityId);

    // then
    assertTrue(physicalInventoryValidationDto.isCanStartInventory());
  }

  private PhysicalInventory mockPhysicalInventory() {
    PhysicalInventory physicalInventory = new PhysicalInventory();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setFacilityId(facilityId);
    physicalInventory.setIsDraft(true);
    return physicalInventory;
  }

  private PhysicalInventoryExtension mockPhysicalInventoryExtensionSingleProgram() {
    PhysicalInventoryExtension physicalInventoryExtension = new PhysicalInventoryExtension();
    physicalInventoryExtension.setPhysicalInventoryId(physicalInventoryId);
    physicalInventoryExtension.setCategory(SINGLE_PROGRAM);
    return physicalInventoryExtension;
  }

  private PhysicalInventoryExtension mockPhysicalInventoryExtensionAllProduct() {
    PhysicalInventoryExtension physicalInventoryExtension = new PhysicalInventoryExtension();
    physicalInventoryExtension.setPhysicalInventoryId(physicalInventoryId);
    physicalInventoryExtension.setCategory(ALL_PROGRAM);
    return physicalInventoryExtension;
  }

}
