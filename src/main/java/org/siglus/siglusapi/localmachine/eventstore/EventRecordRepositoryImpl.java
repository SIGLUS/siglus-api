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

import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventRecordRepositoryImpl implements EventRecordRepositoryCustom {
  private final EntityManager entityManager;

  @Override
  public void importExternalEvent(EventRecord eventRecord) {
    entityManager.persist(eventRecord);
    entityManager.flush();
    entityManager.detach(eventRecord);
  }

  @Override
  public void insertAndAllocateLocalSequenceNumber(EventRecord eventRecord) {
    entityManager
        .createNativeQuery(
            "insert into localmachine.events("
                + "protocolversion, "
                + "senderid, "
                + "receiverid, "
                + "onlinewebsynced, "
                + "receiversynced, "
                + "localreplayed, "
                + "archived, "
                + "occurredtime, "
                + "syncedtime, "
                + "parentid, "
                + "id, "
                + "groupid) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
        .setParameter(1, eventRecord.getProtocolVersion())
        .setParameter(2, eventRecord.getSenderId())
        .setParameter(3, eventRecord.getReceiverId())
        .setParameter(4, eventRecord.isOnlineWebSynced())
        .setParameter(5, eventRecord.isReceiverSynced())
        .setParameter(6, eventRecord.isLocalReplayed())
        .setParameter(7, eventRecord.isArchived())
        .setParameter(8, eventRecord.getOccurredTime())
        .setParameter(9, eventRecord.getSyncedTime())
        .setParameter(10, eventRecord.getParentId())
        .setParameter(11, eventRecord.getId())
        .setParameter(12, eventRecord.getGroupId())
        .executeUpdate();
    entityManager.flush();
  }
}
