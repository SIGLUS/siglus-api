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

import static java.util.Objects.isNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.BatchSize;
import org.openlmis.requisition.dto.AvailableRequisitionColumnOptionDto;

@Entity
@Table(name = "available_requisition_columns", schema = "requisition")
@Getter
@Setter
@AllArgsConstructor
public class AvailableRequisitionColumn extends BaseEntity {

  private String name;

  @ElementCollection(fetch = FetchType.LAZY, targetClass = SourceType.class)
  @Enumerated(EnumType.STRING)
  @Column(name = "value")
  @CollectionTable(
      name = "available_requisition_column_sources",
      joinColumns = @JoinColumn(name = "columnId"))
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private Set<SourceType> sources;

  @OneToMany(
      mappedBy = "requisitionColumn",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY)
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private Set<AvailableRequisitionColumnOption> options;

  private String label;

  private String indicator;

  private Boolean mandatory;

  private Boolean isDisplayRequired;

  private Boolean canChangeOrder;

  private Boolean canBeChangedByUser;

  private Boolean supportsTag;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  private String definition;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ColumnType columnType;

  public AvailableRequisitionColumn() {
    this.sources = new HashSet<>();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AvailableRequisitionColumn)) {
      return false;
    }

    AvailableRequisitionColumn that = (AvailableRequisitionColumn) obj;

    return new EqualsBuilder()
        .append(name, that.name)
        .append(sources, that.sources)
        .append(options, that.options)
        .append(label, that.label)
        .append(indicator, that.indicator)
        .append(mandatory, that.mandatory)
        .append(isDisplayRequired, that.isDisplayRequired)
        .append(canChangeOrder, that.canChangeOrder)
        .append(canBeChangedByUser, that.canBeChangedByUser)
        .append(supportsTag, that.supportsTag)
        .append(definition, that.definition)
        .append(columnType, that.columnType)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(name)
        .append(sources)
        .append(options)
        .append(label)
        .append(indicator)
        .append(mandatory)
        .append(isDisplayRequired)
        .append(canChangeOrder)
        .append(canBeChangedByUser)
        .append(supportsTag)
        .append(definition)
        .append(columnType)
        .toHashCode();
  }

  /**
   * Create a new instance of available requisition column based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of available requisition column.
   */
  public static AvailableRequisitionColumn newInstance(Importer importer) {
    if (isNull(importer)) {
      return null;
    }
    AvailableRequisitionColumn availableRequisitionColumn = new AvailableRequisitionColumn();
    availableRequisitionColumn.setId(importer.getId());
    availableRequisitionColumn.setName(importer.getName());
    availableRequisitionColumn.setSources(importer.getSources());

    availableRequisitionColumn.setOptions(new HashSet<>());
    Optional
        .ofNullable(importer.getOptions())
        .orElse(Collections.emptySet())
        .forEach(option -> availableRequisitionColumn.getOptions()
            .add(AvailableRequisitionColumnOption.newInstance(option)));

    availableRequisitionColumn.setLabel(importer.getLabel());
    availableRequisitionColumn.setIndicator(importer.getIndicator());
    availableRequisitionColumn.setMandatory(importer.getMandatory());
    availableRequisitionColumn.setIsDisplayRequired(importer.getIsDisplayRequired());
    availableRequisitionColumn.setCanChangeOrder(importer.getCanChangeOrder());
    availableRequisitionColumn.setCanBeChangedByUser(importer.getCanBeChangedByUser());
    availableRequisitionColumn.setSupportsTag(importer.getSupportsTag());
    availableRequisitionColumn.setDefinition(importer.getDefinition());
    availableRequisitionColumn.setColumnType(importer.getColumnType());
    return availableRequisitionColumn;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setSources(sources);
    exporter.setOptions(AvailableRequisitionColumnOptionDto.newInstance(options));
    exporter.setLabel(label);
    exporter.setIndicator(indicator);
    exporter.setMandatory(mandatory);
    exporter.setIsDisplayRequired(isDisplayRequired);
    exporter.setCanChangeOrder(canChangeOrder);
    exporter.setCanBeChangedByUser(canBeChangedByUser);
    exporter.setSupportsTag(supportsTag);
    exporter.setDefinition(definition);
    exporter.setColumnType(columnType);
  }

  public interface Importer {
    UUID getId();

    String getName();

    Set<SourceType> getSources();

    Set<AvailableRequisitionColumnOptionDto> getOptions();

    String getLabel();

    String getIndicator();

    Boolean getMandatory();

    Boolean getIsDisplayRequired();

    Boolean getCanChangeOrder();

    Boolean getCanBeChangedByUser();

    Boolean getSupportsTag();

    String getDefinition();

    ColumnType getColumnType();
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setSources(Set<SourceType> sources);

    void setOptions(Set<AvailableRequisitionColumnOptionDto> options);

    void setLabel(String label);

    void setIndicator(String indicator);

    void setMandatory(Boolean mandatory);

    void setIsDisplayRequired(Boolean isDisplayRequired);

    void setCanChangeOrder(Boolean canChangeOrder);

    void setCanBeChangedByUser(Boolean canBeChangedByUser);

    void setSupportsTag(Boolean supportsTag);

    void setDefinition(String definition);

    void setColumnType(ColumnType columnType);
  }
}
