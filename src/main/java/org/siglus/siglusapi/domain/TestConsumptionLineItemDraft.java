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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.springframework.beans.BeanUtils;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "test_consumption_line_items_draft", schema = "siglusintegration")
public class TestConsumptionLineItemDraft extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionDraftId")
  @Getter
  @Setter
  private RequisitionDraft requisitionDraft;

  private UUID testConsumptionLineItemId;

  private UUID requisitionId;

  private String project;

  private String outcome;

  private String service;

  private Integer value;

  public static List<TestConsumptionLineItemDraft> from(RequisitionDraft draft,
      SiglusRequisitionDto requisitionDto) {
    List<TestConsumptionServiceDto> serviceDtos = requisitionDto.getTestConsumptionLineItems();
    List<TestConsumptionLineItem> lineItems = TestConsumptionLineItem
        .from(serviceDtos, requisitionDto.getId());

    return lineItems.stream().map(lineItem -> {
      TestConsumptionLineItemDraft lineItemDraft = new TestConsumptionLineItemDraft();
      BeanUtils.copyProperties(lineItem, lineItemDraft);
      lineItemDraft.setTestConsumptionLineItemId(lineItem.getId());
      lineItemDraft.setId(null);
      lineItemDraft.setRequisitionDraft(draft);
      return lineItemDraft;
    }).collect(Collectors.toList());
  }

  public static List<TestConsumptionServiceDto> getLineItemDto(
      List<TestConsumptionLineItemDraft> draftList) {
    List<TestConsumptionLineItem> lineItems = draftList.stream().map(lineItemDraft -> {
      TestConsumptionLineItem testConsumptionLineItem = new TestConsumptionLineItem();
      BeanUtils.copyProperties(lineItemDraft, testConsumptionLineItem);
      testConsumptionLineItem.setId(lineItemDraft.getTestConsumptionLineItemId());
      return testConsumptionLineItem;
    }).collect(Collectors.toList());
    return TestConsumptionServiceDto.from(lineItems);
  }
}
