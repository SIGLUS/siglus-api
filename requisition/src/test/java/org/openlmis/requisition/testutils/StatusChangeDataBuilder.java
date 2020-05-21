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

package org.openlmis.requisition.testutils;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.testutils.api.DataBuilder;
import org.openlmis.requisition.testutils.api.RepositoryDataBuilder;

public class StatusChangeDataBuilder implements DataBuilder<StatusChange>,
    RepositoryDataBuilder<StatusChange> {

  private UUID id = UUID.randomUUID();
  private Requisition requisition = new RequisitionDataBuilder().build();
  private StatusMessage statusMessage = null;
  private UUID authorId = UUID.randomUUID();
  private UUID supervisoryNodeId = requisition.getSupervisoryNodeId();
  private RequisitionStatus status = requisition.getStatus();
  private ZonedDateTime createdDate = ZonedDateTime.now();

  /**
   * Build an instance of the {@link StatusChange} class.
   *
   * @return the instance of the {@link StatusChange} class
   */
  public StatusChange build() {
    StatusChange statusChange = new StatusChange();
    statusChange.setId(id);
    statusChange.setRequisition(requisition);
    statusChange.setStatusMessage(statusMessage);
    statusChange.setAuthorId(authorId);
    statusChange.setSupervisoryNodeId(supervisoryNodeId);
    statusChange.setStatus(status);
    statusChange.setCreatedDate(createdDate);
    return statusChange;
  }

  /**
   * Build an instance of the {@link StatusChange} class without id.
   *
   * @return the instance of the {@link StatusChange} class without id
   */
  @Override
  public StatusChange buildAsNew() {
    StatusChange statusChange = build();
    statusChange.setId(null);
    return statusChange;
  }

  public StatusChangeDataBuilder withRequisition(Requisition requisition) {
    this.requisition = requisition;
    return this;
  }

  public StatusChangeDataBuilder withStatus(RequisitionStatus status) {
    this.status = status;
    return this;
  }

  public StatusChangeDataBuilder withCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  public StatusChangeDataBuilder withAuthorId(UUID authorId) {
    this.authorId = authorId;
    return this;
  }

  public StatusChangeDataBuilder forInitiatedRequisition(Requisition requisition) {
    return this.forRequisitionWithStatus(requisition, RequisitionStatus.INITIATED);
  }

  public StatusChangeDataBuilder forSubmittedRequisition(Requisition requisition) {
    return this.forRequisitionWithStatus(requisition, RequisitionStatus.SUBMITTED);
  }

  public StatusChangeDataBuilder forAuthorizedRequisition(Requisition requisition) {
    return this.forRequisitionWithStatus(requisition, RequisitionStatus.AUTHORIZED);
  }

  private StatusChangeDataBuilder forRequisitionWithStatus(Requisition requisition,
      RequisitionStatus requisitionStatus) {
    return this.withRequisition(requisition)
          .withStatus(requisitionStatus);
  }
}
