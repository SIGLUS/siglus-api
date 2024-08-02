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
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.FULL_PRODUCT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.PRODUCT_CODE;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.constant.KitConstants;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.domain.DispensableAttributes;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.AvailableOrderablesDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.SiglusOrderableDto;
import org.siglus.siglusapi.dto.SimplifyOrderablesDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.DispensableAttributesRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.repository.SiglusProgramOrderableRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.repository.dto.OrderableVersionDto;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"unused", "PMD.TooManyMethods"})
public class SiglusOrderableServiceTest {

  @InjectMocks
  private SiglusOrderableService siglusOrderableService;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private ArchivedProductRepository archivedProductRepository;

  @Mock
  private SiglusOrderableRepository siglusOrderableRepository;

  @Mock
  private ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private QueryOrderableSearchParams searchParams;

  @Mock
  private SiglusProgramOrderableRepository siglusProgramOrderableRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private DispensableAttributesRepository dispensableAttributesRepository;

  @Mock
  private ProgramOrderableRepository programOrderableRepository;

  @Mock
  private ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Mock
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  private Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID draftId = UUID.randomUUID();

  private final UUID inputProgramId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID initialDraftId = UUID.randomUUID();

  private final UUID targetOrderableId = UUID.randomUUID();

  private final StockManagementDraft draft = StockManagementDraft.builder().build();

  private static final UUID stockCardId = UUID.randomUUID();

  private final UUID stockCardId1 = UUID.randomUUID();

  private final UUID stockCardId2 = UUID.randomUUID();
  private final UUID dispensableId = UUID.randomUUID();

  private final String productCode = "product code";

  private final UUID userId = UUID.randomUUID();

  @Before
  public void prepare() {
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    orderableDto.setPrograms(newHashSet(programOrderableDto));
    when(orderableReferenceDataService.searchOrderables(searchParams, pageable)).thenReturn(
        Pagination.getPage(newArrayList(orderableDto), pageable, 1));

    UserDto user = new UserDto();
    user.setId(userId);
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
  }

  @Test
  public void shouldReturnDataWithArchivedFalseWhenSearchOrderables() {
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(newHashSet());

    Page<OrderableDto> orderableDtoPage = siglusOrderableService
        .searchOrderables(searchParams, pageable, facilityId);

    assertEquals(1, orderableDtoPage.getContent().size());
    orderableDtoPage.getContent().forEach(orderable -> assertFalse(orderable.getArchived()));
  }

  @Test
  public void shouldReturnDataWithArchivedTrueWhenSearchOrderables() {
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(newHashSet(orderableId.toString()));

    Page<OrderableDto> orderableDtoPage = siglusOrderableService
        .searchOrderables(searchParams, pageable, facilityId);

    assertEquals(1, orderableDtoPage.getContent().size());
    orderableDtoPage.getContent().forEach(orderable -> assertTrue(orderable.getArchived()));
  }

  @Test
  public void shouldCallFindExpirationDateWhenGetOrderableExpirationDate() {
    Set<UUID> orderableIds = newHashSet(orderableId);

    siglusOrderableService.getOrderableExpirationDate(orderableIds, facilityId);

    verify(siglusOrderableRepository).findExpirationDate(orderableIds, facilityId);
  }

  @Test
  public void shouldFilterProgramWhenGetAdditionalToAdd() {
    // when
    Page<OrderableDto> orderableDtoPage = siglusOrderableService
        .additionalToAdd(programId, searchParams, pageable);

    // then
    assertEquals(0, orderableDtoPage.getContent().size());
  }

  @Test
  public void shouldFilterAdditionalOrderablesWhenGetAdditionalToAdd() {
    // given
    ProgramAdditionalOrderable additionalOrderable = ProgramAdditionalOrderable.builder()
        .additionalOrderableId(orderableId).build();
    when(programAdditionalOrderableRepository.findAllByProgramId(inputProgramId))
        .thenReturn(newArrayList(additionalOrderable));

    // when
    Page<OrderableDto> orderableDtoPage = siglusOrderableService.additionalToAdd(inputProgramId,
        searchParams, pageable);

    // then
    assertEquals(0, orderableDtoPage.getContent().size());
  }

  @Test
  public void shouldSortByFullProductNameWhenGetAdditionalToAdd() {
    // given
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programId);
    OrderableDto orderableDto1 = new OrderableDto();
    orderableDto1.setId(orderableId);
    orderableDto1.setPrograms(newHashSet(programOrderableDto));
    orderableDto1.setFullProductName("ProductNameLast");
    OrderableDto orderableDto2 = new OrderableDto();
    orderableDto2.setId(orderableId);
    orderableDto2.setPrograms(newHashSet(programOrderableDto));
    orderableDto2.setFullProductName("ProductNameFirst");
    Sort sort = new Sort(FULL_PRODUCT_NAME);
    pageable = new PageRequest(0, Integer.MAX_VALUE, sort);
    when(orderableReferenceDataService.searchOrderables(searchParams, pageable)).thenReturn(
        Pagination.getPage(newArrayList(orderableDto1, orderableDto2), pageable, 2));

    // when
    List<OrderableDto> orderableDtos = siglusOrderableService.additionalToAdd(inputProgramId,
        searchParams, pageable).getContent();

    // then
    assertEquals(2, orderableDtos.size());
    assertEquals("ProductNameFirst", orderableDtos.get(0).getFullProductName());
    assertEquals("ProductNameLast", orderableDtos.get(1).getFullProductName());
  }

  @Test
  public void shouldSortByProductCodeWhenGetAdditionalToAdd() {
    // given
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programId);
    OrderableDto orderableDto1 = new OrderableDto();
    orderableDto1.setId(orderableId);
    orderableDto1.setPrograms(newHashSet(programOrderableDto));
    orderableDto1.setProductCode("02CODE");
    OrderableDto orderableDto2 = new OrderableDto();
    orderableDto2.setId(orderableId);
    orderableDto2.setPrograms(newHashSet(programOrderableDto));
    orderableDto2.setProductCode("01CODE");
    Sort sort = new Sort(PRODUCT_CODE);
    pageable = new PageRequest(0, Integer.MAX_VALUE, sort);
    when(orderableReferenceDataService.searchOrderables(searchParams, pageable)).thenReturn(
        Pagination.getPage(newArrayList(orderableDto1, orderableDto2), pageable, 2));

    // when
    List<OrderableDto> orderableDtos = siglusOrderableService.additionalToAdd(inputProgramId,
        searchParams, pageable).getContent();

    // then
    assertEquals(2, orderableDtos.size());
    assertEquals("01CODE", orderableDtos.get(0).getProductCode());
    assertEquals("02CODE", orderableDtos.get(1).getProductCode());
  }

  @Test
  public void shouldGetOrderableByCode() {
    // given
    OrderableDto orderableDto1 = new OrderableDto();
    orderableDto1.setProductCode("0CODE");
    OrderableDto orderableDto2 = new OrderableDto();
    orderableDto2.setProductCode("10CODE");
    when(orderableReferenceDataService.searchOrderables(any(), any())).thenReturn(
        Pagination.getPage(newArrayList(orderableDto1, orderableDto2), pageable, 2));

    // when
    OrderableDto orderableDto = siglusOrderableService.getOrderableByCode("0CODE");

    // then
    assertEquals("0CODE", orderableDto.getProductCode());
  }

  @Test
  public void shouldSearchDeduplicatedOrderables() {
    StockManagementDraftLineItem lineItem = StockManagementDraftLineItem.builder()
        .orderableId(orderableId).build();
    draft.setInitialDraftId(initialDraftId);
    draft.setLineItems(newArrayList(lineItem));
    searchParams.setIds(newHashSet(orderableId, targetOrderableId));

    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(targetOrderableId);
    orderableDto.setPrograms(newHashSet(programOrderableDto));
    orderableDto.setFullProductName("ProductNameLast");

    when(stockManagementDraftRepository.findOne(draftId)).thenReturn(draft);
    when(stockManagementDraftRepository.findByInitialDraftId(initialDraftId))
        .thenReturn(newArrayList(draft));
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(newHashSet());

    when(orderableReferenceDataService.searchOrderables(searchParams, pageable)).thenReturn(
        Pagination.getPage(newArrayList(orderableDto), pageable, 1));

    Page<OrderableDto> orderableDtos = siglusOrderableService
        .searchDeduplicatedOrderables(draftId, searchParams, pageable, facilityId);

    assertEquals(1, orderableDtos.getContent().size());
  }

  @Test
  public void shouldGetAllProgramOrderables() {
    // given
    when(siglusProgramOrderableRepository.findAllMaxVersionProgramOrderableDtos()).thenReturn(Lists.newArrayList());

    // when
    siglusOrderableService.getAllProgramOrderableDtos();

    // then
    verify(siglusProgramOrderableRepository, times(1)).findAllMaxVersionProgramOrderableDtos();
  }

  @Test
  public void shouldGetAllProductsForOneFacility() {
    // given
    UserDto user = new UserDto();
    user.setId(UUID.randomUUID());
    user.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(mockStockCard()));
    LocalDate outTimeRangeDate = LocalDate.now().minusMonths(12);
    CalculatedStockOnHand calculatedStockOnHand1 = new CalculatedStockOnHand();
    calculatedStockOnHand1.setStockOnHand(0);
    calculatedStockOnHand1.setOccurredDate(outTimeRangeDate);
    calculatedStockOnHand1.setStockCardId(stockCardId1);
    LocalDate inTimeRangeDate = LocalDate.now().minusMonths(2);
    CalculatedStockOnHand calculatedStockOnHand2 = new CalculatedStockOnHand();
    calculatedStockOnHand2.setStockOnHand(120);
    calculatedStockOnHand2.setOccurredDate(inTimeRangeDate);
    calculatedStockOnHand2.setStockCardId(stockCardId2);
    when(calculatedStockOnHandRepository.findLatestStockOnHands(any(), any())).thenReturn(Arrays.asList(
        calculatedStockOnHand1, calculatedStockOnHand2
    ));
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId))).thenReturn(buildMockOrderables());
    when(siglusProgramOrderableRepository.findAllMaxVersionProgramOrderableDtos()).thenReturn(Lists.newArrayList());
    when(dispensableAttributesRepository.findAll(Lists.newArrayList(dispensableId)))
        .thenReturn(Lists.newArrayList(buildMockDispensable()));

    // when
    List<AvailableOrderablesDto> availableOrderables = siglusOrderableService.getAvailableOrderablesByFacility(
        true, null);

    // then
    assertEquals(Lists.newArrayList(), availableOrderables);
  }

  @Test
  public void shouldGetAllProductToCode() {
    // given
    when(orderableReferenceDataService.searchOrderables(any(), any())).thenReturn(
        Pagination.getPage(buildMockOrderableDtos(), pageable, 1));
    HashMap<UUID, String> expectedResult = Maps.newHashMap();
    expectedResult.put(orderableId, productCode);

    // when
    Map<UUID, String> actualResult = siglusOrderableService.getAllProductIdToCode();

    // then
    assertEquals(expectedResult, actualResult);
  }

  @Test
  public void shouldGetAllSimplifyOrderablesDtoWhenThereIsNoDraftIdAndArchivedProduct() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setFullProductName("AAA");
    orderableDto.setId(targetOrderableId);
    OrderableDto orderableDtoTwo = new OrderableDto();
    orderableDtoTwo.setId(orderableId);
    orderableDtoTwo.setFullProductName("BBB");
    when(orderableReferenceDataService.searchOrderables(any(), any())).thenReturn(
        Pagination.getPage(Arrays.asList(orderableDto, orderableDtoTwo), pageable, 1));

    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(programId));
    when(programOrderableRepository.countByProgramId(programId)).thenReturn(Long.valueOf(1));

    org.openlmis.stockmanagement.dto.referencedata.OrderableDto orderableDto1 =
        org.openlmis.stockmanagement.dto.referencedata.OrderableDto.builder().id(orderableId).build();
    ApprovedProductDto approvedProductDto = new ApprovedProductDto(orderableDto1);
    OrderablesAggregator orderablesAggregator = new OrderablesAggregator(Collections.singletonList(approvedProductDto));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programId, Collections.emptyList()))
        .thenReturn(orderablesAggregator);

    when(archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId)).thenReturn(Collections.emptySet());

    List<SimplifyOrderablesDto> expectedSimplifyOrderablesDtos = Collections.singletonList(
        SimplifyOrderablesDto.builder()
            .orderableId(orderableId)
            .isKit(Boolean.FALSE)
            .archived(Boolean.FALSE)
            .fullProductName("BBB")
            .build());

    // when
    List<SimplifyOrderablesDto> simplifyOrderablesDtos = siglusOrderableService.searchOrderablesDropDownList(null);

    // then
    assertEquals(expectedSimplifyOrderablesDtos, simplifyOrderablesDtos);
  }

  @Test
  public void shouldGetSimplifyOrderablesDtoWithArchivedWhenThereIsArchivedProduct() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(targetOrderableId);
    OrderableDto orderableDtoTwo = new OrderableDto();
    orderableDtoTwo.setId(orderableId);
    when(orderableReferenceDataService.searchOrderables(any(), any())).thenReturn(
        Pagination.getPage(Arrays.asList(orderableDto, orderableDtoTwo), pageable, 1));

    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(programId));
    when(programOrderableRepository.countByProgramId(programId)).thenReturn(Long.valueOf(1));

    org.openlmis.stockmanagement.dto.referencedata.OrderableDto orderableDto1 =
        org.openlmis.stockmanagement.dto.referencedata.OrderableDto.builder().id(orderableId).build();
    ApprovedProductDto approvedProductDto = new ApprovedProductDto(orderableDto1);
    OrderablesAggregator orderablesAggregator = new OrderablesAggregator(Collections.singletonList(approvedProductDto));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programId, Collections.emptyList()))
        .thenReturn(orderablesAggregator);

    when(archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId)).thenReturn(Collections.singleton(orderableId.toString()));

    List<SimplifyOrderablesDto> expectedSimplifyOrderablesDtos = Collections.singletonList(
        SimplifyOrderablesDto.builder()
            .orderableId(orderableId)
            .isKit(Boolean.FALSE)
            .archived(Boolean.TRUE)
            .build()
    );

    // when
    List<SimplifyOrderablesDto> simplifyOrderablesDtos = siglusOrderableService.searchOrderablesDropDownList(null);

    // then
    assertEquals(expectedSimplifyOrderablesDtos, simplifyOrderablesDtos);
  }

  @Test
  public void shouldGetFilteredConflictsSimplifyOrderablesDtoWhenThereIsDraftId() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(targetOrderableId);
    OrderableDto orderableDtoTwo = new OrderableDto();
    orderableDtoTwo.setId(orderableId);
    when(orderableReferenceDataService.searchOrderables(any(), any())).thenReturn(
        Pagination.getPage(Arrays.asList(orderableDto, orderableDtoTwo), pageable, 1));

    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(programId));
    when(programOrderableRepository.countByProgramId(programId)).thenReturn(Long.valueOf(1));

    org.openlmis.stockmanagement.dto.referencedata.OrderableDto orderableDto1 =
        org.openlmis.stockmanagement.dto.referencedata.OrderableDto.builder().id(orderableId).build();
    ApprovedProductDto approvedProductDto = new ApprovedProductDto(orderableDto1);
    org.openlmis.stockmanagement.dto.referencedata.OrderableDto orderableDto2 =
        org.openlmis.stockmanagement.dto.referencedata.OrderableDto.builder().id(targetOrderableId).build();
    ApprovedProductDto approvedProductDto2 = new ApprovedProductDto(orderableDto2);
    OrderablesAggregator orderablesAggregator = new OrderablesAggregator(
        Arrays.asList(approvedProductDto, approvedProductDto2));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programId, Collections.emptyList()))
        .thenReturn(orderablesAggregator);

    when(archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId)).thenReturn(Collections.emptySet());

    StockManagementDraft stockManagementDraftTwo = StockManagementDraft.builder()
        .initialDraftId(initialDraftId).build();
    when(stockManagementDraftRepository.findOne(draftId)).thenReturn(stockManagementDraftTwo);

    StockManagementDraftLineItem stockManagementDraftLineItem = StockManagementDraftLineItem.builder()
        .orderableId(orderableId)
        .build();
    StockManagementDraft stockManagementDraft = StockManagementDraft.builder()
        .lineItems(Collections.singletonList(stockManagementDraftLineItem))
        .build();

    when(stockManagementDraftRepository.findByInitialDraftId(initialDraftId))
        .thenReturn(new ArrayList<>(Arrays.asList(stockManagementDraft, stockManagementDraftTwo)));

    List<SimplifyOrderablesDto> expectedSimplifyOrderablesDtos = Collections.singletonList(
        SimplifyOrderablesDto.builder()
            .orderableId(targetOrderableId)
            .isKit(Boolean.FALSE)
            .archived(Boolean.FALSE)
            .build()
    );

    // when
    List<SimplifyOrderablesDto> simplifyOrderablesDtos = siglusOrderableService.searchOrderablesDropDownList(draftId);

    // then
    assertEquals(expectedSimplifyOrderablesDtos, simplifyOrderablesDtos);
  }

  @Test
  public void shouldGetEmptyListWhenFindByIdsGivenIdsIsEmpty() {
    List<OrderableVersionDto> orderables = siglusOrderableService.findByIds(new ArrayList<>());

    assertEquals(0, orderables.size());
  }

  @Test
  public void shouldGetOrderablesWhenFindByIdsGivenIdsIsNotEmpty() {
    Set<UUID> ids = Collections.singleton(UUID.randomUUID());
    when(siglusOrderableRepository.findOrderablesByIds(ids))
        .thenReturn(Collections.singletonList(new OrderableVersionDto()));

    List<OrderableVersionDto> orderables = siglusOrderableService.findByIds(ids);

    assertEquals(1, orderables.size());
  }

  @Test
  public void shouldGetEmptyListWhenFindLatestVersionByIdsGivenIdsIsEmpty() {
    List<OrderableVersionDto> orderables = siglusOrderableService.findLatestVersionByIds(new ArrayList<>());

    assertEquals(0, orderables.size());
  }

  @Test
  public void shouldGetOrderablesWhenFindLatestVersionByIdsGivenIdsIsNotEmpty() {
    Set<UUID> ids = Collections.singleton(UUID.randomUUID());
    when(siglusOrderableRepository.findLatestOrderablesByIds(ids))
        .thenReturn(Collections.singletonList(new OrderableVersionDto()));

    List<OrderableVersionDto> orderables = siglusOrderableService.findLatestVersionByIds(ids);

    assertEquals(1, orderables.size());
  }

  @Test
  public void shouldSuccessWhenFindProgramOrderablesMaxVersionByOrderableIds() {
    Set<UUID> ids = Collections.singleton(UUID.randomUUID());

    siglusOrderableService.findProgramOrderablesMaxVersionByOrderableIds(ids);

    verify(siglusProgramOrderableRepository).findMaxVersionProgramOrderableDtosByOrderableIds(ids);
  }

  @Test
  public void shouldSuccessWhenFindAllKitOrderableIds() {
    siglusOrderableService.findAllKitOrderableIds();

    verify(siglusOrderableRepository).findByProductCodeCodeIn(KitConstants.ALL_KITS);
  }

  @Test
  public void shouldGetEmptyListWhenFindByOrderableIdsGivenIdsIsEmpty() {
    List<SiglusOrderableDto> result = siglusOrderableService.findByOrderableIds(new ArrayList<>());

    assertEquals(0, result.size());
  }

  @Test
  public void shouldSuccessWhenFindByOrderableIdsGivenIdsIsNotEmpty() {
    ProgramOrderablesExtension extension = new ProgramOrderablesExtension();
    extension.setOrderableId(orderableId);
    extension.setUnit("unit");
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any()))
        .thenReturn(Collections.singletonList(extension));
    OrderableVersionDto dto1 = new OrderableVersionDto();
    dto1.setId(orderableId);
    OrderableVersionDto dto2 = new OrderableVersionDto();
    dto2.setId(UUID.randomUUID());
    when(siglusOrderableRepository.findLatestOrderablesByIds(any()))
        .thenReturn(newArrayList(dto1, dto2));

    Set<UUID> ids = Collections.singleton(orderableId);

    List<SiglusOrderableDto> orderableDtos = siglusOrderableService.findByOrderableIds(ids);

    assertEquals(2, orderableDtos.size());
  }

  private List<OrderableDto> buildMockOrderableDtos() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    return Lists.newArrayList(orderableDto);
  }

  private StockCard mockStockCard() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    stockCard.setFacilityId(facilityId);
    stockCard.setOrderableId(orderableId);
    return stockCard;
  }

  private List<Orderable> buildMockOrderables() {
    Orderable orderable = new Orderable(Code.code(productCode), null, 0, 0,
        Boolean.FALSE, orderableId, 0L);
    return Lists.newArrayList(orderable);
  }

  private DispensableAttributes buildMockDispensable() {
    DispensableAttributes dispensableAttributes = new DispensableAttributes();
    dispensableAttributes.setDispensableId(dispensableId);
    dispensableAttributes.setKey("dispensingUnit");
    dispensableAttributes.setValue("each");
    return dispensableAttributes;
  }
}
