/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package net.frogmouth.ddf.nitfinputtransformer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.List;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.*;

public class TestBasicInputTransformer {
    private static final BundleContext context = mock(BundleContext.class);
    private static List<QualifiedMetacardType> qmtList = new ArrayList<QualifiedMetacardType>();

    public static NitfInputTransformer createTransformer() throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        NitfInputTransformer transformer = new NitfInputTransformer();
        ddf.catalog.CatalogFramework catalog = mock(ddf.catalog.CatalogFramework.class);
        when(catalog.query(any(QueryRequest.class))).thenReturn(new QueryResponseImpl(null, "sourceId"));
        transformer.setCatalog(catalog);

        return transformer;
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNullInput() throws IOException, CatalogTransformerException, UnsupportedQueryException, SourceUnavailableException, FederationException {
        createTransformer().transform(null);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadInput() throws IOException, CatalogTransformerException, UnsupportedQueryException, SourceUnavailableException, FederationException {
        createTransformer().transform(new ByteArrayInputStream("{key=".getBytes()));
    }

    @Test()
    public void testSorcerWithBE() throws IOException, CatalogTransformerException, UnsupportedQueryException, SourceUnavailableException, FederationException, ParseException  {
        Metacard metacard = createTransformer().transform(getInputStream());

        assertNotNull(metacard);

        assertNotNull(metacard.getCreatedDate());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertThat(formatter.format(metacard.getCreatedDate()), is("2014-08-17 07:22:41"));
    }

    private InputStream getInputStream() {
        final String testfile = "/WithBE.ntf";

        assertNotNull("Test file missing", getClass().getResource(testfile));
        return getClass().getResourceAsStream(testfile);
    }
}