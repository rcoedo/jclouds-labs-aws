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
package org.jclouds.iam.xml;

import static org.jclouds.util.SaxUtils.currentOrNull;
import static org.jclouds.util.Strings2.urlDecode;

import org.jclouds.http.functions.ParseSax;
import org.jclouds.iam.domain.Policy;
import org.xml.sax.Attributes;

/**
 * @see <a href="http://docs.aws.amazon.com/IAM/latest/APIReference/API_GetGroupPolicy.html" />
 */
public class PolicyHandler extends ParseSax.HandlerForGeneratedRequestWithResult<Policy> {

   private StringBuilder currentText = new StringBuilder();
   private Policy.Builder builder = Policy.builder();

   @Override
   public Policy getResult() {
      try {
         return builder.build();
      } finally {
         builder = Policy.builder();
      }
   }

   @Override
   public void startElement(String url, String name, String qName, Attributes attributes) {
   }

   @Override
   public void endElement(String uri, String name, String qName) {
      if (qName.equals("PolicyName")) {
         builder.name(currentOrNull(currentText));
      } else if (qName.endsWith("Name")) {
         builder.owner(currentOrNull(currentText));
      } else if (qName.equals("PolicyDocument")) {
         builder.document(urlDecode(currentOrNull(currentText)));
      }
      currentText.setLength(0);
   }

   @Override
   public void characters(char ch[], int start, int length) {
      currentText.append(ch, start, length);
   }
}
