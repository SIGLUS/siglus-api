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

import java.util.UUID;
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
@TypeName("IdealStockAmount")
@Table(name = "ideal_stock_amounts", schema = "referencedata")
@AllArgsConstructor
@NoArgsConstructor
public class  IdealStockAmount extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "facilityid", nullable = false)
  @Getter
  @Setter
  private Facility facility;

  @ManyToOne
  @JoinColumn(name = "commoditytypeid", nullable = false)
  @Getter
  @Setter
  private CommodityType commodityType;

  @ManyToOne
  @JoinColumn(name = "processingperiodid", nullable = false)
  @Getter
  @Setter
  private ProcessingPeriod processingPeriod;

  @Column
  @Getter
  @Setter
  private Integer amount;

  /**
   * Creates new ideal stock amount object based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of ideal stock amount.
   */
  public static IdealStockAmount newIdealStockAmount(Importer importer) {
    IdealStockAmount isa = new IdealStockAmount(Facility.newFacility(importer.getFacility()),
        CommodityType.newInstance(importer.getCommodityType()),
        ProcessingPeriod.newPeriod(importer.getProcessingPeriod()),
        importer.getAmount());
    isa.setId(importer.getId());
    return isa;
  }

  /**
   * Exports current state of isa object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setFacility(facility);
    exporter.setCommodityType(commodityType);
    exporter.setProcessingPeriod(processingPeriod);
    exporter.setAmount(amount);
  }

  public interface Exporter {

    void setId(UUID id);

    void setFacility(Facility facility);

    void setCommodityType(CommodityType commodityType);

    void setProcessingPeriod(ProcessingPeriod processingPeriod);

    void setAmount(Integer amount);
  }

  public interface Importer {

    UUID getId();

    Facility.Importer getFacility();

    CommodityType.Importer getCommodityType();

    ProcessingPeriod.Importer getProcessingPeriod();

    Integer getAmount();
  }
}
