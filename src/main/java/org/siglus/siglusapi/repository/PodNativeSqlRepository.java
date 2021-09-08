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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.dto.android.request.PodLotLineRequest;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PodNativeSqlRepository {

  private final JdbcTemplate jdbcTemplate;

  public void insertPodAndShipmentLineItems(UUID podId, UUID shipmentId, List<PodProductLineRequest> podProducts,
      Map<String, OrderableDto> orderableCodeToOrderable, Map<String, Map<String, UUID>> orderableCodeToLots,
      Map<String, UUID> rejectReasonToId) {
    StringBuilder insertPodLineItemsSql = new StringBuilder("insert into fulfillment.proof_of_delivery_line_items values ");
    StringBuilder insertShipmentLineItemsSql = new StringBuilder("insert into fulfillment.shipment_line_items values ");
    for (PodProductLineRequest podProduct : podProducts) {
      for (PodLotLineRequest lot :podProduct.getLots()) {
        insertPodLineItemsSql.append("(");
        insertPodLineItemsSql.append("'").append(UUID.randomUUID()).append("',");
        insertPodLineItemsSql.append("'").append(podId).append("',");
        insertPodLineItemsSql.append("'").append(lot.getNotes()).append("',");
        insertPodLineItemsSql.append(lot.getAcceptedQuantity()).append("',");
        insertPodLineItemsSql.append(",");
        insertPodLineItemsSql.append(",").append(orderableCodeToOrderable.get(podProduct.getCode()).getId()).append("',");
        insertPodLineItemsSql.append(",").append(orderableCodeToLots.get(podProduct.getCode()).get(lot.getLot().getCode())).append("',");
        insertPodLineItemsSql.append(",");
        insertPodLineItemsSql.append(",").append("false").append(",");
        insertPodLineItemsSql.append(",").append(rejectReasonToId.get(lot.getRejectedReason())).append(",");
        insertPodLineItemsSql.append(orderableCodeToOrderable.get(podProduct.getCode()).getVersionNumber());
        insertPodLineItemsSql.append("),");

        insertShipmentLineItemsSql.append("(");
        insertShipmentLineItemsSql.append("'").append(UUID.randomUUID()).append("',");
        insertShipmentLineItemsSql.append(orderableCodeToOrderable.get(podProduct.getCode()).getId()).append("',");
        insertShipmentLineItemsSql.append(orderableCodeToLots.get(podProduct.getCode()).get(lot.getLot().getCode())).append("',");
        insertShipmentLineItemsSql.append(lot.getShippedQuantity()).append(",");
        insertShipmentLineItemsSql.append("'").append(shipmentId).append(",");
        insertShipmentLineItemsSql.append("'',");
        insertShipmentLineItemsSql.append(orderableCodeToOrderable.get(podProduct.getCode()).getVersionNumber()).append(",");
        insertShipmentLineItemsSql.append("),");
      }
    }
    insertPodLineItemsSql.replace(
        insertPodLineItemsSql.lastIndexOf(",") - 1, insertPodLineItemsSql.lastIndexOf(","), ";");
    insertShipmentLineItemsSql.replace(
        insertShipmentLineItemsSql.lastIndexOf(",") -1, insertShipmentLineItemsSql.lastIndexOf(","), ";");
    log.info("batch insert pod line items, pod id: {}", podId);
    jdbcTemplate.execute(insertPodLineItemsSql.toString());
    log.info("batch insert shipment line items, shipment id: {}", shipmentId);
    jdbcTemplate.execute(insertShipmentLineItemsSql.toString());
  }
}
