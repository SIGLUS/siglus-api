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

package org.openlmis.notification.testutils;

import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.openlmis.notification.domain.DigestConfiguration;

public class DigestConfigurationDataBuilder {

  private UUID id = UUID.randomUUID();
  private String message = RandomStringUtils.randomAlphabetic(100);
  private String tag = RandomStringUtils.randomAlphabetic(10);

  public DigestConfigurationDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public DigestConfigurationDataBuilder withMessage(String message) {
    this.message = message;
    return this;
  }

  public DigestConfigurationDataBuilder withTag(String tag) {
    this.tag = tag;
    return this;
  }

  public DigestConfiguration buildAsNew() {
    return new DigestConfiguration(message, tag);
  }

  /**
   * Creates new instance of {@link DigestConfiguration} with passed values from the builder, the id
   * field is also set.
   */
  public DigestConfiguration build() {
    DigestConfiguration built = buildAsNew();
    built.setId(id);

    return built;
  }
}
