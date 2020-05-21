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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Set;
import org.junit.Test;

public class RightTest {
  @Test
  public void shouldHaveAttachedRightsOfSameType() {
    //given
    Right right = Right.newRight("supervisionRight1", RightType.SUPERVISION);

    //when
    Right attachment = Right.newRight("supervisionRight2", RightType.SUPERVISION);
    right.attach(attachment);

    right.attach(Right.newRight("fulfillmentRight1", RightType.ORDER_FULFILLMENT));

    //then
    Set<Right> attachedRights = right.getAttachments();
    assertThat(attachedRights.size(), is(1));
    assertThat(attachedRights.iterator().next(), is(attachment));
  }
}
