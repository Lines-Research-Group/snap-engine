package org.esa.beam.visat.toolviews.pixelinfo;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelInfoView;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

/**
 * The tool window which displays the pixel info view.
 */
public class PixelInfoToolView extends AbstractToolView {
    public static final String ID = PixelInfoToolView.class.getName();

    private PixelInfoView pixelInfoView;
    private JCheckBox pinCheckbox;
    private PinSelectionChangedListener pinSelectionChangedListener;
    private ProductSceneView currentView;
    private HashMap<ProductSceneView, PixelInfoPPL> pixelPosListeners;

    public PixelInfoToolView() {
    }

    @Override
    public JComponent createControl() {
        pinCheckbox = new JCheckBox("Snap to selected pin");
        pinCheckbox.setName("pinCheckbox");
        pinCheckbox.setSelected(false);
        final VisatApp visatApp = VisatApp.getApp();
        pinCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pinCheckbox.isSelected()) {
                    currentView = visatApp.getSelectedProductSceneView();
                    setToSelectedPin(currentView);
                }
            }
        });

        pixelInfoView = new PixelInfoView(visatApp);
        final DisplayFilter bandDisplayValidator = new DisplayFilter(visatApp);
        pixelInfoView.setPreferredSize(new Dimension(320, 480));
        pixelInfoView.setDisplayFilter(bandDisplayValidator);
        final PropertyMap preferences = visatApp.getPreferences();
        preferences.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final String propertyName = evt.getPropertyName();
                if (PixelInfoView.PROPERTY_KEY_SHOW_ONLY_LOADED_OR_DISPLAYED_BAND_PIXEL_VALUES.equals(propertyName)) {
                    setShowOnlyLoadedBands(preferences, bandDisplayValidator);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {
                    setShowPixelPosDecimals(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)) {
                    setPixelOffsetX(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)) {
                    setPixelOffsetY(preferences);
                }
            }
        });
        setShowOnlyLoadedBands(preferences, bandDisplayValidator);
        setShowPixelPosDecimals(preferences);
        setPixelOffsetX(preferences);
        setPixelOffsetY(preferences);

        final JPanel pixelInfoViewPanel = new JPanel(new BorderLayout());
        pixelInfoViewPanel.add(pixelInfoView);
        pixelInfoViewPanel.add(pinCheckbox, BorderLayout.SOUTH);

        visatApp.addInternalFrameListener(new PixelInfoIFL());
        initOpenedFrames();

        return pixelInfoViewPanel;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() || !pixelInfoView.allDocked();
    }

    private void initOpenedFrames() {
        JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                initView((ProductSceneView) contentPane);
            }
        }
    }

    private void initView(ProductSceneView productSceneView) {
        productSceneView.addPixelPositionListener(registerPPL(productSceneView));
        final Product product = productSceneView.getProduct();
        product.addProductNodeListener(getOrCreatePinSelectionChangedListener());
        if (isSnapToPin()) {
            setToSelectedPin(productSceneView);
        }
        if (productSceneView == VisatApp.getApp().getSelectedProductSceneView()) {
            currentView = productSceneView;
        }
    }

    private void setPixelOffsetY(final PropertyMap preferences) {
        pixelInfoView.setPixelOffsetY((float) preferences.getPropertyDouble(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY));
    }

    private void setPixelOffsetX(final PropertyMap preferences) {
        pixelInfoView.setPixelOffsetX((float) preferences.getPropertyDouble(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY));
    }

    private void setShowPixelPosDecimals(final PropertyMap preferences) {
        pixelInfoView.setShowPixelPosDecimals(preferences.getPropertyBool(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS));
    }

    private void setShowOnlyLoadedBands(final PropertyMap preferences, DisplayFilter validator) {
        final boolean showOnlyLoadedOrDisplayedBands = preferences.getPropertyBool(
                PixelInfoView.PROPERTY_KEY_SHOW_ONLY_LOADED_OR_DISPLAYED_BAND_PIXEL_VALUES,
                PixelInfoView.PROPERTY_DEFAULT_SHOW_ONLY_LOADED_OR_DISPLAYED_BAND_PIXEL_VALUES);
        validator.setShowOnlyLoadedOrDisplayedBands(showOnlyLoadedOrDisplayedBands);
    }

    private PinSelectionChangedListener getOrCreatePinSelectionChangedListener() {
        if (pinSelectionChangedListener == null) {
            pinSelectionChangedListener = new PinSelectionChangedListener();
        }
        return pinSelectionChangedListener;
    }

    private boolean isSnapToPin() {
        return pinCheckbox.isSelected();
    }

    private void setToSelectedPin(ProductSceneView sceneView) {
        if (sceneView != null) {
            final Product product = sceneView.getProduct();
            final Pin pin = product.getPinGroup().getSelectedNode();
            if (pin == null) {
                pixelInfoView.updatePixelValues(sceneView, -1, -1, 0, false);
            } else {
                final PixelPos pos = pin.getPixelPos();
                final int x = MathUtils.floorInt(pos.x);
                final int y = MathUtils.floorInt(pos.y);
                pixelInfoView.updatePixelValues(sceneView, x, y, 0, true);
            }
        }
    }

    private PixelInfoPPL registerPPL(ProductSceneView view) {
        if (pixelPosListeners == null) {
            pixelPosListeners = new HashMap<ProductSceneView, PixelInfoPPL>();
        }
        final PixelInfoPPL listener = new PixelInfoPPL(view);
        pixelPosListeners.put(view, listener);
        return listener;
    }

    private PixelInfoPPL unregisterPPL(ProductSceneView view) {
        if (pixelPosListeners != null) {
            return pixelPosListeners.remove(view);
        }
        return null;
    }

    private class PixelInfoIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                initView((ProductSceneView) content);
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                currentView = (ProductSceneView) content;
                final Product product = currentView.getProduct();
                product.addProductNodeListener(getOrCreatePinSelectionChangedListener());
                if (isSnapToPin()) {
                    setToSelectedPin(currentView);
                }
            }
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) content;
                view.removePixelPositionListener(unregisterPPL(view));
                removePinSelectionChangedListener(e);
            }
            pixelInfoView.clearProductNodeRefs();
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            removePinSelectionChangedListener(e);
        }

        private void removePinSelectionChangedListener(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) content;
                final Product product = view.getProduct();
                product.removeProductNodeListener(getOrCreatePinSelectionChangedListener());
                if (isSnapToPin()) {
                    pixelInfoView.updatePixelValues(view, -1, -1, 0, false);
                }
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }

    private class PixelInfoPPL implements PixelPositionListener {

        private final ProductSceneView _view;

        private PixelInfoPPL(ProductSceneView view) {
            _view = view;
        }

        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY, int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (isActive()) {
                pixelInfoView.updatePixelValues(_view, pixelX, pixelY, currentLevel, pixelPosValid);
            }
        }

        public void pixelPosNotAvailable() {
            if (isActive()) {
                pixelInfoView.updatePixelValues(_view, -1, -1, 0, false);
            }
        }

        private boolean isActive() {
            return isVisible() && !isSnapToPin();
        }
    }

    private class PinSelectionChangedListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            if (isActive()) {
                if (!Pin.PROPERTY_NAME_SELECTED.equals(event.getPropertyName())) {
                    return;
                }
                updatePin(event);
            }
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            if (isActive()) {
                updatePin(event);
            }
        }

        public void nodeAdded(ProductNodeEvent event) {
            if (isActive()) {
                updatePin(event);
            }
        }

        public void nodeRemoved(ProductNodeEvent event) {
            if (isActive()) {
                ProductNode sourceNode = event.getSourceNode();
                if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                    setToSelectedPin(currentView);
                }
            }
        }

        private void updatePin(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                setToSelectedPin(currentView);
            }
        }

        private boolean isActive() {
            return isSnapToPin();
        }
    }

    private class DisplayFilter extends PixelInfoView.DisplayFilter {

        private final VisatApp _app;
        private boolean _showOnlyLoadedOrDisplayedBands;

        private DisplayFilter(VisatApp app) {
            _app = app;
        }

        public void setShowOnlyLoadedOrDisplayedBands(boolean v) {
            if (_showOnlyLoadedOrDisplayedBands != v) {
                final boolean oldValue = _showOnlyLoadedOrDisplayedBands;
                _showOnlyLoadedOrDisplayedBands = v;
                firePropertyChange("showOnlyLoadedOrDisplayedBands", oldValue, v);
            }
        }

        @Override
        public boolean accept(ProductNode node) {
            if (node instanceof RasterDataNode) {
                final RasterDataNode rdn = (RasterDataNode) node;
                if (_showOnlyLoadedOrDisplayedBands) {
                    if (rdn.hasRasterData()) {
                        return true;
                    }
                    final JInternalFrame internalFrame = _app.findInternalFrame(rdn);
                    return internalFrame != null && internalFrame.getContentPane() instanceof ProductSceneView;
                }
            }
            return true;
        }
    }
}
