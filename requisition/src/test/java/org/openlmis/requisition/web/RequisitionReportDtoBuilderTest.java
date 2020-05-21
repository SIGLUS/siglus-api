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

package org.openlmis.requisition.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.dto.RequisitionReportDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.i18n.MessageService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.RequisitionExportHelper;

@RunWith(MockitoJUnitRunner.class)
public class RequisitionReportDtoBuilderTest {

  private static final String SYSTEM = "SYSTEM";
  private static final Money TOTAL_COST = Money.of(CurrencyUnit.EUR, 25);
  private static final Money FS_TOTAL_COST = Money.of(CurrencyUnit.EUR, 3);
  private static final Money NFS_TOTAL_COST = Money.of(CurrencyUnit.EUR, 22);

  @Mock
  private RequisitionExportHelper exportHelper;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private RequisitionDtoBuilder requisitionDtoBuilder;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private RequisitionReportDtoBuilder requisitionReportDtoBuilder =
      new RequisitionReportDtoBuilder();

  private Requisition requisition;
  private RequisitionLineItem fullSupply;
  private RequisitionLineItem nonFullSupply;

  private Map<VersionIdentityDto, OrderableDto> orderables = Maps.newHashMap();

  private RequisitionDto requisitionDto;
  private RequisitionLineItemDto fullSupplyDtos;
  private RequisitionLineItemDto nonFullSupplyDtos;

  private UserDto user1 = DtoGenerator.of(UserDto.class, 2).get(0);
  private UserDto user2 = DtoGenerator.of(UserDto.class, 2).get(1);

  @Before
  public void setUp() {
    fullSupply = new RequisitionLineItemDataBuilder()
        .withTotalCost(FS_TOTAL_COST)
        .build();
    nonFullSupply = new RequisitionLineItemDataBuilder()
        .withTotalCost(NFS_TOTAL_COST)
        .build();
    requisition = new RequisitionDataBuilder()
        .addLineItem(fullSupply, false)
        .addLineItem(nonFullSupply, true)
        .buildInitiatedRegularRequisition();

    final OrderableDto fullSupplyOrderable = new OrderableDtoDataBuilder()
        .withId(fullSupply.getOrderable().getId())
        .withVersionNumber(fullSupply.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), true, Money.of(CurrencyUnit.USD, 1), 2)
        .buildAsDto();
    final OrderableDto nonFullSupplyOrderable = new OrderableDtoDataBuilder()
        .withId(nonFullSupply.getOrderable().getId())
        .withVersionNumber(nonFullSupply.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), false, Money.of(CurrencyUnit.USD, 2), 3)
        .buildAsDto();
    final ApprovedProductDto fullSupplyApprovedProductDto = new ApprovedProductDtoDataBuilder()
        .withId(fullSupply.getFacilityTypeApprovedProduct().getId())
        .withVersionNumber(fullSupply.getFacilityTypeApprovedProduct().getVersionNumber())
        .withOrderable(fullSupplyOrderable)
        .buildAsDto();
    final ApprovedProductDto nonFullSupplyApprovedProductDto = new ApprovedProductDtoDataBuilder()
        .withId(nonFullSupply.getFacilityTypeApprovedProduct().getId())
        .withVersionNumber(nonFullSupply.getFacilityTypeApprovedProduct().getVersionNumber())
        .withOrderable(nonFullSupplyOrderable)
        .buildAsDto();

    fullSupplyDtos = new RequisitionLineItemDto();
    fullSupply.export(fullSupplyDtos, fullSupplyOrderable, fullSupplyApprovedProductDto);

    nonFullSupplyDtos = new RequisitionLineItemDto();
    nonFullSupply.export(nonFullSupplyDtos, nonFullSupplyOrderable,
        nonFullSupplyApprovedProductDto);

    requisitionDto = new RequisitionDto();
    requisition.export(requisitionDto);

    orderables.put(fullSupplyOrderable.getIdentity(), fullSupplyOrderable);
    orderables.put(nonFullSupplyOrderable.getIdentity(), nonFullSupplyOrderable);

    when(userReferenceDataService.findOne(user1.getId())).thenReturn(user1);
    when(userReferenceDataService.findOne(user2.getId())).thenReturn(user2);
    when(orderableReferenceDataService.findByIdentities(anySet()))
        .thenReturn(Lists.newArrayList(orderables.values()));
    when(requisitionDtoBuilder.build(requisition)).thenReturn(requisitionDto);

    when(exportHelper.exportToDtos(Lists.newArrayList(fullSupply)))
        .thenReturn(Lists.newArrayList(fullSupplyDtos));
    when(exportHelper.exportToDtos(Lists.newArrayList(nonFullSupply)))
        .thenReturn(Lists.newArrayList(nonFullSupplyDtos));

    Message msg = new Message(MessageKeys.STATUS_CHANGE_USER_SYSTEM);
    when(messageService.localize(msg))
      .thenReturn(msg. new LocalizedMessage(SYSTEM));
  }

  @Test
  public void shouldBuildDtoWithoutStatusChanges() {
    RequisitionReportDto dto = requisitionReportDtoBuilder.build(requisition);

    commonReportDtoAsserts(dto);
    assertNull(dto.getInitiatedBy());
    assertNull(dto.getInitiatedDate());
    assertNull(dto.getSubmittedBy());
    assertNull(dto.getSubmittedDate());
    assertNull(dto.getAuthorizedBy());
    assertNull(dto.getAuthorizedDate());
  }

  @Test
  public void shouldBuildDtoWithStatusChanges() {
    ZonedDateTime initDt = ZonedDateTime.now().minusDays(11);
    ZonedDateTime submitDt = ZonedDateTime.now().minusDays(6);
    ZonedDateTime authorizeDt = ZonedDateTime.now().minusDays(2);
    StatusChange initStatusChange = mock(StatusChange.class);
    StatusChange submitStatusChange = mock(StatusChange.class);
    StatusChange authorizeStatusChange = mock(StatusChange.class);
    when(initStatusChange.getStatus()).thenReturn(RequisitionStatus.INITIATED);
    when(initStatusChange.getCreatedDate()).thenReturn(initDt);
    when(initStatusChange.getAuthorId()).thenReturn(user1.getId());
    when(submitStatusChange.getStatus()).thenReturn(RequisitionStatus.SUBMITTED);
    when(submitStatusChange.getCreatedDate()).thenReturn(submitDt);
    when(submitStatusChange.getAuthorId()).thenReturn(user2.getId());
    when(authorizeStatusChange.getStatus()).thenReturn(RequisitionStatus.AUTHORIZED);
    when(authorizeStatusChange.getCreatedDate()).thenReturn(authorizeDt);
    when(authorizeStatusChange.getAuthorId()).thenReturn(user1.getId());
    List<StatusChange> statusChanges = new ArrayList<>();
    statusChanges.add(initStatusChange);
    statusChanges.add(submitStatusChange);
    statusChanges.add(authorizeStatusChange);

    requisition.setStatusChanges(statusChanges);

    RequisitionReportDto dto = requisitionReportDtoBuilder.build(requisition);

    commonReportDtoAsserts(dto);
    assertEquals(user1, dto.getInitiatedBy());
    assertEquals(initDt, dto.getInitiatedDate());
    assertEquals(user2, dto.getSubmittedBy());
    assertEquals(submitDt, dto.getSubmittedDate());
    assertEquals(user1, dto.getAuthorizedBy());
    assertEquals(authorizeDt, dto.getAuthorizedDate());
  }

  @Test
  public void shouldBuildDtoWithSystemStatusChange() {
    ZonedDateTime now = ZonedDateTime.now();
    StatusChange initStatusChange = mock(StatusChange.class);
    when(initStatusChange.getStatus()).thenReturn(RequisitionStatus.INITIATED);
    when(initStatusChange.getCreatedDate()).thenReturn(now);
    List<StatusChange> statusChanges = Collections.singletonList(initStatusChange);
    requisition.setStatusChanges(statusChanges);

    RequisitionReportDto dto = requisitionReportDtoBuilder.build(requisition);

    commonReportDtoAsserts(dto);
    assertNull(dto.getSubmittedBy());
    assertNull(dto.getSubmittedDate());
    assertNull(dto.getAuthorizedBy());
    assertNull(dto.getAuthorizedDate());

    assertEquals(now, dto.getInitiatedDate());
    UserDto fakeUser = dto.getInitiatedBy();
    assertNotNull(fakeUser);
    assertEquals(SYSTEM, fakeUser.getFirstName());
    assertNull(fakeUser.getLastName());
    assertEquals(SYSTEM, fakeUser.getUsername());
  }

  @Test
  public void shouldExportLineItemsToDtoAndSortByDisplayOrder() {
    RequisitionLineItemDto extra = new RequisitionLineItemDataBuilder()
        .withRequisition(requisition)
        .buildAsDto();

    OrderableDto extraOrderable = new OrderableDtoDataBuilder()
        .withId(extra.getOrderable().getId())
        .withVersionNumber(extra.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), false, Money.of(CurrencyUnit.USD, 2), 10)
        .buildAsDto();

    orderables.put(extraOrderable.getIdentity(), extraOrderable);

    when(exportHelper.exportToDtos(Lists.newArrayList(fullSupply)))
        .thenReturn(Lists.newArrayList(fullSupplyDtos, nonFullSupplyDtos, extra));

    List<RequisitionLineItemDto> lineItemDtos = requisitionReportDtoBuilder
        .exportLinesToDtos(Lists.newArrayList(fullSupply), orderables, requisition.getProgramId());

    assertTrue(getOrderableCategoryDisplayOrder(lineItemDtos.get(0))
        <= getOrderableCategoryDisplayOrder(lineItemDtos.get(1)));
    assertTrue(getOrderableCategoryDisplayOrder(lineItemDtos.get(1))
        <= getOrderableCategoryDisplayOrder(lineItemDtos.get(2)));
  }

  private Integer getOrderableCategoryDisplayOrder(RequisitionLineItemDto lineItemDto) {
    return orderables
        .get(lineItemDto.getOrderableIdentity())
        .getProgramOrderable(requisition.getProgramId())
        .getOrderableCategoryDisplayOrder();
  }

  private void commonReportDtoAsserts(RequisitionReportDto dto) {
    assertEquals(requisitionDto, dto.getRequisition());
    assertEquals(fullSupplyDtos, dto.getFullSupply().get(0));
    assertEquals(nonFullSupplyDtos, dto.getNonFullSupply().get(0));
    assertEquals(TOTAL_COST, dto.getTotalCost());
    assertEquals(FS_TOTAL_COST, dto.getFullSupplyTotalCost());
    assertEquals(NFS_TOTAL_COST, dto.getNonFullSupplyTotalCost());
  }
}
