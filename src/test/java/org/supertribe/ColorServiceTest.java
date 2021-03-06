/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.supertribe;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.openejb.loader.JarLocation;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.sabot.Config;
import org.tomitribe.util.editor.Converter;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static java.util.Collections.singletonList;

/**
 * Arquillian will start the container, deploy all @Deployment bundles, then run all the @Test methods.
 *
 * A strong value-add for Arquillian is that the test is abstracted from the server.
 * It is possible to rerun the same test against multiple adapters or server configurations.
 *
 * A second value-add is it is possible to build WebArchives that are slim and trim and therefore
 * isolate the functionality being tested.  This also makes it easier to swap out one implementation
 * of a class for another allowing for easy mocking.
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ColorServiceTest extends Assert {

    /**
     * ShrinkWrap is used to create a war file on the fly.
     *
     * The API is quite expressive and can build any possible
     * flavor of war file.  It can quite easily return a rebuilt
     * war file as well.
     *
     * More than one @Deployment method is allowed.
     */
    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "color.war")
                .addClasses(ColorService.class, Color.class)
                .addAsLibraries(JarLocation.jarLocation(Config.class))
                .addAsLibraries(JarLocation.jarLocation(Converter.class))
                .addAsResource(new ClassLoaderAsset("dev.properties"), "dev.properties")
                .setWebXML(new File("src/main/webapp/WEB-INF/web.xml"));
    }

    /**
     * This URL will contain the following URL data
     *
     *  - http://<host>:<port>/<webapp>/
     *
     * This allows the test itself to be agnostic of server information or even
     * the name of the webapp
     *
     */
    @ArquillianResource
    private URL webappUrl;

    @Test
    public void postAndGet() throws Exception {
        final WebClient webClient = $(WebClient.create(webappUrl.toExternalForm().replace("http", "https").replace("8080", "8443"), singletonList(new JSONProvider()), "snoopy", "pass", null));

        // POST
        {
            final Response response = webClient.path("color/green").post(null);

            assertEquals(204, response.getStatus());
        }

        // GET
        {
            final Response response = webClient.reset().path("color").accept(MediaType.APPLICATION_JSON).get();

            assertEquals(200, response.getStatus());

            final String content = slurp((InputStream) response.getEntity());

            assertEquals("green", content);
        }

    }

    @Test
    public void getColorObject() throws Exception {

        final WebClient webClient = $(WebClient.create(webappUrl.toURI().toASCIIString(), singletonList(new JSONProvider()), "snoopy", "pass", null));

        final Color color = webClient.path("color/object").accept(MediaType.APPLICATION_JSON).get(Color.class);

        assertNotNull(color);
        assertEquals("blue", color.getName());
        assertEquals(0, color.getR());
        assertEquals(0, color.getG());
        assertEquals(255, color.getB());
    }

    /**
     * Reusable utility method
     * Move to a shared class or replace with equivalent
     */
    public static String slurp(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        out.flush();
        return new String(out.toByteArray());
    }

    /**
     * Utility method that allows us to test with self-signed certs for https access
     */
    private WebClient $(final WebClient webClient) {
        final HTTPConduit httpConduit = WebClient.getConfig(webClient).getHttpConduit();
        httpConduit.getClient().setAutoRedirect(true);

        final TLSClientParameters params = new TLSClientParameters();
        params.setDisableCNCheck(true);
        params.setTrustManagers(new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
                // no-op
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
                // no-op
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }});
        httpConduit.setTlsClientParameters(params);

        return webClient;
    }
}
