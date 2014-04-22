package org.jclouds.glacier.reference;

public interface GlacierHeaders {
   public static final String DEFAULT_AMAZON_HEADERTAG = "amz";
   public static final String HEADER_PREFIX = "x-" + DEFAULT_AMAZON_HEADERTAG + "-";
   public static final String VERSION = HEADER_PREFIX + "glacier-version";
   public static final String ALTERNATE_DATE = HEADER_PREFIX + "date";
}
