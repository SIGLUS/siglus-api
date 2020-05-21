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
@Table(name = "geographic_levels", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@TypeName("GeographicLevel")
public class GeographicLevel extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Getter
  @Setter
  private String code;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String name;

  @Column(nullable = false)
  @Getter
  @Setter
  private Integer levelNumber;

  public GeographicLevel(String code, int levelNumber) {
    this.code = code;
    this.levelNumber = levelNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GeographicLevel)) {
      return false;
    }
    GeographicLevel that = (GeographicLevel) obj;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  /**
   * Creates new geographic level object based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of geographic level.
   */
  public static GeographicLevel newGeographicLevel(Importer importer) {
    GeographicLevel geographicLevel = new GeographicLevel();
    geographicLevel.setId(importer.getId());
    geographicLevel.setCode(importer.getCode());
    geographicLevel.setName(importer.getName());
    geographicLevel.setLevelNumber(importer.getLevelNumber());

    return geographicLevel;
  }

  /**
   * Exports current state of geographic level object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCode(code);
    exporter.setName(name);
    exporter.setLevelNumber(levelNumber);
  }

  public interface Exporter extends BaseExporter {

    void setCode(String code);

    void setName(String name);

    void setLevelNumber(Integer levelNumber);
  }

  public interface Importer extends BaseImporter {

    String getCode();

    String getName();

    Integer getLevelNumber();

  }
}
