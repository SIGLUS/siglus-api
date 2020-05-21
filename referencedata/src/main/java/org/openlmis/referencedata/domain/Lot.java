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

package org.openlmis.referencedata.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "lots", schema = "referencedata")
@TypeName("Lot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lot extends BaseEntity {

  @Column(nullable = false, columnDefinition = "text")
  private String lotCode;

  private LocalDate expirationDate;

  private LocalDate manufactureDate;

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(nullable = false, name = "tradeitemid")
  private TradeItem tradeItem;

  @Column(nullable = false, columnDefinition = "boolean DEFAULT false")
  private boolean active;

  /**
   * Creates new lot object based on data from {@link Importer} and tradeItem argument.
   *
   * @param importer instance of {@link Importer}
   * @param tradeItem tradeItem to set.
   * @return new instance of facility.
   */
  public static Lot newLot(Importer importer, TradeItem tradeItem) {
    Lot lot = new Lot();
    lot.setId(importer.getId());
    lot.setLotCode(importer.getLotCode());
    lot.setActive(importer.isActive());
    if (importer.getExpirationDate() != null) {
      lot.setExpirationDate(importer.getExpirationDate());
    }
    if (importer.getManufactureDate() != null) {
      lot.setManufactureDate(importer.getManufactureDate());
    }
    lot.setTradeItem(tradeItem);
    return lot;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setLotCode(lotCode);
    exporter.setTradeItemId(tradeItem.getId());
    exporter.setActive(active);
    if (expirationDate != null) {
      exporter.setExpirationDate(expirationDate);
    }
    if (manufactureDate != null) {
      exporter.setManufactureDate(manufactureDate);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Lot)) {
      return false;
    }
    Lot lot = (Lot) obj;
    return Objects.equals(lotCode, lot.lotCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lotCode);
  }

  public interface Exporter {
    void setId(UUID id);

    void setLotCode(String lotCode);

    void setActive(boolean active);

    void setTradeItemId(UUID tradeItemId);

    void setExpirationDate(LocalDate expirationDate);

    void setManufactureDate(LocalDate manufactureDate);
  }

  public interface Importer {
    UUID getId();

    String getLotCode();

    boolean isActive();

    UUID getTradeItemId();

    LocalDate getExpirationDate();

    LocalDate getManufactureDate();
  }
}
