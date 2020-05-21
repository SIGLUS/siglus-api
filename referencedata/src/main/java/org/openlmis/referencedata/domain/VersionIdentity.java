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

import static org.openlmis.referencedata.domain.BaseEntity.UUID_TYPE;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@ToString
public class VersionIdentity implements Serializable {

  @Type(type = UUID_TYPE)
  private UUID id;

  private Long versionNumber;

  VersionIdentity() {
    this(null, null);
  }

  /**
   * Version identity. Represents a composite ID of entities that are versioned.
   *
   * @param id the UUID which is an unique identifier of an instance
   * @param versionNumber sequential version number
   */
  public VersionIdentity(UUID id, Long versionNumber) {
    // it seems like we can't use @GeneratedValue and @GenericGenerator annotations
    // in the @Embeddable class like this one. That is why we generate a value for
    // the id field manually but only if the passed value is null.
    this.id = Optional.ofNullable(id).orElseGet(UUID::randomUUID);
    this.versionNumber = Optional.ofNullable(versionNumber).orElse(1L);
  }

  interface VersionExporter {

    void setVersionNumber(Long versionNumber);

  }

  interface VersionImporter {

    Long getVersionNumber();

  }

}
