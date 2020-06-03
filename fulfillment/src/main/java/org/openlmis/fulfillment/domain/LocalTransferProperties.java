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

package org.openlmis.fulfillment.domain;

import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("local")
@NoArgsConstructor
public class LocalTransferProperties extends TransferProperties {

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String path;

  /**
   * Creates a new instance of {@link LocalTransferProperties} based on data from {@link Importer}.
   *
   * @param importer instance that implement {@link Importer}
   * @return an instance of {@link LocalTransferProperties}
   */
  public static LocalTransferProperties newInstance(Importer importer) {
    LocalTransferProperties local = new LocalTransferProperties();
    local.id = importer.getId();

    Optional.ofNullable(importer.getFacility())
        .ifPresent(facility -> local.setFacilityId(facility.getId()));

    local.transferType = importer.getTransferType();
    local.path = importer.getPath();

    return local;
  }

  public interface Exporter extends BaseExporter {

    void setPath(String path);

  }

  public interface Importer extends BaseImporter {

    String getPath();

  }

}
