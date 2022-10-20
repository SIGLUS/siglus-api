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

import com.google.common.collect.Lists;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:LineLength")
public class LocationDraftRepository extends BaseNativeRepository {

  private static final String SQL_1 = "DELETE FROM siglusintegration.stock_management_draft_line_items WHERE stockmanagementdraftid in (SELECT id FROM siglusintegration.stock_management_drafts WHERE facilityid = ?)";
  private static final String SQL_2 = "DELETE FROM siglusintegration.stock_management_drafts WHERE facilityid = ?";
  private static final String SQL_3 = "DELETE FROM siglusintegration.stock_management_initial_drafts WHERE facilityid = ?";
  private static final String SQL_4 = "DELETE FROM stockmanagement.physical_inventory_line_item_adjustments WHERE physicalinventorylineitemid in (SELECT id FROM stockmanagement.physical_inventory_line_items WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE))";
  private static final String SQL_5 = "DELETE FROM stockmanagement.physical_inventory_line_items WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE)";
  private static final String SQL_6 = "DELETE FROM siglusintegration.physical_inventory_line_items_extension WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE)";
  private static final String SQL_7 = "DELETE FROM siglusintegration.physical_inventories_extension WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE)";
  private static final String SQL_8 = "DELETE FROM siglusintegration.physical_inventory_empty_location_line_items WHERE subdraftid  IN (SELECT id FROM siglusintegration.physical_inventory_sub_draft WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE))";
  private static final String SQL_9 = "DELETE FROM siglusintegration.physical_inventory_sub_draft WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE)";
  private static final String SQL_10 = "DELETE FROM stockmanagement.physical_inventories WHERE facilityid = ? AND isdraft is TRUE";
  private static final String SQL_11 = "DELETE FROM siglusintegration.stock_card_location_movement_draft_line_items WHERE stockcardlocationmovementdraftid in (SELECT id FROM siglusintegration.stock_card_location_movement_drafts WHERE facilityid = ?)";
  private static final String SQL_12 = "DELETE FROM siglusintegration.stock_card_location_movement_drafts  WHERE facilityid = ?";
  private static final String SQL_13 = "UPDATE fulfillment.orders SET status = 'ORDERED' WHERE id IN (SELECT orderid FROM fulfillment.shipment_drafts WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE supplyingfacilityid = ?))";
  private static final String SQL_14 = "DELETE FROM siglusintegration.shipment_draft_line_items_extension WHERE shipmentdraftlineitemid IN (SELECT id FROM fulfillment.shipment_draft_line_items WHERE shipmentdraftid IN (SELECT id FROM fulfillment.shipment_drafts WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE supplyingfacilityid = ?)))";
  private static final String SQL_15 = "DELETE FROM fulfillment.shipment_draft_line_items WHERE shipmentdraftid IN (SELECT id FROM fulfillment.shipment_drafts WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE supplyingfacilityid = ?))";
  private static final String SQL_16 = "DELETE FROM fulfillment.shipment_drafts WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE supplyingfacilityid = ?)";
  private static final String SQL_17 = "DELETE FROM siglusintegration.pod_line_items_extension WHERE subdraftid IN (SELECT id FROM siglusintegration.pod_sub_draft WHERE proofofdeliveryid IN (SELECT id FROM fulfillment.proofs_of_delivery WHERE shipmentid IN (SELECT id FROM fulfillment.shipments WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE requestingfacilityid = ?))))";
  private static final String SQL_18 = "DELETE FROM siglusintegration.pod_sub_draft_line_items_by_location WHERE podlineitemid IN (SELECT id FROM fulfillment.proof_of_delivery_line_items WHERE proofofdeliveryid IN (SELECT id FROM fulfillment.proofs_of_delivery WHERE shipmentid IN (SELECT id FROM fulfillment.shipments WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE requestingfacilityid = ?))))";
  private static final String SQL_19 = "DELETE FROM siglusintegration.pod_sub_draft WHERE proofofdeliveryid IN (SELECT id FROM fulfillment.proofs_of_delivery WHERE shipmentid IN (SELECT id FROM fulfillment.shipments WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE requestingfacilityid = ?)))";

  private final JdbcTemplate jdbc;

  @Transactional
  public void deleteFacilityRelatedDrafts(UUID facilityId) {
    Lists.newArrayList(SQL_1, SQL_2, SQL_3, SQL_4, SQL_5, SQL_6, SQL_7, SQL_8, SQL_9, SQL_10, SQL_11, SQL_12, SQL_13,
        SQL_14, SQL_15, SQL_16, SQL_17, SQL_18, SQL_19)
        .forEach(sql -> {
          jdbc.update(sql, facilityId);
        });
  }
}
