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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.codef.xsalt.utils.XSaLTFileSystemUtils;
import com.codef.xsalt.utils.XSaLTGraphicTools;
import com.codef.xsalt.utils.XSaLTStringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MemeFilerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MemeFilerController.class);

	@Autowired
	private Environment env;

	private boolean folderPathsInitialized = false;
	private int fileMemeIndex = 0;
	private List<File> fileMemes = new ArrayList<File>();

	private TreeSet<String> folderPaths = new TreeSet<String>();
	private TreeSet<String> filetypes = new TreeSet<String>();

	private int totalCount = 1;

	@GetMapping("/")
	public String indexLaunch(HttpServletRequest request, Model model) {

		if (request.getParameter("restart") != null) {
			folderPathsInitialized = false;
		}

		if (!folderPathsInitialized) {
			initializeApp();
			checkFileTypes();
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
			return null;
		}
	}

	// ---------------------------------------------------------

	private void initializeApp() {

		totalCount = 1;

		visitFolders(Paths.get(env.getProperty("MEME_TARGET_FOLDER")));

		File directory = new File(env.getProperty("MEME_SORT_FOLDER"));
		if (!directory.exists()) {
			LOGGER.error("MEME_SORT_FOLDER does not exist");
		} else {
			fileMemes = Arrays.asList(directory.listFiles());
			fileMemeIndex = 0;
		}
	}

	private void checkFileTypes() {
		visitFiles(Paths.get(env.getProperty("MEME_TARGET_FOLDER")));
	}

	private void visitFiles(Path path) {
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

	private void visitFileCode(String filePath) {

		String[] fileParts = filePath.split("\\\\");
		String fileName = fileParts[fileParts.length - 1];
		String[] fileNameNew = fileName.split("\\.");
		String nFileExtension = fileNameNew[fileNameNew.length - 1];

		filetypes.add(nFileExtension);

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
		int noOfFileInPath = new File(filePath).list().length;
		if (noOfFileInPath > 50) {
			LOGGER.debug(filePath.replaceAll("\\\\", "/") + " --> " + noOfFileInPath);
		}
		folderPaths.add(filePath.replaceAll("\\\\", "/"));
	}

	private File getNextImageSourcePath() {
		try {
			return fileMemes.get(fileMemeIndex);
		} catch (Exception e) {
			return null;
		}
	}

	private void handleMeme(HttpServletRequest request) {

		fileMemeIndex = fileMemeIndex + 1;

		String sourcePathFull = request.getParameter("sourcePathFull");
		String originalSourceExtension = request.getParameter("sourceExtension").toLowerCase();
		String targetPath = request.getParameter("targetPath");
		String targetSourceExtension = originalSourceExtension;

		if (targetPath.length() > 0 && sourcePathFull.length() > 0) {

			String[] folderParts = targetPath.split("\\/");

			// rename JPEG to JPG
			if (originalSourceExtension.equals("jpeg")) {
				targetSourceExtension = "jpg";
			}

			// convert WEBP to JPEG
			if (originalSourceExtension.equals("webp")) {
				targetSourceExtension = "jpg";
			}

			// convert JFIF to JPEG
			if (originalSourceExtension.equals("jfif")) {
				targetSourceExtension = "jpg";
			}

			String newMemeName = targetPath + "/"
					+ folderParts[folderParts.length - 1].toLowerCase().replaceAll(" ", "_") + "_"
					+ getFileDateTime(totalCount);

			try {

				switch (originalSourceExtension) {

				case "webp":
					newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
					XSaLTGraphicTools.scaleImageFile(sourcePathFull, "jpg", newMemeName);
					break;

				case "jfif":
					newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
					XSaLTGraphicTools.scaleImageFile(sourcePathFull, "jpg", newMemeName);
					break;

				case "jpeg":
					newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
					XSaLTFileSystemUtils.copyFile(sourcePathFull, newMemeName);
					break;

				default:
					newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
					XSaLTFileSystemUtils.copyFile(sourcePathFull, newMemeName);
				}

				LOGGER.info("   Copied from: " + sourcePathFull + " to: " + newMemeName);
				totalCount = totalCount + 1;

				try {
					XSaLTFileSystemUtils.deleteFileNew(sourcePathFull);
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
			model.addAttribute("fileTypes", filetypes.toString());

		} else {

			model.addAttribute("sourcePathFull", "");
			model.addAttribute("sourceExtension", "");
			model.addAttribute("folderPaths", folderPaths);
			model.addAttribute("fileTypes", filetypes.toString());

		}
	}
	
	private String replaceBackSlashes(String input) {
		return input.replaceAll("\\\\", "\\/");
	}
	
	private String getFileDateTime(int fileNumber) {
		DateFormat oDateFormatter = new SimpleDateFormat("MMddyyyy_HHmmss_");
		return oDateFormatter.format(new Date()) + XSaLTStringUtils.padLeftWithCharacter(Integer.toString(fileNumber), '0', 4);
	}

}
