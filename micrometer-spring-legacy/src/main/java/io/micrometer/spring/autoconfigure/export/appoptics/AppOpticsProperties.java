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
package io.micrometer.spring.autoconfigure.export.appoptics;

import io.micrometer.spring.autoconfigure.export.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring AppOptics metrics export.
 *
 * @author Hunter Sherman
 */
@ConfigurationProperties(prefix = "management.metrics.export.appoptics")
public class AppOpticsProperties extends StepRegistryProperties {

    /**
     * AppOptics API token.
     */
    public String token;

    /**
     * The tag that will be mapped to "@host" when shipping metrics to AppOptics, or {@code null} if
     * "@host" should be omitted on publishing.
     */
    public String hostTag;

    /**
     * The URI to ship metrics to.
     */
    public String uri;

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String getHostTag() {
        return hostTag;
    }

    public void setHostTag(String hostTag) {
        this.hostTag = hostTag;
    }

    public String getUri() { return uri; }

    public void setUri(String uri) { this.uri = uri; }
}
