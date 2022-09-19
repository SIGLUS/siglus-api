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

package org.siglus.siglusapi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.siglus.siglusapi.domain.LocalIssueVoucherSubDraftLineItem;
import org.springframework.beans.BeanUtils;

@Data
public class LocalIssueVoucherSubDraftLineItemDto {

  private UUID id;

  private UUID localIssueVoucherSubDraftId;

  private UUID localIssueVoucherId;

  private Integer quantityAccepted;

  private Integer quantityRejected;

  private UUID orderableId;

  private UUID lotId;

  private UUID rejectionReasonId;

  private String notes;

  private Integer quantityOrdered;

  private Integer partialFulfilled;

  private Integer quantityReturned;

  public static List<LocalIssueVoucherSubDraftLineItem> to(
      List<LocalIssueVoucherSubDraftLineItemDto> localIssueVoucherSubDraftDtos) {
    List<LocalIssueVoucherSubDraftLineItem> localIssueVoucherSubDraftLineItems = new ArrayList<>();
    localIssueVoucherSubDraftDtos.forEach(localIssueVoucherSubDraftLineItemDto -> {
      LocalIssueVoucherSubDraftLineItem localIssueVoucherSubDraftLineItem = new LocalIssueVoucherSubDraftLineItem();
      BeanUtils.copyProperties(localIssueVoucherSubDraftLineItemDto, localIssueVoucherSubDraftLineItem);
      localIssueVoucherSubDraftLineItems.add(localIssueVoucherSubDraftLineItem);
    });
    return localIssueVoucherSubDraftLineItems;
  }

  public static List<LocalIssueVoucherSubDraftLineItemDto> from(
      List<LocalIssueVoucherSubDraftLineItem> localIssueVoucherSubDraftLineItems) {
    List<LocalIssueVoucherSubDraftLineItemDto> localIssueVoucherSubDraftLineItemDtos = new ArrayList<>();
    localIssueVoucherSubDraftLineItems.forEach(localIssueVoucherSubDraftLineItem -> {
      LocalIssueVoucherSubDraftLineItemDto localIssueVoucherSubDraftLineItemDto = new LocalIssueVoucherSubDraftLineItemDto();
      BeanUtils.copyProperties(localIssueVoucherSubDraftLineItem, localIssueVoucherSubDraftLineItemDto);
      localIssueVoucherSubDraftLineItemDtos.add(localIssueVoucherSubDraftLineItemDto);
    });
    return localIssueVoucherSubDraftLineItemDtos;
  }
}
