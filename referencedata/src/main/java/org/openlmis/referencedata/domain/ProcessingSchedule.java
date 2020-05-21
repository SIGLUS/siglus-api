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

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "processing_schedules", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@TypeName("ProcessingSchedule")
public class ProcessingSchedule extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Getter
  @Setter
  private Code code;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String description;

  @Getter
  @Setter
  @Column(columnDefinition = "timestamp with time zone")
  private ZonedDateTime modifiedDate;

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Getter
  @Setter
  private String name;

  /**
   * Constructor for processing schedule. Code and name must not be null.
   */
  public ProcessingSchedule(Code code, String name) {
    this.code = Objects.requireNonNull(code);
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Static factory method for constructing a new processing schedule using an importer (DTO). 
   * Uses the {@link #ProcessingSchedule(Code, String)} constructor} to help create the object.
   *
   * @param importer the importer (DTO)
   */
  public static ProcessingSchedule newProcessingSchedule(Importer importer) {
    ProcessingSchedule newProcessingSchedule = new ProcessingSchedule(
        Code.code(importer.getCode()), importer.getName());
    newProcessingSchedule.id = importer.getId();
    newProcessingSchedule.description = importer.getDescription();
    return newProcessingSchedule;
  }

  @PrePersist
  @PreUpdate
  private void setModifiedDate() {
    this.modifiedDate = ZonedDateTime.now();
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setCode(code.toString());
    exporter.setDescription(description);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ProcessingSchedule)) {
      return false;
    }
    ProcessingSchedule that = (ProcessingSchedule) obj;
    return code.equals(that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code.hashCode());
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setCode(String code);

    void setDescription(String description);
  }

  public interface Importer {
    UUID getId();

    String getName();

    String getCode();

    String getDescription();
  }
}
