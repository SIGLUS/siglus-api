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

import static org.siglus.common.constant.FieldConstants.EXTRA_DATA_IS_SAVED;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
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
import org.openlmis.requisition.domain.ExtraDataEntity;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.UserDto;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "requisitions_draft", schema = "siglusintegration")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RequisitionDraft extends BaseEntity {

  private UUID requisitionId;

  private UUID facilityid;

  private String draftStatusMessage;

  @OneToMany(
      mappedBy = "requisitionDraft",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  private List<RequisitionLineItemDraft> lineItems = Collections.emptyList();

  @OneToMany(
      mappedBy = "requisitionDraft",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  private List<KitUsageLineItemDraft> kitUsageLineItems = Collections.emptyList();

  @OneToMany(
      mappedBy = "requisitionDraft",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  private List<UsageInformationLineItemDraft> usageInformationLineItemDrafts = Collections
      .emptyList();

  @OneToMany(
      mappedBy = "requisitionDraft",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @DiffIgnore
  private List<TestConsumptionLineItemDraft> testConsumptionLineItemDrafts = Collections
      .emptyList();

  @Embedded
  private ExtraDataEntity extraData = new ExtraDataEntity();

  public static RequisitionDraft from(SiglusRequisitionDto requisitionDto,
      RequisitionTemplate template, UUID draftId, UserDto userDto) {
    RequisitionDraft draft = new RequisitionDraft();
    draft.setId(draftId);
    draft.setFacilityid(userDto.getHomeFacilityId());
    draft.setRequisitionId(requisitionDto.getId());
    draft.setDraftStatusMessage(requisitionDto.getDraftStatusMessage());
    draft.extraData.updateFrom(requisitionDto.getExtraData());
    draft.extraData.put(EXTRA_DATA_IS_SAVED, true);
    RequisitionTemplateExtension extension = template.getTemplateExtension();
    if (Boolean.TRUE.equals(extension.getEnableProduct())) {
      draft.setLineItems(requisitionDto.getRequisitionLineItems().stream().map(lineItem ->
          RequisitionLineItemDraft.from(draft, lineItem)).collect(Collectors.toList()));
    }
    if (Boolean.TRUE.equals(extension.getEnableKitUsage())) {
      draft.setKitUsageLineItems(KitUsageLineItemDraft.from(draft, requisitionDto));
    }
    if (Boolean.TRUE.equals(extension.getEnableUsageInformation())) {
      draft.setUsageInformationLineItemDrafts(
          UsageInformationLineItemDraft.from(draft, requisitionDto));
    }
    if (Boolean.TRUE.equals(extension.getEnableRapidTestConsumption())) {
      draft.setTestConsumptionLineItemDrafts(
          TestConsumptionLineItemDraft.from(draft, requisitionDto));
    }
    return draft;
  }

}

