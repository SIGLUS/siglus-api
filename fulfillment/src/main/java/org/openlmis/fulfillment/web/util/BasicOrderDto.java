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

import static org.openlmis.fulfillment.service.ResourceNames.USERS;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.service.ExporterBuilder;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class BasicOrderDto implements Order.Exporter, UpdateDetails.Exporter {

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

  @Getter
  private ObjectReferenceDto lastUpdater;

  @Getter
  private ZonedDateTime lastUpdatedDate;

  @Override
  public void setStatusMessages(List<StatusMessageDto> statusMessages) {
    // nothing to do here
  }

  @Override
  public void setStatusChanges(List<StatusChangeDto> statusChanges) {
    // nothing to do here
  }

  @Override
  public void setUpdateDetails(UpdateDetails updateDetails) {
    if (null != updateDetails) {
      updateDetails.export(this);
    }
  }

  @Override
  public void setUpdaterId(UUID updaterId) {
    if (updaterId != null) {
      this.lastUpdater = ObjectReferenceDto.create(updaterId, serviceUrl, USERS);
    }
  }

  @Override
  public void setUpdatedDate(ZonedDateTime updatedDate) {
    this.lastUpdatedDate = updatedDate;
  }

  /**
   * Create new list of OrderDto based on list of {@link Order}.
   * @param orders list on orders
   * @return list of OrderDto.
   */
  public static List<BasicOrderDto> newInstance(Iterable<Order> orders, ExporterBuilder exporter) {
    List<BasicOrderDto> orderDtos = new ArrayList<>();
    orders.forEach(o -> orderDtos.add(newInstance(o, exporter)));
    return orderDtos;
  }

  /**
   * Create new instance of OrderDto based on given {@link Order}.
   * @param order instance of Order
   * @return new instance od OrderDto.
   */
  public static BasicOrderDto newInstance(Order order, ExporterBuilder exporter) {
    BasicOrderDto orderDto =  new BasicOrderDto();
    exporter.export(order, orderDto);

    return orderDto;
  }

}
