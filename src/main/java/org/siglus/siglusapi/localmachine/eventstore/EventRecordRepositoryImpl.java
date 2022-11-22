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

import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

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
    LinkedList<Pair<String, Object>> fields = new LinkedList<>();
    String sql = buildSqlForInsert(eventRecord, fields);
    Query nativeQuery = entityManager.createNativeQuery(sql);
    for (int i = 0; i < fields.size(); i++) {
      nativeQuery.setParameter(i + 1, fields.get(i).getRight());
    }
    nativeQuery.executeUpdate();
    entityManager.flush();
  }

  static String buildSqlForInsert(EventRecord eventRecord, LinkedList<Pair<String, Object>> fields) {
    fields.add(Pair.of("protocolversion", eventRecord.getProtocolVersion()));
    fields.add(Pair.of("senderid", eventRecord.getSenderId()));
    fields.add(Pair.of("receiverid", eventRecord.getReceiverId()));
    fields.add(Pair.of("onlinewebsynced", eventRecord.isOnlineWebSynced()));
    fields.add(Pair.of("receiversynced", eventRecord.isReceiverSynced()));
    fields.add(Pair.of("localreplayed", eventRecord.isLocalReplayed()));
    fields.add(Pair.of("archived", eventRecord.isArchived()));
    fields.add(Pair.of("occurredtime", eventRecord.getOccurredTime()));
    fields.add(Pair.of("syncedtime", eventRecord.getSyncedTime()));
    fields.add(Pair.of("groupid", eventRecord.getGroupId()));
    fields.add(Pair.of("id", eventRecord.getId()));
    fields.add(Pair.of("category", eventRecord.getCategory()));
    // omit null parentid, otherwise it can't be accepted by jpa due to cast uuid of null failure
    if (Objects.nonNull(eventRecord.getParentId())) {
      fields.add(Pair.of("parentid", eventRecord.getParentId()));
    }
    String placeHolderSection = StringUtils.repeat("?", ",", fields.size());
    String columnSection =
        StringUtils.join(
            fields.stream().map(Pair::getLeft).sequential().collect(Collectors.toList()), ",");
    return "INSERT INTO localmachine.events("
        + columnSection
        + ") VALUES ("
        + placeHolderSection
        + ")";
  }
}
