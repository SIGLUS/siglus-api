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

import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.siglusapi.dto.android.response.PodLotLineResponse;

@Mapper(componentModel = "spring", uses = LotMapper.class)
public interface PodLotLineMapper {

  @Named("toShippedQuantity")
  default Integer toShippedQuantity(ProofOfDeliveryLineItem podLine) {
    if (podLine.getQuantityAccepted() == null || podLine.getQuantityRejected() == null) {
      return null;
    }
    return podLine.getQuantityAccepted() + podLine.getQuantityRejected();
  }

  @Named("toReason")
  default String toReason(UUID reasonId, @Context Map<UUID, String> allReasons) {
    return allReasons.get(reasonId);
  }

  @Mapping(target = "lot", source = "lotId")
  @Mapping(target = "shippedQuantity", source = ".", qualifiedByName = "toShippedQuantity")
  @Mapping(target = "acceptedQuantity", source = "quantityAccepted")
  @Mapping(target = "rejectedReason", source = "rejectionReasonId", qualifiedByName = "toReason")
  PodLotLineResponse toLotResponse(ProofOfDeliveryLineItem podLine, @Context Map<UUID, LotDto> allLots,
      @Context Map<UUID, String> allReasons);

}
