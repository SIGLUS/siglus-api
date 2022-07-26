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

package org.siglus.siglusapi.util;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class AsyncExecutor<I> {
  private static final ExecutorService executorService =
      new ThreadPoolExecutor(5, 50, 5L, TimeUnit.SECONDS, new SynchronousQueue<>());
  private final Collection<I> collection;

  private AsyncExecutor(Collection<I> collection) {
    this.collection = synchronizedCollection(collection);
  }

  public static <I> AsyncExecutor<I> of(Collection<I> collection) {
    return new AsyncExecutor<>(collection);
  }

  public static <R> R supplyWithContext(SecurityContext context, Supplier<R> supplier) {
    SecurityContext old = SecurityContextHolder.getContext();
    try {
      SecurityContextHolder.setContext(context);
      return supplier.get();
    } finally {
      SecurityContextHolder.setContext(old);
    }
  }

  public <R> Stream<R> map(Function<I, R> mapper) {
    List<CompletableFuture<R>> futures =
        collection.stream()
            .map(it -> CompletableFuture.supplyAsync(() -> mapper.apply(it), executorService))
            .collect(Collectors.toList());
    return futures.stream().map(CompletableFuture::join);
  }
}
