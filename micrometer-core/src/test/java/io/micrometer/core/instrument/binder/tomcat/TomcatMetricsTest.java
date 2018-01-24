/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.core.instrument.binder.tomcat;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.TooManyActiveSessionsException;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Clint Checketts
 * @author Jon Schneider
 */
class TomcatMetricsTest {
    private SimpleMeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());

    @Test
    void managerBasedMetrics() {
        Context context = new StandardContext();

        ManagerBase manager = new ManagerBase() {
            @Override
            public void load() throws ClassNotFoundException, IOException {
            }

            @Override
            public void unload() throws IOException {
            }

            @Override
            public Context getContext() {
                return context;
            }
        };

        manager.setMaxActiveSessions(3);

        manager.createSession("first");
        manager.createSession("second");
        manager.createSession("third");

        try {
            manager.createSession("fourth");
            fail("TooManyActiveSessionsException expected.");
        } catch (TooManyActiveSessionsException exception) {
            //ignore error, testing rejection
        }

        StandardSession expiredSession = new StandardSession(manager);
        expiredSession.setId("third");
        expiredSession.setCreationTime(System.currentTimeMillis() - 10_000);
        manager.remove(expiredSession, true);

        Iterable<Tag> tags = Tags.zip("metricTag", "val1");
        TomcatMetrics.monitor(registry, manager, tags);

        assertThat(registry.mustFind("tomcat.sessions.active.max").tags(tags).gauge().value()).isEqualTo(3.0);
        assertThat(registry.mustFind("tomcat.sessions.active.current").tags(tags).gauge().value()).isEqualTo(2.0);
        assertThat(registry.mustFind("tomcat.sessions.expired").tags(tags).functionCounter().count()).isEqualTo(1.0);
        assertThat(registry.mustFind("tomcat.sessions.rejected").tags(tags).functionCounter().count()).isEqualTo(1.0);
        assertThat(registry.mustFind("tomcat.sessions.created").tags(tags).functionCounter().count()).isEqualTo(3.0);
        assertThat(registry.mustFind("tomcat.sessions.alive.max").tags(tags).timeGauge().value()).isGreaterThan(1.0);
    }

    @Test
    void mbeansAvailableAfterBinder() throws LifecycleException, InterruptedException {
        TomcatMetrics.monitor(registry, null);

        CountDownLatch latch = new CountDownLatch(1);
        registry.config().onMeterAdded(m -> {
            if(m.getId().getName().equals("tomcat.global.received"))
                latch.countDown();
        });

        Tomcat server = new Tomcat();
        try {
            StandardHost host = new StandardHost();
            host.setName("localhost");
            server.setHost(host);
            server.setPort(61000);
            server.start();

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

            registry.find("tomcat.global.received").functionCounter();
        } finally {
            server.stop();
            server.destroy();
        }
    }

    @Test
    void mbeansAvailableBeforeBinder() throws LifecycleException {
        Tomcat server = new Tomcat();
        try {
            StandardHost host = new StandardHost();
            host.setName("localhost");
            server.setHost(host);
            server.setPort(61000);
            server.start();

            TomcatMetrics.monitor(registry, null);
            registry.find("tomcat.global.received").functionCounter();
        } finally {
            server.stop();
            server.destroy();
        }
    }

}