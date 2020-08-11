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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.ORDERABLE_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import org.openlmis.stockmanagement.dto.referencedata.OrderableFulfillDto;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusStockCardSummariesServiceTest {

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockCardSummariesService stockCardSummariesService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private PermissionStrings.Handler permissionStringsHandler;

  @Mock
  private StockCardSummariesV2DtoBuilder stockCardSummariesV2DtoBuilder;

  @InjectMocks
  private SiglusStockCardSummariesService service;

  private UUID userId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private String rightName = "STOCK_CARDS_VIEW";

  private UUID orderableId = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto user = new UserDto();
    user.setId(userId);
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    Set<PermissionStringDto> dtos = new HashSet<>();
    dtos.add(PermissionStringDto.create(rightName, facilityId, programId));
    when(permissionStringsHandler.get())
        .thenReturn(dtos);
    when(permissionService.getPermissionStrings(userId))
        .thenReturn(permissionStringsHandler);
    when(archiveProductService.searchArchivedProducts(facilityId)).thenReturn(new HashSet<>());
  }

  @Test
  public void shouldNUllIfStockCardIsEmpty() {
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setStockCardsForFulfillOrderables(newArrayList());
    summaries.setOrderableFulfillMap(new HashMap<>());
    summaries.setAsOfDate(LocalDate.now());
    when(stockCardSummariesService.findStockCards(any(StockCardSummariesV2SearchParams.class)))
        .thenReturn(summaries);

    StockCardSummaries resultSummaries = service.findSiglusStockCard(getProgramsParms());

    assertEquals(true, resultSummaries.getStockCardsForFulfillOrderables().isEmpty());
  }

  @Test
  public void shouldHaveTwoValueIfStockCardHaveTwoValue() {
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setOrderableFulfillMap(new HashMap<>());
    summaries.setAsOfDate(LocalDate.now());
    StockCard stockCard = StockCard.builder()
        .stockOnHand(15)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard2 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    summaries.setStockCardsForFulfillOrderables(Arrays.asList(stockCard, stockCard2));
    when(stockCardSummariesService.findStockCards(any(StockCardSummariesV2SearchParams.class)))
        .thenReturn(summaries);

    StockCardSummaries resultSummaries = service.findSiglusStockCard(getProgramsParms());

    assertEquals(2, resultSummaries.getStockCardsForFulfillOrderables().size());
  }

  @Test
  public void shouldHaveTwoValueIfStockCardHaveTwoValueForParentProgram() {
    MultiValueMap<String, String> params = getProgramsParms();
    params.set("programId", programId.toString());
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setOrderableFulfillMap(new HashMap<>());
    summaries.setAsOfDate(LocalDate.now());
    StockCard stockCard = StockCard.builder()
        .stockOnHand(15)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard2 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    summaries.setStockCardsForFulfillOrderables(Arrays.asList(stockCard, stockCard2));
    StockCardSummariesV2SearchParams v2SearchParams = new
        StockCardSummariesV2SearchParams(getProgramsParms());
    when(stockCardSummariesService.findStockCards(v2SearchParams))
        .thenReturn(summaries);

    StockCardSummaries resultSummaries = service.findSiglusStockCard(params);

    assertEquals(2, resultSummaries.getStockCardsForFulfillOrderables().size());
  }

  @Test
  public void shouldHaveTwoValueIfStockCardHaveTwoValueForExistProgram() {
    MultiValueMap<String, String> params = getProgramsParms();
    params.set("programId", ALL_PRODUCTS_PROGRAM_ID.toString());
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setOrderableFulfillMap(new HashMap<>());
    summaries.setAsOfDate(LocalDate.now());
    StockCard stockCard = StockCard.builder()
        .stockOnHand(15)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard2 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    summaries.setStockCardsForFulfillOrderables(Arrays.asList(stockCard, stockCard2));
    StockCardSummariesV2SearchParams v2SearchParams = new
        StockCardSummariesV2SearchParams(getProgramsParms());
    when(stockCardSummariesService.findStockCards(v2SearchParams))
        .thenReturn(summaries);

    StockCardSummaries resultSummaries = service.findSiglusStockCard(params);

    assertEquals(2, resultSummaries.getStockCardsForFulfillOrderables().size());
  }

  @Test
  public void shouldExcludeArchivedProductIfSearchExcludeArchived() {
    MultiValueMap<String, String> params = getProgramsParms();
    params.add("excludeArchived", Boolean.toString(true));
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setOrderableFulfillMap(new HashMap<>());
    summaries.setAsOfDate(LocalDate.now());
    StockCard stockCard = StockCard.builder()
        .stockOnHand(15)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard2 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard3 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    summaries.setStockCardsForFulfillOrderables(Arrays.asList(stockCard, stockCard2, stockCard3));
    Set<String> archivedProduct = new HashSet<>();
    archivedProduct.add(stockCard.getOrderableId().toString());
    when(archiveProductService.searchArchivedProducts(facilityId))
        .thenReturn(archivedProduct);
    StockCardSummariesV2SearchParams v2SearchParams = new
        StockCardSummariesV2SearchParams(getProgramsParms());
    when(stockCardSummariesService.findStockCards(v2SearchParams))
        .thenReturn(summaries);

    StockCardSummaries resultSummaries = service.findSiglusStockCard(params);

    assertEquals(2, resultSummaries.getStockCardsForFulfillOrderables().size());
  }

  @Test
  public void shouldGetArchivedProductIfSearchArchived() {
    MultiValueMap<String, String> params = getProgramsParms();
    params.add("archivedOnly", Boolean.toString(true));
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setOrderableFulfillMap(new HashMap<>());
    summaries.setAsOfDate(LocalDate.now());
    StockCard stockCard = StockCard.builder()
        .stockOnHand(15)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard2 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    StockCard stockCard3 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    summaries.setStockCardsForFulfillOrderables(Arrays.asList(stockCard, stockCard2, stockCard3));
    Set<String> archivedProduct = new HashSet<>();
    archivedProduct.add(stockCard.getOrderableId().toString());
    when(archiveProductService.searchArchivedProducts(facilityId))
        .thenReturn(archivedProduct);
    StockCardSummariesV2SearchParams v2SearchParams = new
        StockCardSummariesV2SearchParams(getProgramsParms());
    when(stockCardSummariesService.findStockCards(v2SearchParams))
        .thenReturn(summaries);

    StockCardSummaries resultSummaries = service.findSiglusStockCard(params);
    assertEquals(1, resultSummaries.getStockCardsForFulfillOrderables().size());
  }

  @Test
  public void shouldReturnSpecifiedOrderablesIfOrderableIdsNotEmpty() {
    Map<UUID, OrderableFulfillDto> orderableFulfillMap = newHashMap();
    orderableFulfillMap.put(orderableId, new OrderableFulfillDto(newArrayList(orderableId), null));
    orderableFulfillMap.put(UUID.randomUUID(),
        new OrderableFulfillDto(newArrayList(UUID.randomUUID()), null));
    StockCardSummaries summaries = new StockCardSummaries();
    summaries.setOrderableFulfillMap(orderableFulfillMap);
    summaries.setAsOfDate(LocalDate.now());
    StockCard stockCard = StockCard.builder()
        .stockOnHand(15)
        .orderableId(orderableId)
        .build();
    StockCard stockCard2 = StockCard.builder()
        .stockOnHand(20)
        .orderableId(UUID.randomUUID())
        .build();
    summaries.setStockCardsForFulfillOrderables(newArrayList(stockCard, stockCard2));
    when(stockCardSummariesService.findStockCards(any()))
        .thenReturn(summaries);
    MultiValueMap<String, String> params = getProgramsParms();
    params.add(ORDERABLE_ID, orderableId.toString());

    StockCardSummaries resultSummaries = service.findSiglusStockCard(params);
    assertEquals(1, resultSummaries.getStockCardsForFulfillOrderables().size());
    assertEquals(1, resultSummaries.getOrderableFulfillMap().size());
  }

  private MultiValueMap<String, String> getProgramsParms() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("programId", programId.toString());
    params.add(FACILITY_ID, facilityId.toString());
    params.add(RIGHT_NAME, rightName);
    return params;
  }

}
