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

package org.siglus.siglusapi.service;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.AgeGroupLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgeGroupDataProcessor implements UsageReportDataProcessor {

  @Autowired
  private AgeGroupLineItemRepository ageGroupLineItemRepository;

  @Override
  public void doInitiate(SiglusRequisitionDto siglusRequisitionDto,
      List<UsageTemplateColumnSection> templateColumnSections) {
    List<UsageTemplateColumn> rows = new LinkedList<>();
    List<UsageTemplateColumn> columns = new LinkedList<>();
    List<AgeGroupLineDto> ageGroupLineDtoList;
    List<AgeGroupLineItem> ageGroupLineItemList = new LinkedList<>();
    templateColumnSections.forEach(usageTemplateColumnSection -> {
      if (!usageTemplateColumnSection.getCategory().equals(UsageCategory.AGEGROUP)) {
        return;
      }
      if (FieldConstants.AGE_GROUP_LABEL.equals(usageTemplateColumnSection.getLabel())) {
        columns.addAll(usageTemplateColumnSection.getColumns());
      }
      if (FieldConstants.SECTION_SERVICE_LABEL.equals(usageTemplateColumnSection.getLabel())) {
        rows.addAll(usageTemplateColumnSection.getColumns());
      }
    });
    rows.forEach(usageTemplateRow -> {
      columns.forEach(usageTemplateColumn -> {
        AgeGroupLineItem ageGroupLineItem = AgeGroupLineItem.builder().groupName(usageTemplateRow.getLabel())
            .columnName(usageTemplateColumn.getLabel()).requisitionId(siglusRequisitionDto.getId()).build();
        ageGroupLineItemList.add(ageGroupLineItem);
      });
    });
    List<AgeGroupLineItem> ageGroupLineItems = ageGroupLineItemRepository.save(ageGroupLineItemList);
    ageGroupLineDtoList = AgeGroupLineDto.from(ageGroupLineItems);
    siglusRequisitionDto.setAgeGroupLineItems(ageGroupLineDtoList);

  }

  @Override
  public void get(SiglusRequisitionDto siglusRequisitionDto) {
    List<AgeGroupLineItem> ageGroupLineItems = ageGroupLineItemRepository.findByRequisitionId(
        siglusRequisitionDto.getId());
    siglusRequisitionDto.setAgeGroupLineItems(AgeGroupLineDto.from(ageGroupLineItems));
  }

  @Override
  public void update(SiglusRequisitionDto siglusRequisitionDto, SiglusRequisitionDto siglusRequisitionUpdatedDto) {
    List<AgeGroupLineDto> ageGroupLineDtoList = siglusRequisitionDto.getAgeGroupLineItems();
    ageGroupLineItemRepository.save(AgeGroupLineItem.from(ageGroupLineDtoList));

  }

  @Override
  public void delete(UUID requisitionId) {
    ageGroupLineItemRepository.deleteByRequisitionId(requisitionId);
  }

  @Override
  public boolean isDisabled(SiglusRequisitionDto siglusRequisitionDto) {
    return false;
  }
}
