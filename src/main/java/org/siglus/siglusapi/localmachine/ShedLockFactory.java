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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShedLockFactory {
  private final LockProvider lockProvider;

  public AutoClosableLock waitLock(String lockId, long timeoutMillis) throws InterruptedException {
    long waitUntil = System.currentTimeMillis() + timeoutMillis;
    while (true) {
      AutoClosableLock lock = this.lock(lockId);
      if (lock.isPresent()) {
        return lock;
      }
      if (System.currentTimeMillis() >= waitUntil) {
        return new AutoClosableLock(Optional.empty());
      }
      TimeUnit.MILLISECONDS.sleep(100);
    }
  }

  public AutoClosableLock lock(String lockId) {
    // the lock will be extended after 1/2 minutes until task done.
    Duration lockAtMost10Minutes = Duration.ofSeconds(30);
    Duration lockAtLeast100MilliSeconds = Duration.ofMillis(0);
    LockConfiguration lockConfiguration =
        new LockConfiguration(
            Instant.now(), lockId, lockAtMost10Minutes, lockAtLeast100MilliSeconds);
    Optional<SimpleLock> lock = lockProvider.lock(lockConfiguration);
    return new AutoClosableLock(lock);
  }

  public static class AutoClosableLock implements AutoCloseable {
    private final Optional<SimpleLock> lock;

    public AutoClosableLock(Optional<SimpleLock> lock) {
      this.lock = lock;
    }

    public boolean isPresent() {
      return lock.isPresent();
    }

    public void ifPresent(Runnable runnable) {
      if (this.isPresent()) {
        runnable.run();
      }
    }

    @Override
    public void close() {
      lock.ifPresent(SimpleLock::unlock);
    }
  }
}
