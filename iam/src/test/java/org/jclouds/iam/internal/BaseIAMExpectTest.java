/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.iam.internal;

import java.util.TimeZone;

import org.jclouds.date.DateService;
import org.jclouds.iam.config.IAMHttpApiModule;
import org.jclouds.rest.ConfiguresHttpApi;
import org.jclouds.rest.internal.BaseRestApiExpectTest;

import com.google.inject.Module;

public class BaseIAMExpectTest<T> extends BaseRestApiExpectTest<T> {

   public BaseIAMExpectTest() {
      provider = "iam";
      TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
   }

   @ConfiguresHttpApi
   private static final class TestIAMHttpApiModule extends IAMHttpApiModule {

      @Override
      protected String provideTimeStamp(final DateService dateService) {
         return "2009-11-08T15:54:08.897Z";
      }
   }

   @Override
   protected Module createModule() {
      return new TestIAMHttpApiModule();
   }
}
