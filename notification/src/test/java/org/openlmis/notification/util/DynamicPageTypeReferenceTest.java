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

package org.openlmis.notification.util;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.junit.Test;
import org.openlmis.notification.service.PageDto;

public class DynamicPageTypeReferenceTest {

  @Test
  public void shouldGetType() {
    Type result = new DynamicPageTypeReference<>(String.class).getType();

    assertThat(result instanceof ParameterizedType, is(true));

    ParameterizedType parameterizedType = (ParameterizedType) result;

    Type[] types = {String.class};
    assertThat(parameterizedType.getActualTypeArguments(), is(equalTo(types)));
    assertEquals(parameterizedType.getRawType(), PageDto.class);
    assertNull(parameterizedType.getOwnerType());
  }

}
