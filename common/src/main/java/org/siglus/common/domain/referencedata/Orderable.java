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

package org.siglus.common.domain.referencedata;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.siglus.common.domain.BaseEntity.BaseExporter;
import org.siglus.common.domain.BaseEntity.BaseImporter;
import org.siglus.common.domain.referencedata.ExtraDataEntity.ExtraDataExporter;
import org.siglus.common.domain.referencedata.ExtraDataEntity.ExtraDataImporter;
import org.siglus.common.domain.referencedata.VersionIdentity.VersionExporter;
import org.siglus.common.domain.referencedata.VersionIdentity.VersionImporter;
import org.siglus.common.dto.referencedata.OrderableChildDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;

/**
 * Products that are Orderable by Program.  An Orderable represent any medical commodities that may
 * be ordered/requisitioned, typically by a {@link Program}.
 */
@Entity
@Table(name = "orderables", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(name = "unq_productcode_versionid",
        columnNames = {"code", "versionnumber"}))
@NoArgsConstructor
@Cacheable
@Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(attributeNodes = {
    @NamedAttributeNode("programOrderables"),
    @NamedAttributeNode("children"),
    @NamedAttributeNode("identifiers")
})
public class Orderable implements Versionable {

  private static final int FETCH_SIZE = 1000;

  public static final String TRADE_ITEM = "tradeItem";
  public static final String COMMODITY_TYPE = "commodityType";
  public static final String VALUE = "value";

  @Embedded
  @Getter
  private Code productCode;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "dispensableid", nullable = false)
  @Getter
  @Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
  private Dispensable dispensable;

  @Getter
  @Setter
  private String fullProductName;

  @Getter
  @Setter
  private String description;

  @Getter(AccessLevel.PACKAGE)
  private long netContent;

  @Getter(AccessLevel.PACKAGE)
  private long packRoundingThreshold;

  @Getter(AccessLevel.PACKAGE)
  private boolean roundToZero;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = FETCH_SIZE)
  @Getter
  @Setter
  @Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
  private List<ProgramOrderable> programOrderables;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = FETCH_SIZE)
  @Setter
  @Getter
  @Cache(usage =  CacheConcurrencyStrategy.READ_WRITE)
  private Set<OrderableChild> children;

  @ElementCollection
  @MapKeyColumn(name = "key")
  @BatchSize(size = FETCH_SIZE)
  @Column(name = VALUE)
  @CollectionTable(name = "orderable_identifiers", schema = "referencedata",
      joinColumns = {
          @JoinColumn(name = "orderableId", referencedColumnName = "id"),
          @JoinColumn(name = "orderableVersionNumber", referencedColumnName = "versionNumber")})
  @Setter
  private Map<String, String> identifiers;

  @Embedded
  private ExtraDataEntity extraData = new ExtraDataEntity();

  @EmbeddedId
  private VersionIdentity identity;

  @Getter
  @Setter
  private ZonedDateTime lastUpdated;

  /**
   * Default constructor.
   *
   * @param productCode product code
   * @param dispensable dispensable
   * @param netContent net content
   * @param packRoundingThreshold pack rounding threshold
   * @param roundToZero round to zero
   * @param id id
   * @param versionNumber version number
   */
  public Orderable(Code productCode, Dispensable dispensable, long netContent,
      long packRoundingThreshold, boolean roundToZero, UUID id, Long versionNumber) {
    this.productCode = productCode;
    this.dispensable = dispensable;
    this.netContent = netContent;
    this.packRoundingThreshold = packRoundingThreshold;
    this.roundToZero = roundToZero;
    this.identity = new VersionIdentity(id, versionNumber);
    this.lastUpdated = ZonedDateTime.now();
  }

  @PrePersist
  @PreUpdate
  public void updateLastUpdatedDate() {
    lastUpdated = ZonedDateTime.now();
  }

  @Override
  public UUID getId() {
    return identity.getId();
  }

  @Override
  public Long getVersionNumber() {
    return identity.getVersionNumber();
  }

  public String getTradeItemIdentifier() {
    return identifiers.get(TRADE_ITEM);
  }

  /**
   * Determines equality based on product codes.
   *
   * @param object another Orderable, ideally.
   * @return true if the two are semantically equal.  False otherwise.
   */
  @Override
  public final boolean equals(Object object) {
    return object instanceof Orderable
        && Objects.equals(productCode, ((Orderable) object).productCode);
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(productCode);
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(identity.getId());
    exporter.setProductCode(productCode.toString());
    exporter.setDispensable(dispensable);
    exporter.setFullProductName(fullProductName);
    exporter.setDescription(description);
    exporter.setNetContent(netContent);
    exporter.setPackRoundingThreshold(packRoundingThreshold);
    exporter.setRoundToZero(roundToZero);
    exporter.setPrograms(ProgramOrderableDto.newInstance(programOrderables));
    exporter.setChildren(OrderableChildDto.newInstance(children));
    exporter.setIdentifiers(identifiers);

    extraData = ExtraDataEntity.defaultEntity(extraData);
    extraData.export(exporter);

    exporter.setVersionNumber(identity.getVersionNumber());
    exporter.setLastUpdated(lastUpdated);
  }

  public interface Exporter extends BaseExporter, ExtraDataExporter, VersionExporter {

    void setProductCode(String productCode);

    void setDispensable(Dispensable dispensable);

    void setFullProductName(String fullProductName);

    void setDescription(String description);

    void setNetContent(Long netContent);

    void setPackRoundingThreshold(Long packRoundingThreshold);

    void setRoundToZero(Boolean roundToZero);

    void setPrograms(Set<ProgramOrderableDto> programOrderables);

    void setChildren(Set<OrderableChildDto> children);

    void setIdentifiers(Map<String, String> identifiers);

    void setLastUpdated(ZonedDateTime lastUpdated);
  }

  public interface Importer extends BaseImporter, ExtraDataImporter, VersionImporter {

    String getProductCode();

    Dispensable.Importer getDispensable();

    String getFullProductName();

    String getDescription();

    Long getNetContent();

    Long getPackRoundingThreshold();

    Boolean getRoundToZero();

    Set<ProgramOrderableDto> getPrograms();

    Set<OrderableChildDto> getChildren();

    Map<String, String> getIdentifiers();
  }
}
