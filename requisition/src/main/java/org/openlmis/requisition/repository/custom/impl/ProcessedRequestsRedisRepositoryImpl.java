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

package org.openlmis.requisition.repository.custom.impl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.repository.custom.ProcessedRequestsRedisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedRequestsRedisRepositoryImpl implements ProcessedRequestsRedisRepository {

  private static final String HASH_KEY = "PROCESSED_REQUESTS";

  private RedisTemplate<String, String> redisTemplate;
  private HashOperations hashOperations;

  @Autowired
  public ProcessedRequestsRedisRepositoryImpl(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  private void init() {
    hashOperations = redisTemplate.opsForHash();
  }

  @Override
  public boolean exists(UUID idempotencyKey) {
    String key = idempotencyKey.toString();
    return hashOperations.hasKey(key, HASH_KEY);
  }

  @Override
  public UUID findByIdempotencyKey(UUID idempotencyKey) {
    String resource = (String) hashOperations.get(idempotencyKey.toString(), HASH_KEY);
    return StringUtils.isBlank(resource) ? null : UUID.fromString(resource);
  }

  @Override
  public void addOrUpdate(UUID key, UUID resourceId) {
    hashOperations.put(key.toString(), HASH_KEY, resourceId == null
        ? StringUtils.EMPTY : resourceId.toString());
    redisTemplate.expire(key.toString(), 24, TimeUnit.HOURS);
  }
}
