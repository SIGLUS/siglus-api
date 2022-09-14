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
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class LocationDraftRepository extends BaseNativeRepository {

  private static final String WHERE_IS_DRAFT = "WHERE facilityid = ? AND isdraft is TRUE";
  private static final String WHERE_INVENTORY_IS_DRAFT =
      "IN (SELECT id FROM stockmanagement.physical_inventories " + WHERE_IS_DRAFT + ")";

  private final JdbcTemplate jdbc;

  @Transactional
  public void deleteLocationRelatedDrafts(UUID facilityId) {
    jdbc.update("DELETE FROM siglusintegration.stock_management_draft_line_items WHERE stockmanagementdraftid in "
            + "(SELECT id FROM siglusintegration.stock_management_drafts WHERE facilityid = ?)",
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.stock_management_drafts WHERE facilityid = ?",
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.stock_management_initial_drafts WHERE facilityid = ?",
        facilityId);
    jdbc.update("DELETE FROM stockmanagement.physical_inventory_line_item_adjustments "
            + "WHERE physicalinventorylineitemid IN (SELECT id FROM stockmanagement.physical_inventory_line_items "
            + "WHERE physicalinventoryid IN (SELECT id FROM stockmanagement.physical_inventories "
            + WHERE_IS_DRAFT + "))",
        facilityId);
    jdbc.update("DELETE FROM stockmanagement.physical_inventory_line_items WHERE physicalinventoryid "
            + WHERE_INVENTORY_IS_DRAFT,
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.physical_inventory_line_items_extension WHERE physicalinventoryid "
            + WHERE_INVENTORY_IS_DRAFT,
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.physical_inventories_extension WHERE physicalinventoryid "
            + WHERE_INVENTORY_IS_DRAFT,
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.physical_inventory_sub_draft WHERE physicalinventoryid "
            + WHERE_INVENTORY_IS_DRAFT,
        facilityId);
    jdbc.update("DELETE FROM stockmanagement.physical_inventories " + WHERE_IS_DRAFT,
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.stock_card_location_movement_draft_line_items "
            + "WHERE stockcardlocationmovementdraftid IN (SELECT id FROM "
            + "siglusintegration.stock_card_location_movement_drafts WHERE facilityid = ?)",
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.stock_card_location_movement_drafts WHERE facilityid = ?",
        facilityId);
    jdbc.update("UPDATE fulfillment.orders SET status = 'ORDERED' WHERE id IN (SELECT orderid FROM "
            + "fulfillment.shipment_drafts WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE facilityid = ?))",
        facilityId);
    jdbc.update("DELETE FROM fulfillment.shipment_draft_line_items WHERE shipmentdraftid IN (SELECT id FROM"
            + " fulfillment.shipment_drafts WHERE orderid IN (SELECT id FROM fulfillment.orders WHERE facilityid = ?))",
        facilityId);
    jdbc.update("DELETE FROM fulfillment.shipment_drafts WHERE orderid IN "
            + "(SELECT id FROM fulfillment.orders WHERE facilityid = ?)",
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.pod_line_items_extension WHERE subdraftid "
            + "IN (SELECT id FROM siglusintegration.pod_sub_draft WHERE proofofdeliveryid "
            + "IN (SELECT id FROM fulfillment.proofs_of_delivery WHERE shipmentid "
            + "IN (SELECT id FROM fulfillment.shipments WHERE orderid "
            + "IN (SELECT id FROM fulfillment.orders WHERE requestingfacilityid = ?))))",
        facilityId);
    jdbc.update("DELETE FROM siglusintegration.pod_sub_draft WHERE proofofdeliveryid "
            + "IN (SELECT id FROM fulfillment.proofs_of_delivery WHERE shipmentid "
            + "IN (SELECT id FROM fulfillment.shipments WHERE orderid "
            + "IN (SELECT id FROM fulfillment.orders WHERE requestingfacilityid = ?)))",
        facilityId);
  }

}
