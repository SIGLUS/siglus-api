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

package org.openlmis.notification.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.openlmis.notification.service.NotificationChannel;

@Getter
@Entity
@Table(name = "postpone_message")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "configuration")
@NamedQueries({
    @NamedQuery(name = "PostponeMessage.getPostponeMessages",
        query = "SELECT p"
            + " FROM PostponeMessage p"
            + " INNER JOIN FETCH p.configuration AS c"
            + " WHERE c.id = :configurationId"
            + "   AND p.userId = :userId"
            + "   AND p.channel = :channel")
})
public class PostponeMessage extends BaseEntity {

  public static final String GET_POSTPONE_MESSAGES_NAMED_QUERY =
      "PostponeMessage.getPostponeMessages";

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "configurationId", nullable = false)
  private DigestConfiguration configuration;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION, nullable = false)
  private String body;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  private String subject;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  @Enumerated(value = EnumType.STRING)
  private NotificationChannel channel;
}
