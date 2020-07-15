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

package org.siglus.siglusapi.dto;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.siglus.common.dto.referencedata.BaseDto;
import org.siglus.common.dto.referencedata.DispensableDto;
import org.siglus.common.dto.referencedata.MetadataDto;
import org.siglus.common.dto.referencedata.OrderableChildDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SiglusOrderableDto extends BaseDto {

  private String productCode;

  private DispensableDto dispensable;

  private String fullProductName;

  private String description;

  private Long netContent;

  private Long packRoundingThreshold;

  private Boolean roundToZero;

  // [SIGLUS change start]
  // [change reason]: need return program's parent id.
  // private Set<ProgramOrderableDto> programs;
  private Set<SiglusProgramOrderableDto> programs;
  // [SIGLUS change end]

  private Set<OrderableChildDto> children;

  private Map<String, String> identifiers;

  private Map<String, Object> extraData;

  private MetadataDto meta = new MetadataDto();

  // [SIGLUS change start]
  // [change reason]: support for archive.
  private Boolean archived;
  // [SIGLUS change end]

  // [SIGLUS change start]
  // [change reason]: need return isKit.
  public boolean getIsKit() {
    return !children.isEmpty();
  }
  // [SIGLUS change end]

}
