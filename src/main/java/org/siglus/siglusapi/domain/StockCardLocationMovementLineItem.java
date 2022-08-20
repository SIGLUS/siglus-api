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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "stock_card_location_movement_line_items", schema = "siglusintegration")
public class StockCardLocationMovementLineItem extends BaseEntity {

  @Column(nullable = false)
  private UUID stockCardId;

  @Column(nullable = false)
  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")
  private LocalDate occurredDate;

  private String signature;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String srcArea;

  @Column(nullable = false)
  private String srcLocationCode;

  @Column(nullable = false)
  private String destArea;

  @Column(nullable = false)
  private String destLocationCode;

  @Column(nullable = false)
  private Integer quantity;
}
