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

package org.openlmis.referencedata.testbuilder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.openlmis.referencedata.dto.VersionIdentityDto;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.web.FacilityTypeApprovedProductSearchParams;

public class FacilityTypeApprovedProductSearchParamsDataBuilder {

  private Set<String> facilityTypeCodes = Sets.newHashSet();
  private String programCode;
  private Boolean active;
  private List<VersionIdentityDto> identities = Lists.newArrayList();
  private Set<UUID> orderableIds = Sets.newHashSet();
  private int page = Pagination.DEFAULT_PAGE_NUMBER;
  private int size = Pagination.NO_PAGINATION;

  public FacilityTypeApprovedProductSearchParamsDataBuilder withFacilityTypeCode(
      String facilityTypeCode) {
    this.facilityTypeCodes.add(facilityTypeCode);
    return this;
  }

  public FacilityTypeApprovedProductSearchParamsDataBuilder withOrderableIds(
      Set<UUID> orderableIds) {
    this.orderableIds.addAll(orderableIds);
    return this;
  }

  public FacilityTypeApprovedProductSearchParamsDataBuilder withProgramCode(String programCode) {
    this.programCode = programCode;
    return this;
  }

  public FacilityTypeApprovedProductSearchParamsDataBuilder withActive(Boolean active) {
    this.active = active;
    return this;
  }

  public FacilityTypeApprovedProductSearchParamsDataBuilder withIdentity(UUID id, Long versionId) {
    this.identities.add(new VersionIdentityDto(id, versionId));
    return this;
  }

  public FacilityTypeApprovedProductSearchParamsDataBuilder withPage(int page) {
    this.page = page;
    return this;
  }

  public FacilityTypeApprovedProductSearchParamsDataBuilder withSize(int size) {
    this.size = size;
    return this;
  }

  public FacilityTypeApprovedProductSearchParams build() {
    return new FacilityTypeApprovedProductSearchParams(
        facilityTypeCodes, programCode, active, identities, orderableIds, page, size);
  }
}
