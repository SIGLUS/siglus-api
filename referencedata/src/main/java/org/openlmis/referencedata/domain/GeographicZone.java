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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.referencedata.domain.ExtraDataEntity.ExtraDataExporter;

@Entity
@Table(name = "geographic_zones", schema = "referencedata")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"code"}, callSuper = false)
@TypeName("GeographicZone")
@NamedQueries({
    @NamedQuery(name = "GeographicZone.findIdsByParent",
        query = "SELECT DISTINCT id FROM GeographicZone WHERE parent.id = :parentId")
    })
public class GeographicZone extends BaseEntity implements FhirLocation {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Getter
  @Setter
  private String code;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String name;

  @ManyToOne
  @JoinColumn(name = "levelid", nullable = false)
  @Getter
  @Setter
  private GeographicLevel level;

  @ManyToOne
  @JoinColumn(name = "parentid")
  @Getter
  @Setter
  private GeographicZone parent;

  @Getter
  @Setter
  private Integer catchmentPopulation;

  @Column(columnDefinition = "numeric(8,5)")
  @Getter
  @Setter
  private Double latitude;

  @Column(columnDefinition = "numeric(8,5)")
  @Getter
  @Setter
  private Double longitude;

  @Type(type = "jts_geometry")
  @Getter
  @Setter
  private Polygon boundary;

  @Embedded
  @Setter(AccessLevel.PRIVATE)
  private ExtraDataEntity extraData = new ExtraDataEntity();

  /**
   * Creates new geographic zone object based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of geographic zone.
   */
  public static GeographicZone newGeographicZone(Importer importer) {
    GeographicZone geographicZone = new GeographicZone();
    geographicZone.setId(importer.getId());
    geographicZone.setLevel(GeographicLevel.newGeographicLevel(importer.getLevel()));

    if (null != importer.getParent()) {
      geographicZone.setParent(GeographicZone.newGeographicZone(importer.getParent()));
    }

    geographicZone.updateFrom(importer);

    return geographicZone;
  }

  /**
   * Updates data based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   */
  public void updateFrom(Importer importer) {
    code = importer.getCode();
    name = importer.getName();

    catchmentPopulation = importer.getCatchmentPopulation();
    latitude = importer.getLatitude();
    longitude = importer.getLongitude();

    boundary = importer.getBoundary();

    extraData = ExtraDataEntity.defaultEntity(extraData);
    extraData.updateFrom(importer.getExtraData());
  }

  /**
   * Exports current state of geographic zone object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCode(code);
    exporter.setName(name);
    exporter.setLevel(level);

    if (null != parent) {
      exporter.setParent(parent);
    }

    exporter.setCatchmentPopulation(catchmentPopulation);
    exporter.setLatitude(latitude);
    exporter.setLongitude(longitude);
    exporter.setBoundary(boundary);

    extraData = ExtraDataEntity.defaultEntity(extraData);
    extraData.export(exporter);
  }

  @Override
  public Map<String, Object> getExtraData() {
    return ExtraDataEntity.defaultEntity(extraData).getExtraData();
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    if (null != boundary) {
      out.writeObject(JTSGeometryJavaTypeDescriptor.INSTANCE.toString(boundary));
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    String boundryAsString = (String) in.readObject();
    if (null != boundryAsString) {
      Geometry geometry = JTSGeometryJavaTypeDescriptor.INSTANCE.fromString(boundryAsString);
      boundary = JTSGeometryJavaTypeDescriptor.INSTANCE.unwrap(geometry, Polygon.class, null);
    }
  }

  public interface Exporter extends BaseExporter, ExtraDataExporter {

    void setCode(String code);

    void setName(String name);

    void setLevel(GeographicLevel level);

    void setCatchmentPopulation(Integer catchmentPopulation);

    void setLatitude(Double latitude);

    void setLongitude(Double longitude);
    
    void setBoundary(Polygon boundary);

    void setParent(GeographicZone parent);

  }

  public interface Importer extends FhirLocation {

    String getCode();

    String getName();

    GeographicLevel.Importer getLevel();

    Integer getCatchmentPopulation();

    Double getLatitude();

    Double getLongitude();
    
    Polygon getBoundary();

    Importer getParent();

  }
}
