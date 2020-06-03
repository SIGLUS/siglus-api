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

import static org.openlmis.fulfillment.domain.BaseEntity.UUID_TYPE;

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Embeddable
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class UpdateDetails {

  @Type(type = UUID_TYPE)
  private final UUID updaterId;

  @Column(columnDefinition = "timestamp with time zone")
  private final ZonedDateTime updatedDate;

  /**
   * Default constructor needed by framework.
   */
  private UpdateDetails() {
    this.updaterId = null;
    this.updatedDate = null;
  }

  public interface Exporter {
    void setUpdaterId(UUID updaterId);

    void setUpdatedDate(ZonedDateTime updatedDate);
  }

  /**
   * Copy data from the given update details to the instance that implement
   * {@link Exporter} interface.
   */
  public void export(Exporter exporter) {
    exporter.setUpdatedDate(updatedDate);
    exporter.setUpdaterId(updaterId);
  }
}
