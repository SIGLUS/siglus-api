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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_INVENTORY_CONFLICT_SUB_DRAFT;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PermissionService;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.service.client.PhysicalInventoryStockManagementService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusPhysicalInventorySubDraftServiceTest {

  @Captor
  private ArgumentCaptor<PhysicalInventoryDto> physicalInventoryDtoArgumentCaptor;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusPhysicalInventorySubDraftService siglusPhysicalInventorySubDraftService;

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
  private OrderableRepository orderableRepository;
  @Mock
  private SiglusOrderableService siglusOrderableService;


  @Mock
  private PhysicalInventoryController inventoryController;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;
  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;


  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();

  private final UUID programIdOne = UUID.randomUUID();

  private final UUID programIdTwo = UUID.randomUUID();

  private final UUID inventoryOne = UUID.randomUUID();

  private final UUID inventoryTwo = UUID.randomUUID();

  private final UUID subDraftId = UUID.randomUUID();


  private final UUID id = UUID.randomUUID();

  private final UUID physicalInventoryId = UUID.randomUUID();

  private final UUID subDraftIdOne = UUID.randomUUID();

  private final UUID subDraftIdTwo = UUID.randomUUID();

  private final String startDate = "startDate";

  private final String endDate = "endDate";

  @Test
  public void shouldCallUpdateSubDraftsWithEmptyLineItemWhenUpdateSubDrafts() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    List<UUID> subDraftIds = Lists.newArrayList(subDraftId);
    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(id)
        .lineItems(Lists.newArrayList())
        .build();

    // when
    siglusPhysicalInventorySubDraftService.updateSubDrafts(subDraftIds, physicalInventoryDto);
    verify(physicalInventorySubDraftRepository, times(2)).findAll(any(List.class));
  }

  @Test
  public void shouldCallUpdateSubDraftsWhenUpdateSubDrafts() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .programId(programId)
        .orderableId(UUID.randomUUID())
        .lotId(UUID.randomUUID())
        .build();
    List<UUID> subDraftIds = Lists.newArrayList(subDraftId);
    List<PhysicalInventorySubDraft> subDrafts = Lists.newArrayList(PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(1)
        .build(), PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(2)
        .build());
    when(physicalInventorySubDraftRepository.findAll(subDraftIds)).thenReturn(subDrafts);

    PhysicalInventoryDto oldPhysicalInventoryDto = PhysicalInventoryDto.builder()
        .id(id)
        .programId(programId)
        .lineItems(Lists.newArrayList(lineItemDtoOne))
        .build();

    when(siglusPhysicalInventoryService.getPhysicalInventory(physicalInventoryId)).thenReturn(oldPhysicalInventoryDto);

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(id)
        .programId(programId)
        .lineItems(Lists.newArrayList(lineItemDtoOne))
        .build();

    // when
    siglusPhysicalInventorySubDraftService.updateSubDrafts(subDraftIds, physicalInventoryDto);
    // then
    verify(siglusPhysicalInventoryService, times(1)).getPhysicalInventory(physicalInventoryId);
  }


  @Test
  public void shouldThrowConflictWhenUpdateSubDraftsWithConflict() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_INVENTORY_CONFLICT_SUB_DRAFT));
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    List<UUID> subDraftIds = Lists.newArrayList(subDraftId);
    List<PhysicalInventorySubDraft> subDrafts = Lists.newArrayList(PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(1)
        .build(), PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(2)
        .build());
    when(physicalInventorySubDraftRepository.findAll(subDraftIds)).thenReturn(subDrafts);
    List<PhysicalInventoryLineItemsExtension> physicalInventories = Lists.newArrayList(
        PhysicalInventoryLineItemsExtension.builder()
            .physicalInventoryId(physicalInventoryId)
            .orderableId(orderableId)
            .lotId(lotId)
            .build(), PhysicalInventoryLineItemsExtension.builder()
            .physicalInventoryId(physicalInventoryId)
            .orderableId(UUID.randomUUID())
            .lotId(UUID.randomUUID())
            .build()
    );

    when(lineItemsExtensionRepository.findByPhysicalInventoryIdIn(Lists.newArrayList(physicalInventoryId))).thenReturn(
        physicalInventories);
    OrderableDto dto = new OrderableDto();
    dto.setId(orderableId);
    dto.setFullProductName("orderable1 name");

    List<OrderableDto> orderables = Lists.newArrayList(dto);
    when(siglusOrderableService.getAllProducts()).thenReturn(orderables);
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .programId(programId)
        .orderableId(orderableId)
        .lotId(lotId)
        .build();
    PhysicalInventoryDto oldPhysicalInventoryDto = PhysicalInventoryDto.builder()
        .id(id)
        .programId(programId)
        .lineItems(Lists.newArrayList(lineItemDtoOne))
        .build();

    when(siglusPhysicalInventoryService.getPhysicalInventory(physicalInventoryId)).thenReturn(oldPhysicalInventoryDto);

    PhysicalInventoryDto physicalInventoryDto = PhysicalInventoryDto.builder()
        .id(id)
        .programId(programId)
        .lineItems(Lists.newArrayList(lineItemDtoOne))
        .build();


    // when
    siglusPhysicalInventorySubDraftService.updateSubDrafts(subDraftIds, physicalInventoryDto);
    // then
    verify(siglusPhysicalInventoryService, times(2)).getPhysicalInventory(physicalInventoryId);


  }

  @Test
  public void shouldCallDeleteSubDraftsWhenDeleteSubDrafts() {
    UUID orderableId2 = UUID.randomUUID();
    UUID lotId2 = UUID.randomUUID();
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));
    PhysicalInventoryLineItemDto lineItemDtoOne = PhysicalInventoryLineItemDto.builder()
        .programId(programId)
        .orderableId(orderableId2)
        .lotId(lotId2)
        .build();
    List<UUID> subDraftIds = Lists.newArrayList(subDraftId);
    List<PhysicalInventorySubDraft> subDrafts = Lists.newArrayList(PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(1)
        .build(), PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(2)
        .build());
    when(physicalInventorySubDraftRepository.findAll(subDraftIds)).thenReturn(subDrafts);

    PhysicalInventoryDto oldPhysicalInventoryDto = PhysicalInventoryDto.builder()
        .id(id)
        .programId(programId)
        .lineItems(Lists.newArrayList(lineItemDtoOne))
        .build();

    when(siglusPhysicalInventoryService.getPhysicalInventory(physicalInventoryId)).thenReturn(oldPhysicalInventoryDto);
    List<PhysicalInventoryLineItemsExtension> physicalInventories = Lists.newArrayList(
        PhysicalInventoryLineItemsExtension.builder()
            .physicalInventoryId(physicalInventoryId)
            .orderableId(orderableId)
            .lotId(lotId)
            .subDraftId(subDraftId)
            .isInitial(true)
            .build(),
        PhysicalInventoryLineItemsExtension.builder()
            .physicalInventoryId(physicalInventoryId)
            .orderableId(orderableId2)
            .lotId(lotId2)
            .subDraftId(subDraftId)
            .isInitial(true)
            .build()
    );
    when(lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryId)).thenReturn(physicalInventories);
    StockCardSummaryV2Dto stockCardSummaryV2Dto = new StockCardSummaryV2Dto();
    ObjectReferenceDto objectReferenceDto = new ObjectReferenceDto("urlxx", "resxx", orderableId);
    ObjectReferenceDto lot = new ObjectReferenceDto("urlxx", "resxx", lotId);
    stockCardSummaryV2Dto.setOrderable(objectReferenceDto);
    Set<CanFulfillForMeEntryDto> canFulfillForMe = new HashSet<>();
    CanFulfillForMeEntryDto canFulfillForMeEntryDto = new CanFulfillForMeEntryDto();
    canFulfillForMeEntryDto.setOrderable(objectReferenceDto);
    canFulfillForMeEntryDto.setLot(lot);
    canFulfillForMe.add(canFulfillForMeEntryDto);
    stockCardSummaryV2Dto.setCanFulfillForMe(canFulfillForMe);
    List<StockCardSummaryV2Dto> stockSummaries = new ArrayList<>();
    stockSummaries.add(stockCardSummaryV2Dto);

    when(siglusStockCardSummariesService.findAllProgramStockSummaries()).thenReturn(stockSummaries);

    // when
    siglusPhysicalInventorySubDraftService.deleteSubDrafts(subDraftIds);
    // then
    verify(physicalInventorySubDraftRepository).save(any(List.class));

  }

  @Test
  public void shouldCallSubmitSubDraftsWhenSubmitSubDrafts() {
    List<UUID> subDraftIds = Lists.newArrayList(subDraftId);
    List<PhysicalInventorySubDraft> subDrafts = Lists.newArrayList(PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(1)
        .build(), PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).num(2)
        .build());
    when(physicalInventorySubDraftRepository.findAll(subDraftIds)).thenReturn(subDrafts);

    // when
    siglusPhysicalInventorySubDraftService.submitSubDrafts(subDraftIds);
    // then
    verify(physicalInventorySubDraftRepository).save(any(List.class));

  }

  @Test
  public void shouldCallDeleteSubDraftsWhenDeleteSubDraftsFindAllEmpty() {
    // given
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    List<UUID> subDraftIds = Lists.newArrayList(subDraftId);

    when(physicalInventorySubDraftRepository.findAll(subDraftIds)).thenReturn(new ArrayList<>());

    // when
    siglusPhysicalInventorySubDraftService.deleteSubDrafts(subDraftIds);
    verify(physicalInventorySubDraftRepository).findAll(any(List.class));

  }

}
