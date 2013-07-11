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

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.interceptor.Interceptor;
import org.apache.flume.interceptor.InterceptorBuilderFactory;
import org.apache.flume.interceptor.InterceptorType;
import org.junit.Assert;
import org.junit.Test;
import org.n3r.flume.interceptor.MultiStaticInterceptor.Constants;

import com.google.common.base.Charsets;

public class TestMultiStaticInterceptor {

    @Test
    public void testOneKeyValue() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Interceptor.Builder builder = InterceptorBuilderFactory.newInstance(
                InterceptorType.MULTI_STATIC.toString());
        Context ctx = new Context();
        ctx.put(Constants.KEY_VALUE_SEPERATOR, "=");
        ctx.put(Constants.KEY_VALUES, "key1=val1");

        builder.configure(ctx);
        Interceptor interceptor = builder.build();

        Event event = EventBuilder.withBody("test", Charsets.UTF_8);
        Assert.assertNull(event.getHeaders().get("key1"));

        event = interceptor.intercept(event);
        String val = event.getHeaders().get("key1");

        Assert.assertEquals("val1", val);
    }

    @Test
    public void testMultiKeyValue() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Interceptor.Builder builder = InterceptorBuilderFactory.newInstance(
                InterceptorType.MULTI_STATIC.toString());
        Context ctx = new Context();
        ctx.put(Constants.KEY_VALUES, "key1:val1 key2:val2");

        builder.configure(ctx);
        Interceptor interceptor = builder.build();

        Event event = EventBuilder.withBody("test", Charsets.UTF_8);
        Assert.assertNull(event.getHeaders().get("key1"));
        Assert.assertNull(event.getHeaders().get("key2"));

        event = interceptor.intercept(event);
        String val = event.getHeaders().get("key1");
        Assert.assertEquals("val1", val);

        val = event.getHeaders().get("key2");
        Assert.assertEquals("val2", val);
    }

    @Test
    public void testReplace() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Interceptor.Builder builder = InterceptorBuilderFactory.newInstance(
                InterceptorType.MULTI_STATIC.toString());
        Context ctx = new Context();
        ctx.put(Constants.PRESERVE, "false");
        ctx.put(Constants.KEY_VALUES, "mutable:replacement");

        builder.configure(ctx);
        Interceptor interceptor = builder.build();

        Event event = EventBuilder.withBody("test", Charsets.UTF_8);
        event.getHeaders().put("mutable", "incumbent");

        Assert.assertEquals("incumbent", event.getHeaders().get("mutable"));

        event = interceptor.intercept(event);
        String val = event.getHeaders().get("mutable");
        Assert.assertEquals("replacement", val);
    }

    @Test
    public void testPreserve() throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Interceptor.Builder builder = InterceptorBuilderFactory.newInstance(
                InterceptorType.MULTI_STATIC.toString());
        Context ctx = new Context();
        ctx.put(Constants.PRESERVE, "true");
        ctx.put(Constants.KEY_VALUES, "immutable:replacement");

        builder.configure(ctx);
        Interceptor interceptor = builder.build();

        Event event = EventBuilder.withBody("test", Charsets.UTF_8);
        event.getHeaders().put("immutable", "incumbent");

        Assert.assertEquals("incumbent", event.getHeaders().get("immutable"));

        event = interceptor.intercept(event);
        String val = event.getHeaders().get("immutable");
        Assert.assertEquals("incumbent", val);
    }
}