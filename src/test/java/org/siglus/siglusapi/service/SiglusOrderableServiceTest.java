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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.siglusapi.domain.DispensableAttributes;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.AvailableOrderablesDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.DispensableAttributesRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesRepository;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
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
  private ProgramOrderablesRepository programOrderablesRepository;

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

  @Before
  public void prepare() {
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setPrograms(newHashSet(programOrderableDto));
    when(orderableReferenceDataService.searchOrderables(searchParams, pageable)).thenReturn(
        Pagination.getPage(newArrayList(orderableDto), pageable, 1));
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
    when(programOrderablesRepository.findAllMaxVersionProgramOrderableDtos()).thenReturn(Lists.emptyList());

    // when
    siglusOrderableService.getAllProgramOrderableDtos();

    // then
    verify(programOrderablesRepository, times(1)).findAllMaxVersionProgramOrderableDtos();
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
    when(programOrderablesRepository.findAllMaxVersionProgramOrderableDtos()).thenReturn(Lists.newArrayList());
    when(dispensableAttributesRepository.findAll(Lists.newArrayList(dispensableId)))
        .thenReturn(Lists.newArrayList(buildMockDispensable()));

    // when
    List<AvailableOrderablesDto> availableOrderables = siglusOrderableService.getAvailableOrderablesByFacility(
        true, null);

    // then
    assertEquals(Lists.newArrayList(), availableOrderables);
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
