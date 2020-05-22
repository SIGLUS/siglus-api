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

package org.openlmis.requisition.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "jasper_templates", schema = "requisition")
@NoArgsConstructor
@AllArgsConstructor
public class JasperTemplate extends BaseEntity {

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION, unique = true, nullable = false)
  @Getter
  @Setter
  private String name;

  @Column
  @Getter
  @Setter
  private byte[] data;

  @OneToMany(
      mappedBy = "template",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Fetch(FetchMode.SELECT)
  @Getter
  @Setter
  private List<JasperTemplateParameter> templateParameters;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String type;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String description;

  @PrePersist
  private void prePersist() {
    forEachParameter(line -> line.setTemplate(this));
  }

  @PreUpdate
  private void preUpdate() {
    forEachParameter(line -> line.setTemplate(this));
  }

  /**
   * Copy values of attributes into new or updated Template.
   *
   * @param jasperTemplate Template with new values.
   */
  public void updateFrom(JasperTemplate jasperTemplate) {
    this.name = jasperTemplate.getName();
    this.data = jasperTemplate.getData();
    this.templateParameters = jasperTemplate.getTemplateParameters();
    this.type = jasperTemplate.getType();
    this.description = jasperTemplate.getDescription();
  }

  public void forEachParameter(Consumer<JasperTemplateParameter> consumer) {
    Optional.ofNullable(templateParameters)
        .ifPresent(list -> list.forEach(consumer));
  }

  /**
   * Create a new instance of Jasper template based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance od template.
   */
  public static JasperTemplate newInstance(Importer importer) {
    JasperTemplate jasperTemplate = new JasperTemplate();
    jasperTemplate.setId(importer.getId());
    jasperTemplate.setName(importer.getName());
    jasperTemplate.setData(importer.getData());
    jasperTemplate.setType(importer.getType());
    jasperTemplate.setDescription(importer.getDescription());
    jasperTemplate.setTemplateParameters(new ArrayList<>());

    if (importer.getTemplateParameters() != null) {
      importer.getTemplateParameters().forEach(tp -> jasperTemplate.getTemplateParameters()
          .add(JasperTemplateParameter.newInstance(tp)));
    }
    return jasperTemplate;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setData(data);
    exporter.setDescription(description);
    exporter.setId(id);
    exporter.setName(name);
    exporter.setType(type);
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setData(byte[] data);

    void setType(String type);

    void setDescription(String description);

  }

  public interface Importer {
    UUID getId();

    String getName();

    byte[] getData();

    String getType();

    String getDescription();

    List<JasperTemplateParameter.Importer> getTemplateParameters();

  }
}
