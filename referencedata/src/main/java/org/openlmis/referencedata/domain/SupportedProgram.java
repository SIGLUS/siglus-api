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
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "supported_programs", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@TypeName("SupportedProgram")
@EqualsAndHashCode(of = "facilityProgram")
public final class SupportedProgram {

  @EmbeddedId
  @Getter
  private SupportedProgramPrimaryKey facilityProgram;

  @Column(nullable = false)
  @Getter
  private Boolean active;

  @Column(nullable = false)
  @Getter
  private Boolean locallyFulfilled;

  @SuppressWarnings("squid:S3437")
  // https://github.com/jhipster/generator-jhipster/issues/4553
  private LocalDate startDate;

  public boolean isActiveFor(Program program) {
    return facilityProgram.getProgram().equals(program) && active;
  }

  public UUID programId() {
    return facilityProgram.getProgram().getId();
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setProgram(facilityProgram.getProgram());
    exporter.setSupportActive(active);
    exporter.setSupportStartDate(startDate);
    exporter.setSupportLocallyFulfilled(locallyFulfilled);
  }

  public interface Exporter {
    void setProgram(Program program);

    void setSupportActive(boolean supportActive);

    void setSupportLocallyFulfilled(boolean supportLocallyFulfilled);

    void setSupportStartDate(LocalDate supportStartDate);
  }
}
