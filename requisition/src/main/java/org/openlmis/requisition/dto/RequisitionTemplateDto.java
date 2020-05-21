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

package org.openlmis.requisition.dto;

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_MAP_IS_NULL;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMN_NOT_IN_TEMPLATE;
import static org.openlmis.requisition.web.ResourceNames.FACILITY_TYPES;
import static org.openlmis.requisition.web.ResourceNames.PROGRAMS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.domain.SourceType;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.utils.Message;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class RequisitionTemplateDto extends BaseRequisitionTemplateDto
    implements RequisitionTemplate.Exporter, RequisitionTemplate.Importer {

  @Setter
  private String serviceUrl;

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private boolean populateStockOnHandFromStockCards;

  @Getter
  @Setter
  private Map<String, RequisitionTemplateColumnDto> columnsMap;

  @Getter
  @Setter
  private ObjectReferenceDto program;

  @Getter
  @Setter
  private Set<ObjectReferenceDto> facilityTypes;

  @Override
  @JsonIgnore
  public UUID getProgramId() {
    return null == program ? null : program.getId();
  }

  @Override
  @JsonIgnore
  public void setProgramId(UUID programId) {
    this.program = new ObjectReferenceDto(programId, serviceUrl, PROGRAMS);
  }

  @Override
  @JsonIgnore
  public Set<UUID> getFacilityTypeIds() {
    return Optional.ofNullable(facilityTypes)
        .orElse(Collections.emptySet())
        .stream()
        .map(ObjectReferenceDto::getId)
        .collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public void setFacilityTypeIds(Set<UUID> facilityTypeIds) {
    this.facilityTypes = Optional
        .ofNullable(facilityTypeIds)
        .orElse(Collections.emptySet())
        .stream()
        .map(elem -> new ObjectReferenceDto(elem, serviceUrl, FACILITY_TYPES))
        .collect(Collectors.toSet());
  }

  @Override
  @JsonIgnore
  public Map<String, ? extends RequisitionTemplateColumn.Importer> getColumns() {
    return new HashMap<>(columnsMap);
  }

  /**
   * Checks if column with given name is displayed.
   *
   * @param name name of requisition column.
   * @return return true if column is displayed
   */
  public boolean isColumnDisplayed(String name) {
    RequisitionTemplateColumnDto column = findColumn(name);

    return column.getIsDisplayed();
  }

  /**
   * Checks if column with given name is calculated.
   *
   * @param name name of requisition column.
   * @return return true if column is calculated
   */
  public boolean isColumnCalculated(String name) {
    RequisitionTemplateColumnDto column = findColumn(name);

    return SourceType.CALCULATED.equals(column.getSource());
  }

  /**
   * Checks if column with given name is input by user.
   *
   * @param name name of requisition column.
   * @return return true if column is calculated
   */
  public boolean isColumnUserInput(String name) {
    RequisitionTemplateColumnDto column = findColumn(name);

    return SourceType.USER_INPUT.equals(column.getSource());
  }

  /**
   * Checks if column with given name is defined in the template.
   *
   * @param columnName name of requisition column.
   * @return return true if column is defined in the template.
   */
  public boolean isColumnInTemplate(String columnName) {
    return getRequisitionTemplateColumn(columnName) != null;
  }


  /**
   * Finds a column by column name or throws exception.
   *
   * @param name name of requisition column.
   * @return {c@link RequisitionTemplateColumn} if found column with the given name.
   */
  public RequisitionTemplateColumnDto findColumn(String name) {
    RequisitionTemplateColumnDto column = getRequisitionTemplateColumn(name);
    if (column == null) {
      throw new ValidationMessageException(new Message(ERROR_COLUMN_NOT_IN_TEMPLATE, name));
    }
    return column;
  }

  private RequisitionTemplateColumnDto getRequisitionTemplateColumn(String name) {
    if (columnsMap == null) {
      throw new ValidationMessageException(new Message(ERROR_COLUMNS_MAP_IS_NULL));
    }
    return columnsMap.get(name);
  }

}
