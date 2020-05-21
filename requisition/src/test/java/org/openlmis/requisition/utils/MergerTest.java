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

package org.openlmis.requisition.utils;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import org.openlmis.requisition.service.PageDto;
import org.springframework.data.domain.PageImpl;

public class MergerTest {

  @Test
  public void shouldReturnNullIfArgumentIsNull() {
    assertNull(Merger.ofArrays(null).merge());
    assertNull(Merger.ofPages(null).merge());
  }

  @Test
  public void shouldReturnDefaultValueIfArgumentIsNull() {
    assertThat(
        Merger.ofArrays(null).withDefaultValue(() -> new String[]{"a"}).merge(),
        arrayContaining("a"));

    assertThat(
        Merger.ofPages(null).withDefaultValue(PageDto::new).merge(),
        hasProperty("content", hasSize(0)));
  }

  @Test
  public void shouldReturnNullIfArgumentListIsEmpty() {
    assertNull(Merger.ofArrays(newArrayList()).merge());
    assertNull(Merger.ofPages(newArrayList()).merge());
  }

  @Test
  public void shouldReturnDefaultValueIfArgumentListIsEmpty() {
    assertThat(
        Merger.ofArrays(newArrayList()).withDefaultValue(() -> new String[]{"a"}).merge(),
        arrayContaining("a"));

    assertThat(
        Merger.ofPages(newArrayList()).withDefaultValue(PageDto::new).merge(),
        hasProperty("content", hasSize(0)));
  }

  @Test
  public void shouldReturnElementIfArgumentListContainsOnlyOne() {
    List<String[]> arrays = ImmutableList.of(new String[]{"a", "b"});
    List<PageDto<String>> pages = ImmutableList
        .of(new PageDto<>(new PageImpl<>(ImmutableList.of("a"))));

    assertThat(Merger.ofArrays(arrays).merge(), is(arrays.get(0)));
    assertThat(Merger.ofPages(pages).merge(), is(pages.get(0)));
  }

  @Test
  public void shouldMergeArrays() {
    String[] array1 = new String[]{"a", "b"};
    String[] array2 = new String[]{"b", "c"};
    String[] array3 = new String[]{"c", "d"};
    String[] merged = Merger.ofArrays(ImmutableList.of(array1, array2, array3)).merge();

    assertThat(merged, is(notNullValue()));
    assertThat(merged.length, is(4));
    assertThat(merged, arrayContaining("a", "b", "c", "d"));
  }

  @Test
  public void shouldMergePages() {
    PageDto<String> page1 = new PageDto<>(
        new PageImpl<>(ImmutableList.of("a")));
    PageDto<String> page2 = new PageDto<>(
        new PageImpl<>(ImmutableList.of("b", "d")));
    PageDto<String> page3 = new PageDto<>(
        new PageImpl<>(ImmutableList.of("c")));
    PageDto<String> merged = Merger
        .ofPages(ImmutableList.of(page1, page2, page3))
        .merge();

    assertThat(merged, is(notNullValue()));
    assertThat(merged.getContent(), hasSize(4));
    assertThat(merged.getContent(), hasItems("a", "b", "c", "d"));
  }
}
