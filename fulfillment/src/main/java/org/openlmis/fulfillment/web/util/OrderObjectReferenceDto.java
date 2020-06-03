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

import static org.openlmis.fulfillment.service.ResourceNames.ORDERS;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class OrderObjectReferenceDto extends ObjectReferenceDto {
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
  private UserObjectReferenceDto lastUpdater;
  private ZonedDateTime lastUpdatedDate;

  public OrderObjectReferenceDto(UUID id) {
    this(id, null);
  }

  public OrderObjectReferenceDto(UUID id, String serviceUrl) {
    super(id, serviceUrl, ORDERS);
  }

}
