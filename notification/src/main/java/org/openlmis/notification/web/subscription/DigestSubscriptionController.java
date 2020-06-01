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

package org.openlmis.notification.web.subscription;

import static org.openlmis.notification.i18n.MessageKeys.ERROR_INVALID_TAG_IN_SUBSCRIPTION;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_USER_CONTACT_DETAILS_NOT_FOUND;
import static org.openlmis.notification.web.BaseController.API_PREFIX;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.openlmis.notification.service.DigestionService;
import org.openlmis.notification.service.PermissionService;
import org.openlmis.notification.web.BaseController;
import org.openlmis.notification.web.NotFoundException;
import org.openlmis.notification.web.ValidationException;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Transactional
@RestController
@RequestMapping(API_PREFIX)
public class DigestSubscriptionController extends BaseController {

  private static final String USER_ENDPOINT_URL = "/users/{id}/subscriptions";

  @Autowired
  private UserContactDetailsRepository userContactDetailsRepository;

  @Autowired
  private DigestSubscriptionRepository digestSubscriptionRepository;

  @Autowired
  private DigestConfigurationRepository digestConfigurationRepository;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private DigestionService digestionService;

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Gets users subscriptions.
   *
   * @param userId user's UUID.
   * @return a list of current users subscriptions.
   */
  @GetMapping(USER_ENDPOINT_URL)
  public List<DigestSubscriptionDto> getUserSubscriptions(@PathVariable("id") UUID userId) {
    Profiler profiler = getProfiler("GET_USER_SUBSCRIPTIONS", userId);
    checkPermission(userId, profiler);

    profiler.start("CHECK_IF_USER_CONTACT_DETAILS");
    boolean exists = userContactDetailsRepository.exists(userId);

    if (!exists) {
      NotFoundException exception = new NotFoundException(ERROR_USER_CONTACT_DETAILS_NOT_FOUND);
      stopProfilerAndThrowException(profiler, exception);
    }

    profiler.start("RETRIEVE_USER_SUBSCRIPTIONS");
    List<DigestSubscription> subscriptions = digestSubscriptionRepository
        .getUserSubscriptions(userId);

    List<DigestSubscriptionDto> subscriptionDtos = toDto(subscriptions, profiler);

    return stopProfilerAndReturnValue(profiler, subscriptionDtos);
  }

  /**
   * Creates users subscriptions. Old subscriptions would be removed and to avoid that they should
   * be inside the request body.
   *
   * @param userId user's UUID.
   * @param subscriptions a list of users subscriptions.
   * @return a list of current users subscriptions.
   */
  @PostMapping(USER_ENDPOINT_URL)
  public List<DigestSubscriptionDto> createUserSubscriptions(@PathVariable("id") UUID userId,
      @RequestBody List<DigestSubscriptionDto> subscriptions) {
    Profiler profiler = getProfiler("CREATE_USER_SUBSCRIPTIONS", userId, subscriptions);
    checkPermission(userId, profiler);

    profiler.start("GET_USER_CONTACT_DETAILS");
    UserContactDetails contactDetails = userContactDetailsRepository.findOne(userId);

    if (Objects.isNull(contactDetails)) {
      NotFoundException exception = new NotFoundException(ERROR_USER_CONTACT_DETAILS_NOT_FOUND);
      stopProfilerAndThrowException(profiler, exception);
    }

    profiler.start("DELETE_OLD_USER_SUBSCRIPTIONS");
    digestSubscriptionRepository.deleteUserSubscriptions(userId);

    List<DigestSubscription> digestSubscriptions = toDomain(contactDetails,
        subscriptions, profiler);

    profiler.start("SAVE_USER_SUBSCRIPTIONS");
    digestSubscriptions = digestSubscriptionRepository.save(digestSubscriptions);

    profiler.start("STOP_EXISTING_MESSAGE_SOURCES");
    digestionService.dropExistingPollingAdapters(userId);

    List<DigestSubscriptionDto> subscriptionDtos = toDto(digestSubscriptions, profiler);
    return stopProfilerAndReturnValue(profiler, subscriptionDtos);
  }

  private void checkPermission(UUID userId, Profiler profiler) {
    profiler.start("CHECK_PERMISSION");
    permissionService.canManageUserSubscriptions(userId);
  }

  private List<DigestSubscription> toDomain(UserContactDetails userContactDetails,
      List<DigestSubscriptionDto> subscriptions, Profiler profiler) {
    profiler.start("GET_DIGEST_CONFIGURATION_TAGS_FROM_REQUEST");
    Set<UUID> ids = subscriptions
        .stream()
        .map(DigestSubscriptionDto::getDigestConfigurationId)
        .collect(Collectors.toSet());

    profiler.start("GET_DIGEST_CONFIGURATIONS");
    Map<UUID, DigestConfiguration> configurations = digestConfigurationRepository
        .findAll(ids)
        .stream()
        .collect(Collectors.toMap(DigestConfiguration::getId, Function.identity()));

    profiler.start("CONVERT_TO_DOMAIN");
    List<DigestSubscription> digestSubscriptions = Lists.newArrayList();
    for (int i = 0, size = subscriptions.size(); i < size; i++) {
      DigestSubscriptionDto subscriptionDto = subscriptions.get(i);
      DigestConfiguration digestConfiguration = configurations
          .get(subscriptionDto.getDigestConfigurationId());

      if (null == digestConfiguration) {
        ValidationException exception = new ValidationException(
            ERROR_INVALID_TAG_IN_SUBSCRIPTION,
            subscriptionDto.getDigestConfigurationId().toString());

        stopProfilerAndThrowException(profiler, exception);
      }

      digestSubscriptions.add(DigestSubscription.create(
          userContactDetails, digestConfiguration, subscriptionDto.getCronExpression(),
          subscriptionDto.getPreferredChannel(), subscriptionDto.getUseDigest()));
    }

    return digestSubscriptions;
  }

  private List<DigestSubscriptionDto> toDto(List<DigestSubscription> digestSubscriptions,
      Profiler profiler) {
    profiler.start("CONVERT_TO_DTO");
    return digestSubscriptions
        .stream()
        .map(domain -> {
          DigestSubscriptionDto dto = new DigestSubscriptionDto();
          dto.setServiceUrl(serviceUrl);

          domain.export(dto);

          return dto;
        })
        .collect(Collectors.toList());
  }

}
