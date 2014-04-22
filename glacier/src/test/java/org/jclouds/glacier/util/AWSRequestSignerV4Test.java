package org.jclouds.glacier.util;

import static org.testng.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.jclouds.encryption.internal.JCECrypto;
import org.jclouds.http.HttpRequest;
import org.testng.annotations.Test;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

@Test(groups = "unit", testName = "AWSRequestSignerV4Test")
public class AWSRequestSignerV4Test {

   @Test
   public void testSignatureCalculationExampleTest() throws NoSuchAlgorithmException, CertificateException {
      String auth = "AWS4-HMAC-SHA256 " +
            "Credential=AKIAIOSFODNN7EXAMPLE/20120525/us-east-1/glacier/aws4_request," +
            "SignedHeaders=host;x-amz-date;x-amz-glacier-version," +
            "Signature=3ce5b2f2fffac9262b4da9256f8d086b4aaf42eba5f111c21681a65a127b7c2a";
      String identity = "AKIAIOSFODNN7EXAMPLE";
      String credential = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
      AWSRequestSignerV4 signer = new AWSRequestSignerV4(identity, credential, new JCECrypto());
      HttpRequest request = signer.sign(createRequest());
      assertEquals(request.getFirstHeaderOrNull("Authorization"),auth);
   }

   private HttpRequest createRequest() {
      Multimap<String, String> headers = TreeMultimap.create();
      headers.put("Host", "glacier.us-east-1.amazonaws.com");
      headers.put("x-amz-date", "20120525T002453Z");
      headers.put("x-amz-glacier-version", "2012-06-01");
      HttpRequest request = HttpRequest.builder()
            .method("PUT")
            .endpoint("https://glacier.us-east-1.amazonaws.com/-/vaults/examplevault")
            .headers(headers)
            .build();
      return request;
   }

}
