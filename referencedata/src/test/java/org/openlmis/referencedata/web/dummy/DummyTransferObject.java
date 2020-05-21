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

package org.openlmis.referencedata.web.dummy;

import static org.openlmis.referencedata.web.csv.processor.CsvCellProcessors.FACILITY_TYPE;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.dto.BasicFacilityDto;
import org.openlmis.referencedata.web.csv.model.ImportField;

@Data
@EqualsAndHashCode(callSuper = false)
public class DummyTransferObject extends BaseDto {
  public static final String MANDATORY_STRING_FIELD = "Mandatory String Field";
  public static final String OPTIONAL_FACILITY_FIELD = "Optional Facility Field";
  public static final String OPTIONAL_NESTED_FIELD = "Optional Nested Field";

  @ImportField(mandatory = true, name = MANDATORY_STRING_FIELD)
  private String mandatoryStringField;

  @ImportField(mandatory = true, type = FACILITY_TYPE)
  private int mandatoryIntField;

  @ImportField
  private String optionalStringField;

  @ImportField(type = "Facility", name = OPTIONAL_FACILITY_FIELD)
  private BasicFacilityDto optionalIntField;

  @ImportField(name = OPTIONAL_NESTED_FIELD, nested = "code")
  private DummyNestedField dummyNestedField;

  private String nonAnnotatedField;
}
