package org.jclouds.glacier;

import static org.jclouds.reflect.Reflection2.method;

import org.jclouds.http.HttpRequest;
import org.jclouds.rest.internal.BaseAsyncClientTest;
import org.jclouds.rest.internal.GeneratedHttpRequest;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.google.common.reflect.Invokable;

@Test(groups = "unit", testName = "GlacierAsyncClientTest")
public class GlacierAsyncClientTest extends BaseAsyncClientTest<GlacierAsyncClient> {

   protected String url = "glacier.us-east-1.amazonaws.com";

   public void testCreateVault() {
      Invokable<?, ?> method = method(GlacierAsyncClient.class, "createVault", String.class);
      GeneratedHttpRequest request = processor.createRequest(method, Lists.<Object> newArrayList("VaultName"));
      assertRequestLineEquals(request, "PUT https://" + url + "/-/vaults/VaultName HTTP/1.1");
      assertPayloadEquals(request, null, null, false);
   }
   
   @Override
   public GlacierApiMetadata createApiMetadata() {
      return new GlacierApiMetadata();
   }

   @Override
   protected void checkFilters(HttpRequest arg0) {
      // TODO Auto-generated method stub
      
   }
}
