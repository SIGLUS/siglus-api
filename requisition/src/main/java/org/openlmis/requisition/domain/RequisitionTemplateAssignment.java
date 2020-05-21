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

import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "requisition_template_assignments", schema = "requisition")
public class RequisitionTemplateAssignment extends BaseEntity {

  @Column(nullable = false)
  @Type(type = BaseEntity.UUID_TYPE)
  @Getter(AccessLevel.PACKAGE)
  private UUID programId;

  @Type(type = BaseEntity.UUID_TYPE)
  @Getter(AccessLevel.PACKAGE)
  private UUID facilityTypeId;

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "templateId", nullable = false)
  private RequisitionTemplate template;

  // Custom equals and hashCode methods to avoid stack overflow:
  // RequisitionTemplateAssignment > RequisitionTemplate > Set<RequisitionTemplateAssignment>

  private UUID getTemplateId() {
    return null == template ? null : template.getId();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof RequisitionTemplateAssignment) {
      RequisitionTemplateAssignment that = (RequisitionTemplateAssignment) obj;

      return Objects.equals(this.getProgramId(), that.getProgramId())
          && Objects.equals(this.getFacilityTypeId(), that.getFacilityTypeId())
          && Objects.equals(this.getTemplateId(), that.getTemplateId());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getProgramId(), getFacilityTypeId(), getTemplateId());
  }
}
