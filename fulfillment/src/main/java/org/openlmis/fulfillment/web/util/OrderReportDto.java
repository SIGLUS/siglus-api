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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import org.openlmis.fulfillment.domain.ExternalStatus;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.service.ExporterBuilder;
import org.openlmis.fulfillment.service.referencedata.GeographicZoneDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;

@EqualsAndHashCode(callSuper = true)
public final class OrderReportDto extends OrderDto implements Order.Exporter {

  public static final Integer DISTRICT_LEVEL = 3;
  public static final Integer REGION_LEVEL = 2;

  /**
   * Create new instance of OrderReportDto based on given {@link Order}.
   * @param order instance of Order
   * @return new instance od OrderReportDto.
   */
  public static OrderReportDto newInstance(Order order, ExporterBuilder exporter) {
    OrderReportDto orderDto = new OrderReportDto();
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
      orderDto.setStatusChanges(exporter.convertToDtos(order.getStatusChanges()));
    }

    return orderDto;
  }

  /**
   * Get zone of the facility that has the district level.
   * @return district of the facility.
   */
  @JsonIgnore
  public GeographicZoneDto getThirdLevelFacility() {
    return getFacility().getZoneByLevelNumber(DISTRICT_LEVEL);
  }

  /**
   * Get zone of the facility that has the region level.
   * @return region of the facility.
   */
  @JsonIgnore
  public GeographicZoneDto getSecondLevelFacility() {
    return getFacility().getZoneByLevelNumber(REGION_LEVEL);
  }

  /**
   * Get status change that is AUTHORIZED.
   * @return authorized status change.
   */
  @JsonIgnore
  public StatusChangeDto getAuthorizedStatusChange() {
    return Optional.ofNullable(getStatusChangeByStatus(ExternalStatus.AUTHORIZED))
            .orElse(new StatusChangeDto());
  }

  /**
   * Get status change that is APPROVED.
   * @return approved status change.
   */
  @JsonIgnore
  public StatusChangeDto getApprovedStatusChange() {
    return Optional.ofNullable(getStatusChangeByStatus(ExternalStatus.APPROVED))
            .orElse(new StatusChangeDto());
  }

  /**
   * Get status change that is RELEASED.
   * @return released status change.
   */
  @JsonIgnore
  public StatusChangeDto getReleasedStatusChange() {
    return Optional.ofNullable(getStatusChangeByStatus(ExternalStatus.RELEASED))
            .orElse(new StatusChangeDto());
  }
}
