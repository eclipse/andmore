package org.eclipse.andmore.internal.editors.ui;

import static org.eclipse.ui.ISharedImages.IMG_DEC_FIELD_ERROR;
import static org.eclipse.ui.ISharedImages.IMG_DEC_FIELD_WARNING;
import static org.eclipse.ui.ISharedImages.IMG_OBJS_ERROR_TSK;
import static org.eclipse.ui.ISharedImages.IMG_OBJS_WARN_TSK;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * ImageDescriptor that adds a error marker.
 * Based on {@link DecorationOverlayIcon} only available in Eclipse 3.3
 */
public class ErrorImageComposite extends CompositeImageDescriptor {

    private Image mBaseImage;
    private ImageDescriptor mErrorImageDescriptor;
    private Point mSize;

    /**
     * Creates a new {@link ErrorImageComposite}
     *
     * @param baseImage the base image to overlay an icon on top of
     */
    public ErrorImageComposite(Image baseImage) {
        this(baseImage, false);
    }

    /**
     * Creates a new {@link ErrorImageComposite}
     *
     * @param baseImage the base image to overlay an icon on top of
     * @param warning if true, add a warning icon, otherwise an error icon
     */
    public ErrorImageComposite(Image baseImage, boolean warning) {
        mBaseImage = baseImage;
        ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
        mErrorImageDescriptor = sharedImages.getImageDescriptor(
                warning ? IMG_DEC_FIELD_WARNING : IMG_DEC_FIELD_ERROR);
        if (mErrorImageDescriptor == null) {
            mErrorImageDescriptor = sharedImages.getImageDescriptor(
                    warning ? IMG_OBJS_WARN_TSK : IMG_OBJS_ERROR_TSK);
        }
        mSize = new Point(baseImage.getBounds().width, baseImage.getBounds().height);
    }

    @Override
    protected void drawCompositeImage(int width, int height) {
        ImageData baseData = mBaseImage.getImageData();
        drawImage(baseData, 0, 0);

        ImageData overlayData = mErrorImageDescriptor.getImageData();
        if (overlayData.width == baseData.width && overlayData.height == baseData.height) {
            overlayData = overlayData.scaledTo(14, 14);
            drawImage(overlayData, -3, mSize.y - overlayData.height + 3);
        } else {
            drawImage(overlayData, 0, mSize.y - overlayData.height);
        }
    }

    @Override
    protected Point getSize() {
        return mSize;
    }
}
