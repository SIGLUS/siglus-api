/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute event and/or modify event under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that event will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.localmachine.agent;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.domain.ErrorPayload;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.repository.ErrorRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandleService {

  private final ErrorRecordRepository errorRecordRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void storeErrorRecord(Throwable t, ErrorType errorType) {
    errorRecordRepository.save(buildSyncDownError(t, errorType));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void storeErrorRecord(Event event, Throwable t, ErrorType errorType) {
    errorRecordRepository.save(buildSyncDownError(event, t, errorType));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void storeErrorRecord(List<Event> events, Throwable t) {
    errorRecordRepository.save(buildSyncUpError(events, t));
  }

  //build sync down error with event
  private ErrorRecord buildSyncDownError(Event event, Throwable t, ErrorType errorType) {
    if (t instanceof BusinessDataException) {
      return ErrorRecord.builder()
          .occurredTime(ZonedDateTime.now())
          .type(errorType)
          .errorPayload(buildBusinessErrorPayload(event, t))
          .build();
    }
    return ErrorRecord.builder()
        .occurredTime(ZonedDateTime.now())
        .type(errorType)
        .errorPayload(buildGeneralErrorPayload(event, t))
        .build();
  }

  //build sync down error without event
  private ErrorRecord buildSyncDownError(Throwable t, ErrorType errorType) {
    if (t instanceof BusinessDataException) {
      return ErrorRecord.builder()
          .occurredTime(ZonedDateTime.now())
          .type(errorType)
          .errorPayload(buildBusinessErrorPayload(t))
          .build();
    }
    return ErrorRecord.builder()
        .occurredTime(ZonedDateTime.now())
        .type(errorType)
        .errorPayload(buildGeneralErrorPayload(t))
        .build();
  }

  //build business error payload with event
  private ErrorPayload buildBusinessErrorPayload(Event event, Throwable t) {
    BusinessDataException businessDataException = (BusinessDataException) t;
    return ErrorPayload.builder()
        .errorName(businessDataException.getClass().getName())
        .messageKey(businessDataException.asMessage().getKey())
        .eventId(event.getId())
        .rootStackTrace(getRootStackTrace(businessDataException))
        .build();
  }

  //build business error payload without event
  private ErrorPayload buildBusinessErrorPayload(Throwable t) {
    BusinessDataException businessDataException = (BusinessDataException) t;
    return ErrorPayload.builder()
        .errorName(businessDataException.getClass().getName())
        .messageKey(businessDataException.asMessage().getKey())
        .rootStackTrace(getRootStackTrace(businessDataException))
        .build();
  }

  //build general error payload with event
  private ErrorPayload buildGeneralErrorPayload(Event event, Throwable t) {
    return ErrorPayload.builder()
        .errorName(t.getClass().getName())
        .eventId(event.getId())
        .detailMessage(t.toString())
        .rootStackTrace(getRootStackTrace(t))
        .build();
  }

  //build general error payload without event
  private ErrorPayload buildGeneralErrorPayload(Throwable t) {

    return ErrorPayload.builder()
        .errorName(t.getClass().getName())
        .detailMessage(t.toString())
        .rootStackTrace(getRootStackTrace(t))
        .build();
  }

  private String getRootStackTrace(Throwable t) {
    if (t.getCause() != null) {
      getRootStackTrace(t.getCause());
    }
    StackTraceElement[] traceElements = t.getStackTrace();
    for (int i = 0; i <= traceElements.length; i++) {
      if (traceElements[i].getClassName().startsWith("org.siglus.siglusapi")) {
        return traceElements[i].toString();
      }
    }
    return traceElements[0].toString();
  }

  public List<ErrorRecord> buildSyncUpError(List<Event> events, Throwable t) {
    if (t instanceof BusinessDataException) {
      return events.stream().map(event -> {
        return ErrorRecord.builder()
            .occurredTime(ZonedDateTime.now())
            .type(ErrorType.SYNC_UP)
            .errorPayload(buildBusinessErrorPayload(t))
            .build();
      }).collect(Collectors.toList());
    }
    return events.stream().map(event -> {
      return ErrorRecord.builder()
          .occurredTime(ZonedDateTime.now())
          .type(ErrorType.SYNC_UP)
          .errorPayload(buildGeneralErrorPayload(t))
          .build();
    }).collect(Collectors.toList());
  }
}
