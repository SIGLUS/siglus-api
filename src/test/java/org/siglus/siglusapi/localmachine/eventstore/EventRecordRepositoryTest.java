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

package org.siglus.siglusapi.localmachine.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.UUID;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class EventRecordRepositoryTest extends LocalMachineIntegrationTest {
  @Autowired private EventRecordRepository eventRecordRepository;

  @Test
  public void hasParentIdWhenBuildSqlForInsertGivenParentIdIsNull() {
    // given
    EventRecord eventRecord = getEventRecord();
    eventRecord.setParentId(UUID.randomUUID());
    // when
    String sql = EventRecordRepositoryImpl.buildSqlForInsert(eventRecord, new LinkedList<>());
    assertThat(sql)
        .isEqualTo(
            "INSERT INTO localmachine.events("
                + "protocolversion,senderid,receiverid,onlinewebsynced,receiversynced,localreplayed,archived,"
                + "occurredtime,syncedtime,groupid,id,category,parentid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
  }

  @Test
  public void noParentIdWhenBuildSqlForInsertGivenParentIdIsNull() {
    // given
    EventRecord eventRecord = getEventRecord();
    eventRecord.setParentId(null);
    // when
    String sql = EventRecordRepositoryImpl.buildSqlForInsert(eventRecord, new LinkedList<>());
    assertThat(sql)
        .isEqualTo(
            "INSERT INTO localmachine.events("
                + "protocolversion,senderid,receiverid,onlinewebsynced,receiversynced,localreplayed,archived,"
                + "occurredtime,syncedtime,groupid,id,category) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
  }

  @Test
  public void couldRetrieveEventGivenEventInserted() {
    // given
    EventRecord eventRecord = getEventRecord();

    // when
    eventRecordRepository.insertAndAllocateLocalSequenceNumber(eventRecord);
    EventRecord got = eventRecordRepository.findOne(eventRecord.getId());
    assertThat(got.getLocalSequenceNumber()).isPositive();
  }

  @Test
  public void shouldIncreaseLocalSeqWhenInsertNewRecord() {
    // given
    EventRecord evt1 = getEventRecord();
    EventRecord evt2 = getEventRecord();
    eventRecordRepository.insertAndAllocateLocalSequenceNumber(evt1);
    eventRecordRepository.insertAndAllocateLocalSequenceNumber(evt2);

    // when
    EventRecord got1 = eventRecordRepository.findOne(evt1.getId());
    EventRecord got2 = eventRecordRepository.findOne(evt2.getId());
    assertThat(got2.getLocalSequenceNumber() - 1).isEqualTo(got1.getLocalSequenceNumber());
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void shouldThrowWhenInsertEventGivenDuplicatedEvent() {
    // given
    EventRecord eventRecord = getEventRecord();
    eventRecordRepository.insertAndAllocateLocalSequenceNumber(eventRecord);

    // when
    eventRecordRepository.insertAndAllocateLocalSequenceNumber(eventRecord);
  }

  private EventRecord getEventRecord() {
    return EventRecord.builder()
        .id(UUID.randomUUID())
        .protocolVersion(0)
        .occurredTime(ZonedDateTime.now())
        .senderId(UUID.randomUUID())
        .payload("payload".getBytes())
        .syncedTime(ZonedDateTime.now())
        .build();
  }
}
