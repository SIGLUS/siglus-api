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

package org.siglus.siglusapi.web;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusNotificationService.ViewableStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class NotificationControllerTest {

  private static final int PAGE_NO = 0;

  @InjectMocks
  private NotificationController controller;

  @Mock
  private SiglusNotificationService service;

  @Test
  public void shouldCallServiceWithDescSortWhenListGiveLatestOnTopIsTrue() {
    // given
    List<NotificationDto> notificationDtos = new ArrayList<>();
    notificationDtos.add(new NotificationDto());
    when(service.searchNotifications(any())).thenReturn(new PageImpl<>(notificationDtos));
    int pageSize = nextInt();

    // when
    List<NotificationDto> ret = controller.list(pageSize, true);

    // then
    assertEquals(notificationDtos, ret);
    Pageable pageable = verifyPageable();
    assertEquals(PAGE_NO, pageable.getPageNumber());
    assertEquals(pageSize, pageable.getPageSize());
    Order expected = new Order(Direction.DESC, "createDate");
    assertThat(pageable.getSort(), hasItems(expected));
  }

  @Test
  public void shouldCallServiceWithAscSortWhenListGiveLatestOnTopIsFalse() {
    // given
    when(service.searchNotifications(any())).thenReturn(new PageImpl<>(emptyList()));
    int pageSize = nextInt();

    // when
    controller.list(pageSize, false);

    // then
    Pageable pageable = verifyPageable();
    assertEquals(PAGE_NO, pageable.getPageNumber());
    assertEquals(pageSize, pageable.getPageSize());
    Order expected = new Order(Direction.ASC, "createDate");
    assertThat(pageable.getSort(), hasItems(expected));
  }

  @Test
  public void shouldReturn204WhenViewNotificationGivenServiceReturnNotViewed() {
    // given
    UUID id = UUID.randomUUID();
    when(service.viewNotification(id)).thenReturn(ViewableStatus.NOT_VIEWED);

    // when
    ResponseEntity<Void> response = controller.view(id);

    // then
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  public void shouldReturn410WhenViewNotificationGivenServiceReturnViewed() {
    // given
    UUID id = UUID.randomUUID();
    when(service.viewNotification(id)).thenReturn(ViewableStatus.VIEWED);

    // when
    ResponseEntity<Void> response = controller.view(id);

    // then
    assertEquals(HttpStatus.GONE, response.getStatusCode());
  }

  @Test
  public void shouldReturn409WhenViewNotificationGivenServiceReturnProcessed() {
    // given
    UUID id = UUID.randomUUID();
    when(service.viewNotification(id)).thenReturn(ViewableStatus.PROCESSED);

    // when
    ResponseEntity<Void> response = controller.view(id);

    // then
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  private Pageable verifyPageable() {
    ArgumentCaptor<Pageable> arg = ArgumentCaptor.forClass(Pageable.class);
    verify(service).searchNotifications(arg.capture());
    return arg.getValue();
  }

}
