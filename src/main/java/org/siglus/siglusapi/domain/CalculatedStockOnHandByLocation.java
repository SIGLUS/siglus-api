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

import java.util.Date;
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
import org.siglus.siglusapi.repository.dto.StockCardStockDto;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "calculated_stocks_on_hand_by_location", schema = "siglusintegration")
public class CalculatedStockOnHandByLocation extends BaseEntity {

  @Column(name = "stockcardid")
  private UUID stockCardId;

  @Column(name = "occurreddate")
  private Date occurredDate;

  @Column(name = "stockonhand")
  private Integer stockOnHand;

  @Column(name = "calculatedstockonhandid")
  private UUID calculatedStocksOnHandId;

  @Column(name = "locationcode")
  private String locationCode;

  @Column(name = "area")
  private String area;

  public StockCardStockDto toStockCardStockDto() {
    return StockCardStockDto.builder()
            .stockCardId(stockCardId)
            .stockOnHand(stockOnHand)
            .locationCode(locationCode)
            .area(area)
            .build();
  }
}
