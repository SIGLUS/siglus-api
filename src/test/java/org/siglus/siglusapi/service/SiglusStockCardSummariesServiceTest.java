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
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.hibernate.validator.internal.util.CollectionHelper.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.ORDERABLE_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.service.referencedata.PermissionStrings;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.web.Pagination;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotLocationSohDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.StockCardDetailsDto;
import org.siglus.siglusapi.dto.StockCardSummaryWithLocationDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusStockCardStockManagementService;
import org.siglus.siglusapi.testutils.CanFulfillForMeEntryDtoDataBuilder;
import org.siglus.siglusapi.testutils.OrderableDtoDataBuilder;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusStockCardSummariesServiceTest {

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private PermissionService permissionService;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private PermissionStrings.Handler permissionStringsHandler;

  @Mock
  private SiglusStockCardStockManagementService siglusStockManagementService;

  @Mock
  private StockCardSummariesService stockCardSummariesService;

  @Mock
  private StockCardSummariesV2DtoBuilder stockCardSummariesV2DtoBuilder;

  @Mock
  private ProgramOrderableRepository programOrderableRepository;

  @Mock
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Mock
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private SiglusLotReferenceDataService siglusLotReferenceDataService;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;

  @InjectMocks
  private SiglusStockCardSummariesService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID subDraftId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID physicalInventoryId = UUID.randomUUID();
  private final String rightName = "STOCK_CARDS_VIEW";
  private final UUID initialDraftId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final LotDto lotDto = new LotDto();

  private final UUID draftId = UUID.randomUUID();

  private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
  private final Pageable pageable = new PageRequest(DEFAULT_PAGE_NUMBER, Integer.MAX_VALUE);

  @Before
  public void prepare() {
    UserDto user = new UserDto();
    user.setId(userId);
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    Set<PermissionStringDto> dtos = new HashSet<>();
    dtos.add(PermissionStringDto.create(rightName, facilityId, programId));
    when(permissionStringsHandler.get())
        .thenReturn(dtos);
    when(permissionService.getPermissionStrings(userId))
        .thenReturn(permissionStringsHandler);
    when(archiveProductService.searchArchivedProductsByFacilityId(facilityId))
        .thenReturn(new HashSet<>());
  }

  @Test
  public void shouldNUllIfStockCardIsEmpty() {
    // given
    when(siglusStockManagementService.search(any(StockCardSummariesV2SearchParams.class),
        any(Pageable.class))).thenReturn(Pagination.getPage(Collections.emptyList()));

    // when
    Page<StockCardSummaryV2Dto> resultSummaries =
        service.findSiglusStockCard(getProgramsParms(), null, pageable);

    // then
    assertEquals(true, resultSummaries.getContent().isEmpty());
  }

  @Test
  public void shouldHaveTwoValueIfStockCardHaveTwoValue() {
    // given
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(newArrayList(createSummaryV2Dto(UUID.randomUUID(), 10),
            createSummaryV2Dto(UUID.randomUUID(), 15)));

    // when
    Page<StockCardSummaryV2Dto> resultSummaries =
        service.findSiglusStockCard(getProgramsParms(), Collections.emptyList(), pageable);

    // then
    assertEquals(2, resultSummaries.getContent().size());
  }

  @Test
  public void shouldExcludeArchivedProductIfSearchExcludeArchived() {
    // given
    MultiValueMap<String, String> params = getProgramsParms();
    params.add("excludeArchived", Boolean.toString(true));
    UUID firstOrderableId = UUID.randomUUID();
    Set<String> archivedProduct = new HashSet<>();
    archivedProduct.add(firstOrderableId.toString());
    when(archiveProductService.searchArchivedProductsByFacilityId(facilityId))
        .thenReturn(archivedProduct);
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    StockCardSummaryV2Dto summaryV2Dto = createSummaryV2Dto(firstOrderableId, 15);
    StockCardSummaryV2Dto summaryV2Dto2 = createSummaryV2Dto(UUID.randomUUID(), 20);
    StockCardSummaryV2Dto summaryV2Dto3 = createSummaryV2Dto(UUID.randomUUID(), 20);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(Arrays.asList(summaryV2Dto, summaryV2Dto2, summaryV2Dto3));

    // when
    Page<StockCardSummaryV2Dto> resultSummaries = service.findSiglusStockCard(params, null, pageable);

    // then
    assertEquals(2, resultSummaries.getContent().size());
    assertEquals(summaryV2Dto2, resultSummaries.getContent().get(0));
  }

  @Test
  public void shouldGetArchivedProductIfSearchArchived() {
    // given
    MultiValueMap<String, String> params = getProgramsParms();
    params.add("archivedOnly", Boolean.toString(true));
    UUID firstOrderableId = UUID.randomUUID();
    Set<String> archivedProduct = new HashSet<>();
    archivedProduct.add(firstOrderableId.toString());
    when(archiveProductService.searchArchivedProductsByFacilityId(facilityId))
        .thenReturn(archivedProduct);

    // when
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    StockCardSummaryV2Dto summaryV2Dto = createSummaryV2Dto(firstOrderableId, 15);
    StockCardSummaryV2Dto summaryV2Dto2 = createSummaryV2Dto(UUID.randomUUID(), 20);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(Arrays.asList(summaryV2Dto, summaryV2Dto2));

    // then
    Pageable pageable = new PageRequest(DEFAULT_PAGE_NUMBER, Integer.MAX_VALUE);
    Page<StockCardSummaryV2Dto> resultSummaries = service.findSiglusStockCard(params, null, pageable);
    assertEquals(1, resultSummaries.getContent().size());
  }

  @Test
  public void shouldReturnSpecifiedOrderablesIfOrderableIdsNotEmpty() {
    // given
    StockCardSummaryV2Dto summaryV2Dto = createSummaryV2Dto(orderableId, 15);
    StockCardSummaryV2Dto summaryV2Dto2 = createSummaryV2Dto(UUID.randomUUID(), 20);
    List<StockCardSummaryV2Dto> dtos = new ArrayList<>();
    dtos.addAll(Arrays.asList(summaryV2Dto, summaryV2Dto2));
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(dtos);
    MultiValueMap<String, String> params = getProgramsParms();
    params.add(ORDERABLE_ID, orderableId.toString());

    // when
    Page<StockCardSummaryV2Dto> resultSummaries = service.findSiglusStockCard(params, null, pageable);

    // then
    assertEquals(1, resultSummaries.getContent().size());
  }

  @Test
  public void shouldReturnWhenIfSubDraftIdsIsNotEmpty() {
    // given
    StockCardSummaryV2Dto summaryV2Dto = createSummaryV2Dto(orderableId, 15);
    StockCardSummaryV2Dto summaryV2Dto2 = createSummaryV2Dto(UUID.randomUUID(), 20);
    List<StockCardSummaryV2Dto> dtos = new ArrayList<>();
    dtos.addAll(Arrays.asList(summaryV2Dto, summaryV2Dto2));
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(dtos);
    MultiValueMap<String, String> params = getProgramsParms();
    params.add(ORDERABLE_ID, orderableId.toString());
    when(physicalInventorySubDraftRepository.findOne(subDraftId)).thenReturn(PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).build());

    // when
    Page<StockCardSummaryV2Dto> resultSummaries = service.findSiglusStockCard(params,
        Lists.newArrayList(subDraftId), pageable);

    // then
    assertEquals(1, resultSummaries.getContent().size());
  }

  @Test
  public void shouldReturnWhenFilterLineItemsExtension() {
    // given
    StockCardSummaryV2Dto summaryV2Dto = createSummaryV2Dto(orderableId, 15);
    StockCardSummaryV2Dto summaryV2Dto2 = createSummaryV2Dto(UUID.randomUUID(), 20);
    List<StockCardSummaryV2Dto> dtos = new ArrayList<>();
    dtos.addAll(Arrays.asList(summaryV2Dto, summaryV2Dto2));
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(dtos);
    MultiValueMap<String, String> params = getProgramsParms();
    params.add(ORDERABLE_ID, orderableId.toString());
    when(physicalInventorySubDraftRepository.findOne(subDraftId)).thenReturn(PhysicalInventorySubDraft.builder()
        .physicalInventoryId(physicalInventoryId).build());

    // when
    Page<StockCardSummaryV2Dto> resultSummaries = service.findSiglusStockCard(params,
        Lists.newArrayList(subDraftId), pageable);

    // then
    assertEquals(1, resultSummaries.getContent().size());
  }

  @Test
  public void shouldReturnStockCardDetailsDtoByGroup() {
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(newArrayList(createSummaryV2Dto(orderableId, 10)));

    StockManagementDraftLineItem lineItem = StockManagementDraftLineItem.builder().orderableId(orderableId).build();
    StockManagementDraft stockManagementDraft = StockManagementDraft.builder()
        .lineItems(newArrayList(lineItem))
        .initialDraftId(initialDraftId)
        .build();
    when(stockManagementDraftRepository.findOne(subDraftId)).thenReturn(stockManagementDraft);
    when(stockManagementDraftRepository.findByInitialDraftId(initialDraftId))
        .thenReturn(newArrayList(stockManagementDraft));
    when(siglusLotReferenceDataService.findByIds(newArrayList(lotId))).thenReturn(newArrayList(lotDto));
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(new LinkedMultiValueMap());
    searchParams.setIds(newHashSet(newArrayList(orderableId, orderableId)));
    org.openlmis.referencedata.dto.OrderableDto orderableDto = new org.openlmis.referencedata.dto.OrderableDto();
    orderableDto.setId(orderableId);
    Page<org.openlmis.referencedata.dto.OrderableDto> page = Pagination.getPage(newArrayList(orderableDto));
    when(siglusOrderableService.searchOrderables(searchParams, pageable, facilityId))
        .thenReturn(page);

    List<List<StockCardDetailsDto>> stockCardDetailsDtoByGroup = service
        .getStockCardDetailsDtoByGroup(getProgramsParms(), null, subDraftId, pageable);

    assertEquals(1, stockCardDetailsDtoByGroup.size());
    assertEquals(orderableId, stockCardDetailsDtoByGroup.get(0).get(0).getOrderable().getId());
    assertEquals(lotId, stockCardDetailsDtoByGroup.get(0).get(0).getLot().getId());
    assertEquals(10, stockCardDetailsDtoByGroup.get(0).get(0).getStockOnHand().intValue());
  }

  @Test
  public void shouldReturnStockCardSummaryWithLocation() {
    StockCardSummaries summaries = new StockCardSummaries(newArrayList(), newArrayList(),
        newHashMap(), null, null);
    when(programOrderableRepository.countByProgramId(any())).thenReturn(1L);
    when(stockCardSummariesService.findStockCards(any())).thenReturn(summaries);
    when(stockCardSummariesV2DtoBuilder
        .build(any(List.class), any(List.class), any(Map.class), any(boolean.class)))
        .thenReturn(newArrayList(createSummaryV2Dto(orderableId, 10)));

    StockManagementDraftLineItem lineItem = StockManagementDraftLineItem.builder().orderableId(orderableId).build();
    StockManagementDraft stockManagementDraft = StockManagementDraft.builder()
        .lineItems(newArrayList(lineItem))
        .initialDraftId(initialDraftId)
        .build();
    when(stockManagementDraftRepository.findOne(subDraftId)).thenReturn(stockManagementDraft);
    when(stockManagementDraftRepository.findByInitialDraftId(initialDraftId))
        .thenReturn(newArrayList(stockManagementDraft));
    when(siglusLotReferenceDataService.findByIds(newArrayList(lotId))).thenReturn(newArrayList(lotDto));
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(new LinkedMultiValueMap());
    searchParams.setIds(newHashSet(newArrayList(orderableId, orderableId)));
    org.openlmis.referencedata.dto.OrderableDto orderableDto = new org.openlmis.referencedata.dto.OrderableDto();
    orderableDto.setId(orderableId);
    Page<org.openlmis.referencedata.dto.OrderableDto> page = Pagination.getPage(newArrayList(orderableDto));
    when(siglusOrderableService.searchOrderables(searchParams, pageable, facilityId))
        .thenReturn(page);
    LotLocationSohDto locationSohDto =
        LotLocationSohDto.builder()
            .identify(orderableId.toString())
            .orderableId(orderableId)
            .lotId(lotId)
            .locationCode(null)
            .stockOnHand(1)
            .build();
    when(calculatedStockOnHandByLocationRepository.getLocationSoh(any(), facilityId)).thenReturn(
        newArrayList(locationSohDto));
    List<StockCardSummaryWithLocationDto> stockCardSummaryWithLocationDtos =
        service.getStockCardSummaryWithLocationDtos(getProgramsParms(), null, subDraftId, pageable);
    assertEquals(1, stockCardSummaryWithLocationDtos.size());
    assertEquals(1, stockCardSummaryWithLocationDtos.get(0).getStockCardDetails().size());
  }

  private StockCardSummaryV2Dto createSummaryV2Dto(UUID orderableId, Integer stockOnHand) {
    StockCard stockCard = StockCard.builder()
        .stockOnHand(stockOnHand)
        .orderableId(orderableId)
        .build();
    OrderableDto orderable = new OrderableDtoDataBuilder()
        .withId(orderableId)
        .build();
    lotDto.setId(lotId);
    VersionObjectReferenceDto referenceDto = new VersionObjectReferenceDto(orderableId, "", "", 1L);
    return new StockCardSummaryV2Dto(referenceDto, asSet(
        new CanFulfillForMeEntryDtoDataBuilder()
            .buildWithStockCardAndOrderable(stockCard, orderable, lotDto)));
  }

  private MultiValueMap<String, String> getProgramsParms() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("programId", programId.toString());
    params.add(FACILITY_ID, facilityId.toString());
    params.add(RIGHT_NAME, rightName);
    return params;
  }

}
