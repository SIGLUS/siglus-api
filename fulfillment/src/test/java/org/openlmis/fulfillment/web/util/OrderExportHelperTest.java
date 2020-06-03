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

package org.openlmis.fulfillment.web.util;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.OrderLineItemDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.ProgramOrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.testutils.DtoGenerator;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;


@SuppressWarnings({"PMD.TooManyMethods"})
@RunWith(MockitoJUnitRunner.class)
public class OrderExportHelperTest {

  private static final long PACK_SIZE = 2;

  private Order order;
  private OrderableDto orderableDto;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @InjectMocks
  private OrderExportHelper orderExportHelper;

  @Captor
  private ArgumentCaptor<Set<VersionEntityReference>> argumentCaptor;

  private FacilityDto facility = DtoGenerator.of(FacilityDto.class);
  private FacilityDto requestingFacility = DtoGenerator.of(FacilityDto.class);
  private FacilityDto receivingFacility = DtoGenerator.of(FacilityDto.class);
  private FacilityDto supplyingFacility = DtoGenerator.of(FacilityDto.class);
  private ProcessingPeriodDto period = DtoGenerator.of(ProcessingPeriodDto.class);
  private ProgramDto program = DtoGenerator.of(ProgramDto.class);
  private UserDto user = DtoGenerator.of(UserDto.class);

  @Before
  public void setUp() {
    generateInstances();
  }

  @Test
  public void shouldExportOrderLinesToDtos() {
    OrderLineItem orderLineItem =
        generateOrderLineItem(orderableDto.getId(), orderableDto.getVersionNumber());

    when(orderableReferenceDataService.findByIdentities(argumentCaptor.capture()))
            .thenReturn(Collections.singletonList(orderableDto));

    List<OrderLineItemDto> items =
        orderExportHelper.exportToDtos(singletonList(orderLineItem));
    OrderLineItemDto item = items.get(0);
    assertNotNull(item);
    assertEquals(item.getId(), orderLineItem.getId());
    assertEquals(item.getOrderable().getId(), orderLineItem.getOrderable().getId());
    assertEquals(item.getOrderedQuantity(), orderLineItem.getOrderedQuantity());
    assertEquals((long) item.getTotalDispensingUnits(),
        orderableDto.getNetContent() * orderLineItem.getOrderedQuantity());

    Set<VersionEntityReference> searchedIds = argumentCaptor.getValue();
    assertTrue(searchedIds.contains(new VersionEntityReference(
        orderableDto.getId(), orderableDto.getVersionNumber())));
    assertTrue(searchedIds.size() == 1);
  }

  @Test
  public void exportShouldNotSetOrderableIfNoneReturned() {
    when(orderableReferenceDataService.findByIdentities(argumentCaptor.capture()))
            .thenReturn(Collections.emptyList());

    OrderLineItem orderLineItem =
        generateOrderLineItem(orderableDto.getId(), orderableDto.getVersionNumber());
    List<OrderLineItemDto> items =
            orderExportHelper.exportToDtos(singletonList(orderLineItem));
    OrderLineItemDto item = items.get(0);
    assertNotNull(item);

    assertEquals(item.getOrderable(), null);

    Set<VersionEntityReference> searchedIds = argumentCaptor.getValue();
    assertTrue(searchedIds.contains(new VersionEntityReference(
        orderableDto.getId(), orderableDto.getVersionNumber())));
    assertTrue(searchedIds.size() == 1);
  }

  @Test
  public void shouldSetSubResources() {
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);
    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodService.findOne(period.getId())).thenReturn(period);
    when(userReferenceDataService.findOne(user.getId())).thenReturn(user);
    when(facilityReferenceDataService.findOne(supplyingFacility.getId()))
        .thenReturn(supplyingFacility);
    when(facilityReferenceDataService.findOne(requestingFacility.getId()))
        .thenReturn(requestingFacility);
    when(facilityReferenceDataService.findOne(receivingFacility.getId()))
        .thenReturn(receivingFacility);
    OrderDto orderDto = new OrderDto();

    orderExportHelper.setSubResources(orderDto, order, null, null, null, null);

    assertEquals(order.getFacilityId(), orderDto.getFacility().getId());
    assertEquals(order.getRequestingFacilityId(), orderDto.getRequestingFacility().getId());
    assertEquals(order.getReceivingFacilityId(), orderDto.getReceivingFacility().getId());
    assertEquals(order.getSupplyingFacilityId(), orderDto.getSupplyingFacility().getId());
    assertEquals(order.getProgramId(), orderDto.getProgram().getId());
    assertEquals(order.getProcessingPeriodId(), orderDto.getProcessingPeriod().getId());
    assertEquals(order.getCreatedById(), orderDto.getCreatedBy().getId());
  }

  @Test
  public void shouldSetSubResourcesFromExistingMaps() {
    Map<UUID, FacilityDto> facilities = new HashMap<>();
    facilities.put(facility.getId(), facility);
    facilities.put(supplyingFacility.getId(), supplyingFacility);
    facilities.put(requestingFacility.getId(), requestingFacility);
    facilities.put(receivingFacility.getId(), receivingFacility);
    Map<UUID, ProgramDto> programs = new HashMap<>();
    programs.put(program.getId(), program);
    Map<UUID, ProcessingPeriodDto> periods = new HashMap<>();
    periods.put(period.getId(), period);
    Map<UUID, UserDto> users = new HashMap<>();
    users.put(user.getId(), user);
    OrderDto orderDto = new OrderDto();

    orderExportHelper.setSubResources(orderDto, order, facilities, programs, periods, users);

    assertEquals(order.getFacilityId(), orderDto.getFacility().getId());
    assertEquals(order.getRequestingFacilityId(), orderDto.getRequestingFacility().getId());
    assertEquals(order.getReceivingFacilityId(), orderDto.getReceivingFacility().getId());
    assertEquals(order.getSupplyingFacilityId(), orderDto.getSupplyingFacility().getId());
    assertEquals(order.getProgramId(), orderDto.getProgram().getId());
    assertEquals(order.getProcessingPeriodId(), orderDto.getProcessingPeriod().getId());
    assertEquals(order.getCreatedById(), orderDto.getCreatedBy().getId());
  }

  private OrderLineItem generateOrderLineItem(UUID orderableId, Long versionNumber) {
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(program.getId());
    Set<ProgramOrderableDto> products = new HashSet<>();
    products.add(programOrderableDto);
    orderableDto.setPrograms(products);

    return new OrderLineItemDataBuilder()
        .withOrder(order)
        .withOrderable(orderableId, versionNumber)
        .build();
  }

  private void generateInstances() {
    order = new OrderDataBuilder()
        .withStatus(OrderStatus.RECEIVED)
        .withFacilityId(facility.getId())
        .withRequestingFacilityId(requestingFacility.getId())
        .withReceivingFacilityId(receivingFacility.getId())
        .withSupplyingFacilityId(supplyingFacility.getId())
        .withProgramId(program.getId())
        .withProcessingPeriodId(period.getId())
        .withCreatedById(user.getId())
        .build();

    orderableDto = new OrderableDataBuilder()
        .withId(UUID.randomUUID())
        .withVersionNumber(1L)
        .withNetContent(PACK_SIZE)
        .build();

    OrderLineItem orderLineItem = generateOrderLineItem(
        orderableDto.getId(), orderableDto.getVersionNumber());
    order.setOrderLineItems(new ArrayList<>(
        singletonList(orderLineItem)));
  }
}
