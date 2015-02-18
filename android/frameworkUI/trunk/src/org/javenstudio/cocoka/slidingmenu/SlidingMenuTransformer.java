package org.javenstudio.cocoka.slidingmenu;

import android.graphics.Canvas;
import android.view.View;

import org.javenstudio.common.util.Logger;

public class SlidingMenuTransformer implements SlidingMenu.CanvasTransformer {
	private static final Logger LOG = Logger.getLogger(SlidingMenuTransformer.class);
	private static final boolean DEBUG = SlidingMenu.DEBUG;
	
	@Override
	public void transformCanvas(SlidingMenuViews views, Canvas canvas,
			int type, float percentOpen) {
		if (views == null || canvas == null) return;
		
		float dx = 0, dy = 0;
		float sx = 1.0f, sy = 1.0f;
		
		if (type == SlidingMenu.VIEW_MENU) {
			int width = views.getMenuWidth();
			dx = (float) (percentOpen - 1.0f) * width;
			
		} else if (type == SlidingMenu.VIEW_SECONDARYMENU) {
			int width = views.getSecondaryMenuWidth();
			dx = (float) (1.0f - percentOpen) * width;
			
		} else if (type == SlidingMenu.VIEW_CONTENT) {
			if (views.getMenuVisibility() == View.VISIBLE) {
				int width = views.getMenuWidth();
				
				dx = (float) ((0 - percentOpen) * width + percentOpen * 10.0f);
				dy = (float) (percentOpen * 20.0f);
				sx = 1.0f - (0.04f * percentOpen);
				sy = 1.0f - (0.04f * percentOpen);
				
			} else if (views.getSecondaryMenuVisibility() == View.VISIBLE) {
				int width = views.getSecondaryMenuWidth();
				
				dx = (float) ((0 + percentOpen) * width + percentOpen * 20.0f);
				dy = (float) (percentOpen * 20.0f);
				sx = 1.0f - (0.04f * percentOpen);
				sy = 1.0f - (0.04f * percentOpen);
			}
		}
		
		canvas.translate(dx, dy);
		canvas.scale(sx, sy);
		
		if (DEBUG && LOG.isDebugEnabled()) {
			LOG.debug("transformCanvas: type=" + type 
					+ " dx=" + dx + " dy=" + dy + " sx=" + sx + " sy=" + sy 
					+ " percentOpen=" + percentOpen);
		}
	}

}
