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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface EventRecordRepository extends JpaRepository<EventRecord, UUID>, EventRecordRepositoryCustom {

  Stream<EventRecord> streamByLocalReplayedOrderBySyncedTime(Boolean localReplayed);

  EventRecord findTopByOnlineWebSynced(boolean onlineWebSynced);

  List<EventRecord> findTop100ByOnlineWebSyncedAndArchivedOrderByOccurredTimeAsc(
      boolean onlineWebSynced, boolean archived);

  List<EventRecord> findEventRecordByGroupId(String groupId);

  @Query(value = "select * from localmachine.events e left join"
      + " localmachine.event_payload p on e.id=p.eventid "
      + " where e.groupid=:groupId and e.localreplayed=false and e.id not in "
      + " (select e2.parentid from localmachine.events e2 where e2.groupid=:groupId and e2.parentid is not null)",
      nativeQuery = true)
  List<EventRecord> findNotReplayedLeafNodeIdsInGroup(@Param("groupId") String groupId);

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
          "update localmachine.events set receiversynced=true where id in :ids",
      nativeQuery = true)
  void markAsReceived(@Param("ids") Collection<UUID> ids);

  @Query(value = "select cast(id as varchar) as id from localmachine.events where id in :ids", nativeQuery = true)
  Set<String> filterExistsEventIds(@Param("ids") Set<UUID> ids);

  @Query(
      value = "select * from localmachine.events e left join localmachine.event_payload ep on e.id=ep.eventid "
          + "where e.receiverid=:receiverId and e.receiversynced=false and e.archived=false "
          + "order by e.syncedtime limit :limit",
      nativeQuery = true)
  List<EventRecord> findEventsForReceiver(@Param("receiverId") UUID receiverId, @Param("limit") int limit);

  List<EventRecord> findFirst100ByArchivedFalseAndReceiverSyncedTrueAndOnlineWebSyncedTrueAndLocalReplayedTrue();

  @Query(value = "select cast(id as varchar) \n"
      + "from localmachine.events e \n"
      + "where e.groupid is not null \n"
      + "and e.receiverid != :facilityId \n"
      + "and e.receiversynced = false;", nativeQuery = true)
  List<String> findExportEventIds(@Param("facilityId") UUID facilityId);

  @Query(value = "select cast(id as varchar) as id from localmachine.events "
      + " where groupid=:groupId order by syncedtime desc limit 1", nativeQuery = true)
  Optional<String> findLastEventIdGroupId(@Param("groupId") String groupId);

  @Query(value = "select cast(id as varchar) as id from localmachine.events "
      + " where category != 'REQUISITION_FINAL_APPROVED' and groupid=:groupId order by syncedtime desc limit 1",
      nativeQuery = true)
  Optional<String> findLastNoFinalApproveEventIdGroupId(@Param("groupId") String groupId);
}
