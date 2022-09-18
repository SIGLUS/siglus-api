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
import org.siglus.siglusapi.domain.LocalIssueVoucherDraftLineItem;
import org.springframework.beans.BeanUtils;

@Data
public class LocalIssueVoucherDraftLineItemDto {

  private UUID id;

  private UUID localIssueVoucherSubDraftId;

  private UUID localIssueVoucherId;

  private Integer quantityAccepted;

  private Integer quantityrejected;

  private UUID orderableId;

  private UUID lotId;

  private UUID rejectionReasonId;

  private String notes;

  private Integer quantityOrdered;

  private Integer partialFulfilled;

  private Integer quantityReturned;

  public static List<LocalIssueVoucherDraftLineItem> to(
      List<LocalIssueVoucherDraftLineItemDto> localIssueVoucherSubDraftDtos) {
    List<LocalIssueVoucherDraftLineItem> localIssueVoucherDraftLineItems = new ArrayList<>();
    localIssueVoucherSubDraftDtos.forEach(localIssueVoucherDraftLineItemDto -> {
      LocalIssueVoucherDraftLineItem localIssueVoucherDraftLineItem = new LocalIssueVoucherDraftLineItem();
      BeanUtils.copyProperties(localIssueVoucherDraftLineItemDto, localIssueVoucherDraftLineItem);
      localIssueVoucherDraftLineItems.add(localIssueVoucherDraftLineItem);
    });
    return localIssueVoucherDraftLineItems;
  }

  public static List<LocalIssueVoucherDraftLineItemDto> from(
      List<LocalIssueVoucherDraftLineItem> localIssueVoucherDraftLineItems) {
    List<LocalIssueVoucherDraftLineItemDto> localIssueVoucherDraftLineItemDtos = new ArrayList<>();
    localIssueVoucherDraftLineItems.forEach(localIssueVoucherDraftLineItem -> {
      LocalIssueVoucherDraftLineItemDto localIssueVoucherDraftLineItemDto = new LocalIssueVoucherDraftLineItemDto();
      BeanUtils.copyProperties(localIssueVoucherDraftLineItem, localIssueVoucherDraftLineItem);
      localIssueVoucherDraftLineItemDtos.add(localIssueVoucherDraftLineItemDto);
    });
    return localIssueVoucherDraftLineItemDtos;
  }
}
