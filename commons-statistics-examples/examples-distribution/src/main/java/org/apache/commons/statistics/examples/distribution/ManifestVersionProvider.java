/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.statistics.examples.distribution;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import picocli.CommandLine.IVersionProvider;

/**
 * {@link IVersionProvider} implementation that returns version information from
 * the package jar file's {@code /META-INF/MANIFEST.MF} file.
 *
 * @see <a
 * href="https://github.com/remkop/picocli/blob/master/picocli-examples/src/main/java/picocli/examples/VersionProviderDemo2.java">PicoCLI
 * version provider demo</a>
 */
class ManifestVersionProvider implements IVersionProvider {
    /** The manifest key for the implementation title. */
    private static final String KEY = "Implementation-Title";

    /** {@inheritDoc} */
    @Override
    public String[] getVersion() throws Exception {
        final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                                           .getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            try (InputStream stream = url.openStream()) {
                final Manifest manifest = new Manifest(stream);
                if (isApplicableManifest(manifest)) {
                    final Attributes attr = manifest.getMainAttributes();
                    return new String[] {get(attr, KEY) + " version \"" +
                                         get(attr, "Implementation-Version") + "\""};
                }
            } catch (final IOException ex) {
                return new String[] {"Unable to read from " + url + ". " + ex};
            }
        }
        return new String[0];
    }

    /**
     * Checks if this is the applicable manifest for the package.
     *
     * @param manifest The manifest.
     * @return true if is the applicable manifest
     */
    private static boolean isApplicableManifest(Manifest manifest) {
        final Attributes attributes = manifest.getMainAttributes();
        return "Apache Commons Statistics Distribution Utilities".equals(get(attributes, KEY));
    }

    /**
     * Gets the named object from the attributes using the key.
     *
     * @param attributes The attributes.
     * @param key The key.
     * @return the object
     */
    private static Object get(Attributes attributes, String key) {
        return attributes.get(new Attributes.Name(key));
    }
}
