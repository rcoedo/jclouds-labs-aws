package org.jclouds.glacier.filters;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import org.jclouds.Constants;
import org.jclouds.crypto.Crypto;
import org.jclouds.date.TimeStamp;
import org.jclouds.domain.Credentials;
import org.jclouds.glacier.reference.GlacierHeaders;
import org.jclouds.glacier.util.AWSRequestSignerV4;
import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.http.HttpUtils;
import org.jclouds.logging.Logger;

import com.google.common.base.Supplier;

@Singleton
public class RequestAuthorizeSignature implements HttpRequestFilter {
   private final AWSRequestSignerV4 signer;

   @Resource
   @Named(Constants.LOGGER_SIGNATURE)
   Logger signatureLog = Logger.NULL;

   private final Provider<String> timeStampProvider;
   private final HttpUtils utils;

   @Inject
   public RequestAuthorizeSignature(
         @TimeStamp Provider<String> timeStampProvider,
         @org.jclouds.location.Provider Supplier<Credentials> creds,
         Crypto crypto, HttpUtils utils) {
      this.signer = new AWSRequestSignerV4(creds.get().identity, creds.get().credential, crypto);
      this.timeStampProvider = timeStampProvider;
      this.utils = utils;
   }

   @Override
   public HttpRequest filter(HttpRequest request) throws HttpException {
      request = this.replaceDateHeader(request);
      request = this.replaceHostHeader(request);
      utils.logRequest(signatureLog, request, ">>");
      request = this.signer.sign(request);
      utils.logRequest(signatureLog, request, "<<");
      return request;
   }

   HttpRequest replaceDateHeader(HttpRequest request) {
      request = request.toBuilder().removeHeader(HttpHeaders.DATE).build();
      request = request.toBuilder().replaceHeader(GlacierHeaders.ALTERNATE_DATE, timeStampProvider.get()).build();
      return request;
   }

   HttpRequest replaceHostHeader(HttpRequest request) {
      request = request.toBuilder().replaceHeader(HttpHeaders.HOST, request.getEndpoint().getHost()).build();
      return request;
   }
}
