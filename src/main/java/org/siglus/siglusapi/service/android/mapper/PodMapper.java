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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.android.response.PodResponse;

@Mapper(componentModel = "spring", uses = {PodOrderMapper.class, PodProductLineMapper.class})
public interface PodMapper {

  default LocalDate toLocalDate(ZonedDateTime dateTime) {
    return dateTime.toLocalDate();
  }

  @Mapping(target = "order", source = "shipment.order.id")
  @Mapping(target = "products", source = "shipment.order.id")
  @Mapping(target = "shippedDate", source = "shipment.shippedDate")
  @Mapping(target = "documentNo", source = "shipment", qualifiedByName = "toDocumentNo")
  PodResponse toResponse(ProofOfDelivery pod, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, FacilityDto> orderIdToFacility, @Context Map<UUID, Requisition> orderIdToRequisition);

  @Named("toDocumentNo")
  default String toDocumentNo(Shipment domain) {
    if (domain.getExtraData() == null) {
      return null;
    }
    return domain.getExtraData().get("documentNo");
  }

}
