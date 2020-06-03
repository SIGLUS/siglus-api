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

package org.openlmis.fulfillment.domain;

import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "status_messages", schema = "fulfillment")
@NoArgsConstructor
public class StatusMessage extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "orderId", nullable = false)
  @Getter
  @Setter
  private Order order;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID authorId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  private ExternalStatus status;

  @Column(nullable = false)
  @Getter
  @Setter
  private String body;

  /**
   * Create new instance of StatusMessage based on given {@link Importer}.
   * @param importer instance of {@link Importer}
   * @return instance of StatusMessage.
   */
  public static StatusMessage newInstance(Importer importer) {
    StatusMessage statusMessage = new StatusMessage();
    statusMessage.setId(importer.getId());
    statusMessage.setAuthorId(importer.getAuthorId());
    statusMessage.setStatus(importer.getStatus());
    statusMessage.setBody(importer.getBody());
    return statusMessage;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setAuthorId(authorId);
    exporter.setBody(body);
    exporter.setStatus(status);
  }

  public interface Exporter {
    void setId(UUID id);

    void setAuthorId(UUID authorId);

    void setBody(String body);

    void setStatus(ExternalStatus status);
  }

  public interface Importer {
    UUID getId();

    UUID getAuthorId();

    String getBody();

    ExternalStatus getStatus();
  }

}
