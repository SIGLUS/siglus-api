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

package org.openlmis.requisition.domain;

import static java.util.Objects.isNull;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "available_requisition_column_options", schema = "requisition")
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRequisitionColumnOption extends BaseEntity {

  public static final String DEFAULT = "default";

  @ManyToOne(cascade = {CascadeType.REFRESH})
  @JoinColumn(name = "columnId", nullable = false)
  @Getter
  @Setter
  private AvailableRequisitionColumn requisitionColumn;

  @Column(nullable = false)
  @Getter
  @Setter
  private String optionName;

  @Column(nullable = false)
  @Getter
  @Setter
  private String optionLabel;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AvailableRequisitionColumnOption)) {
      return false;
    }

    AvailableRequisitionColumnOption columnOption = (AvailableRequisitionColumnOption) obj;
    return Objects.equals(columnOption.getOptionName(), getOptionName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getOptionName());
  }

  /**
   * Create a new instance of available requisition column option based on data.
   * from {@link Importer}
   *
   * @param importer instance of {@link Importer}
   * @return new instance of available requisition column option.
   */
  public static AvailableRequisitionColumnOption newInstance(Importer importer) {
    if (isNull(importer)) {
      return null;
    }
    AvailableRequisitionColumnOption option = new AvailableRequisitionColumnOption();
    option.setId(importer.getId());
    option.setOptionName(importer.getOptionName());
    option.setOptionLabel(importer.getOptionLabel());
    return option;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setOptionName(optionName);
    exporter.setOptionLabel(optionLabel);
  }

  public interface Importer {
    UUID getId();

    String getOptionName();

    String getOptionLabel();
  }

  public interface Exporter {
    void setId(UUID id);

    void setOptionName(String optionName);

    void setOptionLabel(String optionLabel);
  }

}
