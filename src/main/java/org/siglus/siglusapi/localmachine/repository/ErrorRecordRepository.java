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

package org.siglus.siglusapi.localmachine.repository;

import java.util.UUID;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRecordRepository extends JpaRepository<ErrorRecord, UUID> {

  @Query(value = "select * from localmachine.error_records e where e.type <> 'REPLAY'"
      + " and e.occurredtime >= (select lastsyncedtime from localmachine.last_sync_replay_record)"
      + " UNION ALL select * from localmachine.error_records e where e.type = 'REPLAY'"
      + " and e.occurredtime >= (select lastreplayedtime from localmachine.last_sync_replay_record)"
      + " order by occurredtime limit 1", nativeQuery = true)
  ErrorRecord findLastErrorRecord();

}
