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

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;
import org.siglus.common.domain.BaseEntity;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "programs", schema = "referencedata")
@TypeName("Program")
public class Program extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Embedded
  private Code code;

  @Column(columnDefinition = "text")
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  private Boolean active;

  @Column(nullable = false)
  private Boolean periodsSkippable;

  @Column(nullable = false)
  private Boolean skipAuthorization;

  private Boolean showNonFullSupplyTab;

  @Column(nullable = false)
  private Boolean enableDatePhysicalStockCountCompleted;

  private Program() {
    code = null;
  }

  /**
   * Creates a new Program with given id.
   *
   * @param id the program id
   */
  public Program(UUID id) {
    setId(id);
  }

  @PrePersist
  private void prePersist() {
    if (periodsSkippable == null) {
      periodsSkippable = false;
    }
    if (enableDatePhysicalStockCountCompleted == null) {
      enableDatePhysicalStockCountCompleted = false;
    }
    if (skipAuthorization == null) {
      skipAuthorization = false;
    }
  }

  /**
   * Equal by a Program's code.
   *
   * @param other the other Program
   * @return true if the two Program's {@link Code} are equal.
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Program)) {
      return false;
    }

    Program otherProgram = (Program) other;
    return code.equals(otherProgram.code);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(code);
  }

}
