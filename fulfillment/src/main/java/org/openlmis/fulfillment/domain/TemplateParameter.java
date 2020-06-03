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

import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template_parameters", schema = "fulfillment")
@NoArgsConstructor
@AllArgsConstructor
public class TemplateParameter extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "templateId", nullable = false)
  @Getter
  @Setter
  private Template template;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String name;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String displayName;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String defaultValue;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String dataType;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String selectSql;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String description;

  /**
   * Create new instance of TemplateParameter based on given {@link Importer}.
   * @param importer instance of {@link Importer}
   * @return instance of TemplateParameter.
   */
  public static TemplateParameter newInstance(Importer importer) {
    TemplateParameter templateParameter = new TemplateParameter();
    templateParameter.setId(importer.getId());
    templateParameter.setName(importer.getName());
    templateParameter.setDisplayName(importer.getDisplayName());
    templateParameter.setDefaultValue(importer.getDefaultValue());
    templateParameter.setSelectSql(importer.getSelectSql());
    templateParameter.setDescription(importer.getDescription());
    templateParameter.setDataType(importer.getDataType());
    return templateParameter;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setDescription(description);
    exporter.setDataType(dataType);
    exporter.setDefaultValue(defaultValue);
    exporter.setDisplayName(displayName);
    exporter.setSelectSql(selectSql);
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setDisplayName(String displayName);

    void setDefaultValue(String defaultValue);

    void setDataType(String dataType);

    void setSelectSql(String selectSql);

    void setDescription(String description);

  }

  public interface Importer {
    UUID getId();

    String getName();

    String getDisplayName();

    String getDefaultValue();

    String getDataType();

    String getSelectSql();

    String getDescription();

  }
}
