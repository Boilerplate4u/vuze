package com.aelitis.azureus.ui.swt.views.skin;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class BlankDetailPage
	extends AbstractDetailPage
{
	private Composite content = null;

	private Canvas spinnerCanvas = null;

	private GC spinnerGC;

	private Image[] spinnerImages;

	private Rectangle spinnerBounds;

	private boolean isBusy;

	private boolean busyAlready;

	private Display display;

	private Listener listener;

	private Label propmptLabel;

	public BlankDetailPage(DetailPanel detailPanel, String pageID) {
		super(detailPanel, pageID);
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.INHERIT_DEFAULT);
		display = content.getDisplay();

		propmptLabel = new Label(content, SWT.WRAP);
		propmptLabel.setLocation(50, 50);

		FontData[] fDatas = propmptLabel.getFont().getFontData();
		for (int i = 0; i < fDatas.length; i++) {
			fDatas[i].height = 10;
		}
		final Font newFont = new Font(display, fDatas);
		propmptLabel.setFont(newFont);
		propmptLabel.setForeground(ColorCache.getColor(display, 100, 100, 100));

		propmptLabel.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (null != newFont && false == newFont.isDisposed()) {
					newFont.dispose();
				}
			}
		});

		spinnerCanvas = new Canvas(content, SWT.NO_BACKGROUND);
		if (null == spinnerGC) {
			spinnerGC = new GC(spinnerCanvas);
			spinnerGC.setBackground(content.getBackground());
		}

		propmptLabel.setText(MessageText.getString("message.taking.too.long"));
		propmptLabel.setSize(propmptLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		propmptLabel.setVisible(false);

		content.addControlListener(new ControlListener() {

			public void controlResized(ControlEvent e) {
				Utils.execSWTThread(new AERunnable() {

					public void runSupport() {
						if (true == isBusy) {
							Utils.centerRelativeTo(spinnerBounds, content.getBounds());
							spinnerCanvas.setBounds(spinnerBounds);
						}
					}
				});
			}

			public void controlMoved(ControlEvent e) {
			}
		});

		listener = new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.ESC) {
					System.out.println("ESC pressed");//KN: sysout
					showBusy(false, 0);
					
					getDetailPanel().show(false);
					ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
					if (null != buttonBar) {
						buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
					}
				}
			}
		};

	}

	public void showBusy(boolean value, long delayInMilli) {
		isBusy = value;

		if (true == isBusy && false == busyAlready) {
			display.addFilter(SWT.KeyUp, listener);

			/*
			 * Display a message to the user if this is taking too long
			 */
			Utils.execSWTThreadLater(10000, new AERunnable() {
				public void runSupport() {
					if (true == isBusy) {
						propmptLabel.setVisible(true);
					}
				}
			});
			showSpinner(Math.max(0, delayInMilli));
		}
		if (false == isBusy) {
			display.removeFilter(SWT.KeyUp, listener);
			propmptLabel.setVisible(false);
		}
	}

	private void showSpinner(final long delayInMilli) {

		/*
		 * Create the images off-line and store them in the array if not done already;
		 * we will use these to draw onto the canvas to animate the spinner
		 */
		if (null == spinnerImages) {
			InputStream is = ImageRepository.getImageAsStream("spinner_big");

			if (null == is) {
				return;
			}
			ImageLoader loader = new ImageLoader();
			ImageData[] imageDataArray = loader.load(is);
			spinnerBounds = new Rectangle(0, 0, loader.logicalScreenWidth,
					loader.logicalScreenHeight);

			spinnerImages = new Image[imageDataArray.length];
			for (int i = 0; i < imageDataArray.length; i++) {
				ImageData imageData = imageDataArray[i];
				/*
				 * Setting the transparent pixel to be black
				 */
				imageData.transparentPixel = 0;

				spinnerImages[i] = new Image(display, spinnerBounds.width,
						spinnerBounds.height);
				GC offScreenImageGC = new GC(spinnerImages[i]);
				offScreenImageGC.setBackground(content.getBackground());
				offScreenImageGC.fillRectangle(0, 0, spinnerBounds.width,
						spinnerBounds.height);

				Image tempImage = new Image(display, imageData);
				offScreenImageGC.drawImage(tempImage, 0, 0, imageData.width,
						imageData.height, imageData.x, imageData.y, imageData.width,
						imageData.height);

				tempImage.dispose();
				offScreenImageGC.dispose();
			}
		}

		/*
		 * Adjust the spinner bounds to be centered on the lightbox shell itself
		 */
		Utils.centerRelativeTo(spinnerBounds, content.getBounds());

		/*
		 * Create the canvas for the spinner; size the canvas to be just enough for the image
		 */
		if (null == spinnerCanvas) {
			spinnerCanvas = new Canvas(content, SWT.NONE);
		}
		spinnerCanvas.setBounds(spinnerBounds);
		if (null == spinnerGC) {
			spinnerGC = new GC(spinnerCanvas);
			spinnerGC.setBackground(content.getBackground());
		}

		/*
		 * Spinner animation 
		 */

		AEThread2 spinnerThread = new AEThread2("spinner-animator", true) {
			public void run() {
				final int[] imageDataIndex = new int[1];
				busyAlready = true;

				/* 
				 * First we sleep for the specified delay before we start painting; if during this time
				 * isBusy is set to false (by another thread) then it's not necessary to show the spinner. 
				 */
				if (delayInMilli > 0) {
					try {
						Thread.sleep(delayInMilli);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				/*
				 * Loop through and draw the images sequentially until we're no longer busy
				 */
				while (true == isBusy) {
					if (null == content || true == content.isDisposed()) {
						break;
					}

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							/* 
							 * Draw the image onto the canvas. 
							 */
							if (null != spinnerCanvas && false == spinnerCanvas.isDisposed()) {
								spinnerGC.drawImage(spinnerImages[imageDataIndex[0]], 0, 0);
							}
						}
					});

					/* 
					 * If we have just drawn the last image start over from the beginning
					 */
					if (imageDataIndex[0] == spinnerImages.length - 1) {
						imageDataIndex[0] = 0;
					} else {
						imageDataIndex[0]++;
					}

					/* 
					 * Sleep for a bit.
					 */
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Debug.out(e);
					}

				}

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						/* 
						 * Fill the image area with lbShell background color to 'erase' the last image drawn 
						 */
						if (null != spinnerCanvas && false == spinnerCanvas.isDisposed()) {
							spinnerGC.fillRectangle(spinnerCanvas.getClientArea());
						}
					}
				});

				busyAlready = false;
			}

		};
		spinnerThread.start();

	}

	public Control getControl() {
		return content;
	}

	public ClientMessageContext getMessageContext() {
		return null;
	}

	public void refresh(RefreshListener refreshListener) {
	}

}
