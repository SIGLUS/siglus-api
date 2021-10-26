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

package org.siglus.siglusapi.service;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.LotConflict;
import org.siglus.siglusapi.repository.LotConflictRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LotConflictService {

  private final LotConflictRepository repo;

  public void handleLotConflict(UUID facilityId, String lotCode, UUID lotId, LocalDate expirationDate,
      @Nonnull LocalDate existedExpirationDate) {
    requireNonNull(existedExpirationDate);
    if (existedExpirationDate.equals(expirationDate)) {
      return;
    }
    log.info("the date of lot {} is different: [in-request: {}, in-db: {}]", lotCode, expirationDate,
        existedExpirationDate);
    LotConflict exitedConflict = repo
        .findOneByFacilityIdAndLotIdAndLotCodeAndExpirationDate(facilityId, lotId, lotCode, existedExpirationDate);
    if (exitedConflict != null) {
      log.info("conflict is already recorded");
      return;
    }
    LotConflict lotConflict = LotConflict.of(facilityId, lotId, lotCode, expirationDate);
    lotConflict = repo.save(lotConflict);
    log.info("record conflict with id {}", lotConflict.getId());
  }


}
