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

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "system_notifications")
public class SystemNotification extends BaseEntity {

  private String title;

  @Column(nullable = false)
  private String message;

  private ZonedDateTime startDate;

  private ZonedDateTime expiryDate;

  @Column(nullable = false)
  private ZonedDateTime createdDate;

  @Column(nullable = false, columnDefinition = "boolean DEFAULT true")
  private boolean active;

  @ManyToOne
  @JoinColumn(name = "authorid", nullable = false)
  private User author;

  /**
   * Creates new system notification object based on data from {@link Importer}
   * and author argument.
   *
   * @param importer instance of {@link Importer}
   * @param author notification author to set.
   * @return new instance of facility.
   */
  public static SystemNotification newInstance(Importer importer, User author) {
    SystemNotification systemNotification = new SystemNotification();
    systemNotification.setId(importer.getId());
    systemNotification.setTitle(importer.getTitle());
    systemNotification.setMessage(importer.getMessage());
    systemNotification.setStartDate(importer.getStartDate());
    systemNotification.setExpiryDate(importer.getExpiryDate());
    systemNotification.setCreatedDate(importer.getCreatedDate());
    systemNotification.setActive(importer.isActive());
    systemNotification.setAuthor(author);
    return systemNotification;
  }

  /**
   * Exports current state of system notification object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setTitle(title);
    exporter.setMessage(message);
    exporter.setStartDate(startDate);
    exporter.setExpiryDate(expiryDate);
    exporter.setCreatedDate(createdDate);
    exporter.setActive(active);
    if (author != null) {
      exporter.setAuthor(author);
    }
  }

  public interface Exporter extends BaseExporter {

    void setTitle(String title);

    void setMessage(String message);

    void setStartDate(ZonedDateTime startDate);

    void setExpiryDate(ZonedDateTime expiryDate);

    void setCreatedDate(ZonedDateTime createdDate);

    void setAuthor(User author);

    void setActive(boolean active);
  }

  public interface Importer extends BaseImporter {

    String getTitle();

    String getMessage();

    ZonedDateTime getStartDate();

    ZonedDateTime getExpiryDate();

    ZonedDateTime getCreatedDate();

    UUID getAuthorId();

    boolean isActive();
  }

  @PrePersist
  private void prePersist() {
    this.createdDate = ZonedDateTime.now();
  }

}
