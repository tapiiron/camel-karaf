/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.test.blueprint.management;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.ServiceStatus;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.test.junit5.TestSupport.isPlatform;
import static org.junit.jupiter.api.Assertions.*;

public class ManagedEndpointInjectRefEndpointTest extends CamelBlueprintTestSupport {

    @Override
    protected boolean useJmx() {
        return true;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "org/apache/camel/test/blueprint/management/managedEndpointInjectRefEndpointTest.xml";
    }

    protected MBeanServer getMBeanServer() {
        return context.getManagementStrategy().getManagementAgent().getMBeanServer();
    }

    @Test
    public void testRef() throws Exception {
        // JMX tests dont work well on AIX CI servers (hangs them)
        if (isPlatform("aix")) {
            return;
        }
        // don't test well on windows
        if (isPlatform("windows")) {
            return;
        }

        // fire a message to get it running
        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("foo").expectedMessageCount(1);
        template.sendBody("direct:start", "Hello World");
        assertMockEndpointsSatisfied();

        MBeanServer mbeanServer = getMBeanServer();

        Set<ObjectName> set = mbeanServer.queryNames(new ObjectName("*:type=producers,*"), null);
        assertEquals(2, set.size());

        Set<String> uris = new HashSet<>(Arrays.asList("mock://foo", "mock://result"));
        for (ObjectName on : set) {
            boolean registered = mbeanServer.isRegistered(on);
            assertTrue(registered, "Should be registered");

            String uri = (String) mbeanServer.getAttribute(on, "EndpointUri");
            assertTrue(uris.contains(uri), uri);

            // should be started
            String state = (String) mbeanServer.getAttribute(on, "State");
            assertEquals(ServiceStatus.Started.name(), state, "Should be started");
        }

        set = mbeanServer.queryNames(new ObjectName("*:type=endpoints,*"), null);
        assertEquals(4, set.size());

        uris = new HashSet<>(Arrays.asList("direct://start", "mock://foo", "mock://result", "ref://foo"));
        for (ObjectName on : set) {
            boolean registered = mbeanServer.isRegistered(on);
            assertTrue(registered, "Should be registered");

            String uri = (String) mbeanServer.getAttribute(on, "EndpointUri");
            assertTrue(uris.contains(uri), uri);
        }
    }

}
