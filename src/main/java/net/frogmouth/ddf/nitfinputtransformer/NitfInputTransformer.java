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
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;

import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.CatalogFramework;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.codice.nitf.filereader.ImageCoordinates;
import org.codice.nitf.filereader.ImageCoordinatesRepresentation;
import org.codice.nitf.filereader.NitfFile;
import org.codice.nitf.filereader.NitfFileFactory;
import org.codice.nitf.filereader.NitfFileSecurityMetadata;
import org.codice.nitf.filereader.NitfImageSegment;
import org.codice.nitf.filereader.NitfSecurityMetadata;
import org.codice.nitf.filereader.Tre;
import org.codice.nitf.filereader.TreCollection;
import org.codice.nitf.filereader.TreEntry;
import org.codice.nitf.filereader.TreGroup;

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
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id) throws IOException, CatalogTransformerException {
        if (input == null) {
            throw new CatalogTransformerException("Cannot transform null input.");
        }

        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        try {
            NitfFile nitfFile = NitfFileFactory.parse(input);
            metacard.setCreatedDate(nitfFile.getFileDateTime());
            // TODO: modified date from HISTOA?
            metacard.setTitle(nitfFile.getFileTitle());

            setLocation(nitfFile, metacard);

            setMetadata(nitfFile, metacard);

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

    private void setLocation(NitfFile nitfFile, MetacardImpl metacard) {
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
            // TODO: handle case where its really a point.
            if ((segment.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.GEOGRAPHIC) ||
                (segment.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.DECIMALDEGREES)) {
                Polygon polygon = getPolygonForSegment(segment, geomFactory);
                metacard.setLocation(polygon.toText());
            } else if (segment.getImageCoordinatesRepresentation() != ImageCoordinatesRepresentation.NONE) {
                System.out.println("Unsupported representation:" + segment.getImageCoordinatesRepresentation());
            }
        } else {
            ArrayList<Polygon> polygons = new ArrayList<Polygon>();
            for (int i = 0; i < nitfFile.getNumberOfImageSegments(); ++i) {
                NitfImageSegment segment = nitfFile.getImageSegmentZeroBase(i);
                if ((segment.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.GEOGRAPHIC) ||
                    (segment.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.DECIMALDEGREES)) {
                    polygons.add(getPolygonForSegment(segment, geomFactory));
                } else if (segment.getImageCoordinatesRepresentation() != ImageCoordinatesRepresentation.NONE) {
                    System.out.println("Unsupported representation:" + segment.getImageCoordinatesRepresentation());
                }
            }
            Polygon[] polyAry = polygons.toArray(new Polygon[0]);
            MultiPolygon multiPolygon = geomFactory.createMultiPolygon(polyAry);
            metacard.setLocation(multiPolygon.toText());
        }
    }

    private Polygon getPolygonForSegment(NitfImageSegment segment, GeometryFactory geomFactory) {
        Coordinate[] coords = new Coordinate[5];
        ImageCoordinates imageCoordinates = segment.getImageCoordinates();
        coords[0] = new Coordinate(imageCoordinates.getCoordinate00().getLongitude(), imageCoordinates.getCoordinate00().getLatitude());
        coords[4] = new Coordinate(coords[0]);
        coords[1] = new Coordinate(imageCoordinates.getCoordinate0MaxCol().getLongitude(), imageCoordinates.getCoordinate0MaxCol().getLatitude());
        coords[2] = new Coordinate(imageCoordinates.getCoordinateMaxRowMaxCol().getLongitude(), imageCoordinates.getCoordinateMaxRowMaxCol().getLatitude());
        coords[3] = new Coordinate(imageCoordinates.getCoordinateMaxRow0().getLongitude(), imageCoordinates.getCoordinateMaxRow0().getLatitude());
        LinearRing externalRing = geomFactory.createLinearRing(coords);
        return geomFactory.createPolygon(externalRing, null);
    }

    private void setMetadata(NitfFile nitfFile, MetacardImpl metacard) {
        StringBuilder metadataXml = new StringBuilder();
        metadataXml.append("<metadata>\n");
        metadataXml.append("  <file>\n");
        metadataXml.append(buildMetadataEntry("fileType", nitfFile.getFileType().toString()));
        metadataXml.append(buildMetadataEntry("complexityLevel", nitfFile.getComplexityLevel()));
        metadataXml.append(buildMetadataEntry("originatingStationId", nitfFile.getOriginatingStationId()));
        metadataXml.append(buildMetadataEntry("fileDateTime", nitfFile.getFileDateTime()));
        metadataXml.append(buildMetadataEntry("fileTitle", nitfFile.getFileTitle()));
        addFileSecurityMetadata(metadataXml, nitfFile);
        // TODO: FBKGC
        metadataXml.append(buildMetadataEntry("originatorsName", nitfFile.getOriginatorsName()));
        metadataXml.append(buildMetadataEntry("originatorsPhoneNumber", nitfFile.getOriginatorsPhoneNumber()));
        metadataXml.append(buildTREsMetadata(nitfFile.getTREsRawStructure()));
        metadataXml.append("  </file>\n");
        for (int i = 0; i < nitfFile.getNumberOfImageSegments(); ++i) {
            NitfImageSegment image = nitfFile.getImageSegmentZeroBase(i);
            metadataXml.append("  <image>\n");
            metadataXml.append(buildMetadataEntry("imageIdentifer1", image.getIdentifier()));
            metadataXml.append(buildMetadataEntry("imageDateTime", image.getImageDateTime()));
            metadataXml.append(buildMetadataEntry("imageTargetId", image.getImageTargetId()));
            metadataXml.append(buildMetadataEntry("imageIdentifer2", image.getImageIdentifier2()));
            addSecurityMetadata(metadataXml, image.getSecurityMetadata());
            metadataXml.append(buildMetadataEntry("imageSource", image.getImageSource()));
            metadataXml.append(buildMetadataEntry("numberOfRows", image.getNumberOfRows()));
            metadataXml.append(buildMetadataEntry("numberOfColumns", image.getNumberOfColumns()));
            metadataXml.append(buildMetadataEntry("pixelValueType", image.getPixelValueType().toString()));
            metadataXml.append(buildMetadataEntry("imageRepresentation", image.getImageRepresentation().toString()));
            metadataXml.append(buildMetadataEntry("imageCategory", image.getImageCategory().toString()));
            metadataXml.append(buildMetadataEntry("actualBitsPerPixelPerBand", image.getActualBitsPerPixelPerBand()));
            metadataXml.append(buildMetadataEntry("pixelJustification", image.getPixelJustification().toString()));
            metadataXml.append(buildMetadataEntry("imageCoordinatesRepresentation", image.getImageCoordinatesRepresentation().toString()));
            for (int j = 0; j < image.getNumberOfImageComments(); ++j) {
                metadataXml.append(buildMetadataEntry("imageComment", image.getImageCommentZeroBase(j)));
            }
            metadataXml.append(buildMetadataEntry("imageCompression", image.getImageCompression().toString()));
            metadataXml.append(buildMetadataEntry("compressionRate", image.getCompressionRate()));
            metadataXml.append(buildMetadataEntry("imageMode", image.getImageMode().toString()));
            // TODO: output rest of image metadata
            // TODO: output TREs for each image
            metadataXml.append("  </image>\n");
        }
        // TODO: data and TREs for graphic, symbol, label, text
        metadataXml.append("</metadata>\n");
        metacard.setMetadata(metadataXml.toString());
    }

    private String buildTREsMetadata(TreCollection treCollection) {
        StringBuilder treXml = new StringBuilder();
        for (Tre tre : treCollection.getTREs()) {
            outputThisTre(treXml, tre);
        }
        return treXml.toString();
    }

    private static void outputThisTre(StringBuilder treXml, Tre tre) {
        treXml.append("    <tre name=\"" + tre.getName().trim() + "\">\n");
        for (TreEntry entry : tre.getEntries()) {
            outputThisEntry(treXml, entry, 2);
        }
        treXml.append("    </tre>\n");
    }

    private static void doIndent(StringBuilder treXml, int indentLevel) {
        for (int i = 0; i < indentLevel; ++i) {
            treXml.append("  ");
        }
    }

    private static void outputThisEntry(StringBuilder treXml, TreEntry entry, int indentLevel) {
        if (entry.getFieldValue() != null) {
            doIndent(treXml, indentLevel);
            treXml.append("<field name=\"" + entry.getName() + "\" value=\"" + entry.getFieldValue() + "\" />\n");
        }
        if ((entry.getGroups() != null) && (entry.getGroups().size() > 0)) {
            doIndent(treXml, indentLevel);
            treXml.append("<repeated name=\"" + entry.getName() + "\" number=\"" + entry.getGroups().size() + "\">\n");
            int i = 0;
            for (TreGroup group : entry.getGroups()) {
                doIndent(treXml, indentLevel + 1);
                treXml.append(String.format("<group index=\"%d\">\n", i));
                for (TreEntry groupEntry : group.getEntries()) {
                    outputThisEntry(treXml, groupEntry, indentLevel + 2);
                }
                doIndent(treXml, indentLevel + 1);
                treXml.append(String.format("</group>\n"));
                i = i + 1;
            }
            doIndent(treXml, indentLevel);
            treXml.append("</repeated>\n");
        }
    }

    private String buildMetadataEntry(String label, int value) {
        return buildMetadataEntry(label, Integer.toString(value));
    }

    private String buildMetadataEntry(String label, long value) {
        return buildMetadataEntry(label, Long.toString(value));
    }

    private String buildMetadataEntry(String label, Date value) {
        return buildMetadataEntry(label, value.toString());
    }

    private String buildMetadataEntry(String label, String value) {
        StringBuilder entryBuilder = new StringBuilder();
        entryBuilder.append("    <");
        entryBuilder.append(label);
        entryBuilder.append(">");
        entryBuilder.append(StringEscapeUtils.escapeXml(value));
        entryBuilder.append("</");
        entryBuilder.append(label);
        entryBuilder.append(">\n");
        return entryBuilder.toString();
    }

    private void addFileSecurityMetadata(StringBuilder metadataXml, NitfFile nitfFile) {
        NitfFileSecurityMetadata security = nitfFile.getFileSecurityMetadata();
        addSecurityMetadata(metadataXml, security);
        // TODO: add FSCOP / FSCPYN values.
    }

    private void addSecurityMetadata(StringBuilder metadataXml, NitfSecurityMetadata security) {
        metadataXml.append(buildMetadataEntry("securityClassification", security.getSecurityClassification().toString()));
        // TODO: security classifications system if NITF 2.1/NSIF 1.0.
        metadataXml.append(buildMetadataEntry("securityCodewords", security.getCodewords()));
        // TODO: add rest of security fields, checking which ones are valid.
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
