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

package org.siglus.siglusapi.localmachine;

import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventMulticaster extends SimpleApplicationEventMulticaster {
  private final ShadowFacilityRepository shadowFacilityRepository;

  @Bean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)
  public ApplicationEventMulticaster applicationEventMulticaster() {
    return this;
  }

  public void importEventsFromOfflineChanel() {
    // FIXME: 2022/8/12 import event queues&ack from usb
  }

  public void exportEventsToOfflineChanel() {
    // FIXME: 2022/8/12 export event queues&ack for usb transfer
  }

  public void pullEventFromOnlineWeb() {
    // FIXME: 2022/8/13
  }

  public void sendEventToOnlineWeb() {
    // FIXME: 2022/8/12 send events(sender=shadowFacility,receiver=*) to web and get ack to update
    // watermark of my
    //  own queue
  }

  public void routeEventsToOnlineWeb() {
    // FIXME: 2022/8/12 send routing events(sender!=shadowFacility,receiver=*) to web and get ack
  }

  @EventListener(OutgoingEvent.class)
  public void handleOutgoingEvent(OutgoingEvent outgoingEvent) {
    ShadowFacility currentShadowFacility = shadowFacilityRepository.getCurrentShadowFacility();
    currentShadowFacility.sendEvent(outgoingEvent.getRawEvent());
    shadowFacilityRepository.save(currentShadowFacility);
  }
}
