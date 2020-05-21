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

package org.openlmis.requisition.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;

/**
 * Represents the results of a batch processing of requisitions.
 */
@Getter
public class RequisitionsProcessingStatusDto {

  private Set<ApproveRequisitionDto> requisitionDtos;
  private Set<RequisitionErrorMessage> requisitionErrors;

  public RequisitionsProcessingStatusDto() {
    this.requisitionDtos = new HashSet<>();
    this.requisitionErrors = new HashSet<>();
  }

  public void addProcessedRequisition(ApproveRequisitionDto requisitionDto) {
    requisitionDtos.add(requisitionDto);
  }

  public void addProcessingError(RequisitionErrorMessage errorMessage) {
    this.requisitionErrors.add(errorMessage);
  }

  /**
   * Removes skipped line items if they are skipped in all requisitions.
   */
  public void removeSkippedProducts() {
    // all requisition line items
    List<ApproveRequisitionLineItemDto> requisitionLineItems = requisitionDtos
        .stream()
        .map(ApproveRequisitionDto::getRequisitionLineItems)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    // all products. This list contains products that would be removed because all selected
    // requisitions skipped those products.
    Set<UUID> products = requisitionLineItems
        .stream()
        .map(ApproveRequisitionLineItemDto::getOrderable)
        .map(BasicOrderableDto::getId)
        .collect(Collectors.toSet());

    // if the given line item is not skipped in any requisition, it will not be removed
    // because it will not be present on the list.
    requisitionLineItems
        .stream()
        .filter(line -> BooleanUtils.isFalse(line.getSkipped()))
        .map(ApproveRequisitionLineItemDto::getOrderable)
        .map(BasicOrderableDto::getId)
        .forEach(products::remove);

    // find those requisition line items that contain skipped (in all requisitions) product
    // and remove it.
    for (ApproveRequisitionDto requisition : requisitionDtos) {
      Iterator<ApproveRequisitionLineItemDto> iterator = requisition
          .getRequisitionLineItems()
          .iterator();

      while (iterator.hasNext()) {
        ApproveRequisitionLineItemDto line = iterator.next();
        BasicOrderableDto orderable = line.getOrderable();
        UUID id = orderable.getId();

        if (products.contains(id)) {
          iterator.remove();
        }
      }
    }
  }

}
