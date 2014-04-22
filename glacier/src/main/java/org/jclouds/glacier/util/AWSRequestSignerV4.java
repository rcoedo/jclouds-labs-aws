package org.jclouds.glacier.util;

import static com.google.common.base.Charsets.UTF_8;

import java.io.IOException;
import java.util.Locale;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.ws.rs.core.HttpHeaders;

import org.jclouds.crypto.Crypto;
import org.jclouds.glacier.reference.GlacierHeaders;
import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

//TODO: Query parameters, not necessary for Glacier
//TODO: Endpoint on buildCredentialScope is being read from the static string. Uncool.
/**
 * Signs requests using the AWSv4 signing algorithm
 * <p/>
 *
 * @see <a href="http://docs.aws.amazon.com/general/latest/gr/sigv4_signing.html" />
 * @author Roman Coedo
 */
public class AWSRequestSignerV4 {

   public static final String AUTH_TAG = "AWS4";
   public static final String HEADER_TAG = "x-amz-";
   public static final String ALGORITHM = AUTH_TAG + "-HMAC-SHA256";
   public static final String TERMINATION_STRING = "aws4_request";
   public static final String REGION = "us-east-1";
   public static final String SERVICE = "glacier";

   private final Crypto crypto;
   private final String identity;
   private final String credential;

   public AWSRequestSignerV4(String identity, String credential, Crypto crypto) {
      this.crypto = crypto;
      this.identity = identity;
      this.credential = credential;
   }

   private String buildHashedCanonicalRequest(String method, String endpoint, String hashedPayload,
         String canonicalizedHeadersString, String signedHeaders) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(method).append("\n");
      buffer.append(endpoint).append("\n");
      buffer.append("").append("\n");
      buffer.append(canonicalizedHeadersString).append("\n");
      buffer.append(signedHeaders).append("\n");
      buffer.append(hashedPayload);
      return sha256(buffer.toString().getBytes());
   }

   private String createStringToSign(String date, String credentialScope, String hashedCanonicalRequest) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(ALGORITHM).append("\n");
      buffer.append(date).append("\n");
      buffer.append(credentialScope).append("\n");
      buffer.append(hashedCanonicalRequest);
      return buffer.toString();
   }

   private String formatDateWithoutTimestamp(String date) {
      return date.substring(0, 8);
   }

   private String buildCredentialScope(String dateWithoutTimeStamp) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(dateWithoutTimeStamp).append("/");
      buffer.append(REGION).append("/");
      buffer.append(SERVICE).append("/");
      buffer.append(TERMINATION_STRING);
      return buffer.toString();
   }

   private Multimap<String, String> buildCanonicalizedHeadersMap(HttpRequest request) {
      Multimap<String, String> headers = request.getHeaders();
      SortedSetMultimap<String, String> canonicalizedHeaders = TreeMultimap.create();
      for (Entry<String, String> header : headers.entries()) {
         if (header.getKey() == null)
            continue;
         String key = header.getKey().toString().toLowerCase(Locale.getDefault());
         // Ignore any headers that are not particularly interesting.
         if (key.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE) || key.equalsIgnoreCase("Content-MD5")
                  || key.equalsIgnoreCase(HttpHeaders.HOST) || key.startsWith(HEADER_TAG)) {
            canonicalizedHeaders.put(key, header.getValue());
         }
      }
      return canonicalizedHeaders;
   }

   private String buildCanonicalizedHeadersString(Multimap<String, String> canonicalizedHeadersMap) {
      StringBuilder canonicalizedHeadersBuffer = new StringBuilder();
      for (Entry<String, String> header : canonicalizedHeadersMap.entries()) {
         String key = header.getKey();
         canonicalizedHeadersBuffer.append(String.format("%s:%s\n", key.toLowerCase(), header.getValue()));
      }
      return canonicalizedHeadersBuffer.toString();
   }

   private String buildSignedHeaders(Multimap<String, String> canonicalizedHeadersMap) {
      StringBuilder signedHeadersBuffer = new StringBuilder();
      for (Entry<String, String> header : canonicalizedHeadersMap.entries()) {
         String key = header.getKey();
         signedHeadersBuffer.append(key.toLowerCase()).append(";");
      }
      if(signedHeadersBuffer.length() > 0) {
         signedHeadersBuffer.deleteCharAt(signedHeadersBuffer.length()-1);
      }
      return signedHeadersBuffer.toString();
   }

   private String sha256(byte[] unhashedBytes) {
      return Hashing.sha256().hashBytes(unhashedBytes).toString();
   }

   private String buildHashedPayload(HttpRequest request) {
      String hashedPayload = "";
      try {
         byte[] unhashedBytes = request.getPayload() == null ?
               "".getBytes() : ByteStreams.toByteArray(request.getPayload().getInput());
         hashedPayload = sha256(unhashedBytes);
      } catch (IOException e) {
         throw new HttpException("Error signing request", e);
      }
      return hashedPayload;
   }

   private byte[] hmacSha256(byte[] key, String s) {
      try {
         Mac hmacSHA256 = crypto.hmacSHA256(key);
         return hmacSHA256.doFinal(s.getBytes());
      } catch (Exception e) {
         throw new HttpException("Error signing request", e);
      }
   }

   @VisibleForTesting
   private String buildSignature(String dateWithoutTimestamp, String stringToSign) {
      byte[] kSecret = (AUTH_TAG + credential).getBytes(UTF_8);
      byte[] kDate = hmacSha256(kSecret, dateWithoutTimestamp);
      byte[] kRegion = hmacSha256(kDate, REGION);
      byte[] kService = hmacSha256(kRegion, SERVICE);
      byte[] kSigning = hmacSha256(kService, TERMINATION_STRING);
      return BaseEncoding.base16().encode(hmacSha256(kSigning, stringToSign)).toLowerCase();
   }

   private String buildAuthHeader(String accessKey, String credentialScope, String signedHeaders, String signature) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(ALGORITHM).append(" ");
      buffer.append("Credential=").append(accessKey).append("/").append(credentialScope).append(",");
      buffer.append("SignedHeaders=").append(signedHeaders).append(",");
      buffer.append("Signature=").append(signature);
      return buffer.toString();
   }

   public HttpRequest sign(HttpRequest request) {
      //Grab the needed data to build the signature
      Multimap<String, String> canonicalizedHeadersMap = buildCanonicalizedHeadersMap(request);
      String canonicalizedHeadersString = buildCanonicalizedHeadersString(canonicalizedHeadersMap);
      String signedHeaders = buildSignedHeaders(canonicalizedHeadersMap);
      String date = request.getFirstHeaderOrNull(GlacierHeaders.ALTERNATE_DATE);
      String dateWithoutTimestamp = formatDateWithoutTimestamp(date);
      String method = request.getMethod();
      String endpoint = request.getEndpoint().getRawPath();
      String credentialScope = buildCredentialScope(dateWithoutTimestamp);
      String hashedPayload = buildHashedPayload(request);

      //Task 1: Create a Canonical Request For Signature Version 4.
      String hashedCanonicalRequest = buildHashedCanonicalRequest(method, endpoint,
            hashedPayload, canonicalizedHeadersString, signedHeaders);

      //Task 2: Create a String to Sign for Signature Version 4.
      String stringToSign = this.createStringToSign(date, credentialScope, hashedCanonicalRequest);

      //Task 3: Calculate the AWS Signature Version 4.
      String signature = buildSignature(dateWithoutTimestamp, stringToSign);

      //Sign the request
      String authHeader = buildAuthHeader(identity, credentialScope, signedHeaders, signature);
      request = request.toBuilder().replaceHeader(HttpHeaders.AUTHORIZATION, authHeader).build();
      return request;
   }
}
