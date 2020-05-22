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

package org.openlmis.requisition.domain.requisition;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.openlmis.requisition.domain.BaseEntity;

@Entity
@Table(name = "stock_adjustments", schema = "requisition")
@NoArgsConstructor
public class StockAdjustment extends BaseEntity {

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID reasonId;

  @Column(nullable = false)
  @Getter
  @Setter
  private Integer quantity;

  public StockAdjustment(UUID reasonId, Integer quantity) {
    this.reasonId = reasonId;
    this.quantity = quantity;
  }

  /**
   * Creates new StockAdjustment object based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of StockAdjustment.
   */
  public static StockAdjustment newStockAdjustment(Importer importer) {
    StockAdjustment stockAdjustment = new StockAdjustment(
        importer.getReasonId(), importer.getQuantity());
    stockAdjustment.id = importer.getId();

    return stockAdjustment;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setReasonId(reasonId);
    exporter.setQuantity(quantity);
  }

  public interface Exporter {
    void setId(UUID id);

    void setQuantity(Integer quantity);

    void setReasonId(UUID reasonId);
  }

  public interface Importer {
    UUID getId();

    UUID getReasonId();

    Integer getQuantity();
  }
}
