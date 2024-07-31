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

package org.siglus.siglusapi.scheduledtask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.repository.RequisitionAvailableProductRepository;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class ClearRequisitionAvailableProductsTask {

  private final RequisitionAvailableProductRepository requisitionAvailableProductRepository;

  // @Scheduled(cron = "${clear.requisition.available.products.cron}", zone = "${time.zoneId}")
  // @SchedulerLock(name = "clear_requisition_available_products_task")
  public void clear() {
    log.info("clear unused requisition available products for more than 1 year");
    requisitionAvailableProductRepository.clearRequisitionAvailableProducts();
  }
}
