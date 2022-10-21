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

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Function;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.localmachine.MasterDataEvent;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "master_data_events", schema = "localmachine")
public class MasterDataEventRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "snapshotversion")
  private String snapshotVersion;
  @Column(name = "payload")
  private byte[] payload;
  @Column(name = "facilityid", columnDefinition = "uuid")
  private UUID facilityId;
  @Column(name = "occurredtime")
  private ZonedDateTime occurredTime;

  public static MasterDataEventRecord from(MasterDataEvent masterDataEvent, byte[] payload) {
    return MasterDataEventRecord.builder()
        .id(masterDataEvent.getId())
        .snapshotVersion(masterDataEvent.getSnapshotVersion())
        .payload(payload)
        .facilityId(masterDataEvent.getFacilityId())
        .occurredTime(masterDataEvent.getOccurredTime())
        .build();
  }

  public MasterDataEvent toMasterDataEvent(Function<byte[], Object> payloadMapper) {
    return org.siglus.siglusapi.localmachine.MasterDataEvent.builder()
        .id(id)
        .snapshotVersion(snapshotVersion)
        .payload(payloadMapper.apply(payload))
        .facilityId(facilityId)
        .occurredTime(occurredTime)
        .build();
  }
}
