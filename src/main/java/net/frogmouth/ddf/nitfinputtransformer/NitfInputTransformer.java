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
import java.util.List;

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

import org.codice.imaging.nitf.core.ImageCoordinates;
import org.codice.imaging.nitf.core.ImageCoordinatesRepresentation;
import org.codice.imaging.nitf.core.NitfDateTime;
import org.codice.imaging.nitf.core.Nitf;
import org.codice.imaging.nitf.core.NitfFileFactory;
import org.codice.imaging.nitf.core.NitfFileSecurityMetadata;
import org.codice.imaging.nitf.core.NitfLabelSegment;
import org.codice.imaging.nitf.core.NitfGraphicSegment;
import org.codice.imaging.nitf.core.NitfImageSegment;
import org.codice.imaging.nitf.core.NitfSecurityMetadata;
import org.codice.imaging.nitf.core.NitfSymbolSegment;
import org.codice.imaging.nitf.core.NitfTextSegment;
import org.codice.imaging.nitf.core.Tre;
import org.codice.imaging.nitf.core.TreCollection;
import org.codice.imaging.nitf.core.TreEntry;
import org.codice.imaging.nitf.core.TreGroup;

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
            Nitf nitfFile = NitfFileFactory.parseHeadersOnly(input);
            metacard.setCreatedDate(nitfFile.getFileDateTime().toDate());
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

    private void setLocation(Nitf nitfFile, MetacardImpl metacard) {
        GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(com.vividsolutions.jts.geom.PrecisionModel.FLOATING), 4326);
        if (nitfFile.getImageSegments().isEmpty()) {
            return;
        }
        if (nitfFile.getImageSegments().size() == 1) {
            NitfImageSegment segment = nitfFile.getImageSegments().get(0);
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
            List<Polygon> polygons = new ArrayList<Polygon>();
            for (NitfImageSegment segment : nitfFile.getImageSegments()) {
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

    private void setMetadata(Nitf nitfFile, MetacardImpl metacard) {
        StringBuilder metadataXml = new StringBuilder();
        metadataXml.append("<metadata>\n");
        metadataXml.append("  <file>\n");
        metadataXml.append(buildMetadataEntry("fileType", nitfFile.getFileType().toString()));
        metadataXml.append(buildMetadataEntry("complexityLevel", nitfFile.getComplexityLevel()));
        metadataXml.append(buildMetadataEntry("originatingStationId", nitfFile.getOriginatingStationId()));
        metadataXml.append(buildMetadataEntry("fileDateTime", nitfFile.getFileDateTime()));
        metadataXml.append(buildMetadataEntry("fileTitle", nitfFile.getFileTitle()));
        addFileSecurityMetadata(metadataXml, nitfFile);
        if (nitfFile.getFileBackgroundColour() != null) {
            metadataXml.append(buildMetadataEntry("fileBackgroundColour", nitfFile.getFileBackgroundColour().toString()));
        }
        metadataXml.append(buildMetadataEntry("originatorsName", nitfFile.getOriginatorsName()));
        metadataXml.append(buildMetadataEntry("originatorsPhoneNumber", nitfFile.getOriginatorsPhoneNumber()));
        metadataXml.append(buildTREsMetadata(nitfFile.getTREsRawStructure()));
        metadataXml.append("  </file>\n");
        for (NitfImageSegment image : nitfFile.getImageSegments()) {
            metadataXml.append("  <image>\n");
            metadataXml.append(buildMetadataEntry("imageIdentifer1", image.getIdentifier()));
            metadataXml.append(buildMetadataEntry("imageDateTime", image.getImageDateTime()));
            metadataXml.append(buildMetadataEntry("imageBasicEncyclopediaNumber", image.getImageTargetId().getBasicEncyclopediaNumber().trim()));
            metadataXml.append(buildMetadataEntry("imageOSuffix", image.getImageTargetId().getOSuffix().trim()));
            metadataXml.append(buildMetadataEntry("imageCountryCode", image.getImageTargetId().getCountryCode().trim()));
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
            for (String comment : image.getImageComments()) {
                metadataXml.append(buildMetadataEntry("imageComment", comment));
            }
            metadataXml.append(buildMetadataEntry("imageCompression", image.getImageCompression().toString()));
            metadataXml.append(buildMetadataEntry("compressionRate", image.getCompressionRate()));
            metadataXml.append(buildMetadataEntry("imageMode", image.getImageMode().toString()));
            metadataXml.append(buildMetadataEntry("numberOfBlocksPerRow", image.getNumberOfBlocksPerRow()));
            metadataXml.append(buildMetadataEntry("numberOfBlocksPerColumn", image.getNumberOfBlocksPerColumn()));
            metadataXml.append(buildMetadataEntry("numberOfPixelsPerBlockHorizontal", image.getNumberOfPixelsPerBlockHorizontal()));
            metadataXml.append(buildMetadataEntry("numberOfPixelsPerBlockVertical", image.getNumberOfPixelsPerBlockVertical()));
            metadataXml.append(buildMetadataEntry("numberOfBitsPerPixelPerBand", image.getNumberOfBitsPerPixelPerBand()));
            metadataXml.append(buildMetadataEntry("imageDisplayLevel", image.getImageDisplayLevel()));
            metadataXml.append(buildMetadataEntry("imageAttachmentLevel", image.getAttachmentLevel()));
            metadataXml.append(buildMetadataEntry("imageLocationRow", image.getImageLocationRow()));
            metadataXml.append(buildMetadataEntry("imageLocationColumn", image.getImageLocationColumn()));
            if (image.getImageMagnification() != null) {
                metadataXml.append(buildMetadataEntry("imageMagnification", image.getImageMagnification()));
            }
            if (image.getImageCoordinates() != null) {
                metadataXml.append(buildMetadataEntry("imageCoordinates", image.getImageCoordinates().toString()));
            }
            metadataXml.append(buildTREsMetadata(image.getTREsRawStructure()));
            metadataXml.append("  </image>\n");
        }
        for (NitfGraphicSegment graphic : nitfFile.getGraphicSegments()) {
            metadataXml.append("  <graphic>\n");
            metadataXml.append(buildMetadataEntry("graphicIdentifier", graphic.getIdentifier()));
            metadataXml.append(buildMetadataEntry("graphicName", graphic.getGraphicName()));
            addSecurityMetadata(metadataXml, graphic.getSecurityMetadata());
            metadataXml.append(buildMetadataEntry("graphicDisplayLevel", graphic.getGraphicDisplayLevel()));
            metadataXml.append(buildMetadataEntry("graphicAttachmentLevel", graphic.getAttachmentLevel()));
            metadataXml.append(buildMetadataEntry("graphicLocationRow", graphic.getGraphicLocationRow()));
            metadataXml.append(buildMetadataEntry("graphicLocationColumn", graphic.getGraphicLocationColumn()));
            metadataXml.append(buildMetadataEntry("graphicBoundingBox1Row", graphic.getBoundingBox1Row()));
            metadataXml.append(buildMetadataEntry("graphicBoundingBox1Column", graphic.getBoundingBox1Column()));
            metadataXml.append(buildMetadataEntry("graphicBoundingBox2Row", graphic.getBoundingBox2Row()));
            metadataXml.append(buildMetadataEntry("graphicBoundingBox2Column", graphic.getBoundingBox2Column()));
            metadataXml.append(buildMetadataEntry("graphicColour", graphic.getGraphicColour().toString()));
            metadataXml.append(buildTREsMetadata(graphic.getTREsRawStructure()));
            metadataXml.append("  </graphic>\n");
        }
        for (NitfSymbolSegment symbol : nitfFile.getSymbolSegments()) {
            metadataXml.append("  <symbol>\n");
            metadataXml.append(buildMetadataEntry("symbolIdentifier", symbol.getIdentifier()));
            metadataXml.append(buildMetadataEntry("symbolName", symbol.getSymbolName()));
            addSecurityMetadata(metadataXml, symbol.getSecurityMetadata());
            metadataXml.append(buildMetadataEntry("symbolType", symbol.getSymbolType().toString()));
            metadataXml.append(buildMetadataEntry("symbolColour", symbol.getSymbolColour().toString()));
            metadataXml.append(buildMetadataEntry("numberOfLinesPerSymbol", symbol.getNumberOfLinesPerSymbol()));
            metadataXml.append(buildMetadataEntry("numberOfPixelsPerLine", symbol.getNumberOfPixelsPerLine()));
            metadataXml.append(buildMetadataEntry("lineWidth", symbol.getLineWidth()));
            metadataXml.append(buildMetadataEntry("numberOfBitsPerPixel", symbol.getNumberOfBitsPerPixel()));
            metadataXml.append(buildMetadataEntry("symbolDisplayLevel", symbol.getSymbolDisplayLevel()));
            metadataXml.append(buildMetadataEntry("symbolAttachmentLevel", symbol.getAttachmentLevel()));
            metadataXml.append(buildMetadataEntry("symbolLocationRow", symbol.getSymbolLocationRow()));
            metadataXml.append(buildMetadataEntry("symbolLocationColumn", symbol.getSymbolLocationColumn()));
            metadataXml.append(buildMetadataEntry("symbolLocation2Row", symbol.getSymbolLocation2Row()));
            metadataXml.append(buildMetadataEntry("symbolLocation2Column", symbol.getSymbolLocation2Column()));
            metadataXml.append(buildMetadataEntry("symbolNumber", symbol.getSymbolNumber()));
            metadataXml.append(buildMetadataEntry("symbolRotation", symbol.getSymbolRotation()));
            metadataXml.append(buildTREsMetadata(symbol.getTREsRawStructure()));
            metadataXml.append("  </symbol>\n");
        }
        for (NitfLabelSegment label : nitfFile.getLabelSegments()) {
            metadataXml.append("  <label>\n");
            metadataXml.append(buildMetadataEntry("labelIdentifier", label.getIdentifier()));
            addSecurityMetadata(metadataXml, label.getSecurityMetadata());
            metadataXml.append(buildMetadataEntry("labelLocationRow", label.getLabelLocationRow()));
            metadataXml.append(buildMetadataEntry("labelLocationColumn", label.getLabelLocationColumn()));
            metadataXml.append(buildMetadataEntry("labelCellWidth", label.getLabelCellWidth()));
            metadataXml.append(buildMetadataEntry("labelCellHeight", label.getLabelCellHeight()));
            metadataXml.append(buildMetadataEntry("labelDisplayLevel", label.getLabelDisplayLevel()));
            metadataXml.append(buildMetadataEntry("labelAttachmentLevel", label.getAttachmentLevel()));
            metadataXml.append(buildMetadataEntry("labelTextColour", label.getLabelTextColour().toString()));
            metadataXml.append(buildMetadataEntry("labelBackgroundColour", label.getLabelBackgroundColour().toString()));
            metadataXml.append(buildTREsMetadata(label.getTREsRawStructure()));
            metadataXml.append("  </label>\n");
        }
        for (NitfTextSegment text : nitfFile.getTextSegments()) {
            metadataXml.append("  <text>\n");
            metadataXml.append(buildMetadataEntry("textIdentifier", text.getIdentifier()));
            addSecurityMetadata(metadataXml, text.getSecurityMetadata());
            metadataXml.append(buildMetadataEntry("textDateTime", text.getTextDateTime().toString()));
            metadataXml.append(buildMetadataEntry("textTitle", text.getTextTitle()));
            metadataXml.append(buildMetadataEntry("textFormat", text.getTextFormat().toString()));
            metadataXml.append(buildTREsMetadata(text.getTREsRawStructure()));
            metadataXml.append("  </text>\n");
        }
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
        if ((entry.getGroups() != null) && (!entry.getGroups().isEmpty())) {
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

    private String buildMetadataEntry(String label, NitfDateTime value) {
        return buildMetadataEntry(label, value.toDate().toString());
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

    private void addFileSecurityMetadata(StringBuilder metadataXml, Nitf nitfFile) {
        NitfFileSecurityMetadata security = nitfFile.getFileSecurityMetadata();
        addSecurityMetadata(metadataXml, security);
        metadataXml.append(buildMetadataEntry("securityFileCopyNumber", nitfFile.getFileSecurityMetadata().getFileCopyNumber()));
        metadataXml.append(buildMetadataEntry("securityFileNumberOfCopies", nitfFile.getFileSecurityMetadata().getFileNumberOfCopies()));
    }

    private void addSecurityMetadata(StringBuilder metadataXml, NitfSecurityMetadata security) {
        metadataXml.append(buildMetadataEntry("securityClassification", security.getSecurityClassification().toString()));
        addMetadataIfNotNull(metadataXml, "securityClassificationSystem", security.getSecurityClassificationSystem());
        metadataXml.append(buildMetadataEntry("securityCodewords", security.getCodewords()));
        addMetadataIfNotNull(metadataXml, "securityControlAndHandling", security.getControlAndHandling());
        addMetadataIfNotNull(metadataXml, "securityReleaseInstructions", security.getReleaseInstructions());
        addMetadataIfNotNull(metadataXml, "securityDeclassificationType", security.getDeclassificationType());
        addMetadataIfNotNull(metadataXml, "securityDeclassificationDate", security.getDeclassificationDate());
        addMetadataIfNotNull(metadataXml, "securityDeclassificationExemption", security.getDeclassificationExemption());
        addMetadataIfNotNull(metadataXml, "securityDowngrade", security.getDowngrade());
        addMetadataIfNotNull(metadataXml, "securityDowngradeDate", security.getDowngradeDate());
        addMetadataIfNotNull(metadataXml, "securityDowngradeDateOrSpecialCase", security.getDowngradeDateOrSpecialCase());
        addMetadataIfNotNull(metadataXml, "securityDowngradeEvent", security.getDowngradeEvent());
    }

    private void addMetadataIfNotNull(StringBuilder metadataXml, String label, String value) {
        if (value != null) {
            metadataXml.append(buildMetadataEntry(label, value));
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
