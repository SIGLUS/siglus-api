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

package org.openlmis.referencedata.service;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class LotService {

  @Autowired
  private LotRepository lotRepository;

  @Autowired
  private TradeItemRepository tradeItemRepository;

  /**
   * Returns page of lots matching the given request parameters.
   *
   * @param requestParams  the request parameters (trade item ID, expiration date, code or IDs).
   * @param pageable  the page to get, or one page with all if null.
   * @return the Page of lots found, or an empty page.
   */
  public Page<Lot> search(@NotNull LotSearchParams requestParams, Pageable pageable) {
    List<TradeItem> tradeItems = Collections.emptyList();

    if (!isEmpty(requestParams.getTradeItemId())) {
      tradeItems = tradeItemRepository.findAll(requestParams.getTradeItemId());

      if (tradeItems.isEmpty()) {
        return Pagination.getPage(Collections.emptyList(), pageable, 0);
      }
    }

    return lotRepository.search(
        tradeItems,
        requestParams.getExpirationDate(),
        requestParams.getLotCode(),
        requestParams.getId(),
        pageable
    );
  }

}
