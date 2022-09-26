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

package org.siglus.siglusapi.localmachine.event.stockmovement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.cdc.CdcListener;
import org.siglus.siglusapi.localmachine.cdc.CdcRecord;
import org.siglus.siglusapi.localmachine.cdc.CdcRecordMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile({"localmachine"})
public class LocalMovementEventEmitter implements CdcListener {

  private final EventPublisher eventPublisher;
  private final CdcRecordMapper cdcRecordMapper;

  @Override
  public String[] acceptedTables() {
    return new String[]{
        "stockmanagement.stock_events",
        "stockmanagement.stock_event_line_items",
        "stockmanagement.stock_cards",
        "stockmanagement.stock_card_line_items",
        "stockmanagement.physical_inventories",
        "stockmanagement.physical_inventory_line_items",
        "stockmanagement.calculated_stocks_on_hand",
        "siglusintegration.stock_card_extension",
        "referencedata.lots"
    };
  }

  @Transactional
  @Override
  public void on(List<CdcRecord> records) {
    eventPublisher.emitNonGroupEvent(new LocalMovementEvent(cdcRecordMapper.buildEvents(records)));
  }
}
