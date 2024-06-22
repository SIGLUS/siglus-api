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
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.CENTRAL;
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
import static org.siglus.siglusapi.constant.ProgramConstants.MMC_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACCEPTABLE;
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
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.RequisitionService;
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
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.PhysicalInventoryEmptyLocationLineItem;
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
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryEmptyLocationLineItemRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryHistoryRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.repository.SiglusPhysicalInventoryRepository;
import org.siglus.siglusapi.repository.dto.SiglusPhysicalInventoryBriefDto;
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
  private FacilityLocationsRepository facilityLocationsRepository;

  @Mock
  private RequisitionService requisitionService;

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
  private PhysicalInventoryHistoryRepository physicalInventoryHistoryRepository;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private PhysicalInventoryEmptyLocationLineItemRepository physicalInventoryEmptyLocationLineItemRepository;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private SiglusPhysicalInventoryRepository siglusPhysicalInventoryRepository;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID programIdOne = UUID.randomUUID();

  private final UUID programIdTwo = UUID.randomUUID();

  private final UUID programIdThree = UUID.randomUUID();

  private final UUID programIdFour = UUID.randomUUID();

  private final UUID inventoryOne = UUID.randomUUID();

  private final UUID inventoryTwo = UUID.randomUUID();

  private final UUID id = UUID.randomUUID();

  private final UUID physicalInventoryIdOne = UUID.randomUUID();

  private final UUID physicalInventoryIdTwo = UUID.randomUUID();

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
    mockGetViaMmcProgram();

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
    when(physicalInventoryExtensionRepository.save(any(PhysicalInventoryExtension.class)))
        .thenReturn(new PhysicalInventoryExtension());
    PhysicalInventoryDto actualCalled1 = PhysicalInventoryDto.builder()
        .programId(programIdOne).build();
    PhysicalInventoryDto actualCalled2 = PhysicalInventoryDto.builder()
        .programId(programIdTwo).build();
    // when
    siglusPhysicalInventoryService.createNewDraftForAllPrograms(physicalInventoryDto, null);
    // then
    verify(inventoryController, times(1)).createEmptyPhysicalInventory(actualCalled1);
    verify(inventoryController, times(1)).createEmptyPhysicalInventory(actualCalled2);
  }

  @Test
  public void shouldCallV3MultipleTimesWhenSaveDraftForAllProducts() {
    // given
    mockGetViaMmcProgram();
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
    siglusPhysicalInventoryService.saveDraftForAllPrograms(physicalInventoryDto);

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
        .id(physicalInventoryIdOne)
        .programId(programIdOne)
        .facilityId(facilityId)
        .lineItems(newArrayList(lineItemDtoOne, lineItemDtoTwo))
        .build();
    when(inventoryController.getPhysicalInventory(physicalInventoryIdOne)).thenReturn(physicalInventoryDto);
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
    siglusPhysicalInventoryService.deletePhysicalInventoryDraftForOneProgram(facilityId, programIdOne);

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
    PhysicalInventorySubDraft physicalInventorySubDraftOne = new PhysicalInventorySubDraft();
    when(physicalInventorySubDraftRepository.findByPhysicalInventoryId(inventoryOne))
        .thenReturn(Collections.singletonList(physicalInventorySubDraftOne));
    PhysicalInventorySubDraft physicalInventorySubDraftTwo = new PhysicalInventorySubDraft();
    when(physicalInventorySubDraftRepository.findByPhysicalInventoryId(inventoryTwo))
        .thenReturn(Collections.singletonList(physicalInventorySubDraftTwo));

    // when
    siglusPhysicalInventoryService.deletePhysicalInventoryDraftForAllPrograms(facilityId);

    // then
    verify(inventoryController, times(2)).deletePhysicalInventory(any());
    verify(lineItemsExtensionRepository, times(2)).deleteByPhysicalInventoryIdIn(any());
    verify(physicalInventoryEmptyLocationLineItemRepository, times(2))
        .deletePhysicalInventoryEmptyLocationLineItemsBySubDraftIdIn(any());
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

  private void mockGetViaMmcProgram() {
    ProgramDto viaDto = new ProgramDto();
    viaDto.setId(programIdThree);
    when(siglusProgramService.getProgramByCode(VIA_PROGRAM_CODE)).thenReturn(Optional.of(viaDto));
    ProgramDto mmcDto = new ProgramDto();
    mmcDto.setId(programIdFour);
    when(siglusProgramService.getProgramByCode(MMC_PROGRAM_CODE)).thenReturn(Optional.of(mmcDto));
  }

  @Test
  public void shouldCallV3MultipleTimesWhenGetPhysicalInventoryForAllProducts() {
    // given
    mockGetViaMmcProgram();
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
    siglusPhysicalInventoryService.getPhysicalInventoryForAllPrograms(facilityId);

    // then
    verify(inventoryController, times(2))
        .searchPhysicalInventory(any(), any(), any());
  }

  @Test
  public void shouldSaveExtensionTextWhenSaveDraftForAllProducts() {
    // given
    mockGetViaMmcProgram();

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
        .saveDraftForAllPrograms(physicalInventoryDto);

    // then
    assertNull(dto.getLineItems().get(0).getReasonFreeText());
    assertEquals("saveText", dto.getLineItems().get(1).getReasonFreeText());
  }

  @Test
  public void shouldGetExtensionTextWhenGetPhysicalInventoryForAllProducts() {
    // given
    mockGetViaMmcProgram();

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
    List<PhysicalInventoryExtension> physicalInventoryExtensions = Collections.singletonList(
        PhysicalInventoryExtension.builder().category(ALL_PROGRAM).build());
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(any())).thenReturn(physicalInventoryExtensions);

    // when
    PhysicalInventoryDto dto = siglusPhysicalInventoryService
        .getPhysicalInventoryForAllPrograms(facilityId);

    // then
    assertEquals("extension", dto.getLineItems().get(0).getReasonFreeText());
    assertNull(dto.getLineItems().get(1).getReasonFreeText());
  }

  @Test
  public void shouldSplicingDataWhenGetPhysicalInventoryDtosForProductsInOneProgramAndThereIsMappingInventories() {
    // given
    List<PhysicalInventoryExtension> physicalInventoryExtensions = Collections.singletonList(
        PhysicalInventoryExtension.builder().category(SINGLE_PROGRAM).build());
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = Collections.singletonList(
        PhysicalInventoryLineItemDto.builder().build());
    List<PhysicalInventoryDto> inventories = Collections.singletonList(
        PhysicalInventoryDto.builder().id(id).lineItems(physicalInventoryLineItemDtos).build());
    List<PhysicalInventoryLineItemsExtension> extensions = Collections.singletonList(
        PhysicalInventoryLineItemsExtension.builder().reasonFreeText("hello world").build());
    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(inventoryController.searchPhysicalInventory(programId, facilityId, true))
        .thenReturn(makeResponseEntity(inventories));
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(any())).thenReturn(physicalInventoryExtensions);
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
    mockGetViaMmcProgram();
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_PERMISSION_NOT_SUPPORTED));

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryDtosForAllPrograms(facilityId, true, false);
  }

  @Test
  public void shouldReturnDistinctOccurredDatesWhenFindPhysicalInventoryDates() {
    // given
    SiglusPhysicalInventoryBriefDto physicalInventory1 = new SiglusPhysicalInventoryBriefDto();
    physicalInventory1.setOccurredDate(LocalDate.of(2020, 6, 10));
    SiglusPhysicalInventoryBriefDto physicalInventory2 = new SiglusPhysicalInventoryBriefDto();
    physicalInventory2.setOccurredDate(LocalDate.of(2020, 6, 13));
    SiglusPhysicalInventoryBriefDto physicalInventory3 = new SiglusPhysicalInventoryBriefDto();
    physicalInventory3.setOccurredDate(LocalDate.of(2020, 6, 10));
    when(siglusPhysicalInventoryRepository.findByProgramIdAndFacilityIdAndStartDateAndEndDate(programId,
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
    mockGetViaMmcProgram();
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
    when(siglusPhysicalInventoryRepository
        .queryForOneProgram(facilityId, programIdOne, true))
        .thenReturn(Collections.singletonList(SiglusPhysicalInventoryBriefDto
            .builder()
            .programId(programIdOne)
            .facilityId(facilityId)
            .isDraft(true)
            .id(physicalInventoryIdOne)
            .build()));

    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .num(1)
        .physicalInventoryId(physicalInventoryIdOne)
        .status(PhysicalInventorySubDraftEnum.DRAFT)
        .operatorId(operatorId)
        .build();
    physicalInventorySubDraft.setId(subDraftIdOne);
    when(physicalInventorySubDraftRepository
        .findByPhysicalInventoryId(physicalInventoryIdOne))
        .thenReturn(Collections.singletonList(physicalInventorySubDraft));
    when(authenticationHelper.getUserNameByUserId(operatorId)).thenReturn("saver");
    when(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).thenReturn(true);

    DraftListDto expectedDraftListDto = DraftListDto.builder().physicalInventoryId(physicalInventoryIdOne).subDrafts(
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
    SiglusPhysicalInventoryBriefDto briefDto = SiglusPhysicalInventoryBriefDto.builder().id(id).build();
    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .num(1)
        .status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
        .build();
    physicalInventorySubDraft.setId(subDraftIdOne);
    List<SubDraftDto> expectedSubDraftDtoList = Collections.singletonList(
        SubDraftDto.builder().groupNum(1).status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
            .subDraftId(Collections.singletonList(subDraftIdOne)).build());
    when(siglusPhysicalInventoryRepository
        .queryForOneProgram(facilityId, programId, true))
        .thenReturn(Collections.singletonList(briefDto));
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
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryIdOne)
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
        parameters, Collections.emptyList(), new PageRequest(0, Integer.MAX_VALUE), false))
        .thenReturn(new PageImpl<>(summaryV2Dtos, new PageRequest(0, Integer.MAX_VALUE), 0));
    when(physicalInventorySubDraftRepository.findByPhysicalInventoryIdIn(anyList()))
        .thenReturn(Collections.singletonList(PhysicalInventorySubDraft.builder().build()));
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(physicalInventoriesRepository.findByProgramIdAndFacilityIdAndIsDraft(any(), eq(facilityId), eq(true)))
        .thenReturn(Collections.emptyList());
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(VIA_PROGRAM_CODE);
    programDto.setId(programId);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);
    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(physicalInventoryExtensionRepository.save(any(PhysicalInventoryExtension.class)))
        .thenReturn(new PhysicalInventoryExtension());
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(programId));

    // when
    siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(physicalInventoryDto, 3, null, false);
  }

  @Test
  public void shouldThrowExceptionWhenThereIsDraftExistAlready() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("there is not draft exists for program"));
    // given
    mockGetViaMmcProgram();

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryIdOne)
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
    when(physicalInventoryExtensionRepository.save(any(PhysicalInventoryExtension.class)))
        .thenReturn(new PhysicalInventoryExtension());

    // when
    siglusPhysicalInventoryService.createAndSplitNewDraftForAllPrograms(
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
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, programIdOne))
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
        parameters, emptyList(), pageable, false)).thenReturn(
        (new PageImpl<>(Collections.singletonList(stockCardSummaryV2Dto), new PageRequest(0, Integer.MAX_VALUE), 0)));

    PhysicalInventoryDto createdPhysicalInventoryDto = PhysicalInventoryDto.builder().id(physicalInventoryIdOne)
        .facilityId(facilityId).programId(programId).build();
    when(inventoryController.createEmptyPhysicalInventory(physicalInventoryDto))
        .thenReturn(createdPhysicalInventoryDto);
//    when(physicalInventorySubDraftRepository.findByPhysicalInventoryId(physicalInventoryIdOne)).thenReturn(
//        emptyList());

    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder().physicalInventoryId(physicalInventoryIdOne).build();
    physicalInventorySubDraft.setId(subDraftIdOne);
    physicalInventorySubDraft.setNum(1);
    when(physicalInventorySubDraftRepository
        .findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryIdOne))).thenReturn(
        emptyList());
    when(physicalInventorySubDraftRepository.save(anyList()))
        .thenReturn(Collections.singletonList(physicalInventorySubDraft));

    Orderable orderable = new Orderable();
    orderable.setFullProductName("fakeProductName");
    when(orderableRepository.findLatestById(orderableId)).thenReturn(Optional.of(orderable));

    when(lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryIdOne))).thenReturn(
        Collections.singletonList(
            PhysicalInventoryLineItemsExtension.builder().physicalInventoryId(physicalInventoryIdOne).lotId(lotId)
                .orderableId(orderableId).build()));
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programId,
        facilityId, true)).thenReturn(String.valueOf(physicalInventoryIdOne));

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
        .id(physicalInventoryIdOne)
        .facilityId(facilityId)
        .lineItems(Collections.singletonList(expectedPhysicalInventoryLineItem))
        .build();
    when(inventoryController.getPhysicalInventory(physicalInventoryIdOne)).thenReturn(expectedPhysicalInventoryDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(VIA_PROGRAM_CODE);
    programDto.setId(programId);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);
    org.openlmis.referencedata.dto.OrderableDto orderableDto1 = new org.openlmis.referencedata.dto.OrderableDto();
    orderableDto1.setId(orderableId);
    orderableDto1.setProductCode("22A01");
    List<org.openlmis.referencedata.dto.OrderableDto> orderableDtos = singletonList(orderableDto1);
    when(siglusOrderableService.getAllProducts()).thenReturn(orderableDtos);
    when(physicalInventoryExtensionRepository.save(any(PhysicalInventoryExtension.class)))
        .thenReturn(new PhysicalInventoryExtension());
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(programId));

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
    siglusPhysicalInventoryService.createAndSplitNewDraftForAllPrograms(physicalInventoryDto, 3, true, null, false);
  }


  @Test
  public void shouldThrowExceptionWhenSubDraftIdsIsEmpty() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("empty subDraftIds"));

    //given
    List<UUID> subDraftIds = Collections.emptyList();

    // when
    siglusPhysicalInventoryService.getPhysicalInventoryDtoBySubDraftIds(subDraftIds);
  }

  @Test
  public void shouldGetCorrespondingSubLocationPhysicalInventoryDtoWithLegalSubDraftIds() {
    // given
    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .physicalInventoryId(physicalInventoryIdOne)
        .num(1)
        .build();

    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = Collections.singletonList(
        PhysicalInventoryLineItemDto
            .builder()
            .orderableId(orderableId)
            .lotId(lotId)
            .build());

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryIdOne)
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
    when(inventoryController.getPhysicalInventory(physicalInventoryIdOne))
        .thenReturn(physicalInventoryDto);
    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(inventoryController
        .searchPhysicalInventory(programId, facilityId, true)).thenReturn(makeResponseEntity(Collections.singletonList(
        physicalInventoryDto
    )));
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryIdOne)))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryIdOne))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(orderableRepository.findLatestById(orderableId)).thenReturn(Optional.of(new Orderable()));
    List<UUID> subDraftIds = Collections.singletonList(subDraftIdOne);
    PhysicalInventoryExtension extension = PhysicalInventoryExtension.builder()
        .locationOption(LocationManagementOption.BY_PRODUCT).category(SINGLE_PROGRAM).build();
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryIdOne))
        .thenReturn(Collections.singletonList(extension));
    // when
    SiglusPhysicalInventoryDto actualPhysicalInventoryDto = siglusPhysicalInventoryService
        .getPhysicalInventoryDtoBySubDraftIds(subDraftIds);
    actualPhysicalInventoryDto.setLineItems(physicalInventoryLineItemDtos);

    // then
    assertEquals(siglusPhysicalInventoryDto, actualPhysicalInventoryDto);

  }

  @Test
  public void shouldThrowExceptionWhenFacilityIsVirtualFacility() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_NOT_ACCEPTABLE));
    // given

    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode(CENTRAL);
    facilityDto.setType(typeDto);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(10);
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryIdOne)
        .programId(programId)
        .facilityId(facilityId)
        .build();
    // when
    siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(
        physicalInventoryDto, 2, "product", false);

  }

  @Test
  public void shouldThrowExceptionWhenFacilityNeedToDoInitialInventoryFirstly() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_NOT_ACCEPTABLE));
    // given
    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryIdOne)
        .programId(programId)
        .facilityId(facilityId)
        .build();
    // when
    siglusPhysicalInventoryService.createAndSpiltNewDraftForOneProgram(
        physicalInventoryDto, 2, "product", false);

  }

  @Test
  public void shouldGetCorrespondingSubPhysicalInventoryDtoWithLegalSubDraftIds() {
    // given
    PhysicalInventorySubDraft physicalInventorySubDraft = PhysicalInventorySubDraft
        .builder()
        .physicalInventoryId(physicalInventoryIdOne)
        .num(1)
        .build();

    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = Collections.singletonList(
        PhysicalInventoryLineItemDto
            .builder()
            .orderableId(orderableId)
            .lotId(lotId)
            .build());

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(physicalInventoryIdOne)
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
    when(inventoryController.getPhysicalInventory(physicalInventoryIdOne))
        .thenReturn(physicalInventoryDto);
    doNothing().when(physicalInventoryService).checkPermission(programId, facilityId);
    when(inventoryController
        .searchPhysicalInventory(programId, facilityId, true)).thenReturn(makeResponseEntity(Collections.singletonList(
        physicalInventoryDto
    )));
    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(Collections.singletonList(physicalInventoryIdOne)))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryIdOne))
        .thenReturn(Collections.singletonList(physicalInventoryLineItemsExtension));
    when(orderableRepository.findLatestById(orderableId)).thenReturn(Optional.of(new Orderable()));
    List<PhysicalInventoryExtension> physicalInventoryExtensions = Collections.singletonList(
        PhysicalInventoryExtension.builder()
            .category(SINGLE_PROGRAM).locationOption(LocationManagementOption.BY_PRODUCT).build());
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(any())).thenReturn(physicalInventoryExtensions);
    List<UUID> subDraftIds = Collections.singletonList(subDraftIdOne);
    // when
    PhysicalInventoryDto actualPhysicalInventoryDto = siglusPhysicalInventoryService
        .getPhysicalInventoryDtoBySubDraftIds(subDraftIds);
    // then
    assertEquals(physicalInventoryDto.getProgramId(), actualPhysicalInventoryDto.getProgramId());
    assertEquals(physicalInventoryDto.getFacilityId(), actualPhysicalInventoryDto.getFacilityId());
    assertEquals(physicalInventoryDto.getId(), actualPhysicalInventoryDto.getId());
    assertEquals(physicalInventoryDto.getLineItems(), actualPhysicalInventoryDto.getLineItems());
  }

  @Test
  public void shouldThrowExceptionWhenThereIsNoSubDraftListForAnyProgram() {
    // then
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(containsString("there is no subDraft for any record"));

    // given
    mockGetViaMmcProgram();

    Set<UUID> supportedPrograms = new HashSet<>();
    supportedPrograms.add(programIdOne);
    supportedPrograms.add(programIdTwo);
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(supportedPrograms);
    doNothing().when(physicalInventoryService).checkPermission(programIdOne, facilityId);
    doNothing().when(physicalInventoryService).checkPermission(programIdTwo, facilityId);

    // when
    siglusPhysicalInventoryService.getSubDraftListForAllPrograms(facilityId, true);
  }

  @Test
  public void shouldGetExceptionWhenConflictForAllProduct() {
    exception.expect(PermissionMessageException.class);
    exception.expectMessage("stockmanagement.error.authorization.program.not.supported");
    siglusPhysicalInventoryService.checkConflictForAllPrograms(facilityId, null);
  }

  @Test
  public void shouldGetExceptionWhenConflictForOneProgram() {
    exception.expect(PermissionMessageException.class);
    exception.expectMessage("stockmanagement.error.authorization.program.not.supported");
    siglusPhysicalInventoryService.checkConflictForOneProgram(facilityId, programId, null);
  }

  @Test
  public void shouldGetConflictWhenClickAllProductWhileOneProgramHaveDraft() {
    // given
    HashSet<UUID> supportedPrograms = Sets.newHashSet(programIdOne);
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(supportedPrograms);
    when(siglusPhysicalInventoryRepository.queryAllDraftByFacility(facilityId))
        .thenReturn(newArrayList(SiglusPhysicalInventoryBriefDto.builder()
            .category(SINGLE_PROGRAM)
            .programId(programIdOne)
            .build()));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForAllPrograms(facilityId, null);
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
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryIdOne))
        .thenReturn(Lists.newArrayList(physicalInventoryExtension));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForAllPrograms(facilityId, null);

    // then
    assertTrue(physicalInventoryValidationDto.isCanStartInventory());
  }

  @Test
  public void shouldGetConflictWhenClickOneProgramWhileAllProductHaveDraft() {
    // given
    HashSet<UUID> supportedPrograms = Sets.newHashSet(programIdOne);
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(
        supportedPrograms);
    when(siglusPhysicalInventoryRepository.queryAllDraftByFacility(facilityId))
        .thenReturn(newArrayList(SiglusPhysicalInventoryBriefDto.builder()
            .category(ALL_PROGRAM)
            .build()));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForOneProgram(facilityId, programIdOne, null);

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
        programIdOne, facilityId, true)).thenReturn(null);
    when(physicalInventoryExtensionRepository.findByPhysicalInventoryId(physicalInventoryIdOne))
        .thenReturn(Lists.newArrayList(physicalInventoryExtension));

    // when
    PhysicalInventoryValidationDto physicalInventoryValidationDto = siglusPhysicalInventoryService
        .checkConflictForOneProgram(facilityId, programIdOne, null);

    // then
    assertTrue(physicalInventoryValidationDto.isCanStartInventory());
  }

  @Test
  public void shouldReturnPhysicalInventoryIdsWhenProgramIdIsAllProductProgramId() {
    // given
    mockGetViaMmcProgram();
    Set<UUID> programIds = new HashSet<>();
    programIds.add(programIdOne);
    programIds.add(programIdTwo);
    when(supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds()).thenReturn(programIds);
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
        programIdOne, facilityId, true)).thenReturn(String.valueOf(physicalInventoryIdOne));
    when(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
        programIdTwo, facilityId, true)).thenReturn(String.valueOf(physicalInventoryIdTwo));
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder().programId(ALL_PRODUCTS_PROGRAM_ID)
        .facilityId(facilityId).build();
    // when
    List<UUID> physicalInventoryIds = siglusPhysicalInventoryService.getPhysicalInventoryIds(physicalInventoryDto,
        facilityId);

    // then
    assertTrue(physicalInventoryIds.contains(physicalInventoryIdOne));
    assertTrue(physicalInventoryIds.contains(physicalInventoryIdTwo));
  }

  @Test
  public void shouldReturnPhysicalInventoryEmptyLocationLineItemByFacilityIdAndPhysicalInventoryLineItem() {
    // given
    PhysicalInventoryLineItemDto lineItem1 = PhysicalInventoryLineItemDto.builder().locationCode("11A11").build();
    PhysicalInventoryLineItemDto lineItem2 = PhysicalInventoryLineItemDto.builder().locationCode("22A22").build();
    FacilityLocations location1 = FacilityLocations.builder().locationCode("11A11").area("A").build();
    FacilityLocations location2 = FacilityLocations.builder().locationCode("22A22").area("B").build();
    FacilityLocations location3 = FacilityLocations.builder().locationCode("33A33").area("C").build();
    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(Arrays.asList(
        location1, location2, location3));
    PhysicalInventoryLineItemDto expectedPhysicalInventoryLineItemDto =
        PhysicalInventoryLineItemDto.builder().locationCode("33A33").area("C").build();

    // when
    List<PhysicalInventoryLineItemDto> physicalInventoryEmptyLocationLineItemDtos =
        siglusPhysicalInventoryService.getPhysicalInventoryEmptyLocationLineItemDtos(
            facilityId, Arrays.asList(lineItem1, lineItem2));

    // then

    assertEquals(Collections.singletonList(expectedPhysicalInventoryLineItemDto),
        physicalInventoryEmptyLocationLineItemDtos);


  }

  @Test
  public void shouldReturnSortedAndAggregateListByLocationCode() {
    // given
    PhysicalInventoryLineItemDto lineItemDto1 = PhysicalInventoryLineItemDto.builder().locationCode("11A11").build();
    PhysicalInventoryLineItemDto lineItemDto2 = PhysicalInventoryLineItemDto.builder().locationCode("11A11").build();
    PhysicalInventoryLineItemDto lineItemDto3 = PhysicalInventoryLineItemDto.builder().locationCode("22A22").build();

    List<PhysicalInventoryLineItemDto> lineItemDtosGroup1 = Arrays.asList(lineItemDto1, lineItemDto2);
    List<PhysicalInventoryLineItemDto> lineItemDtosGroup2 = Collections.singletonList(lineItemDto3);
    List<List<PhysicalInventoryLineItemDto>> expectedLineItemDtos =
        Arrays.asList(lineItemDtosGroup1, lineItemDtosGroup2);

    // when
    List<List<PhysicalInventoryLineItemDto>> lineItemDtos = siglusPhysicalInventoryService.groupByLocationCode(
        Arrays.asList(lineItemDto1, lineItemDto2, lineItemDto3));

    // then
    assertEquals(expectedLineItemDtos, lineItemDtos);
  }

  @Test
  public void shouldSavePhysicalInventoryEmptyLocationLineItemByGroupList() {
    // given
    PhysicalInventoryLineItemDto lineItemDto1 = PhysicalInventoryLineItemDto.builder().locationCode("11A11")
        .orderableId(orderableId).build();
    PhysicalInventoryLineItemDto lineItemDto2 = PhysicalInventoryLineItemDto.builder().locationCode("11A11")
        .orderableId(orderableId).build();
    PhysicalInventoryLineItemDto lineItemDto3 = PhysicalInventoryLineItemDto.builder().locationCode("22A22")
        .area("C").build();

    List<PhysicalInventoryLineItemDto> lineItemDtosGroup1 = Arrays.asList(lineItemDto1, lineItemDto2);
    List<PhysicalInventoryLineItemDto> lineItemDtosGroup2 = Collections.singletonList(lineItemDto3);
    List<List<List<PhysicalInventoryLineItemDto>>> splitGroupList = Arrays.asList(
        Collections.singletonList(lineItemDtosGroup1),
        Collections.singletonList(lineItemDtosGroup2));

    PhysicalInventorySubDraft subDraft1 = PhysicalInventorySubDraft.builder().num(1).build();
    subDraft1.setId(subDraftIdOne);
    PhysicalInventorySubDraft subDraft2 = PhysicalInventorySubDraft.builder().num(2).build();
    subDraft2.setId(subDraftIdTwo);
    List<PhysicalInventorySubDraft> subDraftList = Arrays.asList(subDraft1, subDraft2);

    List<PhysicalInventoryEmptyLocationLineItem> expectedEmptyLocationLineItems = Collections.singletonList(
        PhysicalInventoryEmptyLocationLineItem.builder().locationCode("22A22").area("C").skipped(false)
            .hasProduct(false)
            .subDraftId(subDraftIdTwo).build());

    // when
    siglusPhysicalInventoryService.associateEmptyLocation(subDraftList, splitGroupList);
    // then
    verify(physicalInventoryEmptyLocationLineItemRepository).save(expectedEmptyLocationLineItems);

  }

  private PhysicalInventory mockPhysicalInventory() {
    PhysicalInventory physicalInventory = new PhysicalInventory();
    physicalInventory.setId(physicalInventoryIdOne);
    physicalInventory.setFacilityId(facilityId);
    physicalInventory.setIsDraft(true);
    return physicalInventory;
  }

  private PhysicalInventoryExtension mockPhysicalInventoryExtensionSingleProgram() {
    PhysicalInventoryExtension physicalInventoryExtension = new PhysicalInventoryExtension();
    physicalInventoryExtension.setPhysicalInventoryId(physicalInventoryIdOne);
    physicalInventoryExtension.setCategory(SINGLE_PROGRAM);
    return physicalInventoryExtension;
  }

  private PhysicalInventoryExtension mockPhysicalInventoryExtensionAllProduct() {
    PhysicalInventoryExtension physicalInventoryExtension = new PhysicalInventoryExtension();
    physicalInventoryExtension.setPhysicalInventoryId(physicalInventoryIdOne);
    physicalInventoryExtension.setCategory(ALL_PROGRAM);
    return physicalInventoryExtension;
  }
}
