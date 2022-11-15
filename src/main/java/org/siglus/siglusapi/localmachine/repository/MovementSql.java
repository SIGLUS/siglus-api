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

package org.siglus.siglusapi.localmachine.repository;

import java.util.HashMap;
import java.util.Map;

public class MovementSql {

  public static final String LOTS = "referencedata.lots";

  public static final String LOTS_QUERY = "select * from referencedata.lots ";

  public static final String LOCALMACHINE_EVENTS = "localmachine.events";

  public static final String LOCALMACHINE_EVENTS_QUERY =
      "select * from localmachine.events where receiverid = '@@' "
          + "and (localreplayed is true or receiversynced  is true)";

  public static final String LOCALMACHINE_EVENT_PAYLOAD = "localmachine.event_payload";

  public static final String LOCALMACHINE_EVENT_PAYLOAD_QUERY =
      "select * from localmachine.event_payload where eventid in "
          + " (select id from localmachine.events where receiverid = '@@' "
          + " and (localreplayed is true or receiversynced is true))";

  private static final String WHERE_ID_IN_STOCK_CARD =
      "where stockcardid in (select id from stockmanagement.stock_cards where facilityid = '@@')";

  private static final String WHERE_ID_IN_PHYSICAL_INVENTORYS =
      "where physicalinventoryid in (select id from stockmanagement.physical_inventories where facilityid = '@@')";

  private static final String SELECT_PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS =
      "select pilia.* from stockmanagement.physical_inventory_line_item_adjustments pilia ";

  private static final String WHERE_FACILITYCODE_IN_FACILITY =
      "where facilitycode in (select code from referencedata.facilities where id = '@@')";

  private MovementSql() {
    throw new IllegalStateException("Utility class");
  }

  public static final String STOCK_EVENTS = "stockmanagement.stock_events";

  public static final String STOCK_EVENTS_QUERY = "select * from stockmanagement.stock_events where facilityid = '@@'";

  public static final String STOCK_EVENT_LINE_ITEMS = "stockmanagement.stock_event_line_items";

  public static final String STOCK_EVENT_LINE_ITEMS_QUERY = "select * from stockmanagement.stock_event_line_items "
      + "where stockeventid in (select id from stockmanagement.stock_events where facilityid = '@@') ";

  public static final String STOCK_CARDS = "stockmanagement.stock_cards";

  public static final String STOCK_CARDS_QUERY = "select * from stockmanagement.stock_cards where facilityid = '@@'";

  public static final String STOCK_CARD_LINE_ITEMS = "stockmanagement.stock_card_line_items";

  public static final String STOCK_CARD_LINE_ITEMS_QUERY =
      "select * from stockmanagement.stock_card_line_items " + WHERE_ID_IN_STOCK_CARD;

  public static final String PHYSICAL_INVENTORIES = "stockmanagement.physical_inventories";

  public static final String PHYSICAL_INVENTORIES_QUERY =
      "select * from stockmanagement.physical_inventories where facilityid = '@@'";

  public static final String PHYSICAL_INVENTORY_LINE_ITEMS = "stockmanagement.physical_inventory_line_items";

  public static final String PHYSICAL_INVENTORY_LINE_ITEMS_QUERY =
      "select * from stockmanagement.physical_inventory_line_items " + WHERE_ID_IN_PHYSICAL_INVENTORYS;

  public static final String PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS =
      "stockmanagement.physical_inventory_line_item_adjustments";

  public static final String PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS_QUERY =
      SELECT_PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS
          + "left join stockmanagement.physical_inventory_line_items pili "
          + "on pili.id = pilia.physicalinventorylineitemid "
          + "left join stockmanagement.physical_inventories pi on pi.id = pili.physicalinventoryid "
          + "where pi.facilityid = '@@' "
          + "union  "
          + SELECT_PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS
          + "left join stockmanagement.stock_card_line_items scli on scli.id = pilia.stockcardlineitemid "
          + "left join stockmanagement.stock_cards sc on sc.id = scli.stockcardid "
          + "where sc.facilityid = '@@' "
          + "union  "
          + SELECT_PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS
          + "left join stockmanagement.stock_event_line_items seli on seli.id = pilia.stockeventlineitemid "
          + "left join stockmanagement.stock_events se on se.id = seli.stockeventid "
          + "where se.facilityid = '@@'";

  public static final String CALCULATED_STOCKS_ON_HAND = "stockmanagement.calculated_stocks_on_hand";

  public static final String CALCULATED_STOCKS_ON_HAND_QUERY =
      "select * from stockmanagement.calculated_stocks_on_hand " + WHERE_ID_IN_STOCK_CARD;

  public static final String STOCK_MANAGEMENT_INITIAL_DRAFTS = "siglusintegration.stock_management_initial_drafts";

  public static final String STOCK_MANAGEMENT_INITIAL_DRAFTS_QUERY =
      "select * from siglusintegration.stock_management_initial_drafts where facilityid = '@@'";

  public static final String STOCK_MANAGEMENT_DRAFTS = "siglusintegration.stock_management_drafts";

  public static final String STOCK_MANAGEMENT_DRAFTS_QUERY =
      "select * from siglusintegration.stock_management_drafts where facilityid = '@@'";

  public static final String STOCK_MANAGEMENT_DRAFT_LINE_ITEMS = "siglusintegration.stock_management_draft_line_items";

  public static final String STOCK_MANAGEMENT_DRAFT_LINE_ITEMS_QUERY =
      "select * from siglusintegration.stock_management_draft_line_items "
          + "where stockmanagementdraftid in "
          + "(select id from siglusintegration.stock_management_drafts where facilityid = '@@')";

  public static final String STOCK_EVENT_PRODUCT_REQUESTED = "siglusintegration.stock_event_product_requested";

  public static final String STOCK_EVENT_PRODUCT_REQUESTED_QUERY =
      "select * from siglusintegration.stock_event_product_requested "
          + "where stockeventid in (select id from stockmanagement.stock_events where facilityid = '@@')";

  public static final String STOCK_CARD_LOCATION_MOVEMENT_DRAFTS =
      "siglusintegration.stock_card_location_movement_drafts";

  public static final String STOCK_CARD_LOCATION_MOVEMENT_DRAFTS_QUERY =
      "select * from siglusintegration.stock_card_location_movement_drafts where facilityid = '@@'";

  public static final String STOCK_CARD_LOCATION_MOVEMENT_DRAFT_LINE_ITEMS =
      "siglusintegration.stock_card_location_movement_draft_line_items";

  public static final String STOCK_CARD_LOCATION_MOVEMENT_DRAFT_LINE_ITEMS_QUERY =
      "select * from siglusintegration.stock_card_location_movement_draft_line_items "
          + "where stockcardlocationmovementdraftid in "
          + "(select id from siglusintegration.stock_card_location_movement_drafts where facilityid = '@@')";

  public static final String STOCK_CARD_LOCATION_MOVEMENT_LINE_ITEMS =
      "siglusintegration.stock_card_location_movement_line_items";

  public static final String STOCK_CARD_LOCATION_MOVEMENT_LINE_ITEMS_QUERY =
      "select * from siglusintegration.stock_card_location_movement_line_items " + WHERE_ID_IN_STOCK_CARD;

  public static final String STOCK_CARD_LINE_ITEMS_BY_LOCATION = "siglusintegration.stock_card_line_items_by_location";

  public static final String STOCK_CARD_LINE_ITEMS_BY_LOCATION_QUERY =
      "select sclibl.* from siglusintegration.stock_card_line_items_by_location sclibl "
          + "left join stockmanagement.stock_card_line_items scli on scli.id = sclibl.stockcardlineitemid "
          + "left join stockmanagement.stock_cards sc on scli.stockcardid = sc.id "
          + "where sc.facilityid = '@@'";

  public static final String STOCK_CARD_EXTENSION = "siglusintegration.stock_card_extension";

  public static final String STOCK_CARD_EXTENSION_QUERY =
      "select * from siglusintegration.stock_card_extension " + WHERE_ID_IN_STOCK_CARD;

  public static final String PHYSICAL_INVENTORIES_EXTENSION = "siglusintegration.physical_inventories_extension";

  public static final String PHYSICAL_INVENTORIES_EXTENSION_QUERY =
      "select * from siglusintegration.physical_inventories_extension " + WHERE_ID_IN_PHYSICAL_INVENTORYS;

  public static final String PHYSICAL_INVENTORY_LINE_ITEMS_EXTENSION =
      "siglusintegration.physical_inventory_line_items_extension";

  public static final String PHYSICAL_INVENTORY_LINE_ITEMS_EXTENSION_QUERY =
      "select * from siglusintegration.physical_inventory_line_items_extension " + WHERE_ID_IN_PHYSICAL_INVENTORYS;

  public static final String PHYSICAL_INVENTORY_SUB_DRAFT = "siglusintegration.physical_inventory_sub_draft";

  public static final String PHYSICAL_INVENTORY_SUB_DRAFT_QUERY =
      "select * from siglusintegration.physical_inventory_sub_draft " + WHERE_ID_IN_PHYSICAL_INVENTORYS;

  public static final String PHYSICAL_INVENTORY_EMPTY_LOCATION_LINE_ITEMS =
      "siglusintegration.physical_inventory_empty_location_line_items";

  public static final String PHYSICAL_INVENTORY_EMPTY_LOCATION_LINE_ITEMS_QUERY =
      "select pielli.* from siglusintegration.physical_inventory_empty_location_line_items pielli "
          + "left join siglusintegration.physical_inventory_sub_draft pisd on pisd.id = pielli.subdraftid "
          + "left join stockmanagement.physical_inventories pi on pi.id = pisd.physicalinventoryid "
          + "where pi.facilityid = '@@'";

  public static final String CALCULATED_STOCKS_ON_HAND_BY_LOCATION =
      "siglusintegration.calculated_stocks_on_hand_by_location";

  public static final String CALCULATED_STOCKS_ON_HAND_BY_LOCATION_QUERY =
      "select * from siglusintegration.calculated_stocks_on_hand_by_location " + WHERE_ID_IN_STOCK_CARD;

  public static final String ARCHIVED_PRODUCTS = "siglusintegration.archived_products";

  public static final String ARCHIVED_PRODUCTS_QUERY =
      "select * from siglusintegration.archived_products where facilityid = '@@'";

  public static final String CMMS = "siglusintegration.cmms";

  public static final String CMMS_QUERY = "select * from siglusintegration.cmms " + WHERE_FACILITYCODE_IN_FACILITY;

  public static final String CPS = "siglusintegration.cps";

  public static final String CPS_QUERY = "select * from siglusintegration.cps " + WHERE_FACILITYCODE_IN_FACILITY;

  public static final String HF_CMMS = "siglusintegration.hf_cmms";

  public static final String HF_CMMS_QUERY =
      "select * from siglusintegration.hf_cmms " + WHERE_FACILITYCODE_IN_FACILITY;

  public static Map<String, String> getMovementSql() {
    Map<String, String> movementSql = new HashMap<>();
    movementSql.put(LOTS, LOTS_QUERY);
    movementSql.put(LOCALMACHINE_EVENTS, LOCALMACHINE_EVENTS_QUERY);
    movementSql.put(LOCALMACHINE_EVENT_PAYLOAD, LOCALMACHINE_EVENT_PAYLOAD_QUERY);

    movementSql.put(STOCK_EVENTS, STOCK_EVENTS_QUERY);
    movementSql.put(STOCK_EVENT_LINE_ITEMS, STOCK_EVENT_LINE_ITEMS_QUERY);
    movementSql.put(STOCK_CARDS, STOCK_CARDS_QUERY);
    movementSql.put(STOCK_CARD_LINE_ITEMS, STOCK_CARD_LINE_ITEMS_QUERY);
    movementSql.put(PHYSICAL_INVENTORIES, PHYSICAL_INVENTORIES_QUERY);
    movementSql.put(PHYSICAL_INVENTORY_LINE_ITEMS, PHYSICAL_INVENTORY_LINE_ITEMS_QUERY);
    movementSql
        .put(PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS, PHYSICAL_INVENTORY_LINE_ITEM_ADJUSTMENTS_QUERY);
    movementSql.put(CALCULATED_STOCKS_ON_HAND, CALCULATED_STOCKS_ON_HAND_QUERY);

    movementSql.put(STOCK_MANAGEMENT_INITIAL_DRAFTS, STOCK_MANAGEMENT_INITIAL_DRAFTS_QUERY);
    movementSql.put(STOCK_MANAGEMENT_DRAFTS, STOCK_MANAGEMENT_DRAFTS_QUERY);
    movementSql.put(STOCK_MANAGEMENT_DRAFT_LINE_ITEMS, STOCK_MANAGEMENT_DRAFT_LINE_ITEMS_QUERY);
    movementSql.put(STOCK_EVENT_PRODUCT_REQUESTED, STOCK_EVENT_PRODUCT_REQUESTED_QUERY);
    movementSql
        .put(STOCK_CARD_LOCATION_MOVEMENT_DRAFTS, STOCK_CARD_LOCATION_MOVEMENT_DRAFTS_QUERY);
    movementSql
        .put(STOCK_CARD_LOCATION_MOVEMENT_DRAFT_LINE_ITEMS, STOCK_CARD_LOCATION_MOVEMENT_DRAFT_LINE_ITEMS_QUERY);
    movementSql
        .put(STOCK_CARD_LOCATION_MOVEMENT_LINE_ITEMS, STOCK_CARD_LOCATION_MOVEMENT_LINE_ITEMS_QUERY);
    movementSql.put(STOCK_CARD_LINE_ITEMS_BY_LOCATION, STOCK_CARD_LINE_ITEMS_BY_LOCATION_QUERY);
    movementSql.put(STOCK_CARD_EXTENSION, STOCK_CARD_EXTENSION_QUERY);
    movementSql.put(PHYSICAL_INVENTORIES_EXTENSION, PHYSICAL_INVENTORIES_EXTENSION_QUERY);
    movementSql
        .put(PHYSICAL_INVENTORY_LINE_ITEMS_EXTENSION, PHYSICAL_INVENTORY_LINE_ITEMS_EXTENSION_QUERY);
    movementSql.put(PHYSICAL_INVENTORY_SUB_DRAFT, PHYSICAL_INVENTORY_SUB_DRAFT_QUERY);
    movementSql
        .put(PHYSICAL_INVENTORY_EMPTY_LOCATION_LINE_ITEMS, PHYSICAL_INVENTORY_EMPTY_LOCATION_LINE_ITEMS_QUERY);
    movementSql
        .put(CALCULATED_STOCKS_ON_HAND_BY_LOCATION, CALCULATED_STOCKS_ON_HAND_BY_LOCATION_QUERY);
    movementSql.put(ARCHIVED_PRODUCTS, ARCHIVED_PRODUCTS_QUERY);
    movementSql.put(CMMS, CMMS_QUERY);
    movementSql.put(CPS, CPS_QUERY);
    movementSql.put(HF_CMMS, HF_CMMS_QUERY);

    return movementSql;
  }

}
