/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.domui.parts;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.annotation.*;
import javax.imageio.*;

import to.etc.domui.component.input.*;
import to.etc.domui.server.*;
import to.etc.domui.server.parts.*;
import to.etc.domui.state.*;
import to.etc.domui.util.*;
import to.etc.domui.util.resources.*;
import to.etc.util.*;

/**
 * Generates background image for specified search field caption. 
 * Usually used by {@link Text#setSearchMarker(String)}
 * 
 *
 * @author <a href="mailto:btadic@execom.eu">Bojan Tadic</a>
 * Created on Nov 1, 2011
 */
public class SearchImagePart implements IBufferedPartFactory {

	private static final String DEFAULT_ICON = "THEME/icon-search.png";

	private static final Color DEFAULT_COLOR = Color.GRAY;

	@Override
	public Object decodeKey(String rurl, IExtendedParameterInfo param) throws Exception {
		SearchImagePartKey key = SearchImagePartKey.decode(param);
		return key;
	}

	/**
	 * Generate image if is not in cache.
	 * @see to.etc.domui.server.parts.IBufferedPartFactory#generate(to.etc.domui.server.parts.PartResponse, to.etc.domui.server.DomApplication, java.lang.Object, to.etc.domui.util.resources.IResourceDependencyList)
	 */
	@Override
	public void generate(PartResponse pr, DomApplication da, Object key, IResourceDependencyList rdl) throws Exception {
		SearchImagePartKey sipKey = (SearchImagePartKey) key;

		InputStream is = null;

		try {
			BufferedImage bi = PartUtil.loadImage(da, da.getThemedResourceRURL(DomUtil.isBlank(sipKey.getIcon()) ? DEFAULT_ICON : sipKey.getIcon().trim()), rdl);
			is = getInputStream(drawImage(bi, sipKey.getCaption(), sipKey.getColor()));

			if(is == null)
				throw new IllegalStateException("Image is generated incorrectly");
			FileTool.copyFile(pr.getOutputStream(), is);
			pr.setMime("image/png");
			pr.setCacheTime(da.getDefaultExpiryTime());

		} finally {
			try {
				if(is != null)
					is.close();
			} catch(Exception x) {}
		}

	}

	private InputStream getInputStream(BufferedImage bi) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(bi, "png", os);
		InputStream stream = new ByteArrayInputStream(os.toByteArray());
		return stream;
	}

	private static String getURL(String icon, String caption, String color) {
		StringBuilder sb = new StringBuilder();
		sb.append(SearchImagePart.class.getName()).append(".part");
		boolean paramExists = false;
		paramExists = SearchImagePartKey.appendParam(sb, paramExists, SearchImagePartKey.PARAM_ICON, icon);
		paramExists = SearchImagePartKey.appendParam(sb, paramExists, SearchImagePartKey.PARAM_CAPTION, caption);
		paramExists = SearchImagePartKey.appendParam(sb, paramExists, SearchImagePartKey.PARAM_COLOR, color);
		return sb.toString();
	}

	/**
	 * Dynamically add background image for search marker.
	 * Background image have small magnifier icon (THEME/icon-search.png)
	 * @return
	 */
	public static String getBackgroundIconOnly() {
		return getBackgroundImage(null, null, null);
	}

	/**
	 * Dynamically add background image for search marker.
	 * Background image will have only defined icon
	 * 
	 * @param icon
	 * @return
	 */
	public static String getBackgroundIconOnly(String icon) {
		return getBackgroundImage(icon, null, null);
	}

	/**
	 * Dynamically add background image for search marker.
	 * Background image have small magnifier icon and and defined text (caption)
	 *
	 * @param caption
	 * @return
	 */
	public static String getBackgroundImage(String caption) {
		return getBackgroundImage(null, caption, null);
	}

	/**
	 * Dynamically add background image for search marker.
	 * Background image have small defined icon and and defined text (caption)
	 * 
	 * @param icon
	 * @param caption
	 * @return
	 */
	public static String getBackgroundImage(String icon, String caption) {
		return getBackgroundImage(icon, caption, null);
	}

	/**
	 * Dynamically add background image for search marker.
	 * Background image have small defined icon and and defined text (caption) in defined color
	 * @param icon
	 * @param caption
	 * @param color
	 * @return
	 */
	public static String getBackgroundImage(String icon, String caption, String color) {
		String url = UIContext.getRequestContext().getRelativePath(getURL(icon, caption, color));
		return url;
	}

	/**
	 * Draw background image with icon and caption 
	 * @param searchIcon
	 * @param caption
	 * @param captionColor
	 * @return
	 */
	private BufferedImage drawImage(@Nonnull BufferedImage searchIcon, @Nullable String caption, @Nullable String captionColor) {
		BufferedImage bufferedImage = new BufferedImage(200, 20, BufferedImage.TRANSLUCENT);

		Graphics2D g = bufferedImage.createGraphics();
		g.setComposite(makeComposite(0.3F));

		g.drawImage(searchIcon, null, 0, 0);

		if(!DomUtil.isBlank(caption)) {
			Font font = new Font("ARIAL", Font.BOLD, 10);
			Color capColor = null;
			if(!DomUtil.isBlank(captionColor)) {
				try {
					if(captionColor.startsWith("#")) {
						captionColor = captionColor.substring(1);
					}
					capColor = new Color(Integer.parseInt(captionColor, 16));
				} catch(Exception ex) {
					//just ignore
				}
			}
			if(capColor == null) {
				capColor = DEFAULT_COLOR;
			}
			drawText(g, font, caption, 21, 1, Color.WHITE);
			drawText(g, font, caption, 20, 0, capColor);
		}

		return bufferedImage;
	}

	/**
	 * Add opacity to image
	 * @param alpha
	 * @return
	 */
	private AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}

	/**
	 * Draw String on canvas.
	 *
	 * @param g
	 * @param textValue
	 * @param x  X coordinate.
	 * @param y  Y coordinate. Top of text
	 * @param stringColor
	 */
	private void drawText(Graphics2D g, Font font, String textValue, int x, int y, Color stringColor) {
		Font oldFont = g.getFont();
		Color old = g.getColor();
		g.setFont(font);
		g.setColor(stringColor);
		FontMetrics fm = g.getFontMetrics();
		int startX = x;
		int startY = y + fm.getHeight() - 1;
		g.drawString(textValue, startX, startY);
		g.setColor(old);
		g.setFont(oldFont);
	}


}
