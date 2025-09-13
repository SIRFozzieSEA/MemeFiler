package com.codef.memefiler;

import com.codef.xsalt.utils.XSaLTFileSystemUtils;
import com.codef.xsalt.utils.XSaLTGraphicTools;
import com.codef.xsalt.utils.XSaLTStringUtils;
import jakarta.servlet.http.HttpServletRequest;
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

import javax.activation.FileTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class MemeFilerController {

	private static final String SOURCE_PATH_FULL = "sourcePathFull";

	private static final String SOURCE_EXTENSION = "sourceExtension";

	private static final Logger LOGGER = LoggerFactory.getLogger(MemeFilerController.class);

	@Autowired
	private Environment env;

	private boolean folderPathsInitialized = false;
	private int fileMemeIndex = 0;
	private List<File> fileMemes = new ArrayList<>();

	private final TreeSet<String> folderPaths = new TreeSet<>();
	private final TreeSet<String> filetypes = new TreeSet<>();

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
			fileMemes = Arrays.asList(directory.listFiles(File::isFile));
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

		folderPaths.add("DELETE");
		folderPaths.add((System.getProperty("user.home") + "/Desktop").replace("\\", "/"));
	}

	private void visitFolderCode(String filePath) {
		int noOfFileInPath = new File(filePath).list().length;
		String cleanFilePath = filePath.replace("\\", "/");
		if (noOfFileInPath > 50) {
			LOGGER.info("{} ---> {}", cleanFilePath, noOfFileInPath);
		}
		folderPaths.add(cleanFilePath);
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

		String sourcePathFull = request.getParameter(SOURCE_PATH_FULL);
		String originalSourceExtension = request.getParameter(SOURCE_EXTENSION).toLowerCase();
		String targetPath = request.getParameter("targetPath");
		String targetSourceExtension = originalSourceExtension;

		if (!targetPath.isEmpty() && !sourcePathFull.isEmpty()) {

			// Come back and make sure this works
			String[] folderParts = targetPath.split("/");

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

			String newMemeName = targetPath + "/" + folderParts[folderParts.length - 1].toLowerCase().replace(" ", "_")
					+ "_" + getFileDateTime(totalCount);

			try {

				if (targetPath.equalsIgnoreCase("delete")) {

					totalCount = totalCount + 1;
					tryDelete(sourcePathFull);

				} else {

					switch (originalSourceExtension) {

					case "webp":
						newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
						XSaLTGraphicTools.scaleImageFile(sourcePathFull, "jpg", newMemeName);
						break;
						
					case "webm":
						newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
						convertWebmToMp4(sourcePathFull, newMemeName + ".mp4");
						break;

					case "jfif":
						// handle this like a jpg
						newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
						XSaLTGraphicTools.scaleImageFile(sourcePathFull, "jpg".toLowerCase(), newMemeName);
						break;

					default:
						// this assumes JPEG/JPG is the default
						newMemeName = newMemeName + "." + targetSourceExtension.toLowerCase();
						XSaLTFileSystemUtils.copyFile(sourcePathFull, newMemeName);
					}
					
					LOGGER.info("   Copied from: {} to {}", sourcePathFull, newMemeName);
					totalCount = totalCount + 1;
					tryDelete(sourcePathFull);

				}

			} catch (Exception e) {
				LOGGER.error(" Error copying: {} to {}, error={}", sourcePathFull, newMemeName, e.toString());
				renameBadFile(sourcePathFull);
			}

		} else {
			renameBadFile(sourcePathFull);
			LOGGER.error("Done or skipped.");
		}

	}

	private void renameBadFile(String sourcePathFull) {
		String badName = sourcePathFull.substring(0, sourcePathFull.lastIndexOf("/")) + "/XXXXX_"
				+ sourcePathFull.substring(sourcePathFull.lastIndexOf("/") + 1);
		File oldFile = new File(sourcePathFull);
		File newFile = new File(badName);
		oldFile.renameTo(newFile);
	}

	private void tryDelete(String sourcePathFull) {
		try {
			XSaLTFileSystemUtils.deleteFileNew(sourcePathFull);
			LOGGER.info("       Deleted: {}", sourcePathFull);
		} catch (Exception e) {
			LOGGER.info(" Cannot Delete: {}", sourcePathFull);
		}
	}

	private void attachMeme(Model model) {
		File fileMeme = getNextImageSourcePath();

		if (fileMeme != null) {

			String fileMemeFullPath = fileMeme.getAbsolutePath().replace("\\", "/");
			String fileMemeName = fileMeme.getName();
			String[] fileMemeExtensionArray = fileMemeName.split("\\.");
			String fileMemeExtension = fileMemeExtensionArray[fileMemeExtensionArray.length - 1];

			model.addAttribute(SOURCE_PATH_FULL, fileMemeFullPath);
			model.addAttribute(SOURCE_EXTENSION, fileMemeExtension);
			model.addAttribute("folderPaths", folderPaths);
			model.addAttribute("fileTypes", filetypes.toString());

		} else {

			model.addAttribute(SOURCE_PATH_FULL, "");
			model.addAttribute(SOURCE_EXTENSION, "");
			model.addAttribute("folderPaths", folderPaths);
			model.addAttribute("fileTypes", filetypes.toString());

		}
	}

	private String getFileDateTime(int fileNumber) {
		DateFormat oDateFormatter = new SimpleDateFormat("MMddyyyy_HHmmss_");
		return oDateFormatter.format(new Date())
				+ XSaLTStringUtils.padLeftWithCharacter(Integer.toString(fileNumber), '0', 4);
	}

	// TODO: Implement video conversion if needed
	private static void convertWebmToMp4(String inputPath, String outputPath) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", inputPath, "-c:v", "libx264", "-c:a", "aac", outputPath);
		pb.redirectErrorStream(true);
		Process process = pb.start();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
		    while (reader.readLine() != null) {
		        // Do nothing, just consume the output
		    }
		}
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
		}
	}

}
