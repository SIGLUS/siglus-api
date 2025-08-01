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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.domain.PodSubDraftLineItem;
import org.siglus.siglusapi.domain.PodSubDraftLineItemLocation;
import org.springframework.util.ObjectUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDeliverySubDraftLineItemDto {
  @NotNull
  private UUID id;
  @NotNull
  private VersionObjectReferenceDto orderable;
  private LotReferenceDto lot;
  private Integer quantityAccepted;
  private Boolean useVvm;
  private VvmStatus vvmStatus;
  private Integer quantityRejected;
  private UUID rejectionReasonId;
  private String notes;

  private List<PodSubDraftLineItemLocation> locations;
  private boolean added;
  private UUID subDraftId;

  public PodSubDraftLineItem toDraftLineItem(List<PodSubDraftLineItemLocation> locations) {
    VersionEntityReference orderableRef = new VersionEntityReference(orderable.getId(), orderable.getVersionNumber());
    PodSubDraftLineItem item = PodSubDraftLineItem.builder()
        .orderable(orderableRef)
        .lotId(lot == null ? null : lot.getId())
        .quantityAccepted(quantityAccepted)
        .useVvm(useVvm)
        .vvmStatus(vvmStatus)
        .quantityRejected(quantityRejected)
        .rejectionReasonId(rejectionReasonId)
        .notes(notes)
        .lotCode(lot == null ? null : lot.getLotCode())
        .expirationDate(lot == null ? null : lot.getExpirationDate())
        .locations(locations)
        .podSubDraftId(subDraftId)
        .build();
    item.setId(id);
    return item;
  }

  public static ProofOfDeliverySubDraftLineItemDto fromDraftLineItem(PodSubDraftLineItem item) {
    VersionObjectReferenceDto orderable = new VersionObjectReferenceDto(item.getOrderable().getId(),
        null, null, item.getOrderable().getVersionNumber());
    LotReferenceDto lot = LotReferenceDto.builder()
        .id(item.getLotId())
        .lotCode(item.getLotCode())
        .expirationDate(item.getExpirationDate())
        .build();
    return ProofOfDeliverySubDraftLineItemDto.builder()
        .id(item.getId())
        .subDraftId(item.getPodSubDraftId())
        .orderable(orderable)
        .lot(lot)
        .quantityAccepted(item.getQuantityAccepted())
        .useVvm(item.getUseVvm())
        .vvmStatus(item.getVvmStatus())
        .quantityRejected(item.getQuantityRejected())
        .rejectionReasonId(item.getRejectionReasonId())
        .notes(item.getNotes())
        .locations(item.getLocations())
        .build();
  }

  public static ProofOfDeliverySubDraftLineItemDto from(ProofOfDeliveryLineItemDto dto, UUID subDraftId) {
    return ProofOfDeliverySubDraftLineItemDto.builder()
        .id(dto.getId())
        .orderable(dto.getOrderable())
        .lot(LotReferenceDto.from(dto.getLot()))
        .quantityAccepted(dto.getQuantityAccepted())
        .useVvm(dto.getUseVvm())
        .vvmStatus(dto.getVvmStatus())
        .quantityRejected(dto.getQuantityRejected())
        .rejectionReasonId(dto.getRejectionReasonId())
        .notes(dto.getNotes())
        .subDraftId(subDraftId)
        .build();
  }

  public ProofOfDeliveryLineItemDto to() {
    ProofOfDeliveryLineItemDto dto = new ProofOfDeliveryLineItemDto(null, orderable,
        lot == null ? null : lot.toObjectReferenceDto(),
        quantityAccepted, useVvm, vvmStatus, quantityRejected, rejectionReasonId, notes);
    dto.setId(id);
    return dto;
  }

  public ProofOfDeliveryLineItem toPodLineItem() {
    VersionEntityReference orderableRef = new VersionEntityReference(orderable.getId(), orderable.getVersionNumber());
    return new ProofOfDeliveryLineItem(orderableRef, lot.getId(),
        quantityAccepted, vvmStatus, quantityRejected, rejectionReasonId, notes);
  }

  public List<PodLineItemWithLocationDto> getLocationDtos() {
    if (ObjectUtils.isEmpty(locations)) {
      return new ArrayList<>();
    }
    return locations.stream().map(
        location -> PodLineItemWithLocationDto.builder()
            .podLineItemId(id)
            .locationCode(location.getLocationCode())
            .area(location.getArea())
            .quantityAccepted(location.getQuantityAccepted())
            .build()
    ).collect(Collectors.toList());
  }

  public List<PodLineItemsByLocation> toLineItemByLocation() {
    if (ObjectUtils.isEmpty(locations)) {
      return new ArrayList<>();
    }
    return locations.stream().map(
        location -> PodLineItemsByLocation.builder()
            .podLineItemId(id)
            .locationCode(location.getLocationCode())
            .area(location.getArea())
            .quantityAccepted(location.getQuantityAccepted())
            .build()
    ).collect(Collectors.toList());
  }
}
