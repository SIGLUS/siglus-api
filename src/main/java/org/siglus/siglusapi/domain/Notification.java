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

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.siglus.common.domain.BaseEntity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notifications", schema = "siglusintegration")
public class Notification extends BaseEntity {

  /**
   * maybe an requisition, order or shipment
   */
  private UUID refId;

  @Enumerated(EnumType.STRING)
  private NotificationStatus refStatus;

  private UUID refFacilityId;

  private UUID refProgramId;

  /**
   * id of next-step node facility
   */
  private UUID notifyFacilityId;

  private UUID supervisoryNodeId;

  private Boolean viewed = false;

  private Boolean processed = false;

  private UUID viewedUserId;

  private LocalDateTime viewedDate;

  /**
   * current-step operator id(user id)
   */
  @CreatedBy
  private UUID operatorId;

  @Column(updatable = false)
  @CreatedDate
  private LocalDateTime createDate;

}
