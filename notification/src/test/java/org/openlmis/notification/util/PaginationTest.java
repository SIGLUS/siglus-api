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
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class PaginationTest {

  @Test
  public void shouldReturnDefaultPageNumberIfPageableIsNotGiven() {
    assertThat(Pagination.getPageNumber(null), is(Pagination.DEFAULT_PAGE_NUMBER));
  }

  @Test
  public void shouldReturnPageNumberIfPageableIsGiven() {
    PageRequest page = new PageRequest(1, 10);

    assertThat(Pagination.getPageNumber(page), is(1));
  }

  @Test
  public void shouldReturnNoPaginationIfPageableIsNotGiven() {
    assertThat(Pagination.getPageSize(null), is(Pagination.NO_PAGINATION));
  }

  @Test
  public void shouldReturnPageSizeIfPageableIsGiven() {
    PageRequest page = new PageRequest(1, 10);

    assertThat(Pagination.getPageSize(page), is(10));
  }

  @Test
  public void shouldReturnEmptyPageIfTryingToFetchNonExistentPage() {
    List<Integer> items = IntStream.range(0, 15).boxed().collect(Collectors.toList());
    PageRequest pageRequest = new PageRequest(3, 5);

    Page result = Pagination.getPage(items, pageRequest);

    assertThat(result.getTotalElements(), is(15L));
    assertThat(result.getTotalPages(), is(3));
    assertThat(result.getSize(), is(5));
    assertThat(result.getNumberOfElements(), is(0));
  }

  @Test
  public void shouldReturnPage() {
    List<Integer> items = IntStream.range(0, 15).boxed().collect(Collectors.toList());
    PageRequest pageRequest = new PageRequest(1, 5);

    Page result = Pagination.getPage(items, pageRequest);

    assertThat(result.getNumberOfElements(), is(5));
    assertThat(result.getContent(),
        is(equalTo(IntStream.range(5, 10).boxed().collect(Collectors.toList()))));
  }

  @Test
  public void shouldGetPageFromIterable() {
    Iterable items = Arrays.asList(IntStream.range(0, 15).boxed().toArray());

    PageRequest pageRequest = new PageRequest(1, 5);

    Page result = Pagination.getPage(items, pageRequest);

    assertThat(result.getNumberOfElements(), is(5));
    assertThat(result.getContent(),
        is(equalTo(IntStream.range(5, 10).boxed().collect(Collectors.toList()))));
  }
}
