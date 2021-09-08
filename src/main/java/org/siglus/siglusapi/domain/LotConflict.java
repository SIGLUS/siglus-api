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
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.siglus.common.domain.BaseEntity;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "lot_conflicts", schema = "siglusintegration")
public class LotConflict extends BaseEntity {

  // conflict with
  private UUID lotId;

  private String lotCode;

  private LocalDate expirationDate;

  private UUID facilityId;

  @LastModifiedDate
  private ZonedDateTime updateTime;

  public static LotConflict of(UUID facilityId, UUID existedLotId, StockEventLineItemDto eventLineItem) {
    LotConflict conflict = new LotConflict();
    conflict.facilityId = facilityId;
    conflict.lotId = existedLotId;
    conflict.lotCode = eventLineItem.getLotCode();
    conflict.expirationDate = eventLineItem.getExpirationDate();
    return conflict;
  }

  public static LotConflict of(UUID facilityId, UUID existedLotId, String lotCode, LocalDate expirationDate) {
    LotConflict conflict = new LotConflict();
    conflict.facilityId = facilityId;
    conflict.lotId = existedLotId;
    conflict.lotCode = lotCode;
    conflict.expirationDate = expirationDate;
    return conflict;
  }

}
