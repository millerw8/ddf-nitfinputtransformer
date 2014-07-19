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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
// import java.util.Date;
// 
// import javax.xml.bind.JAXBException;
// 
import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.CatalogFramework;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
// import ddf.geo.formatter.CompositeGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.codice.nitf.filereader.ImageCoordinates;
import org.codice.nitf.filereader.ImageCoordinatesRepresentation;
import org.codice.nitf.filereader.NitfHeaderReader;
import org.codice.nitf.filereader.NitfImageSegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Converts NITF images into a Metacard.
 *
 */
public class NitfInputTransformer implements InputTransformer {

    private static final String ID = "nitf";
    private static final String MIME_TYPE = "image/nitf";

    private static final Logger LOGGER = LoggerFactory.getLogger(NitfInputTransformer.class);
    private CatalogFramework mCatalog;

    /**
     * Transforms NITF images into a {@link Metacard}
     */
    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        LOGGER.info("NitfInputTransformer transform(input)");
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id) throws IOException, CatalogTransformerException {
        LOGGER.info("NitfInputTransformer transform(input, id)");
        if (input == null) {
            throw new CatalogTransformerException("Cannot transform null input.");
        }

        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        try {
            NitfHeaderReader nitfFile = new NitfHeaderReader(input);
            metacard.setCreatedDate(nitfFile.getFileDateTime());
            // TODO: modified date from HISTOA?
            metacard.setTitle(nitfFile.getFileTitle());
            setLocation(nitfFile, metacard);
            // TODO: read more metadata here and dump into metacard
            metacard.setMetadata("<xml>NITF HACK</xml>");
            if (id != null) {
                metacard.setId(id);
            } else {
                metacard.setId(null);
            }

            metacard.setContentTypeName(MIME_TYPE);
        } catch (ParseException e) {
            LOGGER.warn("ParseException processing NITF file", e);
            throw new CatalogTransformerException(e);
        }

        return metacard;
    }

    private void setLocation(NitfHeaderReader nitfFile, MetacardImpl metacard) {
        GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(com.vividsolutions.jts.geom.PrecisionModel.FLOATING), 4326);
        if (nitfFile.getNumberOfImageSegments() < 1) {
            return;
        }
        if (nitfFile.getNumberOfImageSegments() == 1) {
            NitfImageSegment segment = nitfFile.getImageSegmentZeroBase(0);
            if (segment == null) {
                return;
            }
            // TODO: add more coordinate support
            if ((segment.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.GEOGRAPHIC) ||
                (segment.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.DECIMALDEGREES)) {
                Coordinate coords[] = new Coordinate[5];
                ImageCoordinates imageCoordinates = segment.getImageCoordinates();
                coords[0] = new Coordinate(imageCoordinates.getCoordinate00().getLongitude(), imageCoordinates.getCoordinate00().getLatitude());
                coords[4] = new Coordinate(coords[0]);
                coords[1] = new Coordinate(imageCoordinates.getCoordinate0MaxCol().getLongitude(), imageCoordinates.getCoordinate0MaxCol().getLatitude());
                coords[2] = new Coordinate(imageCoordinates.getCoordinateMaxRowMaxCol().getLongitude(), imageCoordinates.getCoordinateMaxRowMaxCol().getLatitude());
                coords[3] = new Coordinate(imageCoordinates.getCoordinateMaxRow0().getLongitude(), imageCoordinates.getCoordinateMaxRow0().getLatitude());
                LinearRing externalRing = geomFactory.createLinearRing(coords);
                System.out.println("LinearRing:" + externalRing.toText());
                Polygon polygon = geomFactory.createPolygon(externalRing, null);
                System.out.println("Polygon:" + polygon.toText());
                metacard.setLocation(polygon.toText());
            }
        } else {
            // TODO create multipolygon
            for (int i = 0; i < nitfFile.getNumberOfImageSegments(); ++i) {
                NitfImageSegment segment = nitfFile.getImageSegmentZeroBase(i);
                // TODO: add this polygon to the multipolygon
            }
            // TODO: turn multipolygon into WKT
        }
    }

    @Override
    public String toString() {
        return "InputTransformer {Impl=" + this.getClass().getName() + ", id=" + ID + ", mime-type=" + MIME_TYPE + "}";
    }

    public void setCatalog(CatalogFramework catalog) {
        LOGGER.info("NitfInputTransformer setCatalog()");
        this.mCatalog = catalog;
    }

}
