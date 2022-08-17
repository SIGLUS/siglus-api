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

package org.siglus.siglusapi.domain;

import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftLineItemDto;
import org.springframework.beans.BeanUtils;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Getter
@Setter
@Table(name = "product_location_movement_drafts", schema = "siglusintegration")
public class ProductLocationMovementDraft extends BaseEntity {

  @Column(nullable = false)
  private UUID facilityId;

  @Column(nullable = false)
  private UUID programId;

  private LocalDate createdDate;

  private String signature;

  private UUID userId;

  @LazyCollection(FALSE)
  @OneToMany(cascade = ALL, mappedBy = "productLocationMovementDraft", orphanRemoval = true)
  private List<ProductLocationMovementDraftLineItem> lineItems;

  public static ProductLocationMovementDraft createEmptyStockMovementDraft(
      ProductLocationMovementDraftDto movementDraftDto) {
    ProductLocationMovementDraft movementDraft = new ProductLocationMovementDraft();
    BeanUtils.copyProperties(movementDraftDto, movementDraft);
    return movementDraft;
  }

  public static ProductLocationMovementDraft createMovementDraft(ProductLocationMovementDraftDto movementDraftDto) {
    ProductLocationMovementDraft movementDraft = new ProductLocationMovementDraft();
    BeanUtils.copyProperties(movementDraftDto, movementDraft);
    List<ProductLocationMovementDraftLineItemDto> lineItemDtos = movementDraftDto.getLineItems();
    if (lineItemDtos != null) {
      movementDraft.setLineItems(lineItemDtos.stream()
          .map(lineItemDto -> ProductLocationMovementDraftLineItem.from(lineItemDto, movementDraft))
          .collect(toList()));
    }
    return movementDraft;
  }
}
