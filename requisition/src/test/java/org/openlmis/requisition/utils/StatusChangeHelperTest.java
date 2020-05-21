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

package org.openlmis.requisition.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.INITIATED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.REJECTED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;

import com.google.common.collect.Maps;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.openlmis.requisition.testutils.StatusChangeDataBuilder;

public class StatusChangeHelperTest {

  private Map<String, StatusLogEntry> statusLogEntries = Maps.newHashMap();

  private Requisition requisition = new Requisition();

  private StatusChangeDto initiatedStatusChange = new StatusChangeDto();
  private StatusChangeDto submitted1StatusChange = new StatusChangeDto();
  private StatusChangeDto authorized1StatusChange = new StatusChangeDto();
  private StatusChangeDto rejectedStatusChange = new StatusChangeDto();
  private StatusChangeDto submitted2StatusChange = new StatusChangeDto();
  private StatusChangeDto authorized2StatusChange = new StatusChangeDto();

  private UUID authorId = UUID.randomUUID();

  private ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.of("UTC"));

  @Before
  public void setUp() {
    createStatusChangeDto(INITIATED, currentDate.minusDays(5), initiatedStatusChange);
    createStatusChangeDto(SUBMITTED, currentDate.minusDays(4), submitted1StatusChange);
    createStatusChangeDto(AUTHORIZED, currentDate.minusDays(3), authorized1StatusChange);
    createStatusChangeDto(REJECTED, currentDate.minusDays(2), rejectedStatusChange);
    createStatusChangeDto(SUBMITTED, currentDate.minusDays(1), submitted2StatusChange);
    createStatusChangeDto(AUTHORIZED, currentDate, authorized2StatusChange);
  }

  @Test
  public void shouldNotAddStatusChangeIfItIsNull() {
    // when
    StatusChangeHelper.addOrUpdate(statusLogEntries, null);

    // that
    assertThat(statusLogEntries).isEmpty();
  }

  @Test
  public void shouldNotAddStatusChangeIfStatusIsNull() {
    // given
    initiatedStatusChange.setStatus(null);

    // when
    StatusChangeHelper.addOrUpdate(statusLogEntries, initiatedStatusChange);

    // that
    assertThat(statusLogEntries).isEmpty();
  }

  @Test
  public void shouldNotAddStatusChangeIfCreatedDateIsNull() {
    // given
    initiatedStatusChange.setCreatedDate(null);

    // when
    StatusChangeHelper.addOrUpdate(statusLogEntries, initiatedStatusChange);

    // that
    assertThat(statusLogEntries).isEmpty();
  }

  @Test
  public void shouldAddStatusChangeIfItIsNew() {
    // when
    StatusChangeHelper.addOrUpdate(statusLogEntries, initiatedStatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, submitted1StatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, authorized1StatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, rejectedStatusChange);

    // then
    assertThat(statusLogEntries.keySet())
        .contains(INITIATED.name(), SUBMITTED.name(), AUTHORIZED.name(), REJECTED.name());
    assertThat(statusLogEntries.values())
        .extracting("authorId")
        .contains(authorId);
    assertThat(statusLogEntries.values())
        .extracting("changeDate")
        .contains(initiatedStatusChange.getCreatedDate(), submitted1StatusChange.getCreatedDate(),
            authorized1StatusChange.getCreatedDate(), rejectedStatusChange.getCreatedDate());
  }

  @Test
  public void shouldAddStatusChangeIfItIsLaterThanInMap() {
    // givne
    StatusChangeHelper.addOrUpdate(statusLogEntries, initiatedStatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, submitted1StatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, authorized1StatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, rejectedStatusChange);

    // when
    StatusChangeHelper.addOrUpdate(statusLogEntries, submitted2StatusChange);
    StatusChangeHelper.addOrUpdate(statusLogEntries, authorized2StatusChange);

    assertThat(statusLogEntries.keySet())
        .contains(INITIATED.name(), SUBMITTED.name(), AUTHORIZED.name(), REJECTED.name());
    assertThat(statusLogEntries.values())
        .extracting("authorId")
        .contains(authorId);
    assertThat(statusLogEntries.values())
        .extracting("changeDate")
        .contains(initiatedStatusChange.getCreatedDate(), submitted2StatusChange.getCreatedDate(),
            authorized2StatusChange.getCreatedDate(), rejectedStatusChange.getCreatedDate());
  }

  private void createStatusChangeDto(RequisitionStatus status, ZonedDateTime createdDate,
      StatusChangeDto dto) {
    new StatusChangeDataBuilder()
        .withRequisition(requisition)
        .withStatus(status)
        .withCreatedDate(createdDate)
        .withAuthorId(authorId)
        .build()
        .export(dto);
  }
}
