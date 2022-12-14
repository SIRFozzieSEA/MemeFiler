package com.codef.memefiler;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemeRenamerController {

	private static final Logger LOGGER = LogManager.getLogger(MemeRenamerController.class.getName());

	private Set<String> folderSet = new TreeSet<>();
	private TreeSet<String> filetypes = new TreeSet<String>();
	private LinkedHashMap<String, String> finalRename = new LinkedHashMap<String, String>();

	private int fileCount = 0;
	private int folderCount = 0;
	private int handledCount = 0;

	@Autowired
	private Environment env;

	@GetMapping("/rename")
	public String indexLaunch(HttpServletRequest request, Model model) {

		fileCount = 0;
		folderCount = 0;
		handledCount = 0;

		startVisit(Paths.get(env.getProperty("MEME_TARGET_FOLDER")));
		renameFiles();

		model.addAttribute("TOTAL_FILES_VISITED", Integer.valueOf(fileCount));
		model.addAttribute("TOTAL_FILES_HANDLED", Integer.valueOf(handledCount));
		model.addAttribute("FILE_TYPES_HANDLED", filetypes);

		return "index_rename";
	}

	private void startVisit(Path path) {
		visitFiles(path);
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

		String newFileNameToUse = filePrefixNew + "_" + MemeFilerLibrary.getFileDateTime(folderCount) + "."
				+ nFileExtension.toLowerCase();
		if (!nFileExtension.toLowerCase().equals("ini") && !nFileExtension.toLowerCase().equals("db")) {

			String targetFile = filePath.replace(fileName, newFileNameToUse);
			finalRename.put(filePath, targetFile);
			folderCount++;

		}

	}

	private void renameFiles() {

		for (Map.Entry<String, String> set : finalRename.entrySet()) {

			String filePath = set.getKey();
			String targetFile = set.getValue();

			LOGGER.info(" Copied from: " + filePath + " to: " + targetFile);

			try {
				MemeFilerLibrary.copyFile(filePath, targetFile);
				fileCount++;

				try {
					MemeFilerLibrary.deleteFile(filePath, false);
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

}
