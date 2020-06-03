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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.BaseReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderExportHelper {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(OrderExportHelper.class);

  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  /**
   * Return list of OrderLineItemDtos for a given OrderLineItem.
   *
   * @param lineItems List of OrderLineItems to be exported to Dto
   * @return list of OrderLineItemDtos
   */
  public List<OrderLineItemDto> exportToDtos(List<OrderLineItem> lineItems) {
    return exportToDtos(lineItems, null);
  }

  /**
   * Return list of OrderLineItemDtos for a given OrderLineItem.
   *
   * @param lineItems List of OrderLineItems to be exported to Dto
   * @param orderables Map of Orderables by id
   * @return list of OrderLineItemDtos
   */
  public List<OrderLineItemDto> exportToDtos(List<OrderLineItem> lineItems,
      Map<VersionIdentityDto, OrderableDto> orderables) {
    XLOGGER.entry(lineItems);
    Profiler profiler = new Profiler("EXPORT_LINE_ITEMS_TO_DTOS");
    profiler.setLogger(XLOGGER);

    Map<VersionIdentityDto, OrderableDto> orderablesForLines;
    if (orderables == null) {
      profiler.start("GET_ORDERABLE_IDENTITIES_FROM_LINE_ITEMS");
      Set<VersionEntityReference> orderableIdentities = new HashSet<>(lineItems.size());
      for (OrderLineItem lineItem : lineItems) {
        orderableIdentities.add(lineItem.getOrderable());
      }

      profiler.start("FIND_ORDERABLES_BY_IDENTITIES");
      orderablesForLines =
          orderableReferenceDataService.findByIdentities(orderableIdentities)
              .stream()
              .collect(Collectors.toMap(OrderableDto::getIdentity, orderable -> orderable));

    } else {
      orderablesForLines = orderables;
    }

    profiler.start("CONVERT_LINE_ITEMS_TO_DTOS");
    List<OrderLineItemDto> lineItemDtos =
        new ArrayList<>(lineItems.size());
    for (OrderLineItem lineItem : lineItems) {
      lineItemDtos.add(exportToDto(lineItem, orderablesForLines));
    }

    profiler.stop().log();
    XLOGGER.exit(lineItemDtos);
    return lineItemDtos;
  }

  /**
   * Set sub resources of Order.Exporter
   */
  public void setSubResources(Order.Exporter orderDto, Order order,
      Map<UUID, FacilityDto> facilities, Map<UUID, ProgramDto> programs,
      Map<UUID, ProcessingPeriodDto> periods, Map<UUID, UserDto> users) {

    if (facilities != null) {
      orderDto.setFacility(facilities.get(order.getFacilityId()));
      orderDto.setRequestingFacility(facilities.get(order.getRequestingFacilityId()));
      orderDto.setReceivingFacility(facilities.get(order.getReceivingFacilityId()));
      orderDto.setSupplyingFacility(facilities.get(order.getSupplyingFacilityId()));
    } else {
      orderDto.setFacility(getIfPresent(facilityReferenceDataService, order.getFacilityId()));
      orderDto.setRequestingFacility(getIfPresent(facilityReferenceDataService,
          order.getRequestingFacilityId()));
      orderDto.setReceivingFacility(getIfPresent(facilityReferenceDataService,
          order.getReceivingFacilityId()));
      orderDto.setSupplyingFacility(getIfPresent(facilityReferenceDataService,
          order.getSupplyingFacilityId()));
    }
    if (programs != null) {
      orderDto.setProgram(programs.get(order.getProgramId()));
    } else {
      orderDto.setProgram(getIfPresent(programReferenceDataService, order.getProgramId()));
    }
    if (periods != null) {
      orderDto.setProcessingPeriod(periods.get(order.getProcessingPeriodId()));
    } else {
      orderDto.setProcessingPeriod(getIfPresent(periodReferenceDataService,
          order.getProcessingPeriodId()));
    }
    if (users != null) {
      orderDto.setCreatedBy(users.get(order.getCreatedById()));
    } else {
      orderDto.setCreatedBy(getIfPresent(userReferenceDataService, order.getCreatedById()));
    }
  }

  private <T> T getIfPresent(BaseReferenceDataService<T> service, UUID id) {
    return Optional.ofNullable(id).isPresent() ? service.findOne(id) : null;
  }

  private OrderLineItemDto exportToDto(OrderLineItem lineItem,
      Map<VersionIdentityDto, OrderableDto> orderables) {
    XLOGGER.entry(lineItem, orderables);
    Profiler profiler = new Profiler("EXPORT_LINE_ITEM_TO_DTO");
    profiler.setLogger(XLOGGER);

    profiler.start("GET_LINE_ITEM_ORDERABLE_FROM_ORDERABLES");
    final OrderableDto orderableDto = orderables
        .get(new VersionIdentityDto(lineItem.getOrderable()));

    profiler.start("CONSTRUCT_LINE_ITEM_DTO");
    OrderLineItemDto dto = new OrderLineItemDto();

    profiler.start("EXPORT_TO_DTO");
    lineItem.export(dto);
    if (orderableDto != null) {
      dto.setOrderable(orderableDto);
      dto.setTotalDispensingUnits(orderableDto.getNetContent() * dto.getOrderedQuantity());
    }

    profiler.stop().log();
    XLOGGER.exit(dto);
    return dto;
  }
}
