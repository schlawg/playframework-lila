/*
 * Copyright 2018 Greg Methvin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.methvin.play.autoconfig

import play.api.ConfigLoader

object AutoConfig {

  /** Generate a `ConfigLoader[T]` calling the constructor of a class. Use [[ConfigConstructor]] and [[ConfigName]]
   * annotations to change the constructor to use and the names of the parameters.
   *
   * @tparam T the type for which to create the configuration class
   * @return an instance of the `ConfigLoader` for the given class.
   */
  // TODO make it scala3
  def loader[T]: ConfigLoader[T] = ??? // macro AutoConfigImpl.loader[T]

  // given derived[T: Type](using Quotes): Expr[ConfigLoader[T]] = ???
}
