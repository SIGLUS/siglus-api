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

import static org.openlmis.requisition.domain.BaseEntity.TEXT_COLUMN_DEFINITION;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.dto.AvailableRequisitionColumnDto;
import org.openlmis.requisition.dto.AvailableRequisitionColumnOptionDto;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RequisitionTemplateColumn {
  public static final String COLUMN_DEFINITION = "columnDefinition";
  public static final String DEFINITION_KEY = "definition";

  @Getter
  private String name;

  @Getter
  @Setter(AccessLevel.PACKAGE)
  private String label;

  private String indicator;

  @Getter
  @Setter(AccessLevel.PACKAGE)
  private int displayOrder;

  @Getter
  private Boolean isDisplayed;

  @Column(nullable = false)
  @Getter
  @Setter(AccessLevel.PACKAGE)
  private SourceType source;

  @ManyToOne(
      cascade = {CascadeType.REFRESH},
      fetch = FetchType.EAGER)
  @JoinColumn(name = "requisitionColumnId", nullable = false)
  @Getter
  private AvailableRequisitionColumn columnDefinition;

  @ManyToOne(
      cascade = {CascadeType.REFRESH},
      fetch = FetchType.EAGER)
  @JoinColumn(name = "requisitionColumnOptionId")
  @Getter
  @Setter(AccessLevel.PACKAGE)
  private AvailableRequisitionColumnOption option;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  private String definition;

  @Getter
  private String tag;

  RequisitionTemplateColumn copy() {
    return new RequisitionTemplateColumn(
        name, label, indicator, displayOrder, isDisplayed, source,
        columnDefinition, option, definition, tag
    );
  }

  /**
   * Allows changing visibility of specific column.
   * Modifies display order if column represents product code.
   *
   * @param isDisplayed Should the column be displayed.
   */
  public void setIsDisplayed(boolean isDisplayed) {
    if ("productCode".equals(this.name)) {
      this.displayOrder = 1;
    }
    this.isDisplayed = isDisplayed;
  }

  /**
   * Create a new instance of requisition template column based on data
   * from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance od template column.
   */
  public static RequisitionTemplateColumn newInstance(Importer importer) {
    return new RequisitionTemplateColumn(
        importer.getName(), importer.getLabel(), importer.getIndicator(),
        importer.getDisplayOrder(), importer.getIsDisplayed(), importer.getSource(),
        AvailableRequisitionColumn.newInstance(importer.getColumnDefinition()),
        AvailableRequisitionColumnOption.newInstance(importer.getOption()),
        importer.getDefinition(), importer.getTag()
    );
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setName(name);
    exporter.setLabel(label);
    exporter.setIndicator(indicator);
    exporter.setDisplayOrder(displayOrder);
    exporter.setIsDisplayed(isDisplayed);
    exporter.setSource(source);
    exporter.setOption(AvailableRequisitionColumnOptionDto.newInstance(option));
    exporter.setDefinition(definition);
    exporter.setTag(tag);
  }

  public interface Importer {
    String getName();

    String getLabel();

    String getIndicator();

    int getDisplayOrder();

    Boolean getIsDisplayed();

    SourceType getSource();

    AvailableRequisitionColumnDto getColumnDefinition();

    AvailableRequisitionColumnOptionDto getOption();

    String getDefinition();

    String getTag();
  }

  public interface Exporter {
    void setName(String name);

    void setLabel(String label);

    void setIndicator(String indicator);

    void setDisplayOrder(int displayOrder);

    void setIsDisplayed(Boolean isDisplayed);

    void setSource(SourceType source);

    void setOption(AvailableRequisitionColumnOptionDto option);

    void setDefinition(String definition);

    void setTag(String tag);
  }

}
