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

package org.openlmis.referencedata.domain;

import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_CODE_REQUIRED;
import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_NAME_REQUIRED;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.referencedata.exception.ValidationMessageException;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@TypeName("SupplyPartner")
@Table(name = "supply_partners", schema = "referencedata")
@EqualsAndHashCode(of = "code", callSuper = false)
public final class SupplyPartner extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String code;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "supplyPartnerId", nullable = false)
  private List<SupplyPartnerAssociation> associations = Lists.newArrayList();

  public void removeAllAssociations() {
    associations.clear();
  }

  public void addAssociation(SupplyPartnerAssociation association) {
    associations.add(association);
  }

  /**
   * Update data from the importer.
   * @throws ValidationMessageException if code or name is blank.
   */
  public void updateFrom(Importer importer) {
    name = Optional
        .ofNullable(importer.getName())
        .filter(StringUtils::isNotBlank)
        .orElseThrow(() -> new ValidationMessageException(ERROR_NAME_REQUIRED));

    code = Optional
        .ofNullable(importer.getCode())
        .filter(StringUtils::isNotBlank)
        .orElseThrow(() -> new ValidationMessageException(ERROR_CODE_REQUIRED));
  }

  /**
   * Exports current state of this object to exporter.
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setCode(code);
    associations.forEach(exporter::addEntry);
  }

  public interface Importer extends BaseImporter {

    String getName();

    String getCode();

    List<SupplyPartnerAssociation.Importer> getAssociationEntries();

  }

  public interface Exporter extends BaseExporter {

    void setName(String name);

    void setCode(String code);

    void addEntry(SupplyPartnerAssociation association);

  }


}
