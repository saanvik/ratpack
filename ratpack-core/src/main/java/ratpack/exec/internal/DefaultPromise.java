/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.exec.internal;

import ratpack.exec.Downstream;
import ratpack.exec.ExecutionException;
import ratpack.exec.Promise;
import ratpack.exec.Upstream;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.util.Exceptions;
import ratpack.util.internal.InternalRatpackError;

public class DefaultPromise<T> implements Promise<T> {

  private final Upstream<T> upstream;

  public DefaultPromise(Upstream<T> upstream) {
    this.upstream = upstream;
  }

  @Override
  public void then(final Action<? super T> then) {
    try {
      upstream.connect(new Downstream<T>() {
        @Override
        public void success(T value) {
          try {
            then.execute(value);
          } catch (Throwable e) {
            throwError(e);
          }
        }

        @Override
        public void error(Throwable throwable) {
          throwError(throwable);
        }

        @Override
        public void complete() {}
      });
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalRatpackError("failed to add promise resume action", e);
    }
  }

  private void throwError(Throwable throwable) {
    ExecutionBacking.require().streamSubscribe(h ->
        h.complete(() -> {
          throw Exceptions.toException(throwable);
        })
    );
  }

  @Override
  public <O> Promise<O> transform(Function<? super Upstream<? extends T>, ? extends Upstream<O>> upstreamTransformer) {
    try {
      return new DefaultPromise<>(upstreamTransformer.apply(upstream));
    } catch (Exception e) {
      throw Exceptions.uncheck(e);
    }
  }

}
