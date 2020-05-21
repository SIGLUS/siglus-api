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

package org.openlmis.referencedata.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.SupplyPartner;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation.Importer;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class SupplyPartnerDto extends BaseDto
    implements SupplyPartner.Importer, SupplyPartner.Exporter {

  @Setter
  @JsonIgnore
  private String serviceUrl;

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String code;

  @Getter
  @Setter
  private List<SupplyPartnerAssociationDto> associations = Lists.newArrayList();

  @Override
  @JsonIgnore
  public List<Importer> getAssociationEntries() {
    return null == associations
        ? Collections.emptyList()
        : Lists.newArrayList(associations);
  }

  /**
   * Creates a new instance based on data from a domain object.
   */
  public static SupplyPartnerDto newInstance(SupplyPartner supplyPartner, String serviceUrl) {
    SupplyPartnerDto dto = new SupplyPartnerDto();
    dto.setServiceUrl(serviceUrl);

    supplyPartner.export(dto);

    return dto;
  }

  @Override
  public void addEntry(SupplyPartnerAssociation association) {
    SupplyPartnerAssociationDto dto = new SupplyPartnerAssociationDto();
    dto.setServiceUrl(serviceUrl);

    association.export(dto);

    associations.add(dto);
  }
}
