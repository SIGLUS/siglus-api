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

package org.openlmis.referencedata.repository.custom.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.data.domain.Pageable;

public class PageableUtilTest {

  @Test
  public void querysMaxAndFirstResultShouldBeSafeWithNullPageable() {
    // when
    Pair maxAndFirst = PageableUtil.querysMaxAndFirstResult(null);

    // then
    assertEquals(0, maxAndFirst.getLeft());
    assertEquals(0, maxAndFirst.getRight());
  }

  @Test
  public void querysMaxAndFirstShouldUsePageable() {
    // given
    Pageable pageable = mock(Pageable.class);
    when(pageable.getPageSize()).thenReturn(10);
    when(pageable.getPageNumber()).thenReturn(2);

    // when
    Pair maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);

    // then
    verify(pageable, times(1)).getPageSize();
    verify(pageable, times(1)).getPageNumber();
    assertEquals(10, maxAndFirst.getLeft());
    assertEquals(20, maxAndFirst.getRight());
  }
}
