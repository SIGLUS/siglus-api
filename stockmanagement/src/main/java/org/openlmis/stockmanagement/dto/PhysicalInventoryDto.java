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

package org.openlmis.stockmanagement.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicalInventoryDto {
  private UUID id;

  private UUID programId;

  private UUID facilityId;

  @JsonFormat(shape = STRING)
  private LocalDate occurredDate;

  private String signature;
  private String documentNumber;

  private Boolean isStarter;

  private Boolean isDraft;

  private List<PhysicalInventoryLineItemDto> lineItems;

  /**
   * Convert into physical inventory jpa model for create new draft.
   *
   * @return converted jpa model.
   */
  public PhysicalInventory toEmptyPhysicalInventory() {
    PhysicalInventory inventory = new PhysicalInventory();
    inventory.setProgramId(programId);
    inventory.setFacilityId(facilityId);
    inventory.setIsDraft(true);
    return inventory;
  }

  /**
   * Convert into physical inventory jpa model for submit.
   *
   * @return converted jpa model.
   */
  public PhysicalInventory toPhysicalInventoryForSubmit() {
    return toPhysicalInventory(false);
  }

  /**
   * Convert into physical inventory jpa model for draft.
   *
   * @return converted jpa model.
   */
  public PhysicalInventory toPhysicalInventoryForDraft() {
    return toPhysicalInventory(true);
  }

  /**
   * Create from jpa model.
   *
   * @param inventories inventory jpa model.
   * @return created dto.
   */
  public static List<PhysicalInventoryDto> from(Collection<PhysicalInventory> inventories) {
    List<PhysicalInventoryDto> inventoryDtos = new ArrayList<>(inventories.size());
    inventories.forEach(i -> inventoryDtos.add(from(i)));
    return inventoryDtos;
  }

  /**
   * Create from jpa model.
   *
   * @param inventory inventory jpa model.
   * @return created dto.
   */
  public static PhysicalInventoryDto from(PhysicalInventory inventory) {
    return PhysicalInventoryDto.builder()
        .id(inventory.getId())
        .programId(inventory.getProgramId())
        .facilityId(inventory.getFacilityId())
        .occurredDate(inventory.getOccurredDate())
        .documentNumber(inventory.getDocumentNumber())
        .signature(inventory.getSignature())
        .isStarter(false)
        .isDraft(inventory.getIsDraft())
        .lineItems(inventory.getLineItems().stream().map(
            PhysicalInventoryLineItemDto::from).collect(toList()))
        .build();
  }

  /**
   * Create physical inventory dto object from stock event dto object.
   *
   * @param eventDto event dto.
   * @return created physical inventory dto.
   */
  public static PhysicalInventoryDto fromEventDto(StockEventDto eventDto) {
    return PhysicalInventoryDto.builder()
        .id(eventDto.getResourceId())
        .programId(eventDto.getProgramId())
        .facilityId(eventDto.getFacilityId())
        .signature(eventDto.getSignature())
        .documentNumber(eventDto.getDocumentNumber())
        .occurredDate(eventDto.getLineItems().get(0).getOccurredDate())
        .lineItems(PhysicalInventoryLineItemDto.from(eventDto.getLineItems()))
        .isDraft(false)
        .build();
  }

  private PhysicalInventory toPhysicalInventory(boolean isDraft) {
    PhysicalInventory inventory = new PhysicalInventory();
    inventory.setId(id);
    inventory.setProgramId(programId);
    inventory.setFacilityId(facilityId);
    inventory.setOccurredDate(occurredDate);
    inventory.setDocumentNumber(documentNumber);
    inventory.setSignature(signature);
    inventory.setIsDraft(isDraft);

    if (lineItems != null) {
      inventory.setLineItems(lineItems.stream()
          .map(lineItemDto -> lineItemDto.toPhysicalInventoryLineItem(inventory))
          .collect(toList()));
    }
    return inventory;
  }
}
