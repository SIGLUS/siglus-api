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

package org.openlmis.referencedata.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.dto.VersionIdentityDto;
import org.openlmis.referencedata.repository.custom.OrderableRepositoryCustom;
import org.openlmis.referencedata.util.messagekeys.OrderableMessageKeys;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class OrderableSearchParams
    extends VersionableResourceSearchParams
    implements OrderableRepositoryCustom.SearchParams {

  private String code;
  private String name;
  private String programCode;

  /**
   * Default constructor to set all available parameters.
   */
  public OrderableSearchParams(String code, String name, String programCode,
      List<VersionIdentityDto> identities, Integer page, Integer size) {
    super(identities, page, size);
    this.code = code;
    this.name = name;
    this.programCode = programCode;
  }

  @Override
  @JsonIgnore
  String getInvalidVersionIdentityErrorMessage() {
    return OrderableMessageKeys.ERROR_INVALID_VERSION_IDENTITY;
  }
}
