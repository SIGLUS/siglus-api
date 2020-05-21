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

package org.openlmis.referencedata.repository.custom.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.Setter;
import org.openlmis.referencedata.domain.Identifiable;
import org.openlmis.referencedata.repository.custom.CrudRedisRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public abstract class BaseRedisRepository<T extends Identifiable>
    implements CrudRedisRepository<T> {

  private RedisTemplate redisTemplate;
  private HashOperations hashOperations;
  private ObjectMapper mapper = new ObjectMapper();

  @Setter
  private Class<T> clazz;

  public BaseRedisRepository(RedisTemplate redisTemplate, Class<T> clazz) {
    this.redisTemplate = redisTemplate;
    this.clazz = clazz;
  }

  @PostConstruct
  protected void init() {
    hashOperations = redisTemplate.opsForHash();
  }

  @Override
  public boolean exists(UUID id) {
    return hashOperations.hasKey(getHashKey(), id.toString());
  }

  @Override
  public T findById(UUID id) {
    return mapper.convertValue(this.redisTemplate.opsForHash()
        .get(getHashKey(), id), clazz);
  }

  @Override
  public void save(T entity) {
    hashOperations.put(getHashKey(), entity.getId(), entity);
  }

  @Override
  public void delete(T entity) {
    hashOperations.delete(getHashKey(), entity.getId().toString());
  }

  protected abstract String getHashKey();

}
