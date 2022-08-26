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
import java.util.UUID;
import org.junit.Test;
import org.siglus.siglusapi.localmachine.LocalMachineIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class EventRecordRepositoryTest extends LocalMachineIntegrationTest {
  @Autowired private EventRecordRepository eventRecordRepository;

  @Test
  public void shouldReturnNullWhenGetNextGroupSeqGivenGroupNotExists() {
    Long nextGroupSeq = eventRecordRepository.getNextGroupSequenceNumber("group not exits");
    assertThat(nextGroupSeq).isNull();
  }

  @Test
  public void shouldReturn1WhenGetNextGroupSeqGivenCurrentMaxGroupSeqIs0() {
    // given
    String groupId = "group-id";
    EventRecord eventRecord = getEventRecord();
    eventRecord.setGroupId(groupId);
    eventRecord.setGroupSequenceNumber(0);
    eventRecordRepository.insertAndAllocateLocalSequenceNumber(eventRecord);
    // when
    Long nextGroupSeq = eventRecordRepository.getNextGroupSequenceNumber(groupId);
    // then
    assertThat(nextGroupSeq).isEqualTo(1L);
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
        .build();
  }
}
