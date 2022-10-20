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

package org.siglus.siglusapi.localmachine.event.masterdata;

import io.debezium.data.Envelope.Operation;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.UserRepository;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.cdc.CdcListener;
import org.siglus.siglusapi.localmachine.cdc.CdcRecord;
import org.siglus.siglusapi.localmachine.cdc.CdcRecordMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile({"!localmachine"})
public class MasterDataEventEmitter implements CdcListener {

  private final EventPublisher eventPublisher;
  private final CdcRecordMapper cdcRecordMapper;
  private final UserRepository userRepository;
  private static final String USER_ID = "userid";
  private static final String REFERENCE_DATA = "referencedata";
  private static final String RIGHT_ASSIGNMENTS = "right_assignments";

  @Override
  public String[] acceptedTables() {
    return new String[]{
        "fulfillment.configuration_settings"
    };
  }

  @Transactional
  @Override
  public void on(List<CdcRecord> records) {
    emitNeedNotMarkFacilityEvent(records);
    emitNeedMarkFacilityEvent(records);
  }

  private void emitNeedNotMarkFacilityEvent(List<CdcRecord> records) {
    List<CdcRecord> notNeedMarkFacilityRecords = records.stream()
        .filter(cdcRecord -> !isNeedMarkFacilityTable(cdcRecord))
        .collect(Collectors.toList());
    eventPublisher.emitMasterDataEvent(
        new MasterDataTableChangeEvent(cdcRecordMapper.buildEvents(notNeedMarkFacilityRecords)), null);
  }

  private void emitNeedMarkFacilityEvent(List<CdcRecord> records) {
    Map<UUID, List<CdcRecord>> userIdToRecords = filterNeedMarkFacilityRecords(records);
    if (!userIdToRecords.isEmpty()) {
      Map<UUID, UUID> userIdToFacilityId = userRepository.findAll().stream()
          .filter(user -> user.getHomeFacilityId() != null)
          .collect(Collectors.toMap(User::getId, User::getHomeFacilityId));
      userIdToRecords.forEach((userId, cdcRecord) ->
          eventPublisher.emitMasterDataEvent(
              new MasterDataTableChangeEvent(cdcRecordMapper.buildAlreadyGroupedEvents(cdcRecord)),
              userIdToFacilityId.get(userId)));
    }
  }

  private Map<UUID, List<CdcRecord>> filterNeedMarkFacilityRecords(List<CdcRecord> records) {
    return records.stream()
        .filter(this::isNeedMarkFacilityTable)
        .collect(Collectors.groupingBy(cdcRecord -> UUID.fromString(cdcRecord.getPayload().get(USER_ID).toString())));
  }

  private boolean isNeedMarkFacilityTable(CdcRecord cdcRecord) {
    return !Operation.DELETE.code().equals(cdcRecord.getOperationCode()) && REFERENCE_DATA.equals(cdcRecord.getSchema())
        && RIGHT_ASSIGNMENTS.equals(cdcRecord.getTable());
  }
}
