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

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.LocalReceiptVoucherDto;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "local_receipt_voucher", schema = "siglusintegration")
public class LocalReceiptVoucher extends BaseEntity {

  @Column(nullable = false)
  private String orderCode;

  @Column(nullable = false)
  private OrderStatus status;

  private UUID processingPeriodId;

  @Column(nullable = false)
  private UUID programId;

  @Column(nullable = false)
  private UUID requestingFacilityId;

  @Column(nullable = false)
  private UUID supplyingFacilityId;

  private UUID createdById;

  @CreatedDate
  @Column(updatable = false)
  private ZonedDateTime createdDate;

  public static LocalReceiptVoucher createLocalReceiptVoucher(LocalReceiptVoucherDto localReceiptVoucherDto) {
    LocalReceiptVoucher localReceiptVoucher = new LocalReceiptVoucher();
    BeanUtils.copyProperties(localReceiptVoucherDto, localReceiptVoucher);
    return localReceiptVoucher;
  }
}