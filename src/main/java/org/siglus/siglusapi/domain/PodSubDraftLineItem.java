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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.siglus.common.domain.BaseEntity;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pod_sub_draft_line_items", schema = "siglusintegration")
public class PodSubDraftLineItem extends BaseEntity {

  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableid")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableversionnumber"))
  })
  @Embedded
  private VersionEntityReference orderable;

  private UUID podSubDraftId;

  private Integer quantityAccepted;

  private UUID lotId;

  @Enumerated(EnumType.STRING)
  private VvmStatus vvmStatus;

  private Boolean useVvm;

  private Integer quantityRejected;

  private UUID rejectionReasonId;

  private String notes;

  private String lotCode;

  private LocalDate expirationDate;

  @Column(name = "locations", columnDefinition = "jsonb")
  @Convert(converter = PodSubDraftLineItemLocationsConverter.class)
  private List<PodSubDraftLineItemLocation> locations;
}
