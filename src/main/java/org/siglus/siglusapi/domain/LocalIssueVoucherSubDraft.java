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

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "local_issue_voucher_sub_draft", schema = "siglusintegration")
public class LocalIssueVoucherSubDraft extends BaseEntity {

  @Column
  private Integer number;
  @Column
  private PodSubDraftStatusEnum status;
  private UUID operatorId;
  private UUID localIssueVoucherId;

  @LazyCollection(FALSE)
  @OneToMany(cascade = ALL, mappedBy = "localIssueVoucherSubDraft", orphanRemoval = true)
  private List<LocalIssueVoucherDraftLineItem> lineItems;
}
