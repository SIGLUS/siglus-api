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

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MasterDataEventRecordRepository extends JpaRepository<MasterDataEventRecord, UUID> {

  Stream<MasterDataEventRecord> streamMasterDataEventRecordsByIdAfterOrderById(Long id);

  @Modifying
  @Query(
      value =
          "INSERT INTO localmachine.master_data_events("
              + "payload,"
              + "facilityid,"
              + "occurredtime) VALUES ("
              + ":#{#r.payload},"
              + ":#{#r.facilityId},"
              + ":#{#r.occurredTime})",
      nativeQuery = true)
  void insertMarkFacilityIdMasterDataEvents(@Param("r") MasterDataEventRecord masterDataEventRecord);

  @Modifying
  @Query(
      value =
          "INSERT INTO localmachine.master_data_events("
              + "payload,"
              + "occurredtime) VALUES ("
              + ":#{#r.payload},"
              + ":#{#r.occurredTime})",
      nativeQuery = true)
  void insertMasterDataEvents(@Param("r") MasterDataEventRecord masterDataEventRecord);

  @Query(
      value = "select count(id) from localmachine.master_data_events where id > :id",
      nativeQuery = true)
  Integer findChangesCountAfterLatestSnapshotVersion(@Param("id") Long id);

  MasterDataEventRecord findTopBySnapshotVersionIsNotNullOrderByIdDesc();

  List<MasterDataEventRecord> findBySnapshotVersionIsNotNull();

  @Query(
      value = "select id from localmachine.master_data_events where snapshotversion = null order by id desc limit 1",
      nativeQuery = true)
  Long findLatestRecordId();

  @Query(
      value =
          "delete from localmachine.master_data_events where id <:minimumOffset and snapshotversion is null",
      nativeQuery = true)
  @Modifying
  void deleteIncrementalRecordsLessThan(@Param("minimumOffset") long minimumOffset);
}
