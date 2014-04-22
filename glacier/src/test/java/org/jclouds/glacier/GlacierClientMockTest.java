package org.jclouds.glacier;

import static com.google.common.net.HttpHeaders.ETAG;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.jclouds.Constants.PROPERTY_SO_TIMEOUT;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

@Test(singleThreaded = true)
public class GlacierClientMockTest {

   private static final Set<Module> modules = ImmutableSet.<Module> of(
         new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()));

   static GlacierClient getGlacierClient(URL server) {
      Properties overrides = new Properties();
      // prevent expect-100 bug http://code.google.com/p/mockwebserver/issues/detail?id=6
      overrides.setProperty(PROPERTY_SO_TIMEOUT, "0");
      overrides.setProperty(PROPERTY_MAX_RETRIES, "1");
      return ContextBuilder.newBuilder("glacier")
                           .credentials("accessKey", "secretKey")
                           .endpoint(server.toString())
                           .modules(modules)
                           .overrides(overrides)
                           .buildApi(GlacierClient.class);
   }
   
   public void experiments() throws IOException, InterruptedException {
      MockWebServer server = new MockWebServer();
      server.enqueue(new MockResponse().addHeader(ETAG, "Testing"));
      // hangs on Java 7 without this additional response ?!?
      server.enqueue(new MockResponse().addHeader(ETAG, "Testing"));
      server.play();

      GlacierClient client = getGlacierClient(server.getUrl("/"));
      assertEquals(client.createVault("ConcreteVaultName"), true);
      RecordedRequest request = server.takeRequest();
      System.out.println(request);
      System.out.println(request.getHeaders());
/*      assertEquals(request.getRequestLine(), "PUT /bucket/object HTTP/1.1");
      assertEquals(request.getHeaders(CONTENT_LENGTH), ImmutableList.of("0"));
      // will fail unless -Dsun.net.http.allowRestrictedHeaders=true is set
      assertEquals(request.getHeaders(EXPECT), ImmutableList.of("100-continue"));
      server.shutdown();*/
   }
}
