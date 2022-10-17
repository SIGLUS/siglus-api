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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface EventRecordRepository extends JpaRepository<EventRecord, UUID> {

  List<EventRecord> findEventRecordByLocalReplayed(boolean localReplayed);

  @Query(
      value =
          "select groupsequencenumber + 1 from localmachine.events where groupid=:groupId order by "
              + "groupsequencenumber desc limit 1",
      nativeQuery = true)
  Long getNextGroupSequenceNumber(@Param("groupId") String groupId);

  List<EventRecord> findEventRecordByOnlineWebSyncedAndArchived(boolean onlineWebSynced, boolean archived);

  List<EventRecord> findEventRecordByGroupId(String groupId);

  @Modifying
  @Query(
      value = "update localmachine.events set onlinewebsynced=true where id in :ids",
      nativeQuery = true)
  void updateOnlineWebSyncedToTrueByIds(@Param("ids") List<UUID> ids);

  @Modifying
  @Query(
      value = "update localmachine.events set localreplayed=true where id =:id",
      nativeQuery = true)
  void markAsReplayed(@Param("id") UUID id);

  @Modifying
  @Query(
      value =
          "INSERT INTO localmachine.events("
              + "id,"
              + "protocolversion,"
              + "occurredtime,"
              + "senderid,"
              + "receiverid,"
              + "groupid,"
              + "groupsequencenumber,"
              + "onlinewebsynced,"
              + "localreplayed,"
              + "receiversynced,"
              + "syncedtime) VALUES ("
              + ":#{#r.id},"
              + ":#{#r.protocolVersion},"
              + ":#{#r.occurredTime},"
              + ":#{#r.senderId},"
              + ":#{#r.receiverId},"
              + ":#{#r.groupId},"
              + ":#{#r.groupSequenceNumber},"
              + ":#{#r.onlineWebSynced},"
              + ":#{#r.localReplayed},"
              + ":#{#r.receiverSynced},"
              + ":#{#r.syncedTime})",
      nativeQuery = true)
  void insertAndAllocateLocalSequenceNumber(@Param("r") EventRecord eventRecord);

  @Modifying
  @Query(
      value =
          "INSERT INTO localmachine.events("
              + "id,"
              + "protocolversion,"
              + "occurredtime,"
              + "senderid,"
              + "localsequencenumber,"
              + "receiverid,"
              + "groupid,"
              + "groupsequencenumber,"
              + "onlinewebsynced,"
              + "receiversynced,"
              + "syncedtime) VALUES ("
              + ":#{#r.id},"
              + ":#{#r.protocolVersion},"
              + ":#{#r.occurredTime},"
              + ":#{#r.senderId},"
              + ":#{#r.localSequenceNumber},"
              + ":#{#r.receiverId},"
              + ":#{#r.groupId},"
              + ":#{#r.groupSequenceNumber},"
              + ":#{#r.onlineWebSynced},"
              + ":#{#r.receiverSynced},"
              + ":#{#r.syncedTime})",
      nativeQuery = true)
  void importExternalEvent(@Param("r") EventRecord eventRecord);

  @Modifying
  @Query(
      value =
          "update localmachine.events set receiversynced=true where id in :ids",
      nativeQuery = true)
  void markAsReceived(@Param("ids") Collection<UUID> ids);

  @Query(value = "select cast(id as varchar) as id from localmachine.events where id in :ids", nativeQuery = true)
  Set<String> filterExistsEventIds(@Param("ids") Set<UUID> ids);

  @Query(value = "select id, senderid from localmachine.events where id in :ids", nativeQuery = true)
  List<EventRecord> getPartialEventWithSender(@Param("ids") Set<UUID> ids);

  List<EventRecord> findByReceiverIdAndReceiverSyncedAndArchived(
      UUID receiverId, Boolean receiverSynced, boolean archived);

  List<EventRecord> findByArchived(boolean archived);

  @Query(value = "select cast(id as varchar) \n"
      + "from localmachine.events e \n"
      + "where e.groupid is not null \n"
      + "and e.receiverid != :facilityId \n"
      + "and e.receiversynced = false;", nativeQuery = true)
  List<String> findExportEventIds(@Param("facilityId") UUID facilityId);
}
