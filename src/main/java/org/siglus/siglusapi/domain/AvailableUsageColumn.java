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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.openlmis.requisition.domain.ColumnType;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "available_usage_columns", schema = "siglusintegration")
public class AvailableUsageColumn extends BaseEntity {

  protected static final String TEXT_COLUMN_DEFINITION = "text";

  private String name;

  private String sources;

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

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "sectionId", nullable = false)
  private AvailableUsageColumnSection availableUsageColumnSection;

  private Integer displayOrder;

}
