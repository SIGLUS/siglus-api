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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.domain.ExternalStatus;
import org.openlmis.fulfillment.domain.FulfillmentStatusChange;
import org.openlmis.fulfillment.domain.FulfillmentStatusMessage;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.service.ExporterBuilder;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"serviceUrl"})
public class OrderDto implements Order.Importer, Order.Exporter, UpdateDetails.Exporter {

  @Setter
  private String serviceUrl;

  @Getter
  @Setter
  private UUID id;

  @Getter
  @Setter
  private UUID externalId;

  @Getter
  @Setter
  private Boolean emergency;

  @Getter
  @Setter
  private FacilityDto facility;

  @Getter
  @Setter
  private ProcessingPeriodDto processingPeriod;

  @Getter
  @Setter
  private ZonedDateTime createdDate;

  @Getter
  @Setter
  private UserDto createdBy;

  @Getter
  @Setter
  private ProgramDto program;

  @Getter
  @Setter
  private FacilityDto requestingFacility;

  @Getter
  @Setter
  private FacilityDto receivingFacility;

  @Getter
  @Setter
  private FacilityDto supplyingFacility;

  @Getter
  @Setter
  private String orderCode;

  @Getter
  @Setter
  private OrderStatus status;

  @Getter
  @Setter
  private BigDecimal quotedCost;

  @Setter
  private List<OrderLineItemDto> orderLineItems;

  @Setter
  private List<StatusMessageDto> statusMessages;

  @Setter
  private List<StatusChangeDto> statusChanges;

  @Getter
  private UserObjectReferenceDto lastUpdater;

  @Getter
  private ZonedDateTime lastUpdatedDate;

  // [SIGLUS change start]
  // [change reason]: support for requisition number.
  @Getter
  @Setter
  private String requisitionNumber;
  // [SIGLUS change end]

  @Override
  public List<OrderLineItem.Importer> getOrderLineItems() {
    return new ArrayList<>(
        Optional.ofNullable(orderLineItems).orElse(Collections.emptyList())
    );
  }

  // [SIGLUS change start]
  // [change reason]: support for skip products.
  public List<OrderLineItemDto> orderLineItems() {
    return orderLineItems;
  }
  // [SIGLUS change end]

  @Override
  public List<FulfillmentStatusMessage.Importer> getStatusMessages() {
    return new ArrayList<>(Optional.ofNullable(statusMessages).orElse(Collections.emptyList()));
  }

  @Override
  public List<FulfillmentStatusChange.Importer> getStatusChanges() {
    return new ArrayList<>(Optional.ofNullable(statusChanges).orElse(Collections.emptyList()));
  }

  @Override
  @JsonIgnore
  public UpdateDetails getUpdateDetails() {
    return new UpdateDetails(lastUpdater.getId(), lastUpdatedDate);
  }

  @Override
  @JsonIgnore
  public void setUpdateDetails(UpdateDetails updateDetails) {
    updateDetails.export(this);
  }

  @Override
  public void setUpdaterId(UUID updaterId) {
    if (updaterId != null) {
      this.lastUpdater = UserObjectReferenceDto.create(updaterId, serviceUrl);
    }
  }

  @Override
  @JsonIgnore
  public void setUpdatedDate(ZonedDateTime updatedDate) {
    this.lastUpdatedDate = updatedDate;
  }

  /**
   * Create new list of OrderDto based on list of {@link Order}.
   * @param orders list on orders
   * @return list of OrderDto.
   */
  public static Iterable<OrderDto> newInstance(Iterable<Order> orders,
                                               ExporterBuilder exporter) {
    List<OrderDto> orderDtos = new ArrayList<>();
    orders.forEach(o -> orderDtos.add(newInstance(o, exporter)));
    return orderDtos;
  }

  /**
   * Create new instance of OrderDto based on given {@link Order}.
   * @param order instance of Order
   * @return new instance od OrderDto.
   */
  public static OrderDto newInstance(Order order, ExporterBuilder exporter) {
    OrderDto orderDto =  new OrderDto();
    exporter.export(order, orderDto);

    if (order.getOrderLineItems() != null) {
      List<OrderableDto> orderables = exporter.getLineItemOrderables(order);

      orderDto.setOrderLineItems(order.getOrderLineItems().stream()
          .map(item -> OrderLineItemDto.newInstance(item, exporter, orderables))
          .collect(Collectors.toList()));
    }

    if (order.getStatusMessages() != null) {
      orderDto.setStatusMessages(order.getStatusMessages().stream()
          .map(StatusMessageDto::newInstance).collect(Collectors.toList()));
    }

    if (order.getStatusChanges() != null) {
      orderDto.setStatusChanges(order.getStatusChanges().stream()
          .map(StatusChangeDto::newInstance).collect(Collectors.toList()));
    }

    return orderDto;
  }

  /**
   * Get status change with given status.
   * @return status change
   */
  @JsonIgnore
  public StatusChangeDto getStatusChangeByStatus(ExternalStatus status) {
    return Optional.ofNullable(statusChanges)
        .orElse(new ArrayList<>())
        .stream()
        .filter(statusChange -> status.equals(statusChange.getStatus()))
        .findFirst()
        .orElse(null);
  }

  // [SIGLUS change start]
  /**
   * Create new list of Portuguese order status.
   * @param orderStatus input order status
   * @return string of Portuguese
   */
  public static String getPortugueseStatus(String orderStatus) {
    OrderStatus inputOrderStatus = OrderStatus.fromString(orderStatus);
    switch (inputOrderStatus) {
      case PARTIALLY_FULFILLED:
        return "Aviado parcialmente";
      case CLOSED:
        return "Fechado";
      case ORDERED:
        return "ordenado";
      case FULFILLING:
        return "Aviando";
      case SHIPPED:
        return "Enviado";
      case RECEIVED:
        return "Recebido";
      case TRANSFER_FAILED:
        return "A transferência falhou";
      case IN_ROUTE:
        return "A Caminho";
      case READY_TO_PACK:
        return "Pronto para embalar";
      default:
        return "";
    }
  }
  // [SIGLUS change end]
}
