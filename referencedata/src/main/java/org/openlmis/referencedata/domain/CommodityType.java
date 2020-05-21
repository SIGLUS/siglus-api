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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.referencedata.dto.CommodityTypeDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.CommodityTypeMessageKeys;

/**
 * CommodityTypes are generic commodities to simplify ordering and use.  A CommodityType doesn't
 * have a single manufacturer, nor a specific packaging.  Instead a CommodityType represents a
 * refined categorization of products that may typically be ordered / exchanged for one another.
 */
@Entity
@Table(name = "commodity_types", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name", callSuper = false)
@TypeName("CommodityType")
public final class CommodityType extends BaseEntity {

  @Column(nullable = false)
  @Getter
  private String name;

  @Getter
  @Setter
  @Column(nullable = false)
  private String classificationSystem;

  @Getter
  @Setter
  @Column(nullable = false)
  private String classificationId;

  @Getter
  @ManyToOne
  @JoinColumn(columnDefinition = "parentid")
  private CommodityType parent;

  @Getter
  @Setter
  @OneToMany(mappedBy = "parent")
  @DiffIgnore
  private List<CommodityType> children;

  /**
   * Validates and assigns a parent to this commodity type.
   * No cycles in the hierarchy are allowed.
   *
   * @param parent the parent to assign
   */
  public void assignParent(CommodityType parent) {
    validateIsNotDescendant(parent);

    this.parent = parent;
    parent.children.add(this);
  }

  private void validateIsNotDescendant(CommodityType commodityType) {
    for (CommodityType child : children) {
      if (child.equals(commodityType)) {
        throw new ValidationMessageException(new Message(
            CommodityTypeMessageKeys.ERROR_PARENT_IS_DESCENDANT,
            commodityType.getId(), id));
      }
      child.validateIsNotDescendant(commodityType);
    }
  }

  /**
   * Creates new instance based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of TradeItem.
   */
  public static CommodityType newInstance(Importer importer) {
    if (importer == null) {
      return null;
    }
    CommodityType commodityType = new CommodityType();
    commodityType.id = importer.getId();
    commodityType.name = importer.getName();
    commodityType.classificationSystem = importer.getClassificationSystem();
    commodityType.classificationId = importer.getClassificationId();
    commodityType.parent = CommodityType.newInstance(importer.getParent());
    commodityType.children = new ArrayList<>();

    return commodityType;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setClassificationSystem(classificationSystem);
    exporter.setClassificationId(classificationId);
    exporter.setParent(CommodityTypeDto.newInstance(parent));
  }

  public interface Importer {
    UUID getId();

    String getName();

    String getClassificationSystem();

    String getClassificationId();

    CommodityTypeDto getParent();
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setClassificationSystem(String classificationSystem);

    void setClassificationId(String classificationId);

    void setParent(CommodityTypeDto parent);
  }
}
