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

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.siglus.siglusapi.domain.LocalIssueVoucher;
import org.springframework.beans.BeanUtils;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LocalIssueVoucherDto {

  private UUID id;

  @NotNull
  private String orderCode;

  @NotNull
  private OrderStatus status;

  private UUID processingPeriodId;

  @NotNull
  private UUID programId;

  @NotNull
  private UUID requestingFacilityId;

  @NotNull
  private UUID supplyingFacilityId;

  private UUID createdById;

  private ZonedDateTime createdDate;

  public static LocalIssueVoucherDto from(LocalIssueVoucher localIssueVoucher) {
    LocalIssueVoucherDto localIssueVoucherDto = new LocalIssueVoucherDto();
    BeanUtils.copyProperties(localIssueVoucher, localIssueVoucherDto);
    return localIssueVoucherDto;
  }
}
