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

package org.siglus.siglusapi.localmachine.cdc;

import io.debezium.data.Envelope.Operation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent.RowChangeEvent;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent.TableChangeEventBuilder;
import org.springframework.stereotype.Component;

@Component
public class CdcRecordMapper {

  // todo: schema version using the latest flyway version? checksum of table schema
  private String schemaVersion = "todo";

  public List<TableChangeEvent> buildAlreadyGroupedEvents(List<CdcRecord> records) {
    return Collections.singletonList(buildChangeEvent(records));
  }

  public List<TableChangeEvent> buildEvents(List<CdcRecord> records) {
    return records.stream()
        .collect(Collectors.groupingBy(CdcRecord::tableId)).values().stream()
        .map(this::buildChangeEvent)
        .collect(Collectors.toList());
  }

  protected TableChangeEvent buildChangeEvent(List<CdcRecord> cdcRecords) {
    CdcRecord first = cdcRecords.get(0);
    List<String> columns = getColumn(cdcRecords);
    TableChangeEventBuilder eventBuilder =
        TableChangeEvent.builder()
            .schemaName(first.getSchema())
            .tableName(first.getTable())
            .schemaVersion(schemaVersion)
            .columns(columns);
    List<RowChangeEvent> rowChanges =
        cdcRecords.stream().map(v -> buildRowChangeEvent(columns, v)).collect(Collectors.toList());
    return eventBuilder.rowChangeEvents(rowChanges).build();
  }


  protected RowChangeEvent buildRowChangeEvent(List<String> columns, CdcRecord v) {
    boolean isDeletion = Operation.DELETE.equals(Operation.forCode(v.getOperationCode()));
    // todo: what if the columns changed? e.g. missing/added columns when the batch contains records
    // with old schema
    List<Object> values =
        columns.stream().map(column -> v.getPayload().get(column)).collect(Collectors.toList());
    return new RowChangeEvent(isDeletion, values);
  }

  private List<String> getColumn(List<CdcRecord> cdcRecords) {
    return new ArrayList<>(cdcRecords.stream()
        .max(Comparator.comparingInt(cdcRecord -> cdcRecord.getPayload().size()))
        .orElseThrow(() -> new IllegalStateException("no cdc records"))
        .getPayload().keySet());
  }
}
