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

package org.siglus.siglusapi.validator;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_DRAFT_DESTINATION_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_DRAFT_DOCUMENT_NUMBER_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_DRAFT_SOURCE_ID_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_DRAFT_TYPE_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_EVENT_FACILITY_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_EVENT_INITIAL_DRAFT_ID_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_IS_DRAFT_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_EXPECTED_DRAFT_TYPE_ERROR;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_EXPECTED_USER_DRAFT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PROGRAM_MISSING;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_ID_MISSING;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementInitialDraft;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementInitialDraftsRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("ActiveDraftValidator")
public class ActiveDraftValidator {
  private static final List<String> draftTypes =
      Lists.newArrayList("adjustment", "issue", "receive");

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private StockManagementInitialDraftsRepository stockManagementInitialDraftsRepository;

  public void validateFacilityId(UUID facilityId) {
    if (facilityId == null || new UUID(0L, 0L).equals(facilityId)) {
      throw new ValidationMessageException(ERROR_EVENT_FACILITY_INVALID);
    }
  }

  public void validateProgramId(UUID programId) {
    if (programId == null) {
      throw new ValidationMessageException(ERROR_PROGRAM_MISSING);
    }
  }

  public void validateIsDraft(Boolean isDraft) {
    if (isDraft == null) {
      throw new ValidationMessageException(ERROR_IS_DRAFT_MISSING);
    }
  }

  public void validateDraftType(String draftType) {
    if (draftType.trim().isEmpty()) {
      throw new ValidationMessageException(ERROR_DRAFT_TYPE_MISSING);
    }
    if (!draftTypes.contains(draftType)) {
      throw new ValidationMessageException(ERROR_NOT_EXPECTED_DRAFT_TYPE_ERROR);
    }
  }

  public void validateDraftUser(StockManagementDraft drafts) {
    UserDto user = authenticationHelper.getCurrentUser();
    if (!user.getId().equals(drafts.getUserId())) {
      throw new ValidationMessageException(ERROR_NOT_EXPECTED_USER_DRAFT);
    }
  }

  public void validateUserId(UUID userId) {
    UserDto user = authenticationHelper.getCurrentUser();
    if (userId == null || new UUID(0L, 0L).equals(userId)) {
      throw new ValidationMessageException(ERROR_USER_ID_MISSING);
    }
    if (!user.getId().equals(userId)) {
      throw new ValidationMessageException(ERROR_NOT_EXPECTED_USER_DRAFT);
    }
  }

  public void validateDestinationId(UUID destinationId) {
    if (destinationId == null) {
      throw new ValidationMessageException(ERROR_DRAFT_DESTINATION_ID_MISSING);
    }
  }

  public void validateDocumentNumber(String documentNumber) {
    if (documentNumber == null) {
      throw new ValidationMessageException(ERROR_DRAFT_DOCUMENT_NUMBER_MISSING);
    }
  }

  public void validateSourceId(UUID sourceId) {
    if (sourceId == null) {
      throw new ValidationMessageException(ERROR_DRAFT_SOURCE_ID_MISSING);
    }
  }

  public void validateInitialDraftId(UUID initialDraftId) {
    StockManagementInitialDraft initialDraft = stockManagementInitialDraftsRepository
        .findOne(initialDraftId);
    if (initialDraft == null) {
      throw new ValidationMessageException(ERROR_EVENT_INITIAL_DRAFT_ID_INVALID);
    }
  }
}
