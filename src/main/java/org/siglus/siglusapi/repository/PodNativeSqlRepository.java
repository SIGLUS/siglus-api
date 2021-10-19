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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.android.db.PodLineItem;
import org.siglus.siglusapi.dto.android.db.ShipmentLineItem;
import org.siglus.siglusapi.dto.android.enumeration.RejectionReason;
import org.siglus.siglusapi.dto.android.request.PodLotLineRequest;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.siglus.siglusapi.exception.InvalidReasonException;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class PodNativeSqlRepository {

  private final JdbcTemplate jdbcTemplate;
  private final SiglusStockEventsService siglusStockEventsService;

  public void insertPodAndShipmentLineItems(UUID podId, UUID shipmentId, UUID facilityId,
      List<PodProductLineRequest> podProducts, Map<String, OrderableDto> orderableCodeToOrderable,
      Map<String, UUID> rejectReasonToId) {
    List<PodLineItem> podLineItems = new ArrayList<>();
    List<ShipmentLineItem> shipmentLineItems = new ArrayList<>();
    for (PodProductLineRequest podProduct : podProducts) {
      OrderableDto orderableDto = orderableCodeToOrderable.get(podProduct.getCode());
      for (PodLotLineRequest lotLineRequest :podProduct.getLots()) {
        LotDto lotDto = new LotDto();
        if (lotLineRequest.getLot() != null) {
          lotDto = siglusStockEventsService.createNewLotOrReturnExisted(
              facilityId, orderableDto, lotLineRequest.getLot().getCode(), lotLineRequest.getLot().getExpirationDate());
        }
        UUID reasonId = null;
        if (isNotBlank(lotLineRequest.getRejectedReason())) {
          RejectionReason rejectionReason = RejectionReason.valueOf(lotLineRequest.getRejectedReason());
          reasonId = rejectReasonToId.get(rejectionReason.name());
          if (reasonId == null) {
            throw new InvalidReasonException(lotLineRequest.getRejectedReason());
          }
        }
        podLineItems.add(PodLineItem.of(podId, lotLineRequest.getNotes(), lotLineRequest.getAcceptedQuantity(),
            lotLineRequest.getShippedQuantity() - lotLineRequest.getAcceptedQuantity(), orderableDto.getId(),
            lotDto.getId(), null, false, reasonId, orderableDto.getVersionNumber()));
        shipmentLineItems.add(ShipmentLineItem
            .of(orderableDto.getId(), lotDto.getId(), lotLineRequest.getShippedQuantity(), shipmentId, "",
                orderableDto.getVersionNumber()));
      }
    }
    if (!podLineItems.isEmpty()) {
      log.info("insert podLineItems, pod id : {}", podId);
      batchInsertPodLineItems(podLineItems);
    }
    if (!shipmentLineItems.isEmpty()) {
      log.info("insert shipmentLineItems, shipment id {}", shipmentId);
      batchInsertShipLineItems(shipmentLineItems);
    }
  }

  public void batchInsertPodLineItems(List<PodLineItem> batchInsertList) {
    jdbcTemplate.batchUpdate("insert into fulfillment.proof_of_delivery_line_items values "
        + "(UUID_GENERATE_V4(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            PodLineItem podLineItem = batchInsertList.get(i);
            ps.setObject(1, podLineItem.getProofOfDeliveryId());
            ps.setString(2, podLineItem.getNotes());
            ps.setInt(3, podLineItem.getQuantityAccepted());
            ps.setInt(4, podLineItem.getQuantityRejected());
            ps.setObject(5, podLineItem.getOrderableId());
            ps.setObject(6, podLineItem.getLotId());
            ps.setString(7, podLineItem.getVvmStatus());
            ps.setBoolean(8, podLineItem.getUserVvm());
            ps.setObject(9, podLineItem.getRejectionReasonId());
            ps.setLong(10, podLineItem.getOrderableVersionNumber());
          }

          @Override
          public int getBatchSize() {
            return batchInsertList.size();
          }
        });
  }

  public void batchInsertShipLineItems(List<ShipmentLineItem> batchInsertList) {
    jdbcTemplate.batchUpdate("insert into fulfillment.shipment_line_items "
        + "values (UUID_GENERATE_V4(), ?, ?, ?, ?, ?, ?)", new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            ShipmentLineItem shipmentLineItem = batchInsertList.get(i);
            ps.setObject(1, shipmentLineItem.getOrderableId());
            ps.setObject(2, shipmentLineItem.getLotId());
            ps.setLong(3, shipmentLineItem.getQuantityShipped());
            ps.setObject(4, shipmentLineItem.getShipmentId());
            ps.setObject(5, wrap(shipmentLineItem.getExtraData()));
            ps.setLong(6, shipmentLineItem.getOrderableVersionNumber());
          }

          @Override
          public int getBatchSize() {
            return batchInsertList.size();
          }
        });
  }

  private String wrap(String jsonString) {
    return StringUtils.isEmpty(jsonString) ? null : jsonString;
  }
}
