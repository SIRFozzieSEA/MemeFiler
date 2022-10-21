package com.codef.memefiler;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MemeRenamer {

	private static boolean enableMainMethod = true;

	private static final Logger LOGGER = LogManager.getLogger(MemeRenamer.class.getName());

	private static final String renameFolder = "E:\\Zemez";

	private static Set<String> folderSet = new TreeSet<>();
	public static TreeSet<String> filetypes = new TreeSet<String>();
	public static LinkedHashMap<String, String> finalRename = new LinkedHashMap<String, String>();

	private static int fileCount = 0;
	private static int folderCount = 0;
	private static int handledCount = 0;

	public static void main(String[] args) {

		if (enableMainMethod) {

			startVisit(renameFolder);
			renameFiles();
			LOGGER.info("Total files visited = " + fileCount);
			LOGGER.info("Total files handled = " + handledCount);
			LOGGER.info("File Types = " + filetypes);

		}

	}

	public static void startVisit(String filePath) {
		visitFiles(Paths.get(filePath));
	}

	private static void visitFiles(Path path) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (entry.toFile().isDirectory()) {
					visitFiles(entry);
				} else {
					visitFileCode(entry.toString());
				}
			}
		} catch (IOException e) {
			LOGGER.error(e.toString(), e);
		}
	}

	public static void visitFileCode(String filePath) {

		String[] fileParts = filePath.split("\\\\");

		String folderName = fileParts[fileParts.length - 2];
		String fileName = fileParts[fileParts.length - 1];
		String filePrefixNew = folderName.toLowerCase().replaceAll(" ", "_");
		String[] fileNameNew = fileName.split("\\.");
		String nFileExtension = fileNameNew[fileNameNew.length - 1];

		filetypes.add(nFileExtension);

		if (!folderSet.contains(folderName)) {
			folderSet.add(folderName);
			folderCount = 1;
		}

		String newFileNameToUse = filePrefixNew + "_" + getFileDateTime(folderCount) + "."
				+ nFileExtension.toLowerCase();
		if (!nFileExtension.toLowerCase().equals("ini") && !nFileExtension.toLowerCase().equals("db")) {

			String targetFile = filePath.replace(fileName, newFileNameToUse);
			finalRename.put(filePath, targetFile);
			folderCount++;

		}

	}

	private static void renameFiles() {

		for (Map.Entry<String, String> set : finalRename.entrySet()) {

			String filePath = set.getKey();
			String targetFile = set.getValue();

			LOGGER.info(" Copied from: " + filePath + " to: " + targetFile);

			try {
				MemeFilerController.copyFile(filePath, targetFile);
				fileCount++;

				try {
					MemeFilerController.deleteFile(filePath, false);
					handledCount++;
				} catch (Exception e) {
					e.printStackTrace();
					LOGGER.info(" Cannot Delete: " + filePath);
				}

			} catch (IOException e1) {
				e1.printStackTrace();
				LOGGER.info(" Error handling: " + filePath + " to: " + targetFile);
			}

		}

	}

	private static String getFileDateTime(int fileNumber) {
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

}
