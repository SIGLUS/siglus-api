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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "rights", schema = "referencedata")
@NoArgsConstructor
@TypeName("Right")
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Right extends BaseEntity {
  private static final String TEXT = "text";

  @Column(nullable = false, unique = true, columnDefinition = TEXT)
  @Getter
  private String name;

  @Column(nullable = false, columnDefinition = TEXT)
  @Getter
  @Setter
  @Enumerated(value = EnumType.STRING)
  private RightType type;

  @Column(columnDefinition = TEXT)
  @Getter
  @Setter
  private String description;

  @ManyToMany
  @JoinTable(name = "right_attachments",
      joinColumns = @JoinColumn(name = "rightid", nullable = false),
      inverseJoinColumns = @JoinColumn(name = "attachmentid", nullable = false))
  @Getter
  @DiffIgnore
  private Set<Right> attachments = new HashSet<>();

  @ManyToMany(mappedBy = "rights")
  @DiffIgnore
  private Set<Role> roles;

  private Right(String name, RightType type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Static factory method for constructing a new right with a name and type.
   *
   * @param name right name
   * @param type right type
   */
  public static Right newRight(String name, RightType type) {
    return new Right(name, type);
  }

  /**
   * Static factory method for constructing a new right using an importer (DTO).
   *
   * @param importer the right importer (DTO)
   */
  public static Right newRight(Importer importer) {
    Right newRight = new Right(importer.getName(), importer.getType());
    newRight.id = importer.getId();
    newRight.description = importer.getDescription();
    for (Importer attachmentImporter : importer.getAttachments()) {
      Right newAttachment = newRight(attachmentImporter);
      newRight.attach(newAttachment);
    }
    return newRight;
  }

  /**
   * Attach other rights to this one, to create relationships between rights. The attachment is
   * one-way with this method call. The attached rights must be of the same type; only attachments
   * of the same type are attached.
   *
   * @param attachments the rights being attached
   */
  public void attach(Right... attachments) {
    for (Right attachment : attachments) {
      if (attachment.type == type) {
        this.attachments.add(attachment);
      }
    }
  }

  public void clearAttachments() {
    this.attachments.clear();
  }

  /**
   * Copy values of attributes into new or updated Right.
   *
   * @param right Right with new values.
   */
  public void updateFrom(Right right) {
    this.name = right.getName();
    this.type = right.getType();
    this.description = right.getDescription();
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setName(name);
    exporter.setType(type);
    exporter.setDescription(description);
    exporter.setAttachments(attachments);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Right)) {
      return false;
    }
    Right right = (Right) obj;
    return Objects.equals(name, right.name);
  }

  public interface Exporter {
    void setId(UUID id);

    void setName(String name);

    void setType(RightType type);

    void setDescription(String description);

    void setAttachments(Set<Right> attachments);
  }

  public interface Importer {
    UUID getId();

    String getName();

    RightType getType();

    String getDescription();

    Set<Importer> getAttachments();
  }
}