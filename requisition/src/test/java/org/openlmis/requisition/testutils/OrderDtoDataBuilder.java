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

package org.openlmis.requisition.testutils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderLineItemDto;
import org.openlmis.requisition.dto.OrderStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.openlmis.requisition.dto.StatusMessageDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class OrderDtoDataBuilder implements DtoDataBuilder<OrderDto> {

  private UUID externalId;
  private Boolean emergency;
  private FacilityDto facility;
  private ProcessingPeriodDto processingPeriod;
  private ZonedDateTime createdDate;
  private UserDto createdBy;
  private ProgramDto program;
  private FacilityDto requestingFacility;
  private FacilityDto receivingFacility;
  private FacilityDto supplyingFacility;
  private String orderCode;
  private OrderStatus status;
  private BigDecimal quotedCost;
  private List<OrderLineItemDto> orderLineItems;
  private List<StatusMessageDto> statusMessages;
  private List<StatusChangeDto> statusChanges;
  private ObjectReferenceDto lastUpdater;

  /**
   * Builder for {@link OrderDto}.
   */
  public OrderDtoDataBuilder() {
    externalId = UUID.randomUUID();
    emergency = false;
    facility = new FacilityDtoDataBuilder().buildAsDto();
    processingPeriod = new ProcessingPeriodDtoDataBuilder().buildAsDto();
    createdDate = ZonedDateTime.now();
    createdBy = new UserDtoDataBuilder().buildAsDto();
    program = new ProgramDtoDataBuilder().buildAsDto();
    requestingFacility = new FacilityDtoDataBuilder().buildAsDto();
    receivingFacility = new FacilityDtoDataBuilder().buildAsDto();
    supplyingFacility = new FacilityDtoDataBuilder().buildAsDto();
    orderCode = "code";
    status = OrderStatus.ORDERED;
    quotedCost = BigDecimal.ONE;
    orderLineItems = new ArrayList<>();
    statusMessages = new ArrayList<>();
    statusChanges = new ArrayList<>();
    lastUpdater = new ObjectReferenceDtoDataBuilder().buildAsDto();
  }

  @Override
  public OrderDto buildAsDto() {
    return new OrderDto(externalId,
        emergency,
        facility,
        processingPeriod,
        createdDate,
        createdBy,
        program,
        requestingFacility,
        receivingFacility,
        supplyingFacility,
        orderCode,
        status,
        quotedCost,
        orderLineItems,
        statusMessages,
        statusChanges,
        lastUpdater);
  }

  public OrderDtoDataBuilder withExternalId(UUID externalId) {
    this.externalId = externalId;
    return this;
  }

  public OrderDtoDataBuilder withEmergency(Boolean emergency) {
    this.emergency = emergency;
    return this;
  }

  public OrderDtoDataBuilder withQuotedCost(BigDecimal quotedCost) {
    this.quotedCost = quotedCost;
    return this;
  }

  public OrderDtoDataBuilder withOrderLineItems(List<OrderLineItemDto> orderLineItems) {
    this.orderLineItems = orderLineItems;
    return this;
  }

  public OrderDtoDataBuilder withCreatedBy(UserDto createdBy) {
    this.createdBy = createdBy;
    return this;
  }
}
