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

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "facility_operators", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@TypeName("FacilityOperator")
public class FacilityOperator extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Getter
  @Setter
  private String code;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String name;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String description;

  @Getter
  @Setter
  private Integer displayOrder;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FacilityOperator)) {
      return false;
    }
    FacilityOperator that = (FacilityOperator) obj;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  /**
   * Creates new facility operator object based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of facility operator.
   */
  public static FacilityOperator newFacilityOperator(Importer importer) {
    FacilityOperator facilityOperator = new FacilityOperator();
    facilityOperator.setId(importer.getId());
    facilityOperator.setCode(importer.getCode());
    facilityOperator.setName(importer.getName());
    facilityOperator.setDescription(importer.getDescription());
    facilityOperator.setDisplayOrder(importer.getDisplayOrder());

    return facilityOperator;
  }

  /**
   * Exports current state of facility operator object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCode(code);
    exporter.setName(name);
    exporter.setDescription(description);
    exporter.setDisplayOrder(displayOrder);
  }

  public interface Exporter extends BaseExporter {

    void setCode(String code);

    void setName(String name);

    void setDescription(String description);

    void setDisplayOrder(Integer displayOrder);

  }

  public interface Importer extends BaseImporter {

    String getCode();

    String getName();

    String getDescription();

    Integer getDisplayOrder();

  }
}
