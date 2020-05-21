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

package org.openlmis.referencedata;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

public enum AvailableFeatures implements Feature {
  @Label("Your secret is safe with me")
  SECRET_MESSAGE,

  @EnabledByDefault
  @Label("Multiple suppliers")
  MULTIPLE_SUPPLIERS,

  @EnabledByDefault
  @Label("Consolidate notifications")
  CONSOLIDATE_NOTIFICATIONS,

  @Label("Redis caching")
  REDIS_CACHING;

  public boolean isActive() {
    return FeatureContext.getFeatureManager().isActive(this);
  }
}
