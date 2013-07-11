/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.n3r.flume.interceptor;

import static org.n3r.flume.interceptor.MultiStaticInterceptor.Constants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

/**
 * Interceptor class that appends multiple static, pre-configured headers to all events.
 *
 * Properties:<p>
 *
 *   keyval: Key Value pairs, separated by spaces <p>
 *
 *   preserveExisting: Whether to preserve an existing value for the key
 *                     (default is true)<p>
 *
 * Sample config:<p>
 *
 * <code>
 *   agent.sources.r1.channels = c1<p>
 *   agent.sources.r1.type = SEQ<p>
 *   agent.sources.r1.interceptors = i1<p>
 *   agent.sources.r1.interceptors.i1.type = multi_static<p>
 *   agent.sources.r1.interceptors.i1.preserveExisting = false<p>
 *   agent.sources.r1.interceptors.i1.keyval = key1,val1 key2,val2<p>
 * </code>
 *
 */
public class MultiStaticInterceptor implements Interceptor {

  private static final Logger logger = LoggerFactory.getLogger(MultiStaticInterceptor.class);

  private final boolean preserveExisting;
  private final Map<String, String> keyval;

  /**
   * Only {@link MultiStaticInterceptor.Builder} can build me
   */
  private MultiStaticInterceptor(boolean preserveExisting, Map<String, String> keyval) {
    this.preserveExisting = preserveExisting;
    this.keyval = keyval;
  }

  @Override
  public void initialize() {
    // no-op
  }

  /**
   * Modifies events in-place.
   */
  @Override
  public Event intercept(Event event) {
    if (MapUtils.isEmpty(keyval)) return event;

    Map<String, String> headers = event.getHeaders();

    for(Map.Entry<String, String> entry : keyval.entrySet()) {
        if (preserveExisting && headers.containsKey(entry.getKey()))
            continue;
        headers.put(entry.getKey(), entry.getValue());
    }

    return event;
  }

  /**
   * Delegates to {@link #intercept(Event)} in a loop.
   * @param events
   * @return
   */
  @Override
  public List<Event> intercept(List<Event> events) {
    for (Event event : events) {
      intercept(event);
    }
    return events;
  }

  @Override
  public void close() {
    // no-op
  }

  /**
   * Builder which builds new instance of the StaticInterceptor.
   */
  public static class Builder implements Interceptor.Builder {

    private boolean preserveExisting;
    private String keyval;
    private String kvSeperator;

    @Override
    public void configure(Context context) {
      preserveExisting = context.getBoolean(PRESERVE, PRESERVE_DFLT);
      keyval = context.getString(KEY_VAL, KEY_VAL_DFLT);
      kvSeperator = context.getString(KV_SEPERATOR, KV_SEPERATOR_DFLT);
    }

    @Override
    public Interceptor build() {
      logger.info(String.format("Creating StaticInterceptor: preserveExisting=%s,keyval=%s", preserveExisting, keyval));

      if (StringUtils.isEmpty(keyval))
          return new MultiStaticInterceptor(preserveExisting, null);

      Map<String, String> kvMap = new HashMap<String, String>();
      Splitter splitter = Splitter.onPattern("\\s").omitEmptyStrings().trimResults();
      Iterable<String> kvPair = splitter.split(keyval);
      for (String pair : kvPair) {
          String[] split = pair.split(kvSeperator);
          if (split.length < 2 || StringUtils.isEmpty(split[0])) {
              logger.warn("keyval configure error [{}]", pair);
              continue;
          }
          kvMap.put(split[0], split[1]);
      }
      return new MultiStaticInterceptor(preserveExisting, kvMap);
    }


  }

  public static class Constants {

    public static final String KEY_VAL = "keyval";
    public static final String KEY_VAL_DFLT = "";

    public static final String KV_SEPERATOR = "seperator";
    public static final String KV_SEPERATOR_DFLT = ",";

    public static final String PRESERVE = "preserveExisting";
    public static final boolean PRESERVE_DFLT = true;
  }
}
