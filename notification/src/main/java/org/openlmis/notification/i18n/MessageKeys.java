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

package org.openlmis.notification.i18n;

import java.util.Arrays;

public abstract class MessageKeys {
  private static final String DELIMITER = ".";

  private static final String PERMISSION = "permission";
  private static final String MISSING = "missing";
  private static final String USER_CONTACT_DETAILS = "userContactDetails";
  private static final String VERIFICATIONS = "verifications";
  private static final String TOKEN = "token";
  private static final String EMAIL = "email";
  private static final String MESSAGES = "messages";
  private static final String NOTIFICATION_CHANNEL = "notificationChannel";
  private static final String NOTIFICATION = "notification";
  private static final String SEARCH = "search";
  private static final String DIGEST_CONFIGURATION = "digestConfiguration";
  private static final String DIGEST_SUBSCRIPTION = "digestSubscription";


  private static final String AUTHENTICATION = "authentication";
  private static final String USER = "user";
  private static final String NOT_FOUND = "notFound";
  private static final String UNSUPPORTED = "unsupported";
  private static final String DUPLICATED = "duplicated";
  private static final String INVALID_PARAMS = "invalidParams";
  private static final String INVALID_DATE_FORMAT = "invalidDateFormat";
  private static final String INVALID_UUID_FORMAT = "invalidUuidFormat";
  private static final String INVALID_CHANNEL_FOR_DIGEST = "invalidChannelForDigest";

  private static final String SERVICE_PREFIX = "notification";
  private static final String ERROR_PREFIX = SERVICE_PREFIX + ".error";
  private static final String NOTIFICATION_REQUEST = ERROR_PREFIX + ".notificationRequest";

  public static final String ERROR_CONTEXTUAL_STATE_NULL =
      ERROR_PREFIX + ".validation.contextualState.null";

  public static final String ERROR_USER_CONTACT_DETAILS_NOT_FOUND =
      ERROR_PREFIX + DELIMITER + USER_CONTACT_DETAILS + DELIMITER + NOT_FOUND;
  public static final String ERROR_USER_NOT_FOUND = join(ERROR_PREFIX, USER, NOT_FOUND);
  public static final String ERROR_USER_NOT_ACTIVE_OR_NOT_FOUND =
      ERROR_PREFIX + DELIMITER + USER + DELIMITER + "notActiveOrNotFound";
  public static final String ERROR_USER_CONTACT_DETAILS_SEARCH_INVALID_PARAMS =
      join(ERROR_PREFIX, USER_CONTACT_DETAILS, SEARCH, INVALID_PARAMS);

  public static final String ERROR_NOTIFICATION_REQUEST_NULL = join(NOTIFICATION_REQUEST, "null");
  public static final String ERROR_NOTIFICATION_REQUEST_FIELD_REQUIRED =
      join(NOTIFICATION_REQUEST, "fieldRequired");
  public static final String ERROR_NOTIFICATION_REQUEST_MESSAGES_EMPTY =
      join(NOTIFICATION_REQUEST, MESSAGES, "empty");

  public static final String ERROR_UNSUPPORTED_NOTIFICATION_CHANNEL =
      join(NOTIFICATION_REQUEST, NOTIFICATION_CHANNEL, UNSUPPORTED);

  public static final String ERROR_NOTIFICATION_CHANNEL_DUPLICATED =
      join(NOTIFICATION_REQUEST, NOTIFICATION_CHANNEL, DUPLICATED);

  public static final String ERROR_EMAIL_INVALID =
      ERROR_PREFIX + ".userContactDetails.email.invalid";
  public static final String ERROR_EMAIL_DUPLICATED =
      ERROR_PREFIX + ".userContactDetails.email.duplicated";

  public static final String ERROR_SEND_REQUEST = ERROR_PREFIX + ".sendRequest";
  public static final String ERROR_CONSTRAINT = ERROR_PREFIX + ".constraint";

  public static final String ERROR_FIELD_IS_INVARIANT = ERROR_PREFIX + ".fieldIsInvariant";
  public static final String PERMISSION_MISSING = join(ERROR_PREFIX, PERMISSION, MISSING);
  public static final String PERMISSION_MISSING_GENERIC =
      join(ERROR_PREFIX, PERMISSION, "missingGeneric");
  public static final String USER_NOT_FOUND =
      join(ERROR_PREFIX, AUTHENTICATION, USER, NOT_FOUND);

  public static final String ERROR_USER_CONTACT_DETAILS_ID_MISMATCH =
      join(ERROR_PREFIX, USER_CONTACT_DETAILS, "idMismatch");

  public static final String ERROR_VERIFICATIONS_ID_MISMATCH =
      join(ERROR_PREFIX, VERIFICATIONS, "idMismatch");

  public static final String ERROR_TOKEN_INVALID =
      join(ERROR_PREFIX, VERIFICATIONS, TOKEN, "invalid");
  public static final String ERROR_TOKEN_EXPIRED =
      join(ERROR_PREFIX, VERIFICATIONS, TOKEN, "expired");

  public static final String EMAIL_VERIFICATION_EMAIL_SUBJECT =
      join(SERVICE_PREFIX, VERIFICATIONS, EMAIL, "subject");
  public static final String EMAIL_VERIFICATION_EMAIL_BODY =
      join(SERVICE_PREFIX, VERIFICATIONS, EMAIL, "body");

  public static final String ERROR_VERIFICATION_EMAIL_VERIFIED =
      join(ERROR_PREFIX, VERIFICATIONS, EMAIL, "verified");

  public static final String ERROR_VERIFICATION_EMAIL_DUPLICATED =
      join(ERROR_PREFIX, VERIFICATIONS, EMAIL, "duplicated");

  public static final String ERROR_USER_HAS_NO_EMAIL =
      join(ERROR_PREFIX, VERIFICATIONS, EMAIL, "null");

  public static final String ERROR_SEND_MAIL_FAILURE =
      join(ERROR_PREFIX, "sendMail", "failure");

  public static final String ERROR_SEND_SMS_FAILURE =
      join(ERROR_PREFIX, "sendSms", "failure");

  public static final String EMAIL_VERIFICATION_SUCCESS =
      join(SERVICE_PREFIX, VERIFICATIONS, EMAIL, "success");

  public static final String ERROR_NOTIFICATION_SEARCH_INVALID_PARAMS =
      join(ERROR_PREFIX, NOTIFICATION, SEARCH, INVALID_PARAMS);

  public static final String ERROR_INVALID_DATE_FORMAT =
      join(ERROR_PREFIX, INVALID_DATE_FORMAT);

  public static final String ERROR_INVALID_UUID_FORMAT =
      join(ERROR_PREFIX, INVALID_UUID_FORMAT);

  public static final String ERROR_INVALID_TAG_IN_SUBSCRIPTION =
      join(ERROR_PREFIX, DIGEST_SUBSCRIPTION, "invalidTag");

  public static final String ERROR_INVALID_CRON_EXPRESSION_IN_SUBSCRIPTION =
      join(ERROR_PREFIX, DIGEST_SUBSCRIPTION, "invalidCronExpression");

  public static final String ERROR_MISSING_CRON_EXPRESSION =
      join(ERROR_PREFIX, DIGEST_SUBSCRIPTION, "missingCronExpression");

  public static final String ERROR_DIGEST_CONFIGURATION_NOT_FOUND =
      join(ERROR_PREFIX, DIGEST_CONFIGURATION, NOT_FOUND);

  public static final String ERROR_DIGEST_SUBSCRIPTION_INVALID_CHANNEL_FOR_DIGEST =
      join(ERROR_PREFIX, DIGEST_SUBSCRIPTION, INVALID_CHANNEL_FOR_DIGEST);

  public static final String ERROR_SIZE_NULL =
      join(ERROR_PREFIX, "pageable.size.null");

  public static final String ERROR_SIZE_NOT_POSITIVE =
      join(ERROR_PREFIX, "pageable.size.notPositive");

  private MessageKeys() {
    throw new UnsupportedOperationException();
  }

  private static String join(String... params) {
    return String.join(DELIMITER, Arrays.asList(params));
  }

}
