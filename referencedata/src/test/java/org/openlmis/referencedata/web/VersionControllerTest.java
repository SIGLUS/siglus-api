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

package org.openlmis.referencedata.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openlmis.referencedata.AvailableFeatures;
import org.openlmis.referencedata.util.CrazyVersion;
import org.openlmis.util.Version;
import org.togglz.junit.TogglzRule;

public class VersionControllerTest {

  @Rule
  public TogglzRule togglzRule = TogglzRule.allEnabled(AvailableFeatures.class);

  private VersionController controller = new VersionController();

  @Test
  public void displayShouldReturnCrazyVersion() {
    Version retVersion = controller.display();
    assertTrue("Version returned is not CrazyVersion", retVersion instanceof CrazyVersion);
  }

  @Test
  public void displayShouldReturnOldVersion() {
    togglzRule.disable(AvailableFeatures.SECRET_MESSAGE);
    Version retVersion = controller.display();
    assertFalse("Version returned is not old Version", retVersion instanceof CrazyVersion);
  }
}
