package org.esa.beam.dataio.bigtiff;

import org.esa.beam.framework.dataio.EncodeQualification;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.io.BeamFileFilter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.*;

public class BigGeoTiffProductWriterPlugInTest {

    private BigGeoTiffProductWriterPlugIn plugIn;

    @Before
    public void setUp() {
        plugIn = new BigGeoTiffProductWriterPlugIn();
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertArrayEquals(new String[]{"BigGeoTiff"}, formatNames);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = plugIn.getDefaultFileExtensions();
        assertArrayEquals(new String[]{".tif", ".tiff"}, defaultFileExtensions);
    }

    @Test
    public void testGetOutputTypes() {
        final Class[] outputTypes = plugIn.getOutputTypes();
        assertArrayEquals(new Class[]{String.class, File.class,}, outputTypes);
    }

    @Test
    public void testGetDescription() {
        assertEquals("BigGeoTiff/GeoTiff data product.", plugIn.getDescription(null));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

    @Test
    public void testCreateWriterInstance() {
        final ProductWriter writer = plugIn.createWriterInstance();

        assertNotNull(writer);
        assertTrue(writer instanceof BigGeoTiffProductWriter);
    }

    @Test
    public void testEncodingQualification() throws Exception {
        Product product = new Product("N", "T", 2, 2);

        EncodeQualification encodeQualification = plugIn.getEncodeQualification(product);
        assertNotNull(encodeQualification);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
        assertNotNull(encodeQualification.getInfoString());

        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 1, 1, new float[4]);
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 1, 1, new float[4]);
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
        assertNotNull(encodeQualification.getInfoString());

        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2, 2, 0, 0, 1, 1));
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.FULL, encodeQualification.getPreservation());
    }

}
