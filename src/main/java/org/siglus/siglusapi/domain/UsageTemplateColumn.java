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

package org.siglus.siglusapi.domain;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.BaseEntity;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.springframework.beans.BeanUtils;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "usage_columns_maps", schema = "siglusintegration")
public class UsageTemplateColumn extends BaseEntity {

  private UUID requisitionTemplateId;

  @ManyToOne(
      cascade = {CascadeType.REFRESH},
      fetch = FetchType.EAGER)
  @JoinColumn(name = "usagecolumnid", nullable = false)
  @Getter
  private AvailableUsageColumn columnDefinition;

  private String definition;

  private int displayOrder;

  private String indicator;

  private Boolean isDisplayed;

  private String label;

  private String name;

  private String source;

  private String availableSources;

  private String tag;

  @ManyToOne()
  @JoinColumn(name = "usageSectionId", nullable = false)
  private UsageTemplateColumnSection usageTemplateColumnSection;

  public static UsageTemplateColumn from(UUID requisitionTemplateId,
      AvailableUsageColumn column,
      UsageTemplateColumnSection section) {
    UsageTemplateColumn usageTemplateColumn = new UsageTemplateColumn();
    BeanUtils.copyProperties(column, usageTemplateColumn);
    usageTemplateColumn.setId(null);
    usageTemplateColumn.columnDefinition = column;
    usageTemplateColumn.availableSources = column.getSources();
    usageTemplateColumn.isDisplayed = true;
    usageTemplateColumn.source = column.getSources().isEmpty() ? ""
        : Arrays.asList(column.getSources().split("\\|")).get(0);
    usageTemplateColumn.usageTemplateColumnSection = section;
    usageTemplateColumn.setRequisitionTemplateId(requisitionTemplateId);

    return usageTemplateColumn;
  }

  public static UsageTemplateColumn from(UUID requisitionTemplateId,
      UsageTemplateColumnDto columnDto,
      UsageTemplateColumnSection section,
      List<AvailableUsageColumn> availableUsageColumns) {
    UsageTemplateColumn usageTemplateColumn = new UsageTemplateColumn();
    BeanUtils.copyProperties(columnDto, usageTemplateColumn);
    UUID availableColumnId = columnDto.getColumnDefinition().getId();
    if (availableColumnId != null) {
      usageTemplateColumn.setColumnDefinition(
          findAvailableUsageColumn(availableColumnId, availableUsageColumns));
    }
    List<String> sources = columnDto.getColumnDefinition().getSources();
    usageTemplateColumn.source = sources.isEmpty() ? "" : String.join("|", sources);
    usageTemplateColumn.usageTemplateColumnSection = section;
    usageTemplateColumn.setRequisitionTemplateId(requisitionTemplateId);

    return usageTemplateColumn;
  }

  private static AvailableUsageColumn findAvailableUsageColumn(UUID availableId,
      List<AvailableUsageColumn> availableUsageColumns) {
    return availableUsageColumns.stream()
        .filter(column -> column.getId().equals(availableId)).findFirst().orElse(null);
  }

}
