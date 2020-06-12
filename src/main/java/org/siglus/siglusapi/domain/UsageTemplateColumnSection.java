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

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.springframework.beans.BeanUtils;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "usage_sections_maps", schema = "siglusintegration")
public class UsageTemplateColumnSection extends BaseEntity {

  private String name;

  private String label;

  private int displayOrder;

  private UUID requisitionTemplateId;

  @ManyToOne
  @JoinColumn(name = "sectionId", nullable = false)
  private AvailableUsageColumnSection section;

  @Enumerated(value = EnumType.STRING)
  private UsageCategory category;

  @LazyCollection(FALSE)
  @OneToMany(cascade = ALL, mappedBy = "usageTemplateColumnSection")
  private List<UsageTemplateColumn> columns;

  public static UsageTemplateColumnSection from(
      AvailableUsageColumnSection availableColumnSection,
      UUID templateId) {
    UsageTemplateColumnSection columnSection = new UsageTemplateColumnSection();
    BeanUtils.copyProperties(availableColumnSection, columnSection);
    columnSection.setId(null);
    columnSection.section = availableColumnSection;
    columnSection.columns = availableColumnSection.getColumns()
        .stream()
        .map(column ->
        UsageTemplateColumn.from(templateId, column, columnSection)
    ).collect(Collectors.toList());
    return columnSection;
  }

  public static UsageTemplateColumnSection from(UsageTemplateSectionDto sectionDto,
      UsageCategory category, UUID templateId, List<AvailableUsageColumnSection> sections,
      List<AvailableUsageColumn> availableUsageColumns) {
    UsageTemplateColumnSection section = new UsageTemplateColumnSection();
    BeanUtils.copyProperties(sectionDto, section);
    section.requisitionTemplateId = templateId;
    section.category = category;
    section.setSection(findAvailableUsageSection(section.name, sections));
    section.columns = sectionDto.getColumns().stream().map(columnDto ->
        UsageTemplateColumn.from(templateId, columnDto, section, availableUsageColumns))
        .collect(Collectors.toList());
    return section;
  }

  private static AvailableUsageColumnSection findAvailableUsageSection(String name,
      List<AvailableUsageColumnSection> availableUsageSections) {
    return availableUsageSections.stream()
        .filter(section -> section.getName().equals(name)).findFirst().orElse(null);
  }


  public UsageTemplateColumnSection getNewTemplateSection() {
    this.setId(null);
    this.getColumns().forEach(column -> column.setId(null));
    return this;
  }

}
