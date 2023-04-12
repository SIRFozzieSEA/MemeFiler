package com.codef.memefiler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemeFilerLibrary {

	private static final Logger LOGGER = LoggerFactory.getLogger(MemeFilerLibrary.class);

	public static String getFileDateTime(int fileNumber) {
		DateFormat oDateFormatter = new SimpleDateFormat("MMddyyyy_HHmmss_");
		return oDateFormatter.format(new Date()) + padLeftZeros(Integer.toString(fileNumber), 4);
	}

	public static String padLeftZeros(String inputString, int length) {
		if (inputString.length() >= length) {
			return inputString;
		}
		StringBuilder sb = new StringBuilder();
		while (sb.length() < length - inputString.length()) {
			sb.append('0');
		}
		sb.append(inputString);

		return sb.toString();
	}

	public static String replaceBackSlashes(String input) {
		return input.replaceAll("\\\\", "\\/");
	}

	public static void copyFile(String _sTargetFile, String _sDestinationFile) throws IOException {
		Files.copy(new File(_sTargetFile).toPath(), new File(_sDestinationFile).toPath());
	}

	public static void deleteFile(String _sPath, boolean _bLogEvent) {
		File oFile = new File(_sPath);
		if (oFile.exists()) {
			if (_bLogEvent) {
				LOGGER.info("Deleting: " + _sPath);
			}
			oFile.delete();
		}
	}

	// ------------------------------------------------------

	public static void scaleFile(String _sTargetFile, String _sDestinationFile) {

		try {
			File imageFile = new File(_sTargetFile);
			BufferedImage bufferedImage = ImageIO.read(imageFile);
			File pathFile = new File(_sDestinationFile);
			ImageIO.write(bufferedImage, "jpg", pathFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// THIS IS TO CROP IMAGES

	public static void cropImage(String imageFileName, File pathFile, BufferedImage bufferedImage) throws IOException {
		if (imageFileName.contains("land")) {
			// landscape
			ImageIO.write(cropImage(bufferedImage, 87, 468, 600, 400), "jpg", pathFile);
		} else {
			// portrait
			ImageIO.write(cropImage(bufferedImage, 179, 466, 400, 600), "jpg", pathFile);
		}
	}

	private static BufferedImage cropImage(BufferedImage bufferedImage, int x, int y, int width, int height) {
		BufferedImage croppedImage = bufferedImage.getSubimage(x, y, width, height);
		return croppedImage;
	}

}
