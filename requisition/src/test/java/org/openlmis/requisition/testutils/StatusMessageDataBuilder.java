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

import java.util.UUID;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.testutils.api.DataBuilder;

public class StatusMessageDataBuilder implements DataBuilder<StatusMessage> {

  private UUID id;
  private Requisition requisition;
  private StatusChange statusChange;
  private UUID authorId; 
  private String authorFirstName;
  private String authorLastName;
  private RequisitionStatus status;
  private String body;

  /**
   * Builder for {@link StatusMessage}.
   */
  public StatusMessageDataBuilder() {
    this.id = UUID.randomUUID();
    this.requisition = new RequisitionDataBuilder().build();
    this.statusChange = new StatusChangeDataBuilder().build();
    this.authorId = UUID.randomUUID();
    this.authorFirstName = "authorName";
    this.authorLastName = "authorLastName";
    this.status = RequisitionStatus.INITIATED;
    this.body = "body";
  }

  @Override
  public StatusMessage build() {
    StatusMessage statusMessage = new StatusMessage();
    statusMessage.setId(id);
    statusMessage.setRequisition(requisition);
    statusMessage.setStatusChange(statusChange);
    statusMessage.setAuthorId(authorId);
    statusMessage.setAuthorFirstName(authorFirstName);
    statusMessage.setAuthorLastName(authorLastName);
    statusMessage.setStatus(status);
    statusMessage.setBody(body);
    return statusMessage;
  }

  public StatusMessageDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public StatusMessageDataBuilder withRequisition(Requisition requisition) {
    this.requisition = requisition;
    return this;
  }

  public StatusMessageDataBuilder withStatusChange(StatusChange statusChange) {
    this.statusChange = statusChange;
    return this;
  }

  public StatusMessageDataBuilder withAuthorId(UUID authorId) {
    this.authorId = authorId;
    return this;
  }

  public StatusMessageDataBuilder withAuthorFirstName(String authorFirstName) {
    this.authorFirstName = authorFirstName;
    return this;
  }

  public StatusMessageDataBuilder withAuthorLastName(String authorLastName) {
    this.authorLastName = authorLastName;
    return this;
  }

  public StatusMessageDataBuilder withStatus(RequisitionStatus status) {
    this.status = status;
    return this;
  }

  public StatusMessageDataBuilder withBody(String body) {
    this.body = body;
    return this;
  }
}
