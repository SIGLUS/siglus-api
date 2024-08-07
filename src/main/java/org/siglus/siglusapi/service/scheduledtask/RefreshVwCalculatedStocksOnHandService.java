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

package org.siglus.siglusapi.service.scheduledtask;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.repository.VwCalculatedStocksOnHandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefreshVwCalculatedStocksOnHandService {

  private final VwCalculatedStocksOnHandRepository vwCalculatedStocksOnHandRepository;

  @Transactional
  public void refreshVwCalculatedStocksOnHand() {
    log.info("vw_calculated_stocks_on_hand refresh. start at {} ", LocalDateTime.now());
    vwCalculatedStocksOnHandRepository.refreshVwCalculatedStocksOnHand();
    log.info("vw_calculated_stocks_on_hand refresh. end at {} ", LocalDateTime.now());
  }
}
