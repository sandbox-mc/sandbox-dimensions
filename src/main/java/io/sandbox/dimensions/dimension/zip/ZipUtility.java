package io.sandbox.dimensions.dimension.zip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.file.SimplePathVisitor;

public class ZipUtility {
  /**
   * Size of the buffer to read/write data
   */
  // private static final int BUFFER_SIZE = 4096;

  /**
   * Deletes all items in directory Path
   * @param path
   * @throws IOException
   */
  public static void deleteDirectory(Path path) throws IOException {
		Files.walkFileTree(path, new SimplePathVisitor() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

  // /**
  //  * Extracts a zip entry (file entry)
  //  * @param zipIn
  //  * @param filePath
  //  * @throws IOException
  //  */
  // private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
  //   BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
  //   byte[] bytesIn = new byte[BUFFER_SIZE];
  //   int read = 0;

  //   // Read through buffer
  //   while ((read = zipIn.read(bytesIn)) != -1) {
  //     bos.write(bytesIn, 0, read);
  //   }

  //   bos.close();
  // }

  public static void unzipFile(Path inputZip, Path targetDir) throws IOException {
    InputStream inStream = new FileInputStream(inputZip.toString());
    targetDir = targetDir.toAbsolutePath();
    try (ZipInputStream zipIn = new ZipInputStream(inStream)) {
      for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
        Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
        if (!resolvedPath.startsWith(targetDir)) {
          // see: https://snyk.io/research/zip-slip-vulnerability
          throw new RuntimeException("Entry with an illegal path: " + ze.getName());
        }

        if (ze.isDirectory()) {
          Files.createDirectories(resolvedPath);
        } else {
          Files.createDirectories(resolvedPath.getParent());
          Files.copy(zipIn, resolvedPath);
        }
      }
    }
  }

  // /**
  //  * Extracts a zip file specified by the zipFilePath to a directory specified by
  //  * destDirectory (will be created if does not exists)
  //  * @param zipFilePath
  //  * @param destDirectory
  //  * @throws IOException
  //  */
  // public void unzip(String zipFilePath, String destDirectory) throws IOException {
  //   // Create Initial dir if needed
  //   File destDir = new File(destDirectory);
  //   if (!destDir.exists()) {
  //     destDir.mkdir();
  //   }

  //   ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
  //   ZipEntry entry = zipIn.getNextEntry();

  //   // iterates over entries in the zip file
  //   while (entry != null) {
  //     String filePath = destDirectory + File.separator + entry.getName();
  //     if (!entry.isDirectory()) {
  //       // if the entry is a file, extracts it
  //       extractFile(zipIn, filePath);
  //     } else {
  //       // if the entry is a directory, make the directory
  //       File dir = new File(filePath);
  //       dir.mkdirs();
  //     }

  //     zipIn.closeEntry();

  //     // Set entry to next to keep looping
  //     entry = zipIn.getNextEntry();
  //   }

  //   zipIn.close();
  // }

  public static void zipDirectory(File dir, String targetZipFile) {
    try {
      // Build the list of files to zip
      List<String> filesListInDir = new ArrayList<String>();
      // Yes.. filesListInDir is pass by ref... I'm lazy
      populateFilesList(dir, filesListInDir);

      // create ZipOutputStream to write to the zip file
      FileOutputStream fos = new FileOutputStream(targetZipFile);
      ZipOutputStream zos = new ZipOutputStream(fos);

      // now zip files one by one
      for (String filePath : filesListInDir) {
        System.out.println("Zipping " + filePath);

        // for ZipEntry we need to keep only relative file path, so we used substring on absolute path
        ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1, filePath.length()));
        zos.putNextEntry(ze);

        // read the file and write to ZipOutputStream
        FileInputStream fis = new FileInputStream(filePath);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) > 0) {
          zos.write(buffer, 0, len);
        }
        zos.closeEntry();
        fis.close();
      }

      // close out the zip streams
      zos.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void populateFilesList(File dir, List<String> filesListInDir) throws IOException {
    File[] files = dir.listFiles();
    for (File file : files) {
      if(file.isFile()) {
        filesListInDir.add(file.getAbsolutePath());
      } else {
        populateFilesList(file, filesListInDir);
      }
    }
  }
}