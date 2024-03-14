package io.sandboxmc.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.file.SimplePathVisitor;

import io.sandboxmc.Plunger;

public class ZipUtility {

  public static void copyDirectory(Path source, Path target, CopyOption... options) throws IOException {
    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Files.createDirectories(target.resolve(source.relativize(dir).toString()));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.copy(file, target.resolve(source.relativize(file).toString()), options);
        return FileVisitResult.CONTINUE;
      }
    });
  }

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

  public static void unzipFile(Path inputZip, Path targetDir) throws IOException {
    InputStream inStream = new FileInputStream(inputZip.toString());
    ZipUtility.unzipInputStream(inStream, targetDir);
  }

  public static void unzipInputStream(InputStream inStream, Path targetDir) throws IOException {
    targetDir = targetDir.toAbsolutePath().normalize();
    try (ZipInputStream zipIn = new ZipInputStream(inStream)) {
      for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
        Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
        if (!resolvedPath.startsWith(targetDir)) {
          // see: https://snyk.io/research/zip-slip-vulnerability
          throw new RuntimeException("Attempting to create file with an illegal path!\nzeName: " + ze.getName() + "\ntargetDir: " + targetDir.toString() + "\nresolvedPath: " + resolvedPath.toString());
        }

        if (ze.isDirectory()) {
          Files.createDirectories(resolvedPath);
        } else {
          Files.createDirectories(resolvedPath.getParent());
          Files.copy(zipIn, resolvedPath);
        }
      }
    } catch (Exception e) {
      Plunger.error("Failed in unzipInputStream", e);
    }
  }

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
        // We skip datapacks dir, if we are saving the overworld
        // this would be the folder we save into...
        // TODO:BRENT create filter for correct files
        if (file.toString().equals("datapacks")) {
          continue;
        }

        populateFilesList(file, filesListInDir);
      }
    }
  }
}