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

import static org.apache.commons.lang3.BooleanUtils.isFalse;

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

  /**
   * Creates a new Program with given code.
   *
   * @param programCode the program code
   */
  public Program(String programCode) {
    this.code = Code.code(programCode);
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

  /**
   * Creates new program object based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of program.
   */
  public static Program newProgram(Importer importer) {
    Program program = new Program();
    program.setId(importer.getId());
    program.setCode(Code.code(importer.getCode()));
    program.setName(importer.getName());
    program.setDescription(importer.getDescription());
    program.setActive(importer.getActive());
    program.setPeriodsSkippable(importer.getPeriodsSkippable());
    program.setSkipAuthorization(importer.getSkipAuthorization());
    program.setShowNonFullSupplyTab(importer.getShowNonFullSupplyTab());
    program.setEnableDatePhysicalStockCountCompleted(
        importer.getEnableDatePhysicalStockCountCompleted());

    return program;
  }

  /**
   * Exports current state of program object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(getId());
    String codeString = this.code.toString();
    if (isFalse(codeString.isEmpty())) {
      exporter.setCode(codeString);
    }
    exporter.setName(name);
    exporter.setDescription(description);
    exporter.setActive(active);
    exporter.setPeriodsSkippable(periodsSkippable);
    exporter.setSkipAuthorization(skipAuthorization);
    exporter.setShowNonFullSupplyTab(showNonFullSupplyTab);
    exporter.setEnableDatePhysicalStockCountCompleted(enableDatePhysicalStockCountCompleted);
  }

  public interface Exporter extends BaseExporter {

    void setCode(String code);

    void setName(String name);

    void setDescription(String description);

    void setActive(Boolean active);

    void setPeriodsSkippable(Boolean periodsSkippable);

    void setSkipAuthorization(Boolean skipAuthorization);

    void setShowNonFullSupplyTab(Boolean showNonFullSupplyTab);

    void setEnableDatePhysicalStockCountCompleted(Boolean enableDatePhysicalStockCountCompleted);
  }

  public interface Importer extends BaseImporter {

    String getCode();

    String getName();

    String getDescription();

    Boolean getActive();

    Boolean getPeriodsSkippable();

    Boolean getSkipAuthorization();

    Boolean getShowNonFullSupplyTab();

    Boolean getEnableDatePhysicalStockCountCompleted();
  }
}
