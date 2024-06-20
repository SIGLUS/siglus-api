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

package org.siglus.siglusapi.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.springframework.util.ObjectUtils;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliverySubDraftDto {
  @NotNull
  private UUID id;
  private ShipmentObjectReferenceDto shipment;
  private ProofOfDeliveryStatus status;
  private List<ProofOfDeliverySubDraftLineItemDto> lineItems;
  private String receivedBy;
  private String deliveredBy;
  private LocalDate receivedDate;

  public static ProofOfDeliverySubDraftDto from(ProofOfDeliveryDto dto, List<PodLineItemsExtension> extensions) {
    List<ProofOfDeliverySubDraftLineItemDto> items = null;
    if (!ObjectUtils.isEmpty(dto.getLineItems())) {
      Map<UUID, PodLineItemsExtension> extensionMap = extensions.stream()
          .collect(Collectors.toMap(PodLineItemsExtension::getPodLineItemId, extension -> extension));
      items = dto.getLineItems().stream()
          .map(item -> {
            PodLineItemsExtension extension = extensionMap.get(item.getId());
            UUID subDraftId = ObjectUtils.isEmpty(extension) ? null : extension.getSubDraftId();
            return ProofOfDeliverySubDraftLineItemDto.from((ProofOfDeliveryLineItemDto) item, subDraftId);
          }).collect(Collectors.toList());
    }
    return ProofOfDeliverySubDraftDto.builder()
        .id(dto.getId())
        .shipment(dto.getShipment())
        .status(dto.getStatus())
        .receivedBy(dto.getReceivedBy())
        .deliveredBy(dto.getDeliveredBy())
        .receivedDate(dto.getReceivedDate())
        .lineItems(items)
        .build();
  }

  public static ProofOfDeliverySubDraftDto from(ProofOfDeliveryDto dto, UUID subDraftId) {
    List<ProofOfDeliverySubDraftLineItemDto> items = null;
    if (!ObjectUtils.isEmpty(dto.getLineItems())) {
      items = dto.getLineItems().stream()
          .map(item -> ProofOfDeliverySubDraftLineItemDto.from((ProofOfDeliveryLineItemDto) item, subDraftId))
          .collect(Collectors.toList());
    }
    return ProofOfDeliverySubDraftDto.builder()
        .id(dto.getId())
        .shipment(dto.getShipment())
        .status(dto.getStatus())
        .receivedBy(dto.getReceivedBy())
        .deliveredBy(dto.getDeliveredBy())
        .receivedDate(dto.getReceivedDate())
        .lineItems(items)
        .build();
  }

  public ProofOfDeliveryDto to() {
    List<ProofOfDeliveryLineItemDto> lineItemDtos = lineItems.stream()
        .map(ProofOfDeliverySubDraftLineItemDto::to).collect(Collectors.toList());
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto(null, shipment, status,
        lineItemDtos, receivedBy, deliveredBy, receivedDate);
    dto.setId(id);
    return dto;
  }

  public List<PodLineItemWithLocationDto> getLineItemLocations() {
    if (ObjectUtils.isEmpty(lineItems)) {
      return new ArrayList<>();
    }
    return lineItems.stream().map(ProofOfDeliverySubDraftLineItemDto::getLocationDtos)
        .flatMap(Collection::stream).collect(Collectors.toList());
  }
}
