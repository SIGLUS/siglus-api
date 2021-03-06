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

package org.siglus.common.domain.referencedata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import lombok.Getter;
import org.siglus.common.domain.BaseEntity;

/**
 * A dispensable describes how product is dispensed/given to a patient.
 * Description of the dispensable contains information about product form,
 * dosage, dispensing unit etc.
 */
@Entity
@Table(name = "dispensables", schema = "referencedata")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("abstract")
public abstract class Dispensable extends BaseEntity {

  public static final String KEY_DISPENSING_UNIT = "dispensingUnit";
  public static final String KEY_SIZE_CODE = "sizeCode";
  public static final String KEY_ROUTE_OF_ADMINISTRATION = "routeOfAdministration";

  @ElementCollection(fetch = FetchType.EAGER)
  @MapKeyColumn(name = "key")
  @Column(name = "value")
  @CollectionTable(name = "dispensable_attributes", schema = "referencedata",
      joinColumns = @JoinColumn(name = "dispensableid"))
  @Getter
  protected Map<String, String> attributes;

  protected Dispensable() {
    attributes = new HashMap<>();
  }

  @Override
  public abstract boolean equals(Object object);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setAttributes(Collections.unmodifiableMap(new HashMap<>(attributes)));
    exporter.setToString(toString());
  }

  public static Dispensable createNew(String dispensingUnit) {
    return new DefaultDispensable(dispensingUnit);
  }

  public interface Importer {
    Map<String, String> getAttributes();
  }

  public interface Exporter {
    void setAttributes(Map<String, String> attributes);

    void setToString(String toString);
  }
}
