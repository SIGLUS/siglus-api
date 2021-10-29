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

import static org.siglus.siglusapi.constant.FacilityTypeConstants.DDM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.DPM;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.getAndroidOriginMovementTypes;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;

import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.GeographicZoneDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.siglus.siglusapi.dto.android.response.OrderBasicResponse;

@Mapper(componentModel = "spring", uses = PodRequisitionMapper.class)
public interface PodOrderMapper {

  default OrderBasicResponse toResponse(UUID orderId, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, Requisition> orderIdToRequisition) {
    if (orderId == null) {
      return null;
    }
    return toOrderResponse(orderIdToOrder.get(orderId), orderIdToOrder, orderIdToRequisition);
  }

  @Mapping(target = "code", source = "orderCode")
  @Mapping(target = "lastModifiedDate", source = "lastUpdatedDate")
  @Mapping(target = "supplyFacilityName", source = "supplyingFacility.name")
  @Mapping(target = "requisition", source = "id")
  @Mapping(target = "supplyFacilityDistrict", source = "id", qualifiedByName = "toSupplyFacilityDistrict")
  @Mapping(target = "supplyFacilityProvince", source = "id", qualifiedByName = "toSupplyFacilityProvince")
  @Mapping(target = "supplyFacilityType", source = "id", qualifiedByName = "toSupplyFacilityType")
  OrderBasicResponse toOrderResponse(OrderDto order, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, Requisition> orderIdToRequisition);

  @Named("toSupplyFacilityDistrict")
  default String toSupplyFacilityDistrict(UUID orderId, @Context Map<UUID, OrderDto> orderIdToOrder) {
    GeographicZoneDto geographicZone = orderIdToOrder.get(orderId).getSupplyingFacility().getGeographicZone();
    if (geographicZone == null) {
      return null;
    }
    return geographicZone.getName();
  }

  @Named("toSupplyFacilityProvince")
  default String toSupplyFacilityProvince(UUID orderId, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, Requisition> orderIdToRequisition) {
    GeographicZoneDto geographicZone = orderIdToOrder.get(orderId).getSupplyingFacility().getGeographicZone();
    if (geographicZone == null || geographicZone.getParent() == null) {
      return null;
    }
    return geographicZone.getParent().getName();
  }

  @Named("toSupplyFacilityType")
  default String toSupplyFacilityType(UUID orderId, @Context Map<UUID, OrderDto> orderIdToOrder,
      @Context Map<UUID, Requisition> orderIdToRequisition) {
    OrderDto orderDto = orderIdToOrder.get(orderId);
    FacilityDto supplyingFacility = orderDto.getSupplyingFacility();
    if (supplyingFacility == null || supplyingFacility.getType() == null) {
      return null;
    }
    String facilityType = supplyingFacility.getType().getCode();
    String programCode = orderDto.getProgram().getCode();
    if (getAndroidOriginMovementTypes().contains(facilityType)) {
      return facilityType;
    } else if (VIA_PROGRAM_CODE.equals(programCode)) {
      return DDM;
    }
    return DPM;
  }
}
