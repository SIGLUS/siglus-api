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

package org.siglus.siglusapi.localmachine.agent;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.domain.ErrorPayload;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.repository.ErrorRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

  private final ErrorRecordRepository errorRecordRepository;
  private final SyncRecordService syncRecordService;

  private static final String SIGLUS_PACKAGE_PREFIX = "org.siglus.siglusapi";
  private static final String OPENLMIS_PACKAGE_PREFIX = "org.openlmis";

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void storeErrorRecord(Throwable t, ErrorType errorType) {
    if (!(t instanceof HttpServerErrorException)) {
      syncRecordService.storeLastSyncRecord();
      errorRecordRepository.save(buildSyncDownError(t, errorType));
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void storeErrorRecord(UUID eventId, Throwable t, ErrorType errorType) {
    syncRecordService.storeLastReplayRecord();
    errorRecordRepository.save(buildSyncDownError(eventId, t, errorType));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void storeErrorRecord(List<UUID> eventIds, Throwable t, ErrorType errorType) {
    if (!(t instanceof HttpServerErrorException)) {
      errorRecordRepository.save(buildSyncUpError(eventIds, t, errorType));
    }
  }

  private ErrorRecord buildSyncDownError(UUID eventId, Throwable t, ErrorType errorType) {
    if (t instanceof BusinessDataException) {
      return ErrorRecord.builder()
          .occurredTime(ZonedDateTime.now())
          .type(errorType)
          .errorPayload(buildBusinessErrorPayload(eventId, t))
          .build();
    }
    return ErrorRecord.builder()
        .occurredTime(ZonedDateTime.now())
        .type(errorType)
        .errorPayload(buildGeneralErrorPayload(eventId, t))
        .build();
  }

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

  private ErrorPayload buildBusinessErrorPayload(UUID eventId, Throwable t) {
    BusinessDataException businessDataException = (BusinessDataException) t;
    return ErrorPayload.builder()
        .errorName(businessDataException.getClass().getName())
        .messageKey(businessDataException.asMessage().getKey())
        .eventId(eventId)
        .rootStackTrace(getRootStackTrace(businessDataException))
        .build();
  }

  private ErrorPayload buildBusinessErrorPayload(Throwable t) {
    BusinessDataException businessDataException = (BusinessDataException) t;
    return ErrorPayload.builder()
        .errorName(businessDataException.getClass().getName())
        .messageKey(businessDataException.asMessage().getKey())
        .rootStackTrace(getRootStackTrace(businessDataException))
        .build();
  }

  private ErrorPayload buildGeneralErrorPayload(UUID eventId, Throwable t) {
    return ErrorPayload.builder()
        .errorName(t.getClass().getName())
        .eventId(eventId)
        .detailMessage(t.toString())
        .rootStackTrace(getRootStackTrace(t))
        .build();
  }

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
      if (traceElements[i].getClassName().startsWith(SIGLUS_PACKAGE_PREFIX)
          || traceElements[i].getClassName().startsWith(OPENLMIS_PACKAGE_PREFIX)) {
        return traceElements[i].toString();
      }
    }
    return traceElements[0].toString();
  }

  public List<ErrorRecord> buildSyncUpError(List<UUID> eventIds, Throwable t, ErrorType errorType) {
    if (t instanceof BusinessDataException) {
      return eventIds.stream().map(event -> {
        return ErrorRecord.builder()
            .occurredTime(ZonedDateTime.now())
            .type(errorType)
            .errorPayload(buildBusinessErrorPayload(t))
            .build();
      }).collect(Collectors.toList());
    }
    return eventIds.stream().map(event -> {
      return ErrorRecord.builder()
          .occurredTime(ZonedDateTime.now())
          .type(errorType)
          .errorPayload(buildGeneralErrorPayload(t))
          .build();
    }).collect(Collectors.toList());
  }
}
