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

package org.siglus.siglusapi.localmachine.event.masterdata;

import io.debezium.data.Envelope.Operation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jersey.repackaged.com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.UserRepository;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.cdc.CdcListener;
import org.siglus.siglusapi.localmachine.cdc.CdcRecord;
import org.siglus.siglusapi.localmachine.cdc.CdcRecordMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile({"!localmachine"})
public class MasterDataEventEmitter implements CdcListener {

  private final EventPublisher eventPublisher;
  private final CdcRecordMapper cdcRecordMapper;
  private final UserRepository userRepository;
  private static final String USER_ID = "userid";
  private static final String REFERENCE_DATA = "referencedata";
  private static final String RIGHT_ASSIGNMENTS = "right_assignments";
  public static final Set<String> doNotClearCacheTableName = Sets.newHashSet("auth.auth_users",
      "notification.user_contact_details",
      "referencedata.roles",
      "referencedata.rights",
      "referencedata.role_rights",
      "referencedata.users",
      "referencedata.role_assignments",
      "referencedata.right_assignments"
  );

  @Override
  public String[] acceptedTables() {
    return new String[]{
        "auth.auth_users",
        "fulfillment.configuration_settings",
        "fulfillment.order_number_configurations",
        "fulfillment.file_templates",
        "fulfillment.file_columns",
        "notification.user_contact_details",
        "notification.email_verification_tokens",
        "notification.digest_configurations",
        "referencedata.geographic_levels",
        "referencedata.geographic_zones",
        "referencedata.facility_types",
        "referencedata.facilities",
        "referencedata.programs",
        "referencedata.dispensables",
        "referencedata.dispensable_attributes",
        "referencedata.orderable_display_categories",
        "referencedata.orderables",
        "referencedata.trade_items",
        "referencedata.orderable_identifiers",
        "referencedata.orderable_children",
        "referencedata.facility_type_approved_products",
        "referencedata.program_orderables",
        "referencedata.processing_schedules",
        "referencedata.processing_periods",
        "referencedata.supported_programs",
        "referencedata.supervisory_nodes",
        "referencedata.requisition_groups",
        "referencedata.requisition_group_program_schedules",
        "referencedata.requisition_group_members",
        "referencedata.supply_lines",
        "referencedata.roles",
        "referencedata.rights",
        "referencedata.role_rights",
        "referencedata.users",
        "referencedata.role_assignments",
        "referencedata.right_assignments",
        "referencedata.system_notifications",
        "report.jasper_templates",
        "report.jaspertemplate_requiredrights",
        "report.template_parameters",
        "requisition.available_requisition_columns",
        "requisition.available_requisition_column_options",
        "requisition.available_requisition_column_sources",
        "requisition.requisition_templates",
        "requisition.requisition_template_assignments",
        "requisition.columns_maps",
        "siglusintegration.available_usage_column_sections",
        "siglusintegration.available_usage_columns",
        "siglusintegration.usage_sections_maps",
        "siglusintegration.usage_columns_maps",
        "siglusintegration.basic_product_codes",
        "siglusintegration.facility_extension",
        "siglusintegration.facility_locations",
        "siglusintegration.facility_supplier_level",
        "siglusintegration.facility_type_mapping",
        "siglusintegration.program_additional_orderables",
        "siglusintegration.processing_period_extension",
        "siglusintegration.program_orderables_extension",
        "siglusintegration.program_report_name_mapping",
        "siglusintegration.program_real_program",
        "siglusintegration.regimen_categories",
        "siglusintegration.regimens",
        "siglusintegration.report_types",
        "siglusintegration.requisition_template_extension",
        "stockmanagement.organizations",
        "stockmanagement.nodes",
        "stockmanagement.stock_card_line_item_reasons",
        "stockmanagement.stock_card_line_item_reason_tags",
        "stockmanagement.valid_destination_assignments",
        "stockmanagement.valid_reason_assignments",
        "stockmanagement.valid_source_assignments",
        "stockmanagement.available_stock_card_fields",
        "stockmanagement.available_stock_card_line_item_fields",
        "stockmanagement.jasper_templates"
    };
  }

  @Transactional
  @Override
  public void on(List<CdcRecord> records) {
    emitNeedNotMarkFacilityEvent(records);
    emitNeedMarkFacilityEvent(records);
  }

  private void emitNeedNotMarkFacilityEvent(List<CdcRecord> records) {
    List<CdcRecord> notNeedMarkFacilityRecords = records.stream()
        .filter(cdcRecord -> !isNeedMarkFacilityTable(cdcRecord))
        .collect(Collectors.toList());
    eventPublisher.emitMasterDataEvent(
        new MasterDataTableChangeEvent(cdcRecordMapper.buildEvents(notNeedMarkFacilityRecords)), null);
  }

  private void emitNeedMarkFacilityEvent(List<CdcRecord> records) {
    Map<UUID, List<CdcRecord>> userIdToRecords = filterNeedMarkFacilityRecords(records);
    if (!userIdToRecords.isEmpty()) {
      Map<UUID, UUID> userIdToFacilityId = userRepository.findAll().stream()
          .filter(user -> user.getHomeFacilityId() != null)
          .collect(Collectors.toMap(User::getId, User::getHomeFacilityId));
      userIdToRecords.forEach((userId, cdcRecord) ->
          eventPublisher.emitMasterDataEvent(
              new MasterDataTableChangeEvent(cdcRecordMapper.buildAlreadyGroupedEvents(cdcRecord)),
              userIdToFacilityId.get(userId)));
    }
  }

  private Map<UUID, List<CdcRecord>> filterNeedMarkFacilityRecords(List<CdcRecord> records) {
    return records.stream()
        .filter(this::isNeedMarkFacilityTable)
        .collect(Collectors.groupingBy(cdcRecord -> UUID.fromString(cdcRecord.getPayload().get(USER_ID).toString())));
  }

  private boolean isNeedMarkFacilityTable(CdcRecord cdcRecord) {
    return !Operation.DELETE.code().equals(cdcRecord.getOperationCode()) && REFERENCE_DATA.equals(cdcRecord.getSchema())
        && RIGHT_ASSIGNMENTS.equals(cdcRecord.getTable());
  }
}
