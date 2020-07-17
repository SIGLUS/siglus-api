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

package org.siglus.common.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.EqualsAndHashCode;
import org.siglus.common.util.referencedata.CustomSortDeserializer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

/**
 * PageImplRepresentation offers a convenient substitute for PageImpl.
 * Because the former lacks a default constructor, it is inconvenient to
 * deserialize. PageImplRepresentation may be used in its stead.
 */
@EqualsAndHashCode
public class PageImplRepresentation<T> extends PageImpl<T> {

  private static final long serialVersionUID = 1L;
  private boolean last;
  private boolean first;

  private int totalPages;
  private long totalElements;
  private int size;
  private int number;
  private int numberOfElements;

  private Sort sort;

  private List<T> content;

  public PageImplRepresentation() {
    this(new PageImpl<>(Lists.newArrayList()));
  }

  /**
   * Creates new instance based on data from {@link Page} instance.
   */
  public PageImplRepresentation(Page<T> page) {
    super(page.getContent());
    this.last = page.isLast();
    this.first = page.isFirst();
    this.totalPages = page.getTotalPages();
    this.totalElements = page.getTotalElements();
    this.size = page.getSize();
    this.number = page.getNumber();
    this.numberOfElements = page.getNumberOfElements();
    this.sort = page.getSort();
    this.content = page.getContent();
  }

  @Override
  public int getNumber() {
    return number;
  }

  @Override
  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  @Override
  public int getTotalPages() {
    return totalPages;
  }

  @Override
  public int getNumberOfElements() {
    return numberOfElements;
  }

  @Override
  public long getTotalElements() {
    return totalElements;
  }

  @Override
  public boolean isFirst() {
    return first;
  }

  public void setFirst(boolean first) {
    this.first = first;
  }

  @Override
  public boolean isLast() {
    return last;
  }

  public void setLast(boolean last) {
    this.last = last;
  }

  @Override
  public List<T> getContent() {
    return content;
  }

  public void setContent(List<T> content) {
    this.content = content;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @JsonDeserialize(using = CustomSortDeserializer.class)
  public void setSort(Sort sort) {
    this.sort = sort;
  }

}
