package net.frogmouth.ddf.nitfinputtransformer;

import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class NitfMetacardType extends MetacardTypeImpl {

    public static final String NAME = "nitf";

    public static final String NITF_VERSION = "version";

    public static final String FILE_DATE_TIME = "fileDateTime";

    public static final String FILE_TITLE = "fileTitle";

    /* File Size in Bytes*/
    public static final String FILE_SIZE = "fileSize";

    public static final String COMPLEXITY_LEVEL = "complexityLevel";

    public static final String ORIGINATOR_NAME = "originatorName";

    public static final String ORIGINATING_STATION_ID = "originatingStationId";

    public static final String IMAGE_ID = "imageId";

    public static final String ISOURCE = "isource";

    public static final String NUMBER_OF_ROWS = "numberOfRows";

    public static final String NUMBER_OF_COLUMNS = "numberOfColumns";

    public static final String NUMBER_OF_BANDS = "numberOfBands";

    public static final String NUMBER_OF_MULTISPECTRAL_BANDS = "numberOfMultispectralBands";

    public static final String REPRESENTATION = "representation";

    public static final String SUBCATEGORY = "subcategory";

    public static final String BITS_PER_PIXEL_PER_BAND = "bitsPerPixelPerBand";

    public static final String IMAGE_MODE = "imageMode";

    public static final String COMPRESSION = "compression";

    public static final String RATE_CODE = "rateCode";

    public static final String TARGET_ID = "targetId";

    public static final String COMMENT = "comment";

    /* NITF Security */
    public static final String CODE_WORDS = "codeWords";

    public static final String CONTROL_CODE = "controlCode";

    public static final String RELEASE_INSTRUCTION = "releaseInstruction";

    public static final String CONTROL_NUMBER = "controlNumber";

    public static final String CLASSIFICATION_SYSTEM = "system";

    public static final String CLASSIFICATION_AUTHORITY = "authority";

    public static final String CLASSIFICATION_AUTHORITY_TYPE = "authorityType";

    public static final String CLASSIFICATION_TEXT = "text";

    public static final String CLASSIFICATION_REASON = "reason";

    public static final String CLASSIFICATION_DATE = "classificationDate";

    public static final String DECLASSIFICATION_TYPE = "declassificationType";

    public static final String DECLASSIFICATION_DATE = "declassificationDate";

    public static final String SECURITY = "";

    public NitfMetacardType() {
        super(NAME, null);
        descriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        addNitfDescriptors();
    }

    private void addNitfDescriptors() {
        descriptors.add(new AttributeDescriptorImpl(NITF_VERSION, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(FILE_DATE_TIME, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.DATE_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(FILE_TITLE, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(FILE_SIZE, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl(COMPLEXITY_LEVEL, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(ORIGINATOR_NAME, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(ORIGINATING_STATION_ID, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(IMAGE_ID, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(ISOURCE, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(NUMBER_OF_ROWS, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.LONG_TYPE));
        descriptors.add(new AttributeDescriptorImpl(NUMBER_OF_COLUMNS, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.LONG_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(NUMBER_OF_MULTISPECTRAL_BANDS, true /* indexed */, true /* stored */, false /* tokenized */,
                        true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(BITS_PER_PIXEL_PER_BAND, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.INTEGER_TYPE));
        descriptors.add(new AttributeDescriptorImpl(REPRESENTATION, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(SUBCATEGORY, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(IMAGE_MODE, true /* indexed */, true /* stored */, false /* tokenized */,
                        true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(COMPRESSION, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(RATE_CODE, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(TARGET_ID, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(COMMENT, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(CODE_WORDS, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(CONTROL_CODE, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(RELEASE_INSTRUCTION, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(CONTROL_NUMBER, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(CLASSIFICATION_SYSTEM, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(CLASSIFICATION_AUTHORITY, true /* indexed */, true /* stored */,
                        false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors
                .add(new AttributeDescriptorImpl(CLASSIFICATION_AUTHORITY_TYPE, true /* indexed */,
                        true /* stored */, false /* tokenized */, true /* multivalued */,
                        BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(CLASSIFICATION_TEXT, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(CLASSIFICATION_REASON, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(CLASSIFICATION_DATE, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.DATE_TYPE));
        descriptors.add(new AttributeDescriptorImpl(DECLASSIFICATION_TYPE, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(DECLASSIFICATION_DATE, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
        descriptors.add(new AttributeDescriptorImpl(SECURITY, true /* indexed */, true /* stored */,
                false /* tokenized */, true /* multivalued */, BasicTypes.STRING_TYPE));
    }

}
