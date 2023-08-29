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

package org.siglus.siglusapi.repository;

import java.util.UUID;
import org.siglus.siglusapi.domain.StockCardRequestBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface StockCardRequestBackupRepository extends JpaRepository<StockCardRequestBackup, UUID> {

  StockCardRequestBackup findOneByHash(String hash);

  @Modifying
  @Query(value = "delete from siglusintegration.stock_card_request_backup "
      + "where createddate < (now() - interval '1 month')",
      nativeQuery = true)
  void clearStockCardRequestBackupOneMonthBefore();
}
