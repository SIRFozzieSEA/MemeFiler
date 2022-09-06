package com.codef.memefiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import javax.activation.FileTypeMap;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MemeFilerController {

	private static final Logger LOGGER = LogManager.getLogger(MemeFilerController.class.getName());

	@Autowired
	private Environment env;

	private boolean folderPathsInitialized = false;
	private int fileMemeIndex = 0;
	private List<File> fileMemes = new ArrayList<File>();
	private TreeSet<String> folderPaths = new TreeSet<String>();
	
	private static int totalCount = 0;

	@GetMapping("/")
	public String indexLaunch(Model model) {
		if (!folderPathsInitialized) {
			initializeApp();
			folderPathsInitialized = true;
		}
		attachMeme(model);
		return "index";
	}

	@PostMapping("/")
	public String indexPost(HttpServletRequest request, Model model) {

		handleMeme(request);
		attachMeme(model);
		return "index";
	}

	@GetMapping("/getImage")
	public ResponseEntity<byte[]> getImage(@RequestParam(name = "imageName", required = true) String imageName) {
		try {
			File img = new File(imageName);
			return ResponseEntity.ok()
					.contentType(MediaType.valueOf(FileTypeMap.getDefaultFileTypeMap().getContentType(img)))
					.body(Files.readAllBytes(img.toPath()));
		} catch (IOException e) {
			try {
				File img = new File(env.getProperty("DONE_IMAGE"));
				return ResponseEntity.ok()
						.contentType(MediaType.valueOf(FileTypeMap.getDefaultFileTypeMap().getContentType(img)))
						.body(Files.readAllBytes(img.toPath()));
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
		}
	}

	// ----------------------------

	private void handleMeme(HttpServletRequest request) {

		fileMemeIndex = fileMemeIndex + 1;

		String sourcePathFull = request.getParameter("sourcePathFull");
		String sourceExtension = request.getParameter("sourceExtension");
		String targetPath = request.getParameter("targetPath");

		if (targetPath.length() > 0 && sourcePathFull.length() > 0) {

			String[] folderParts = targetPath.split("\\/");
			String newMemeName = targetPath + "/"
					+ folderParts[folderParts.length - 1].toLowerCase().replaceAll(" ", "_") + "_" + getFileDateTime(totalCount)
					+ "." + sourceExtension.toLowerCase();

			try {
				copyFile(sourcePathFull, newMemeName);
				LOGGER.info("   Copied from: " + sourcePathFull + " to: " + newMemeName);
				totalCount = totalCount + 1;

				try {
					deleteFile(sourcePathFull, false);
					LOGGER.info("       Deleted: " + sourcePathFull + " to: " + newMemeName);
				} catch (Exception e) {
					LOGGER.info(" Cannot Delete: " + sourcePathFull + " to: " + newMemeName);
				}

			} catch (IOException e) {
				LOGGER.info(" Error copying: " + sourcePathFull + " to: " + newMemeName);
			}

		} else {
			LOGGER.error("Done or skipped.");
		}

	}

	private void attachMeme(Model model) {
		File fileMeme = getNextImageSourcePath();

		if (fileMeme != null) {

			String fileMemeFullPath = replaceBackSlashes(fileMeme.getAbsolutePath());
			String fileMemeName = fileMeme.getName();
			String fileMemeExtension = fileMemeName.split("\\.")[1];

			model.addAttribute("sourcePathFull", fileMemeFullPath);
			model.addAttribute("sourceExtension", fileMemeExtension);
			model.addAttribute("folderPaths", folderPaths);

		} else {

			model.addAttribute("sourcePathFull", "");
			model.addAttribute("sourceExtension", "");
			model.addAttribute("folderPaths", folderPaths);

		}
	}

	private String replaceBackSlashes(String input) {
		return input.replaceAll("\\\\", "\\/");
	}

	private String getFileDateTime(int fileNumber) {
		DateFormat oDateFormatter = new SimpleDateFormat("MMddyyyy_HHmmss_");
		return oDateFormatter.format(new Date()) + padLeftZeros(Integer.toString(fileNumber), 4);
	}
	
	public String padLeftZeros(String inputString, int length) {
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

	private File getNextImageSourcePath() {
		try {
			return fileMemes.get(fileMemeIndex);
		} catch (Exception e) {
			return null;
		}
	}

	private void initializeApp() {
		visitFolders(Paths.get(env.getProperty("MEME_TARGET_FOLDER")));

		File directory = new File(env.getProperty("MEME_SORT_FOLDER"));
		if (!directory.exists()) {
			LOGGER.error("MEME_SORT_FOLDER does not exist");
		} else {
			fileMemes = Arrays.asList(directory.listFiles());
			fileMemeIndex = 0;
		}
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

	private void visitFolders(Path path) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (entry.toFile().isDirectory()) {
					visitFolderCode(entry.toString());
					visitFolders(entry);
				}
			}
		} catch (IOException e) {
			LOGGER.error(e.toString(), e);
		}
	}

	private void visitFolderCode(String filePath) {
		folderPaths.add(filePath.replaceAll("\\\\", "/"));
	}
}
