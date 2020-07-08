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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.UserDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "requisitions_draft", schema = "siglusintegration")
public class RequisitionDraft extends BaseEntity {

  private UUID requisitionId;

  private UUID facilityid;

  @OneToMany(
      mappedBy = "requisitionDraft",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  private List<RequisitionLineItemDraft> lineItems;

  @OneToMany(
      mappedBy = "requisitionDraft",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  private List<KitUsageLineItemDraft> kitUsageLineItems;

  public static RequisitionDraft from(SiglusRequisitionDto requisitionDto,
      RequisitionTemplate template, UUID draftId, UserDto userDto) {
    RequisitionDraft draft = new RequisitionDraft();
    draft.setId(draftId);
    draft.setFacilityid(userDto.getHomeFacilityId());
    draft.setRequisitionId(requisitionDto.getId());
    if (template.getTemplateExtension().getEnableProduct()) {
      draft.setLineItems(requisitionDto.getRequisitionLineItems().stream().map(lineItem ->
          RequisitionLineItemDraft.from(draft, lineItem)).collect(Collectors.toList()));
    }
    if (template.getTemplateExtension().getEnableKitUsage()) {
      draft.setKitUsageLineItems(KitUsageLineItemDraft.from(draft, requisitionDto));
    }
    return draft;
  }
}

