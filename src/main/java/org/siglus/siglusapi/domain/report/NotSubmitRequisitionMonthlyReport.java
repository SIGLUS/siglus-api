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

package org.siglus.siglusapi.domain.report;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "vw_requisition_monthly_report", schema = "dashboard")
public class NotSubmitRequisitionMonthlyReport  extends BaseEntity {
  @Column(name = "id")
  private UUID id;
  @Column(name = "inventorydate")
  private String inventoryDate;
  @Column(name = "statusdetail")
  private String statusDetail;
  @Column(name = "submittedstatus")
  private String submittedStatus;
  @Column(name = "reporttype")
  private String reportType;
  @Column(name = "originalperiod")
  private String originalPeriod;
  @Column(name = "submittedtime")
  private LocalDateTime submittedTime;
  @Column(name = "synctime")
  private LocalDateTime syncTime;
  @Column(name = "facilityid")
  private UUID facilityId;
  @Column(name = "programid")
  private UUID programId;
  @Column(name = "processingperiodid")
  private UUID processingPeriodId;

}

