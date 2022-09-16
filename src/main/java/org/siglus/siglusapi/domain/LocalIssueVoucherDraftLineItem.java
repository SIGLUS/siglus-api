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

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "local_issue_voucher_draft_line_item", schema = "siglusintegration")
public class LocalIssueVoucherDraftLineItem extends BaseEntity {

  @ManyToOne
  @JoinColumn(nullable = false)
  private LocalIssueVoucherSubDraft localIssueVoucherSubDraft;

  @Column(name = "localissuevoucherid", nullable = false)
  private UUID localIssueVoucherId;

  @Column(name = "quantityaccepted")
  private Integer quantityAccepted;

  @Column(name = "quantityrejected")
  private Integer quantityrejected;

  @Column(nullable = false)
  private UUID orderableId;

  @Column
  private UUID lotId;

  @Column(name = "rejectionreasonid")
  private UUID rejectionReasonId;

  @Column
  private String notes;

  @Column(name = "quantityordered")
  private Integer quantityOrdered;

  @Column(name = "partialfulfilled")
  private Integer partialFulfilled;

  @Column(name = "quantityreturned")
  private Integer quantityReturned;
}
