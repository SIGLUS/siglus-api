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

public class MasterDataSql {

  private MasterDataSql() {
    throw new IllegalStateException("Utility class");
  }

  public static final String AUTH_USERS = "auth.auth_users";

  public static final String AUTH_USERS_QUERY = "select * from auth.auth_users";

  public static final String OAUTH_ACCESS_TOKEN = "auth.oauth_access_token";

  public static final String OAUTH_ACCESS_TOKEN_QUERY = "select * from auth.oauth_access_token";

  public static final String CONFIGURATION_SETTINGS = "fulfillment.configuration_settings";

  public static final String CONFIGURATION_SETTINGS_QUERY = "select * from fulfillment.configuration_settings";

  public static final String ORDER_NUMBER_CONFIGURATIONS = "fulfillment.order_number_configurations";

  public static final String ORDER_NUMBER_CONFIGURATIONS_QUERY =
      "select * from fulfillment.order_number_configurations";

  public static final String FILE_TEMPLATES = "fulfillment.file_templates";

  public static final String FILE_TEMPLATES_QUERY = "select * from fulfillment.file_templates";

  public static final String FILE_COLUMNS = "fulfillment.file_columns";

  public static final String FILE_COLUMNS_QUERY = "select * from fulfillment.file_columns";

  public static final String USER_CONTACT_DETAILS = "notification.user_contact_details";

  public static final String USER_CONTACT_DETAILS_QUERY = "select * from notification.user_contact_details";

  public static final String EMAIL_VERIFICATION_TOKENS = "notification.email_verification_tokens";

  public static final String EMAIL_VERIFICATION_TOKENS_QUERY = "select * from notification.email_verification_tokens";

  public static final String DIGEST_CONFIGURATIONS = "notification.digest_configurations";

  public static final String DIGEST_CONFIGURATIONS_QUERY = "select * from notification.digest_configurations";

  public static final String GEOGRAPHIC_LEVELS = "referencedata.geographic_levels";

  public static final String GEOGRAPHIC_LEVELS_QUERY = "select * from referencedata.geographic_levels";

  public static final String GEOGRAPHIC_ZONES = "referencedata.geographic_zones";

  public static final String GEOGRAPHIC_ZONES_QUERY = "select * from referencedata.geographic_zones";

  public static final String FACILITY_TYPES = "referencedata.facility_types";

  public static final String FACILITY_TYPES_QUERY = "select * from referencedata.facility_types";

  public static final String FACILITIES = "referencedata.facilities";

  public static final String FACILITIES_QUERY = "select * from referencedata.facilities";

  public static final String PROGRAMS = "referencedata.programs";

  public static final String PROGRAMS_QUERY = "select * from referencedata.programs";

  public static final String DISPENSABLES = "referencedata.dispensables";

  public static final String DISPENSABLES_QUERY = "select * from referencedata.dispensables";

  public static final String DISPENSABLE_ATTRIBUTES = "referencedata.dispensable_attributes";

  public static final String DISPENSABLE_ATTRIBUTES_QUERY = "select * from referencedata.dispensable_attributes";

  public static final String ORDERABLE_DISPLAY_CATEGORIES = "referencedata.orderable_display_categories";

  public static final String ORDERABLE_DISPLAY_CATEGORIES_QUERY =
      "select * from referencedata.orderable_display_categories";

  public static final String ORDERABLES = "referencedata.orderables";

  public static final String ORDERABLES_QUERY = "select * from referencedata.orderables";

  public static final String TRADE_ITEMS = "referencedata.trade_items";

  public static final String TRADE_ITEMS_QUERY = "select * from referencedata.trade_items";

  public static final String ORDERABLE_IDENTIFIERS = "referencedata.orderable_identifiers";

  public static final String ORDERABLE_IDENTIFIERS_QUERY = "select * from referencedata.orderable_identifiers";

  public static final String ORDERABLE_CHILDREN = "referencedata.orderable_children";

  public static final String ORDERABLE_CHILDREN_QUERY = "select * from referencedata.orderable_children";

  public static final String LOTS = "referencedata.lots";

  public static final String LOTS_QUERY = "select * from referencedata.lots";

  public static final String FACILITY_TYPE_APPROVED_PRODUCTS = "referencedata.facility_type_approved_products";

  public static final String FACILITY_TYPE_APPROVED_PRODUCTS_QUERY =
      "select * from referencedata.facility_type_approved_products";

  public static final String PROGRAM_ORDERABLES = "referencedata.program_orderables";

  public static final String PROGRAM_ORDERABLES_QUERY = "select * from referencedata.program_orderables";

  public static final String PROCESSING_SCHEDULES = "referencedata.processing_schedules";

  public static final String PROCESSING_SCHEDULES_QUERY = "select * from referencedata.processing_schedules";

  public static final String PROCESSING_PERIODS = "referencedata.processing_periods";

  public static final String PROCESSING_PERIODS_QUERY = "select * from referencedata.processing_periods";

  public static final String SUPPORTED_PROGRAMS = "referencedata.supported_programs";

  public static final String SUPPORTED_PROGRAMS_QUERY = "select * from referencedata.supported_programs";

  public static final String SUPERVISORY_NODES = "referencedata.supervisory_nodes";

  public static final String SUPERVISORY_NODES_QUERY = "select * from referencedata.supervisory_nodes";

  public static final String REQUISITION_GROUPS = "referencedata.requisition_groups";

  public static final String REQUISITION_GROUPS_QUERY = "select * from referencedata.requisition_groups";

  public static final String REQUISITION_GROUP_PROGRAM_SCHEDULES = "referencedata.requisition_group_program_schedules";

  public static final String REQUISITION_GROUP_PROGRAM_SCHEDULES_QUERY =
      "select * from referencedata.requisition_group_program_schedules";

  public static final String REQUISITION_GROUP_MEMBERS = "referencedata.requisition_group_members";

  public static final String REQUISITION_GROUP_MEMBERS_QUERY = "select * from referencedata.requisition_group_members";

  public static final String SUPPLY_LINES = "referencedata.supply_lines";

  public static final String SUPPLY_LINES_QUERY = "select * from referencedata.supply_lines";

  public static final String ROLES = "referencedata.roles";

  public static final String ROLES_QUERY = "select * from referencedata.roles";

  public static final String RIGHTS = "referencedata.rights";

  public static final String RIGHTS_QUERY = "select * from referencedata.rights";

  public static final String ROLE_RIGHTS = "referencedata.role_rights";

  public static final String ROLE_RIGHTS_QUERY = "select * from referencedata.role_rights";

  public static final String USERS = "referencedata.users";

  public static final String USERS_QUERY = "select * from referencedata.users";

  public static final String ROLE_ASSIGNMENTS = "referencedata.role_assignments";

  public static final String ROLE_ASSIGNMENTS_QUERY = "select * from referencedata.role_assignments";

  public static final String RIGHT_ASSIGNMENTS = "referencedata.right_assignments";

  public static final String RIGHT_ASSIGNMENTS_QUERY = "select * from referencedata.right_assignments";

  public static final String SYSTEM_NOTIFICATIONS = "referencedata.system_notifications";

  public static final String SYSTEM_NOTIFICATIONS_QUERY = "select * from referencedata.system_notifications";

  public static final String JASPER_TEMPLATES = "report.jasper_templates";

  public static final String JASPER_TEMPLATES_QUERY = "select * from report.jasper_templates";

  public static final String JASPERTEMPLATE_REQUIREDRIGHTS = "report.jaspertemplate_requiredrights";

  public static final String JASPERTEMPLATE_REQUIREDRIGHTS_QUERY = "select * from report.jaspertemplate_requiredrights";

  public static final String TEMPLATE_PARAMETERS = "report.template_parameters";

  public static final String TEMPLATE_PARAMETERS_QUERY = "select * from report.template_parameters";

  public static final String AVAILABLE_REQUISITION_COLUMNS = "requisition.available_requisition_columns";

  public static final String AVAILABLE_REQUISITION_COLUMNS_QUERY =
      "select * from requisition.available_requisition_columns";

  public static final String AVAILABLE_REQUISITION_COLUMN_OPTIONS = "requisition.available_requisition_column_options";

  public static final String AVAILABLE_REQUISITION_COLUMN_OPTIONS_QUERY =
      "select * from requisition.available_requisition_column_options";

  public static final String AVAILABLE_REQUISITION_COLUMN_SOURCES = "requisition.available_requisition_column_sources";

  public static final String AVAILABLE_REQUISITION_COLUMN_SOURCES_QUERY =
      "select * from requisition.available_requisition_column_sources";

  public static final String REQUISITION_TEMPLATES = "requisition.requisition_templates";

  public static final String REQUISITION_TEMPLATES_QUERY = "select * from requisition.requisition_templates";

  public static final String REQUISITION_TEMPLATE_ASSIGNMENTS = "requisition.requisition_template_assignments";

  public static final String REQUISITION_TEMPLATE_ASSIGNMENTS_QUERY =
      "select * from requisition.requisition_template_assignments";

  public static final String COLUMNS_MAPS = "requisition.columns_maps";

  public static final String COLUMNS_MAPS_QUERY = "select * from requisition.columns_maps";

  public static final String AVAILABLE_USAGE_COLUMN_SECTIONS = "siglusintegration.available_usage_column_sections";

  public static final String AVAILABLE_USAGE_COLUMN_SECTIONS_QUERY =
      "select * from siglusintegration.available_usage_column_sections";

  public static final String AVAILABLE_USAGE_COLUMNS = "siglusintegration.available_usage_columns";

  public static final String AVAILABLE_USAGE_COLUMNS_QUERY = "select * from siglusintegration.available_usage_columns";

  public static final String USAGE_SECTIONS_MAPS = "siglusintegration.usage_sections_maps";

  public static final String USAGE_SECTIONS_MAPS_QUERY = "select * from siglusintegration.usage_sections_maps";

  public static final String USAGE_COLUMNS_MAPS = "siglusintegration.usage_columns_maps";

  public static final String USAGE_COLUMNS_MAPS_QUERY = "select * from siglusintegration.usage_columns_maps";

  public static final String BASIC_PRODUCT_CODES = "siglusintegration.basic_product_codes";

  public static final String BASIC_PRODUCT_CODES_QUERY = "select * from siglusintegration.basic_product_codes";

  public static final String FACILITY_EXTENSION = "siglusintegration.facility_extension";

  public static final String FACILITY_EXTENSION_QUERY = "select * from siglusintegration.facility_extension";

  public static final String FACILITY_LOCATIONS = "siglusintegration.facility_locations";

  public static final String FACILITY_LOCATIONS_QUERY = "select * from siglusintegration.facility_locations";

  public static final String FACILITY_SUPPLIER_LEVEL = "siglusintegration.facility_supplier_level";

  public static final String FACILITY_SUPPLIER_LEVEL_QUERY = "select * from siglusintegration.facility_supplier_level";

  public static final String FACILITY_TYPE_MAPPING = "siglusintegration.facility_type_mapping";

  public static final String FACILITY_TYPE_MAPPING_QUERY = "select * from siglusintegration.facility_type_mapping";

  public static final String METABASE_CONFIG = "siglusintegration.metabase_config";

  public static final String METABASE_CONFIG_QUERY = "select * from siglusintegration.metabase_config";

  public static final String PROGRAM_ADDITIONAL_ORDERABLES = "siglusintegration.program_additional_orderables";

  public static final String PROGRAM_ADDITIONAL_ORDERABLES_QUERY =
      "select * from siglusintegration.program_additional_orderables";

  public static final String PROCESSING_PERIOD_EXTENSION = "siglusintegration.processing_period_extension";

  public static final String PROCESSING_PERIOD_EXTENSION_QUERY =
      "select * from siglusintegration.processing_period_extension";

  public static final String PROGRAM_ORDERABLES_EXTENSION = "siglusintegration.program_orderables_extension";

  public static final String PROGRAM_ORDERABLES_EXTENSION_QUERY =
      "select * from siglusintegration.program_orderables_extension";

  public static final String PROGRAM_REPORT_NAME_MAPPING = "siglusintegration.program_report_name_mapping";

  public static final String PROGRAM_REPORT_NAME_MAPPING_QUERY =
      "select * from siglusintegration.program_report_name_mapping";

  public static final String PROGRAM_REAL_PROGRAM = "siglusintegration.program_real_program";

  public static final String PROGRAM_REAL_PROGRAM_QUERY = "select * from siglusintegration.program_real_program";

  public static final String REGIMEN_CATEGORIES = "siglusintegration.regimen_categories";

  public static final String REGIMEN_CATEGORIES_QUERY = "select * from siglusintegration.regimen_categories";

  public static final String REGIMENS = "siglusintegration.regimens";

  public static final String REGIMENS_QUERY = "select * from siglusintegration.regimens";

  public static final String REPORT_TYPES = "siglusintegration.report_types";

  public static final String REPORT_TYPES_QUERY = "select * from siglusintegration.report_types";

  public static final String REQUISITION_TEMPLATE_EXTENSION = "siglusintegration.requisition_template_extension";

  public static final String REQUISITION_TEMPLATE_EXTENSION_QUERY =
      "select * from siglusintegration.requisition_template_extension";

  public static final String CUSTOM_PRODUCTS_REGIMENS = "siglusintegration.custom_products_regimens";

  public static final String CUSTOM_PRODUCTS_REGIMENS_QUERY =
      "select * from siglusintegration.custom_products_regimens";

  public static final String APP_INFO = "siglusintegration.app_info";

  public static final String APP_INFO_QUERY = "select * from siglusintegration.app_info";

  public static final String EXPIRED_LOTS_BACKUP = "siglusintegration.expired_lots_backup";

  public static final String EXPIRED_LOTS_BACKUP_QUERY = "select * from siglusintegration.expired_lots_backup";

  public static final String ORGANIZATIONS = "stockmanagement.organizations";

  public static final String ORGANIZATIONS_QUERY = "select * from stockmanagement.organizations";

  public static final String NODES = "stockmanagement.nodes";

  public static final String NODES_QUERY = "select * from stockmanagement.nodes";

  public static final String STOCK_CARD_LINE_ITEM_REASONS = "stockmanagement.stock_card_line_item_reasons";

  public static final String STOCK_CARD_LINE_ITEM_REASONS_QUERY =
      "select * from stockmanagement.stock_card_line_item_reasons";

  public static final String STOCK_CARD_LINE_ITEM_REASON_TAGS = "stockmanagement.stock_card_line_item_reason_tags";

  public static final String STOCK_CARD_LINE_ITEM_REASON_TAGS_QUERY =
      "select * from stockmanagement.stock_card_line_item_reason_tags";

  public static final String VALID_DESTINATION_ASSIGNMENTS = "stockmanagement.valid_destination_assignments";

  public static final String VALID_DESTINATION_ASSIGNMENTS_QUERY =
      "select * from stockmanagement.valid_destination_assignments";

  public static final String VALID_REASON_ASSIGNMENTS = "stockmanagement.valid_reason_assignments";

  public static final String VALID_REASON_ASSIGNMENTS_QUERY = "select * from stockmanagement.valid_reason_assignments";

  public static final String VALID_SOURCE_ASSIGNMENTS = "stockmanagement.valid_source_assignments";

  public static final String VALID_SOURCE_ASSIGNMENTS_QUERY = "select * from stockmanagement.valid_source_assignments";

  public static final String AVAILABLE_STOCK_CARD_FIELDS = "stockmanagement.available_stock_card_fields";

  public static final String AVAILABLE_STOCK_CARD_FIELDS_QUERY =
      "select * from stockmanagement.available_stock_card_fields";

  public static final String AVAILABLE_STOCK_CARD_LINE_ITEM_FIELDS =
      "stockmanagement.available_stock_card_line_item_fields";

  public static final String AVAILABLE_STOCK_CARD_LINE_ITEM_FIELDS_QUERY =
      "select * from stockmanagement.available_stock_card_line_item_fields";

  public static final String STOCKMANAGEMENT_JASPER_TEMPLATES = "stockmanagement.jasper_templates";

  public static final String STOCKMANAGEMENT_JASPER_TEMPLATES_QUERY = "select * from stockmanagement.jasper_templates";


  public static Map<String, String> getMasterDataSqlMap() {
    Map<String, String> masterDataSql = new HashMap<>();
    masterDataSql.put(AUTH_USERS, AUTH_USERS_QUERY);
    masterDataSql.put(OAUTH_ACCESS_TOKEN, OAUTH_ACCESS_TOKEN_QUERY);

    masterDataSql.put(CONFIGURATION_SETTINGS, CONFIGURATION_SETTINGS_QUERY);
    masterDataSql.put(ORDER_NUMBER_CONFIGURATIONS, ORDER_NUMBER_CONFIGURATIONS_QUERY);
    masterDataSql.put(FILE_TEMPLATES, FILE_TEMPLATES_QUERY);
    masterDataSql.put(FILE_COLUMNS, FILE_COLUMNS_QUERY);

    masterDataSql.put(USER_CONTACT_DETAILS, USER_CONTACT_DETAILS_QUERY);
    masterDataSql.put(EMAIL_VERIFICATION_TOKENS, EMAIL_VERIFICATION_TOKENS_QUERY);
    masterDataSql.put(DIGEST_CONFIGURATIONS, DIGEST_CONFIGURATIONS_QUERY);

    masterDataSql.put(GEOGRAPHIC_LEVELS, GEOGRAPHIC_LEVELS_QUERY);
    masterDataSql.put(GEOGRAPHIC_ZONES, GEOGRAPHIC_ZONES_QUERY);
    masterDataSql.put(FACILITY_TYPES, FACILITY_TYPES_QUERY);
    masterDataSql.put(FACILITIES, FACILITIES_QUERY);
    masterDataSql.put(PROGRAMS, PROGRAMS_QUERY);
    masterDataSql.put(DISPENSABLES, DISPENSABLES_QUERY);
    masterDataSql.put(DISPENSABLE_ATTRIBUTES, DISPENSABLE_ATTRIBUTES_QUERY);
    masterDataSql.put(ORDERABLE_DISPLAY_CATEGORIES, ORDERABLE_DISPLAY_CATEGORIES_QUERY);
    masterDataSql.put(ORDERABLES, ORDERABLES_QUERY);
    masterDataSql.put(TRADE_ITEMS, TRADE_ITEMS_QUERY);
    masterDataSql.put(ORDERABLE_IDENTIFIERS, ORDERABLE_IDENTIFIERS_QUERY);
    masterDataSql.put(ORDERABLE_CHILDREN, ORDERABLE_CHILDREN_QUERY);
    masterDataSql.put(LOTS, LOTS_QUERY);
    masterDataSql.put(FACILITY_TYPE_APPROVED_PRODUCTS, FACILITY_TYPE_APPROVED_PRODUCTS_QUERY);
    masterDataSql.put(PROGRAM_ORDERABLES, PROGRAM_ORDERABLES_QUERY);
    masterDataSql.put(PROCESSING_SCHEDULES, PROCESSING_SCHEDULES_QUERY);
    masterDataSql.put(PROCESSING_PERIODS, PROCESSING_PERIODS_QUERY);
    masterDataSql.put(SUPPORTED_PROGRAMS, SUPPORTED_PROGRAMS_QUERY);
    masterDataSql.put(SUPERVISORY_NODES, SUPERVISORY_NODES_QUERY);
    masterDataSql.put(REQUISITION_GROUPS, REQUISITION_GROUPS_QUERY);
    masterDataSql.put(REQUISITION_GROUP_PROGRAM_SCHEDULES, REQUISITION_GROUP_PROGRAM_SCHEDULES_QUERY);
    masterDataSql.put(REQUISITION_GROUP_MEMBERS, REQUISITION_GROUP_MEMBERS_QUERY);
    masterDataSql.put(SUPPLY_LINES, SUPPLY_LINES_QUERY);
    masterDataSql.put(ROLES, ROLES_QUERY);
    masterDataSql.put(RIGHTS, RIGHTS_QUERY);
    masterDataSql.put(ROLE_RIGHTS, ROLE_RIGHTS_QUERY);
    masterDataSql.put(USERS, USERS_QUERY);
    masterDataSql.put(ROLE_ASSIGNMENTS, ROLE_ASSIGNMENTS_QUERY);
    masterDataSql.put(RIGHT_ASSIGNMENTS, RIGHT_ASSIGNMENTS_QUERY);
    masterDataSql.put(SYSTEM_NOTIFICATIONS, SYSTEM_NOTIFICATIONS_QUERY);

    masterDataSql.put(JASPER_TEMPLATES, JASPER_TEMPLATES_QUERY);
    masterDataSql.put(JASPERTEMPLATE_REQUIREDRIGHTS, JASPERTEMPLATE_REQUIREDRIGHTS_QUERY);
    masterDataSql.put(TEMPLATE_PARAMETERS, TEMPLATE_PARAMETERS_QUERY);

    masterDataSql.put(AVAILABLE_REQUISITION_COLUMNS, AVAILABLE_REQUISITION_COLUMNS_QUERY);
    masterDataSql.put(AVAILABLE_REQUISITION_COLUMN_OPTIONS, AVAILABLE_REQUISITION_COLUMN_OPTIONS_QUERY);
    masterDataSql.put(AVAILABLE_REQUISITION_COLUMN_SOURCES, AVAILABLE_REQUISITION_COLUMN_SOURCES_QUERY);
    masterDataSql.put(REQUISITION_TEMPLATES, REQUISITION_TEMPLATES_QUERY);
    masterDataSql.put(REQUISITION_TEMPLATE_ASSIGNMENTS, REQUISITION_TEMPLATE_ASSIGNMENTS_QUERY);
    masterDataSql.put(COLUMNS_MAPS, COLUMNS_MAPS_QUERY);

    masterDataSql.put(AVAILABLE_USAGE_COLUMN_SECTIONS, AVAILABLE_USAGE_COLUMN_SECTIONS_QUERY);
    masterDataSql.put(AVAILABLE_USAGE_COLUMNS, AVAILABLE_USAGE_COLUMNS_QUERY);
    masterDataSql.put(USAGE_SECTIONS_MAPS, USAGE_SECTIONS_MAPS_QUERY);
    masterDataSql.put(USAGE_COLUMNS_MAPS, USAGE_COLUMNS_MAPS_QUERY);
    masterDataSql.put(BASIC_PRODUCT_CODES, BASIC_PRODUCT_CODES_QUERY);
    masterDataSql.put(FACILITY_EXTENSION, FACILITY_EXTENSION_QUERY);
    masterDataSql.put(FACILITY_LOCATIONS, FACILITY_LOCATIONS_QUERY);
    masterDataSql.put(FACILITY_SUPPLIER_LEVEL, FACILITY_SUPPLIER_LEVEL_QUERY);
    masterDataSql.put(FACILITY_TYPE_MAPPING, FACILITY_TYPE_MAPPING_QUERY);
    masterDataSql.put(METABASE_CONFIG, METABASE_CONFIG_QUERY);
    masterDataSql.put(PROGRAM_ADDITIONAL_ORDERABLES, PROGRAM_ADDITIONAL_ORDERABLES_QUERY);
    masterDataSql.put(PROCESSING_PERIOD_EXTENSION, PROCESSING_PERIOD_EXTENSION_QUERY);
    masterDataSql.put(PROGRAM_ORDERABLES_EXTENSION, PROGRAM_ORDERABLES_EXTENSION_QUERY);
    masterDataSql.put(PROGRAM_REPORT_NAME_MAPPING, PROGRAM_REPORT_NAME_MAPPING_QUERY);
    masterDataSql.put(PROGRAM_REAL_PROGRAM, PROGRAM_REAL_PROGRAM_QUERY);
    masterDataSql.put(REGIMEN_CATEGORIES, REGIMEN_CATEGORIES_QUERY);
    masterDataSql.put(REGIMENS, REGIMENS_QUERY);
    masterDataSql.put(REPORT_TYPES, REPORT_TYPES_QUERY);
    masterDataSql.put(REQUISITION_TEMPLATE_EXTENSION, REQUISITION_TEMPLATE_EXTENSION_QUERY);
    masterDataSql.put(CUSTOM_PRODUCTS_REGIMENS, CUSTOM_PRODUCTS_REGIMENS_QUERY);
    masterDataSql.put(APP_INFO, APP_INFO_QUERY);
    masterDataSql.put(EXPIRED_LOTS_BACKUP, EXPIRED_LOTS_BACKUP_QUERY);

    masterDataSql.put(ORGANIZATIONS, ORGANIZATIONS_QUERY);
    masterDataSql.put(NODES, NODES_QUERY);
    masterDataSql.put(STOCK_CARD_LINE_ITEM_REASONS, STOCK_CARD_LINE_ITEM_REASONS_QUERY);
    masterDataSql.put(STOCK_CARD_LINE_ITEM_REASON_TAGS, STOCK_CARD_LINE_ITEM_REASON_TAGS_QUERY);
    masterDataSql.put(VALID_DESTINATION_ASSIGNMENTS, VALID_DESTINATION_ASSIGNMENTS_QUERY);
    masterDataSql.put(VALID_REASON_ASSIGNMENTS, VALID_REASON_ASSIGNMENTS_QUERY);
    masterDataSql.put(VALID_SOURCE_ASSIGNMENTS, VALID_SOURCE_ASSIGNMENTS_QUERY);
    masterDataSql.put(AVAILABLE_STOCK_CARD_FIELDS, AVAILABLE_STOCK_CARD_FIELDS_QUERY);
    masterDataSql.put(AVAILABLE_STOCK_CARD_LINE_ITEM_FIELDS, AVAILABLE_STOCK_CARD_LINE_ITEM_FIELDS_QUERY);
    masterDataSql.put(STOCKMANAGEMENT_JASPER_TEMPLATES, STOCKMANAGEMENT_JASPER_TEMPLATES_QUERY);

    return masterDataSql;
  }

}