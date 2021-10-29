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

package org.siglus.siglusapi.service.android.mapper;

import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.siglus.siglusapi.dto.android.response.RequisitionBasicResponse;

@Mapper(componentModel = "spring")
public interface PodRequisitionMapper {

  default RequisitionBasicResponse toResponse(UUID orderId, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, Requisition> orderIdToRequisition) {
    if (orderId == null) {
      return null;
    }
    return toRequisitionResponse(orderIdToOrder.get(orderId), orderIdToOrder, orderIdToRequisition);
  }

  @Mapping(target = "number", source = "requisitionNumber")
  @Mapping(target = "isEmergency", source = "emergency")
  @Mapping(target = "programCode", source = "program.code")
  @Mapping(target = "processedDate", source = "id", qualifiedByName = "toProcessedDate")
  @Mapping(target = "serverProcessedDate", source = "id", qualifiedByName = "toServerProcessedDate")
  @Mapping(target = ".", source = "processingPeriod")
  RequisitionBasicResponse toRequisitionResponse(OrderDto order, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, Requisition> orderIdToRequisition);

  @Named("toProcessedDate")
  default Instant toProcessedDate(UUID orderId, @Context Map<UUID, Requisition> orderIdToRequisition) {
    Requisition requisition = orderIdToRequisition.get(orderId);
    if (requisition == null) {
      return null;
    }
    Object clientSubmittedTime = requisition.getExtraData().get(CLIENT_SUBMITTED_TIME);
    if (clientSubmittedTime == null) {
      return requisition.getCreatedDate().toInstant();
    }
    return Instant.parse(String.valueOf(clientSubmittedTime));
  }

  @Named("toServerProcessedDate")
  default Instant toServerProcessedDate(UUID orderId, @Context Map<UUID, Requisition> orderIdToRequisition) {
    Requisition requisition = orderIdToRequisition.get(orderId);
    if (requisition == null || requisition.getCreatedDate() == null) {
      return null;
    }
    return Instant.from(requisition.getCreatedDate());
  }
}
