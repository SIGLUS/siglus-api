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

import static com.google.common.collect.Sets.newHashSet;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_ADJUST;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_CARDS_VIEW;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_INVENTORIES_EDIT;
import static org.siglus.common.domain.referencedata.Orderable.COMMODITY_TYPE;
import static org.siglus.common.domain.referencedata.Orderable.TRADE_ITEM;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.joda.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.service.referencedata.PermissionStrings.Handler;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.common.domain.referencedata.Dispensable;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.domain.referencedata.OrderableChild;
import org.siglus.common.domain.referencedata.OrderableDisplayCategory;
import org.siglus.common.domain.referencedata.Program;
import org.siglus.common.domain.referencedata.ProgramOrderable;
import org.siglus.common.domain.referencedata.User;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.common.repository.OrderableRepository;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.dto.OrderableInKitDto;
import org.siglus.siglusapi.dto.SiglusOrdeableKitDto;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.springframework.data.domain.Page;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusUnpackServiceTest {

  @Mock
  private OrderableKitRepository orderableKitRepository;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  PermissionService permissionService;

  @Mock
  private ProgramExtensionService programExtensionService;

  @Mock
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Mock
  private ProgramExtensionRepository programExtensionRepository;

  @Mock
  private Handler permissionStringsHandler;

  @InjectMocks
  private SiglusUnpackService siglusUnpackService;

  private List<Orderable> kits;
  private final UUID facilityId = UUID.randomUUID();
  private final UUID kit1Id = UUID.randomUUID();
  private final UUID kitChildId = UUID.randomUUID();
  private final UUID kitChildTradeItemId = UUID.randomUUID();
  private final UUID kit2Id = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID programParentId = UUID.randomUUID();
  private final ProgramExtension programExtension = new ProgramExtension();
  private final User user = new User();

  @Before
  public void setup() {
    Program program = new Program(programId);
    OrderableDisplayCategory category = OrderableDisplayCategory.createNew(Code.code("category"));
    Orderable kit1 = new Orderable(Code.code("kit1"), Dispensable.createNew("each"), 10,
        5, false, kit1Id, 1L);
    Orderable kitChild = new Orderable(Code.code("kitChild"), Dispensable.createNew("each"), 5,
        2, false, kitChildId, 1L);
    Orderable kit2 = new Orderable(Code.code("kit2"), Dispensable.createNew("each"), 10,
        5, false, kit2Id, 1L);
    List<ProgramOrderable> programOrderables = Arrays.asList(
        ProgramOrderable.createNew(program, category, kit1, CurrencyUnit.USD),
        ProgramOrderable.createNew(program, category, kit2, CurrencyUnit.USD),
        ProgramOrderable.createNew(program, category, kitChild, CurrencyUnit.USD)
    );
    kitChild.setIdentifiers(ImmutableMap.of(TRADE_ITEM, kitChildTradeItemId.toString()));
    kitChild.setProgramOrderables(programOrderables);
    kit1.setProgramOrderables(programOrderables);
    kit1.setChildren(newHashSet(new OrderableChild(kit1, kitChild, 5L)));
    kit2.setProgramOrderables(programOrderables);
    kits = Arrays.asList(kit1, kit2);
    programExtension.setProgramId(programId);
    programExtension.setParentId(programParentId);
    programExtension.setIsVirtual(false);
    user.setId(UUID.randomUUID());
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(permissionService.getPermissionStrings(user.getId())).thenReturn(permissionStringsHandler);
  }

  @Test
  public void shouldReturnNullIfNotHaveStockInventoryRights() {
    // given
    List<StockCard> stockCards = Collections.singletonList(StockCard.builder()
        .stockOnHand(10).build());
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);
    when(permissionStringsHandler.get()).thenReturn(asSet(
        PermissionStringDto.create("OTHER", facilityId, programParentId)
    ));
    when(programExtensionService.findByProgramId(programId)).thenReturn(programExtension);
    when(calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(programParentId,
        facilityId, Collections.singletonList(kit1Id))).thenReturn(stockCards);

    // when
    SiglusOrdeableKitDto result = siglusUnpackService
        .getKitByFacilityIdAndOrderableId(facilityId, kit1Id);

    // then
    assertNull(result);
  }

  @Test
  public void shouldReturnNullIfNotHaveStockInventoryEditRights() {
    // given
    List<StockCard> stockCards = Collections.singletonList(StockCard.builder()
        .stockOnHand(10).build());
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);
    when(permissionStringsHandler.get()).thenReturn(asSet(
        PermissionStringDto.create(STOCK_ADJUST, facilityId, programParentId),
        PermissionStringDto.create(STOCK_CARDS_VIEW, facilityId, programParentId)
    ));
    when(programExtensionService.findByProgramId(programId)).thenReturn(programExtension);
    when(calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(programParentId,
        facilityId, Collections.singletonList(kit1Id))).thenReturn(stockCards);

    // when
    SiglusOrdeableKitDto result = siglusUnpackService
        .getKitByFacilityIdAndOrderableId(facilityId, kit1Id);

    // then
    assertNull(result);
  }

  @Test
  public void shouldReturnNullIfNotHaveStockAdjustRights() {
    // given
    List<StockCard> stockCards = Collections.singletonList(StockCard.builder()
        .stockOnHand(10).build());
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);
    when(permissionStringsHandler.get()).thenReturn(asSet(
        PermissionStringDto.create(STOCK_INVENTORIES_EDIT, facilityId, programParentId),
        PermissionStringDto.create(STOCK_CARDS_VIEW, facilityId, programParentId)
    ));
    when(programExtensionService.findByProgramId(programId)).thenReturn(programExtension);
    when(calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(programParentId,
        facilityId, Collections.singletonList(kit1Id))).thenReturn(stockCards);

    // when
    SiglusOrdeableKitDto result = siglusUnpackService
        .getKitByFacilityIdAndOrderableId(facilityId, kit1Id);

    // then
    assertNull(result);
  }

  @Test
  public void shouldReturnNullIfNotHaveStockCardViewRights() {
    // given
    List<StockCard> stockCards = Collections.singletonList(StockCard.builder()
        .stockOnHand(10).build());
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);
    when(permissionStringsHandler.get()).thenReturn(asSet(
        PermissionStringDto.create(STOCK_INVENTORIES_EDIT, facilityId, programParentId),
        PermissionStringDto.create(STOCK_ADJUST, facilityId, programParentId)
    ));
    when(programExtensionService.findByProgramId(programId)).thenReturn(programExtension);
    when(calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(programParentId,
        facilityId, Collections.singletonList(kit1Id))).thenReturn(stockCards);

    // when
    SiglusOrdeableKitDto result = siglusUnpackService
        .getKitByFacilityIdAndOrderableId(facilityId, kit1Id);

    // then
    assertNull(result);
  }

  @Test
  public void shouldReturnNullWhenStockCardNotFound() {
    // given
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);
    when(permissionStringsHandler.get()).thenReturn(asSet(
        PermissionStringDto.create(STOCK_INVENTORIES_EDIT, facilityId, programParentId),
        PermissionStringDto.create(STOCK_ADJUST, facilityId, programParentId),
        PermissionStringDto.create(STOCK_CARDS_VIEW, facilityId, programParentId)
    ));
    when(programExtensionService.findByProgramId(programId)).thenReturn(programExtension);
    when(calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(programParentId,
        facilityId, Collections.singletonList(kit1Id))).thenReturn(Collections.emptyList());

    // when
    SiglusOrdeableKitDto result = siglusUnpackService
        .getKitByFacilityIdAndOrderableId(facilityId, kit1Id);

    // then
    assertNull(result);
  }

  @Test
  public void shouldReturnWhenGetKitByFacilityIdAndOrderableId() {
    // given
    List<StockCard> stockCards = Collections.singletonList(StockCard.builder()
        .stockOnHand(10).build());
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);
    when(permissionStringsHandler.get()).thenReturn(asSet(
        PermissionStringDto.create(STOCK_INVENTORIES_EDIT, facilityId, programParentId),
        PermissionStringDto.create(STOCK_ADJUST, facilityId, programParentId),
        PermissionStringDto.create(STOCK_CARDS_VIEW, facilityId, programParentId)
    ));
    when(programExtensionService.findByProgramId(programId)).thenReturn(programExtension);
    when(calculatedStockOnHandService.getStockCardsWithStockOnHandByOrderableIds(programParentId,
        facilityId, Collections.singletonList(kit1Id))).thenReturn(stockCards);

    // when
    SiglusOrdeableKitDto result = siglusUnpackService
        .getKitByFacilityIdAndOrderableId(facilityId, kit1Id);

    // then
    assertNotNull(result);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenkitProductIdIsNull() {
    // when
    siglusUnpackService.searchOrderablesInKit(null);
  }

  @Test
  public void shouldGetKitDtoWhenSearchOrderablesInKit() {
    // given
    Page<Orderable> orderablePage = Pagination.getPage(kits, null);
    when(orderableRepository.findAllLatestByIds(newHashSet(kit1Id), null))
        .thenReturn(orderablePage);
    LotDto lotDto = new LotDto();
    lotDto.setLotCode(RandomStringUtils.random(5));
    lotDto.setTradeItemId(kitChildTradeItemId);
    List<LotDto> lots = Collections.singletonList(lotDto);
    when(lotReferenceDataService.getLots(any())).thenReturn(lots);
    ProgramExtension programExtension = new ProgramExtension();
    programExtension.setProgramId(programId);
    programExtension.setParentId(programParentId);
    List<ProgramExtension> programExtensions = Collections.singletonList(programExtension);
    when(programExtensionRepository.findAll()).thenReturn(programExtensions);

    // when
    List<OrderableInKitDto> result = siglusUnpackService.searchOrderablesInKit(kit1Id);

    // then
    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getLots().size());
  }

  @Test
  public void shouldNotGetLotWhenChildIsCommodityType() {
    // given
    Page<Orderable> orderablePage = Pagination.getPage(kits, null);
    orderablePage.getContent().get(0).getChildren().iterator().next().getOrderable()
        .setIdentifiers(ImmutableMap.of(COMMODITY_TYPE, kitChildTradeItemId.toString()));
    when(orderableRepository.findAllLatestByIds(newHashSet(kit1Id), null))
        .thenReturn(orderablePage);
    LotDto lotDto = new LotDto();
    lotDto.setLotCode(RandomStringUtils.random(5));
    lotDto.setTradeItemId(kitChildTradeItemId);
    when(lotReferenceDataService.getLots(any())).thenReturn(Collections.emptyList());
    ProgramExtension programExtension = new ProgramExtension();
    programExtension.setProgramId(programId);
    programExtension.setParentId(programParentId);
    List<ProgramExtension> programExtensions = Collections.singletonList(programExtension);
    when(programExtensionRepository.findAll()).thenReturn(programExtensions);

    // when
    List<OrderableInKitDto> result = siglusUnpackService.searchOrderablesInKit(kit1Id);

    // then
    assertEquals(1, result.size());
    assertEquals(0, result.get(0).getLots().size());
  }

  @Test
  public void shouldGetChildWhenSearchOrderablesInKit() {
    // given
    when(orderableKitRepository.findAllKitProduct()).thenReturn(kits);

    // when
    Set<UUID> result = siglusUnpackService.orderablesInKit();

    // then
    assertEquals(1, result.size());
  }

}