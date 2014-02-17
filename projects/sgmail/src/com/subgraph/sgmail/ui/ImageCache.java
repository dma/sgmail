package com.subgraph.sgmail.ui;

import com.subgraph.sgmail.model.Model;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageCache {
	public final static String USER_IMAGE = "user_64x64.png";
	public final static String INBOX_IMAGE = "inbox_16x16.png";
	public final static String FOLDER_IMAGE = "folder_16x16.png";
	public final static String STARRED_IMAGE = "starred_16x16.png";
	public final static String UNSTARRED_IMAGE = "unstarred_16x16.png";
	
	public final static String FORWARD_IMAGE = "forward_16x16.png";
	public final static String REPLY_IMAGE = "reply_16x16.png";
	public final static String REPLY_ALL_IMAGE = "reply-all_16x16.png";
	public final static String TAG_IMAGE = "tag_16x16.png";
	
	public final static String BLUE_DOT_IMAGE = "bullet_blue_16x16.png";
	
	public final static String COMPOSE_IMAGE = "compose_24x24.png";
    public final static String GEAR_IMAGE = "gear_16x16.png";
	
	public final static String LOCKED_IMAGE = "locked.png";
	public final static String UNLOCKED_IMAGE = "unlocked.png";
	public final static String SIGNED_IMAGE = "signed.png";
	
	
	public static ImageCache _instance;
	
	public static ImageCache getInstance() {
		if(_instance == null) {
			_instance = new ImageCache();
		}
		return _instance;
	}
	
	private Model model;
	
	private final Map<String, Image> imageMap = new HashMap<>();
	private final Map<String, Image> disabledMap = new HashMap<>();
	private final Map<String, Image> avatarMap = new HashMap<>();
	
	public void setModel(Model model) {
		this.model = model;
	}
	
	public Image getAvatarImage(String email) {
		if(!avatarMap.containsKey(email)) {
			avatarMap.put(email, createAvatarImage(email));
		}
		return avatarMap.get(email);
	}

	private Image createAvatarImage(String email) {
		if(model == null) {
			return getDisabledImage(USER_IMAGE);
		}
		final byte[] imageBytes = model.findAvatarImageDataForEmail(email);
		if(imageBytes == null) {
			return getDisabledImage(USER_IMAGE);
		} else {
			return createAvatarImage(imageBytes);
		}
	}

	public Image createAvatarImage(byte[] imageBytes) {
		final ByteArrayInputStream input = new ByteArrayInputStream(imageBytes);
		final ImageData data = new ImageData(input);
		final Image image =  new Image(Display.getDefault(), data);
		return resizeImage(image, 64, 64);
	}
	
	private Image resizeImage(Image image, int width, int height) {
		final Image scaled = new Image(image.getDevice(), width, height);
		final GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		final Rectangle b = image.getBounds();
		gc.drawImage(image, 0, 0, b.width, b.height, 0, 0, width, height);
		gc.dispose();
		image.dispose();
		return scaled;
	}
	
	public Image getImage(String key) {
		if(!imageMap.containsKey(key)) {
			imageMap.put(key, createImage(key, false));
		}
		return imageMap.get(key);
	}
	
	public Image getDisabledImage(String key) {
		if(!disabledMap.containsKey(key)) {
			disabledMap.put(key, createImage(key, true));
		}
		return disabledMap.get(key);
	}

	private Image createImage(String key, boolean greyed) {
		final Image img = loadImage("/icons/" + key);
		if(!greyed) {
			return img;
		}
		Image greyedImage = new Image(Display.getDefault(), img, SWT.IMAGE_DISABLE);
		img.dispose();
		return greyedImage;
	}
	
	private Image loadImage(String path) {
		final Image image = tryResourceLoad(path);
		if(image != null) {
			return image;
		}
		return new Image(Display.getDefault(), System.getProperty("user.dir") + path);
	}
	
	private Image tryResourceLoad(String path) {
		final InputStream in = getClass().getResourceAsStream(path);
		if(in == null) {
			return null;
		}
		try {
			return new Image(Display.getDefault(), in);
		} finally {
			try {
				in.close();
			} catch (IOException e) { }
		}
	}
}
