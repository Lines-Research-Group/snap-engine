/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.dataio.geotiff.internal;

import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * An abstract TIFF value implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public abstract class TiffValue {

    private ProductData data;

    protected void setData(final ProductData data) {
        this.data = data;
    }

    protected ProductData getData() {
        return data;
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (data == null) {
            throw new IllegalStateException("the value has no data to write");
        }
        data.writeTo(ios);
    }

    public int getSizeInBytes() {
        return data.getElemSize() * data.getNumElems();
    }
}
