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

public class RequisitionOrderSql {

  private static final String LEFT_JOIN_USERS =
      "left join referencedata.users u on u.id = n.userid where u.homefacilityid = '@@'";

  private static final String LEFT_JOIN_SUPERVISORY_NODES =
      "left join referencedata.supervisory_nodes sn on sn.id = r.supervisorynodeid ";

  private static final String SHIPMENT_DRAFT_LEFT_JOIN_ORDERS = "left join fulfillment.orders o on o.id = sd.orderid ";

  private static final String SHIPMENT_LEFT_JOIN_ORDERS = "left join fulfillment.orders o on o.id = s.orderid ";

  private static final String POD_LEFT_JOIN_SHIPMENT = "left join fulfillment.shipments s on s.id = pod.shipmentid ";

  private static final String POD_LEFT_JOIN_PROOFS_OF_DELIVERY =
      "left join fulfillment.proofs_of_delivery pod on pod.id = podli.proofofdeliveryid ";

  private static final String REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS =
      "left join requisition.requisitions r on r.id = rd.requisitionid ";

  private static final String WHERE_ID_IN_ORDERS =
      "where orderid in (select id from fulfillment.orders where facilityid = '@@' or supplyingfacilityid = '@@')";

  private static final String WHERE_ID_IN_REQUISITIONS =
      "where requisitionid in (select r.id from requisition.requisitions r ";

  private static final String WHERE_FACILITY_ID =
      "where r.facilityid = '@@' or (sn.facilityid = '@@' and sn.parentid is null)";

  private static final String WHERE_FACILITYID_SUPPLYING_FACILITYID =
      "where o.facilityid = '@@' or o.supplyingfacilityid = '@@'";

  private RequisitionOrderSql() {
    throw new IllegalStateException("Utility class");
  }

  public static final String NOTIFICATIONS = "notification.notifications";

  public static final String NOTIFICATIONS_QUERY = "select n.* from notification.notifications n " + LEFT_JOIN_USERS;

  public static final String NOTIFICATION_MESSAGES = "notification.notification_messages";

  public static final String NOTIFICATION_MESSAGES_QUERY = "select nm.* from notification.notification_messages nm "
      + "left join notification.notifications n on n.id = nm.notificationid "
      + LEFT_JOIN_USERS;

  public static final String REQUISITIONS = "requisition.requisitions";

  public static final String REQUISITIONS_QUERY = "select r.* from requisition.requisitions r "
      + LEFT_JOIN_SUPERVISORY_NODES
      + WHERE_FACILITY_ID;

  public static final String REQUISITIONS_PREVIOUS_REQUISITIONS = "requisition.requisitions_previous_requisitions";

  public static final String REQUISITIONS_PREVIOUS_REQUISITIONS_QUERY =
      "select * from requisition.requisitions_previous_requisitions "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String REQUISITION_PERMISSION_STRINGS = "requisition.requisition_permission_strings";

  public static final String REQUISITION_PERMISSION_STRINGS_QUERY =
      "select * from requisition.requisition_permission_strings "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ")";

  public static final String REQUISITION_LINE_ITEMS = "requisition.requisition_line_items";

  public static final String REQUISITION_LINE_ITEMS_QUERY =
      "select * from requisition.requisition_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ")";

  public static final String AVAILABLE_PRODUCTS = "requisition.available_products";

  public static final String AVAILABLE_PRODUCTS_QUERY =
      "select * from requisition.available_products "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String STATUS_CHANGES = "requisition.status_changes";

  public static final String STATUS_CHANGES_QUERY =
      "select * from requisition.status_changes "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String STATUS_MESSAGES = "requisition.status_messages";

  public static final String STATUS_MESSAGES_QUERY =
      "select * from requisition.status_messages "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String STOCK_ADJUSTMENT_REASONS = "requisition.stock_adjustment_reasons";

  public static final String STOCK_ADJUSTMENT_REASONS_QUERY =
      "select * from requisition.stock_adjustment_reasons "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String ORDERS = "fulfillment.orders";

  public static final String ORDERS_QUERY = "select * from fulfillment.orders "
      + "where facilityid = '@@' or supplyingfacilityid = '@@'";

  public static final String ORDER_LINE_ITEMS = "fulfillment.order_line_items";

  public static final String ORDER_LINE_ITEMS_QUERY =
      "select * from fulfillment.order_line_items " + WHERE_ID_IN_ORDERS;

  public static final String SHIPMENT_DRAFTS = "fulfillment.shipment_drafts";

  public static final String SHIPMENT_DRAFTS_QUERY =
      "select sd.* from fulfillment.shipment_drafts sd "
          + SHIPMENT_DRAFT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String SHIPMENT_DRAFT_LINE_ITEMS = "fulfillment.shipment_draft_line_items";

  public static final String SHIPMENT_DRAFT_LINE_ITEMS_QUERY =
      "select sdli.* from fulfillment.shipment_draft_line_items sdli "
          + "left join fulfillment.shipment_drafts sd on sd.id = sdli.shipmentdraftid "
          + SHIPMENT_DRAFT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String SHIPMENTS = "fulfillment.shipments";

  public static final String SHIPMENTS_QUERY = "select s.* from fulfillment.shipments s "
      + SHIPMENT_LEFT_JOIN_ORDERS
      + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String SHIPMENT_LINE_ITEMS = "fulfillment.shipment_line_items";

  public static final String SHIPMENT_LINE_ITEMS_QUERY = "select sli.* from fulfillment.shipment_line_items sli "
      + "left join fulfillment.shipments s on s.id = sli.shipmentid "
      + SHIPMENT_LEFT_JOIN_ORDERS
      + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String PROOFS_OF_DELIVERY = "fulfillment.proofs_of_delivery";

  public static final String PROOFS_OF_DELIVERY_QUERY = "select pod.* from fulfillment.proofs_of_delivery pod "
      + POD_LEFT_JOIN_SHIPMENT
      + SHIPMENT_LEFT_JOIN_ORDERS
      + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String PROOF_OF_DELIVERY_LINE_ITEMS = "fulfillment.proof_of_delivery_line_items";

  public static final String PROOF_OF_DELIVERY_LINE_ITEMS_QUERY =
      "select podli.* from fulfillment.proof_of_delivery_line_items podli "
          + POD_LEFT_JOIN_PROOFS_OF_DELIVERY
          + POD_LEFT_JOIN_SHIPMENT
          + SHIPMENT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String FULFILLMENT_STATUS_CHANGES = "fulfillment.status_changes";

  public static final String FULFILLMENT_STATUS_CHANGES_QUERY =
      "select * from fulfillment.status_changes " + WHERE_ID_IN_ORDERS;

  public static final String FULFILLMENT_STATUS_MESSAGES = "fulfillment.status_messages";

  public static final String FULFILLMENT_STATUS_MESSAGES_QUERY =
      "select * from fulfillment.status_messages " + WHERE_ID_IN_ORDERS;

  public static final String BASE_LINE_ITEMS = "siglusintegration.base_line_items";

  public static final String BASE_LINE_ITEMS_QUERY = "select * from siglusintegration.base_line_items "
      + WHERE_ID_IN_REQUISITIONS
      + LEFT_JOIN_SUPERVISORY_NODES
      + WHERE_FACILITY_ID
      + ") ";

  public static final String REQUISITIONS_DRAFT = "siglusintegration.requisitions_draft";

  public static final String REQUISITIONS_DRAFT_QUERY =
      "select * from siglusintegration.requisitions_draft "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String CONSULTATION_NUMBER_LINE_ITEMS = "siglusintegration.consultation_number_line_items";

  public static final String CONSULTATION_NUMBER_LINE_ITEMS_QUERY =
      "select * from siglusintegration.consultation_number_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String CONSULTATION_NUMBER_LINE_ITEM_DRAFTS =
      "siglusintegration.consultation_number_line_item_drafts";

  public static final String CONSULTATION_NUMBER_LINE_ITEM_DRAFTS_QUERY =
      "select cnlid.* from siglusintegration.consultation_number_line_item_drafts cnlid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = cnlid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String GENERATED_NUMBER = "siglusintegration.generated_number";

  public static final String GENERATED_NUMBER_QUERY =
      "select * from siglusintegration.generated_number where facilityid = '@@'";

  public static final String KIT_USAGE_LINE_ITEMS = "siglusintegration.kit_usage_line_items";

  public static final String KIT_USAGE_LINE_ITEMS_QUERY = "select * from siglusintegration.kit_usage_line_items "
      + WHERE_ID_IN_REQUISITIONS
      + LEFT_JOIN_SUPERVISORY_NODES
      + WHERE_FACILITY_ID
      + ") ";

  public static final String KIT_USAGE_LINE_ITEMS_DRAFT = "siglusintegration.kit_usage_line_items_draft";

  public static final String KIT_USAGE_LINE_ITEMS_DRAFT_QUERY =
      "select kulid.* from siglusintegration.kit_usage_line_items_draft kulid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = kulid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String SIGLUSINTEGRATION_NOTIFICATIONS = "siglusintegration.notifications";

  public static final String SIGLUSINTEGRATION_NOTIFICATIONS_QUERY =
      "select n.* from siglusintegration.notifications n "
          + "left join referencedata.supervisory_nodes sn on sn.id = n.notifysupervisorynodeid "
          + "where n.facilityid = '@@' or sn.facilityid = '@@'";

  public static final String ORDER_EXTERNAL_IDS = "siglusintegration.order_external_ids";

  public static final String ORDER_EXTERNAL_IDS_QUERY = "select * from siglusintegration.order_external_ids "
      + WHERE_ID_IN_REQUISITIONS
      + LEFT_JOIN_SUPERVISORY_NODES
      + WHERE_FACILITY_ID
      + ") ";

  public static final String ORDER_LINE_ITEM_EXTENSION = "siglusintegration.order_line_item_extension";

  public static final String ORDER_LINE_ITEM_EXTENSION_QUERY =
      "select olie.* from siglusintegration.order_line_item_extension olie "
          + "left join fulfillment.order_line_items oli on oli.id = olie.orderlineitemid "
          + "left join fulfillment.orders o on o.id = oli.orderid "
          + "where o.facilityid = '@@' or supplyingfacilityid = '@@'";

  public static final String PATIENT_LINE_ITEMS = "siglusintegration.patient_line_items";

  public static final String PATIENT_LINE_ITEMS_QUERY = "select * from siglusintegration.patient_line_items "
      + WHERE_ID_IN_REQUISITIONS
      + LEFT_JOIN_SUPERVISORY_NODES
      + WHERE_FACILITY_ID
      + ") ";

  public static final String PATIENT_LINE_ITEM_DRAFTS = "siglusintegration.patient_line_item_drafts";

  public static final String PATIENT_LINE_ITEM_DRAFTS_QUERY =
      "select plid.* from siglusintegration.patient_line_item_drafts plid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = plid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String SHIPMENTS_EXTENSION = "siglusintegration.shipments_extension";

  public static final String SHIPMENTS_EXTENSION_QUERY = "select pe.* from siglusintegration.shipments_extension pe "
      + "left join fulfillment.shipments s on s.id = pe.shipmentid "
      + SHIPMENT_LEFT_JOIN_ORDERS
      + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String POD_SUB_DRAFT = "siglusintegration.pod_sub_draft";

  public static final String POD_SUB_DRAFT_QUERY = "select psd.* from siglusintegration.pod_sub_draft psd "
      + "left join fulfillment.proofs_of_delivery pod on pod.id = psd.proofofdeliveryid "
      + POD_LEFT_JOIN_SHIPMENT
      + SHIPMENT_LEFT_JOIN_ORDERS
      + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String POD_LINE_ITEMS_EXTENSION = "siglusintegration.pod_line_items_extension";

  public static final String POD_LINE_ITEMS_EXTENSION_QUERY =
      "select plie.* from siglusintegration.pod_line_items_extension plie "
          + "left join fulfillment.proof_of_delivery_line_items podli on podli.id = plie.podlineitemid "
          + POD_LEFT_JOIN_PROOFS_OF_DELIVERY
          + POD_LEFT_JOIN_SHIPMENT
          + SHIPMENT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String POD_LINE_ITEMS_BY_LOCATION = "siglusintegration.pod_line_items_by_location";

  public static final String POD_LINE_ITEMS_BY_LOCATION_QUERY =
      "select plibl.* from siglusintegration.pod_line_items_by_location plibl "
          + "left join fulfillment.proof_of_delivery_line_items podli on podli.id = plibl.podlineitemid "
          + POD_LEFT_JOIN_PROOFS_OF_DELIVERY
          + POD_LEFT_JOIN_SHIPMENT
          + SHIPMENT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String POD_SUB_DRAFT_LINE_ITEMS_BY_LOCATION =
      "siglusintegration.pod_sub_draft_line_items_by_location";

  public static final String POD_SUB_DRAFT_LINE_ITEMS_BY_LOCATION_QUERY =
      "select psdlibl.* from siglusintegration.pod_sub_draft_line_items_by_location psdlibl "
          + "left join fulfillment.proof_of_delivery_line_items podli on podli.id = psdlibl.podlineitemid "
          + POD_LEFT_JOIN_PROOFS_OF_DELIVERY
          + POD_LEFT_JOIN_SHIPMENT
          + SHIPMENT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String RAPID_TEST_CONSUMPTION_LINE_ITEMS = "siglusintegration.rapid_test_consumption_line_items";

  public static final String RAPID_TEST_CONSUMPTION_LINE_ITEMS_QUERY =
      "select * from siglusintegration.rapid_test_consumption_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String REGIMEN_LINE_ITEMS = "siglusintegration.regimen_line_items";

  public static final String REGIMEN_LINE_ITEMS_QUERY =
      "select * from siglusintegration.regimen_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String REGIMEN_LINE_ITEM_DRAFT = "siglusintegration.regimen_line_item_draft";

  public static final String REGIMEN_LINE_ITEM_DRAFT_QUERY =
      "select rlid.* from siglusintegration.regimen_line_item_draft rlid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = rlid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String REGIMEN_SUMMARY_LINE_ITEMS = "siglusintegration.regimen_summary_line_items";

  public static final String REGIMEN_SUMMARY_LINE_ITEMS_QUERY =
      "select * from siglusintegration.regimen_summary_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String REGIMEN_SUMMARY_LINE_ITEM_DRAFT = "siglusintegration.regimen_summary_line_item_draft";

  public static final String REGIMEN_SUMMARY_LINE_ITEM_DRAFT_QUERY =
      "select rslid.* from siglusintegration.regimen_summary_line_item_draft rslid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = rslid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String REQUISITION_EXTENSION = "siglusintegration.requisition_extension";

  public static final String REQUISITION_EXTENSION_QUERY =
      "select * from siglusintegration.requisition_extension "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String REQUISITION_LINE_ITEMS_DRAFT = "siglusintegration.requisition_line_items_draft";

  public static final String REQUISITION_LINE_ITEMS_DRAFT_QUERY =
      "select rslid.* from siglusintegration.requisition_line_items_draft rslid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = rslid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String REQUISITION_LINE_ITEMS_EXTENSION = "siglusintegration.requisition_line_items_extension";

  public static final String REQUISITION_LINE_ITEMS_EXTENSION_QUERY =
      "select rlie.* from siglusintegration.requisition_line_items_extension rlie "
          + "left join requisition.requisition_line_items rli on rli.id = rlie.requisitionlineitemid "
          + "left join requisition.requisitions r on r.id = rli.requisitionid "
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String RECEIPT_PLANS = "siglusintegration.receipt_plans";

  public static final String RECEIPT_PLANS_QUERY = "select rp.* from siglusintegration.receipt_plans rp "
      + "left join referencedata.facilities f on f.code = rp.facilitycode "
      + "where f.id = '@@'";

  public static final String RECEIPT_PLAN_LINE_ITEMS = "siglusintegration.receipt_plan_line_items";

  public static final String RECEIPT_PLAN_LINE_ITEMS_QUERY =
      "select rpli.* from siglusintegration.receipt_plan_line_items rpli "
          + "left join siglusintegration.receipt_plans rp on rp.id = rpli.receiptplanid "
          + "left join referencedata.facilities f on f.code = rp.facilitycode "
          + "where f.id = '@@'";

  public static final String SHIPMENT_DRAFT_LINE_ITEMS_EXTENSION =
      "siglusintegration.shipment_draft_line_items_extension";

  public static final String SHIPMENT_DRAFT_LINE_ITEMS_EXTENSION_QUERY =
      "select sdlie.* from siglusintegration.shipment_draft_line_items_extension sdlie "
          + "left join fulfillment.shipment_draft_line_items sdli on sdli.id = sdlie.shipmentdraftlineitemid "
          + "left join fulfillment.shipment_drafts sd on sd.id = sdli.shipmentdraftid "
          + SHIPMENT_DRAFT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String SHIPMENT_LINE_ITEMS_EXTENSION = "siglusintegration.shipment_line_items_extension";

  public static final String SHIPMENT_LINE_ITEMS_EXTENSION_QUERY =
      "select slie.* from siglusintegration.shipment_line_items_extension slie "
          + "left join fulfillment.shipment_line_items sli on sli.id = slie.shipmentlineitemid "
          + "left join fulfillment.shipments s on s.id = sli.shipmentid "
          + SHIPMENT_LEFT_JOIN_ORDERS
          + WHERE_FACILITYID_SUPPLYING_FACILITYID;

  public static final String TEST_CONSUMPTION_LINE_ITEMS_DRAFT = "siglusintegration.test_consumption_line_items_draft";

  public static final String TEST_CONSUMPTION_LINE_ITEMS_DRAFT_QUERY =
      "select tclid.* from siglusintegration.test_consumption_line_items_draft tclid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = tclid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String USAGE_INFORMATION_LINE_ITEMS = "siglusintegration.usage_information_line_items";

  public static final String USAGE_INFORMATION_LINE_ITEMS_QUERY =
      "select * from siglusintegration.usage_information_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";

  public static final String USAGE_INFORMATION_LINE_ITEMS_DRAFT =
      "siglusintegration.usage_information_line_items_draft";

  public static final String USAGE_INFORMATION_LINE_ITEMS_DRAFT_QUERY =
      "select uilid.* from siglusintegration.usage_information_line_items_draft uilid "
          + "left join siglusintegration.requisitions_draft rd on rd.id = uilid.requisitiondraftid "
          + REQUISITION_DRAFT_LEFT_JOIN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID;

  public static final String AGE_GROUP_LINE_ITEMS =
      "siglusintegration.age_group_line_items";

  public static final String AGE_GROUP_LINE_ITEMS_QUERY =
      "select * from siglusintegration.age_group_line_items "
          + WHERE_ID_IN_REQUISITIONS
          + LEFT_JOIN_SUPERVISORY_NODES
          + WHERE_FACILITY_ID
          + ") ";


  public static Map<String, String> getRequisitionOrderSql() {
    Map<String, String> requisitionOrderSql = new HashMap<>();
    requisitionOrderSql.put(NOTIFICATIONS, NOTIFICATIONS_QUERY);
    requisitionOrderSql.put(NOTIFICATION_MESSAGES, NOTIFICATION_MESSAGES_QUERY);
    requisitionOrderSql.put(REQUISITIONS, REQUISITIONS_QUERY);
    requisitionOrderSql.put(REQUISITIONS_PREVIOUS_REQUISITIONS, REQUISITIONS_PREVIOUS_REQUISITIONS_QUERY);
    requisitionOrderSql.put(REQUISITION_PERMISSION_STRINGS, REQUISITION_PERMISSION_STRINGS_QUERY);
    requisitionOrderSql.put(REQUISITION_LINE_ITEMS, REQUISITION_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(AVAILABLE_PRODUCTS, AVAILABLE_PRODUCTS_QUERY);
    requisitionOrderSql.put(STATUS_CHANGES, STATUS_CHANGES_QUERY);
    requisitionOrderSql.put(STATUS_MESSAGES, STATUS_MESSAGES_QUERY);
    requisitionOrderSql.put(STOCK_ADJUSTMENT_REASONS, STOCK_ADJUSTMENT_REASONS_QUERY);
    requisitionOrderSql.put(ORDERS, ORDERS_QUERY);
    requisitionOrderSql.put(ORDER_LINE_ITEMS, ORDER_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(SHIPMENT_DRAFTS, SHIPMENT_DRAFTS_QUERY);
    requisitionOrderSql.put(SHIPMENT_DRAFT_LINE_ITEMS, SHIPMENT_DRAFT_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(SHIPMENTS, SHIPMENTS_QUERY);
    requisitionOrderSql.put(SHIPMENT_LINE_ITEMS, SHIPMENT_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(PROOFS_OF_DELIVERY, PROOFS_OF_DELIVERY_QUERY);
    requisitionOrderSql.put(PROOF_OF_DELIVERY_LINE_ITEMS, PROOF_OF_DELIVERY_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(POD_LINE_ITEMS_BY_LOCATION, POD_LINE_ITEMS_BY_LOCATION_QUERY);
    requisitionOrderSql.put(POD_SUB_DRAFT_LINE_ITEMS_BY_LOCATION, POD_SUB_DRAFT_LINE_ITEMS_BY_LOCATION_QUERY);
    requisitionOrderSql.put(FULFILLMENT_STATUS_CHANGES, FULFILLMENT_STATUS_CHANGES_QUERY);
    requisitionOrderSql.put(FULFILLMENT_STATUS_MESSAGES, FULFILLMENT_STATUS_MESSAGES_QUERY);
    requisitionOrderSql.put(BASE_LINE_ITEMS, BASE_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(REQUISITIONS_DRAFT, REQUISITIONS_DRAFT_QUERY);
    requisitionOrderSql.put(CONSULTATION_NUMBER_LINE_ITEMS, CONSULTATION_NUMBER_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(CONSULTATION_NUMBER_LINE_ITEM_DRAFTS, CONSULTATION_NUMBER_LINE_ITEM_DRAFTS_QUERY);
    requisitionOrderSql.put(GENERATED_NUMBER, GENERATED_NUMBER_QUERY);
    requisitionOrderSql.put(KIT_USAGE_LINE_ITEMS, KIT_USAGE_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(KIT_USAGE_LINE_ITEMS_DRAFT, KIT_USAGE_LINE_ITEMS_DRAFT_QUERY);
    requisitionOrderSql.put(SIGLUSINTEGRATION_NOTIFICATIONS, SIGLUSINTEGRATION_NOTIFICATIONS_QUERY);
    requisitionOrderSql.put(ORDER_EXTERNAL_IDS, ORDER_EXTERNAL_IDS_QUERY);
    requisitionOrderSql.put(ORDER_LINE_ITEM_EXTENSION, ORDER_LINE_ITEM_EXTENSION_QUERY);
    requisitionOrderSql.put(PATIENT_LINE_ITEMS, PATIENT_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(PATIENT_LINE_ITEM_DRAFTS, PATIENT_LINE_ITEM_DRAFTS_QUERY);
    requisitionOrderSql.put(SHIPMENTS_EXTENSION, SHIPMENTS_EXTENSION_QUERY);
    requisitionOrderSql.put(POD_SUB_DRAFT, POD_SUB_DRAFT_QUERY);
    requisitionOrderSql.put(POD_LINE_ITEMS_EXTENSION, POD_LINE_ITEMS_EXTENSION_QUERY);
    requisitionOrderSql.put(RAPID_TEST_CONSUMPTION_LINE_ITEMS, RAPID_TEST_CONSUMPTION_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(REGIMEN_LINE_ITEMS, REGIMEN_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(REGIMEN_LINE_ITEM_DRAFT, REGIMEN_LINE_ITEM_DRAFT_QUERY);
    requisitionOrderSql.put(REGIMEN_SUMMARY_LINE_ITEMS, REGIMEN_SUMMARY_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(REGIMEN_SUMMARY_LINE_ITEM_DRAFT, REGIMEN_SUMMARY_LINE_ITEM_DRAFT_QUERY);
    requisitionOrderSql.put(REQUISITION_EXTENSION, REQUISITION_EXTENSION_QUERY);
    requisitionOrderSql.put(REQUISITION_LINE_ITEMS_DRAFT, REQUISITION_LINE_ITEMS_DRAFT_QUERY);
    requisitionOrderSql.put(REQUISITION_LINE_ITEMS_EXTENSION, REQUISITION_LINE_ITEMS_EXTENSION_QUERY);
    requisitionOrderSql.put(RECEIPT_PLANS, RECEIPT_PLANS_QUERY);
    requisitionOrderSql.put(RECEIPT_PLAN_LINE_ITEMS, RECEIPT_PLAN_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(SHIPMENT_DRAFT_LINE_ITEMS_EXTENSION, SHIPMENT_DRAFT_LINE_ITEMS_EXTENSION_QUERY);
    requisitionOrderSql.put(SHIPMENT_LINE_ITEMS_EXTENSION, SHIPMENT_LINE_ITEMS_EXTENSION_QUERY);
    requisitionOrderSql.put(TEST_CONSUMPTION_LINE_ITEMS_DRAFT, TEST_CONSUMPTION_LINE_ITEMS_DRAFT_QUERY);
    requisitionOrderSql.put(USAGE_INFORMATION_LINE_ITEMS, USAGE_INFORMATION_LINE_ITEMS_QUERY);
    requisitionOrderSql.put(USAGE_INFORMATION_LINE_ITEMS_DRAFT, USAGE_INFORMATION_LINE_ITEMS_DRAFT_QUERY);
    requisitionOrderSql.put(AGE_GROUP_LINE_ITEMS, AGE_GROUP_LINE_ITEMS_QUERY);

    return requisitionOrderSql;
  }
}
