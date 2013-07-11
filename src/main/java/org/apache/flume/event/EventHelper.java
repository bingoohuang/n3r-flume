/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.flume.event;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import org.apache.commons.io.HexDump;
import org.apache.flume.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class EventHelper {

  private static final String HEXDUMP_OFFSET = "00000000";
  private static final String EOL = System.getProperty("line.separator", "\n");
  private static final int DEFAULT_MAX_BYTES = 16;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(EventHelper.class);

  public static String dumpEvent(Event event) {
    return dumpEvent(event, DEFAULT_MAX_BYTES);
  }

  public static String dumpEvent(Event event, int maxBytes) {
    StringBuilder buffer = new StringBuilder();
    Map<String,String> headers = event.getHeaders();

    if (event.getBody() == null) {
      buffer.append("null");
    } else if (event.getBody().length == 0) {
      // do nothing... in this case, HexDump.dump() will throw an exception
    } else {
      byte[] body = event.getBody();

        String bodyType = null;
        String bodyCharset = null;
        if (headers != null ) {
            bodyType = headers.get(Event.bodyType);
            bodyCharset = headers.get(Event.bodyCharset);
            for(Map.Entry<String, String> entry : headers.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
        }

        if (bodyCharset == null) bodyCharset = "UTF-8";
        if ("long".equals(bodyType)) {
            return "" + Longs.fromByteArray(body);
        }
        if ("string".equals(bodyType)) {
            return fromBytes(body, bodyCharset);
        }

      byte[] data = Arrays.copyOf(body, Math.min(body.length, maxBytes));
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
        HexDump.dump(data, 0, out, 0);
        String hexDump = new String(out.toByteArray());
        // remove offset since it's not relevant for such a small dataset
        if(hexDump.startsWith(HEXDUMP_OFFSET)) {
          hexDump = hexDump.substring(HEXDUMP_OFFSET.length());
        }
        buffer.append(hexDump);
      } catch (Exception e) {
       if(LOGGER.isInfoEnabled()) {
         LOGGER.info("Exception while dumping event", e);
       }
        buffer.append("...Exception while dumping: ").append(e.getMessage());
      }
      String result = buffer.toString();
      if(result.endsWith(EOL) && buffer.length() > EOL.length()) {
        buffer.delete(buffer.length() - EOL.length(), buffer.length()).toString();
      }
    }
    return "{ headers:" + headers + " body:" + buffer + " }";
  }

    private static String fromBytes(byte[] body, String bodyCharset) {
        try {
            return new String(body, bodyCharset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
