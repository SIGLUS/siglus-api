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

package org.openlmis.requisition.domain.requisition;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.dto.ReasonCategory;
import org.openlmis.requisition.dto.ReasonDto;
import org.openlmis.requisition.dto.ReasonType;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "stock_adjustment_reasons", schema = "requisition")
public class StockAdjustmentReason extends BaseEntity {

  @Column(nullable = false)
  @Type(type = UUID_TYPE)
  private UUID reasonId;

  @Column(nullable = false, columnDefinition = TEXT_COLUMN_DEFINITION)
  private String name;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  private String description;

  @Column(nullable = false, columnDefinition = TEXT_COLUMN_DEFINITION)
  @Enumerated(value = EnumType.STRING)
  private ReasonType reasonType;

  @Column(nullable = false, columnDefinition = TEXT_COLUMN_DEFINITION)
  @Enumerated(value = EnumType.STRING)
  private ReasonCategory reasonCategory;

  @Column(nullable = false)
  private Boolean isFreeTextAllowed = false;

  private Boolean hidden;

  /**
   * Copy constructor.
   *
   * @param original an original stock adjustment reason with data that will be placed in a new
   *                 stock adjustment reason.
   */
  public StockAdjustmentReason(StockAdjustmentReason original) {
    this(original.reasonId, original.name, original.description, original.reasonType,
        original.reasonCategory, original.isFreeTextAllowed, original.hidden);
    setId(original.getId());
  }

  public boolean isCreditReasonType() {
    return getReasonType() == ReasonType.CREDIT;
  }

  public boolean isDebitReasonType() {
    return getReasonType() == ReasonType.DEBIT;
  }

  /**
   * Create a new list of Stock Adjustment Reason based on data from {@link Importer}.
   *
   * @param importerList list of {@link Importer}
   * @return new list of Stock Adjustment Reason.
   */
  public static List<StockAdjustmentReason> newInstance(Collection<ReasonDto> importerList) {
    if (isNull(importerList)) {
      return null;
    }
    List<StockAdjustmentReason> list = new ArrayList<>(importerList.size());
    importerList.forEach(importer -> list.add(newInstance(importer)));

    return list;
  }

  /**
   * Create a new instance of Stock Adjustment Reason based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of Stock Adjustment Reason.
   */
  public static StockAdjustmentReason newInstance(Importer importer) {
    if (isNull(importer)) {
      return null;
    }
    StockAdjustmentReason reason = new StockAdjustmentReason();
    reason.setReasonId(importer.getId());
    reason.setName(importer.getName());
    reason.setDescription(importer.getDescription());
    reason.setReasonCategory(importer.getReasonCategory());
    reason.setReasonType(importer.getReasonType());
    reason.setIsFreeTextAllowed(importer.getIsFreeTextAllowed());
    reason.setHidden(importer.getHidden());
    return reason;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(reasonId);
    exporter.setName(name);
    exporter.setDescription(description);
    exporter.setReasonCategory(reasonCategory);
    exporter.setReasonType(reasonType);
    exporter.setIsFreeTextAllowed(isFreeTextAllowed);
    exporter.setHidden(hidden);
  }

  public interface Exporter {

    void setId(UUID id);

    void setName(String name);

    void setDescription(String description);

    void setReasonType(ReasonType reasonType);

    void setReasonCategory(ReasonCategory reasonCategory);

    void setIsFreeTextAllowed(Boolean isFreeTextAllowed);

    void setHidden(Boolean hidden);
  }

  public interface Importer {

    UUID getId();

    String getName();

    String getDescription();

    ReasonType getReasonType();

    ReasonCategory getReasonCategory();

    Boolean getIsFreeTextAllowed();

    Boolean getHidden();
  }
}


