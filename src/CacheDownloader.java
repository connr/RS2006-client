import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * RS2006 Development
 * 
 * Downloads and unzips a file from a {@link URL}.
 * 
 * @author Ryley Kimmel <ryley.kimmel@live.com>
 * Jul 8, 2013
 * CacheDownloader.java
 * 
 * @see java.lang.Object
 */
public final class CacheDownloader {

	/**
	 * An instance of {@link File} used to determine the root directory.
	 */
	private static final File root = new File(Signlink.findcachedir());

	/**
	 * An instance of {@link File} used to determine the root for the version file.
	 */
	private static final File versionFile = new File(root.getPath(), "version.dat");

	/**
	 * An instance of {@link File} used to determine the root of the archive.
	 */
	private static final File archive = new File(root.getPath(), "assets.zip");

	/**
	 * An <code>int</code> which represents the current file system revision.
	 */
	private static final int CURRENT_VERSION = 1;

	/**
	 * Downloads the file system from an {@link URL}.
	 * @param Game	The {@link Game} instance is used to show the progress of the download.
	 * @param url	The {@link URL} to download our assets from. <must be a direct link!>
	 * @throws IOException	If some I/O exception occurs.
	 */
	protected static void download(Game Game, URL url) throws IOException {
		// If we need to make the root directories, we will return the function
		if (createDirectory(root)) {
			return;
		}

		// If the version file exists and the version equals the current version,
		// we will return from the function, otherwise we will continue with the download.
		if (versionFile.exists() && getVersion(new FileInputStream(versionFile)) == CURRENT_VERSION) {
			return;
		}

		// An instance of {@link URLConnection} which opens a connection referred to by the {@link #url}.
		URLConnection connection = url.openConnection();

		// An instance of {@link InputStream} used to parse from the currently open {@link #connection}.
		InputStream is = new BufferedInputStream(connection.getInputStream());

		// An instance of {@link OutputStream} used to write to the specified {@link File} object.
		OutputStream os = new BufferedOutputStream(new FileOutputStream(archive));

		// XXX: Took part of this from another cache downloader, modified and renamed it a bit.
		byte[] buffer = new byte[1024];
		int bytes;
		long totalBytes = 0;
		while ((bytes = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytes);
			totalBytes += bytes;
			int percentage = (int) (((double) totalBytes / (double) connection.getContentLength()) * 100D);
			Game.drawLoadingText(percentage, "Downloading assets " + percentage + "%");
		}

		// Flush the contents of this {@link OutputStream} to the disk.
		os.flush();
		// Close the {@link OutputStream}.
		os.close();
		// Close the {@link InputStream}.
		is.close();

		// Start the unzip process!
		unzip();
	}

	/**
	 * Unzips the {@link #archive} {@link File} object.
	 * @throws IOException	If some I/O exception occurs.
	 */
	private static void unzip() throws IOException {
		// An instance of {@link ZipInputStream} which represents the {@link #archive} {@link File} object.
		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));

		// An instance of {@link ZipEntry} which represents a file within an archive.
		ZipEntry ze;
		// While the next entry within the archive is _not_ <code>null</code>!
		while ((ze = zis.getNextEntry()) != null) {

			// Returns the root of the entry.
			String entry = root.getPath() + File.separator + ze.getName();

			// If the entry happens to be a directory...
			if (ze.isDirectory()) {
				// We will check to see if we need to create them,
				// if we don't, then break out of the loop.
				if (!createDirectory(new File(entry))) {
					// propagate out of the loop.
					break;
				}
				// Otherwise, continue the unzipping process.
			} else {
				// An instance of {@link OutputStream}, used to write to the specified {@link File} object.
				OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(entry)));

				// While the data can be read and the data does not equal <tt>-1</tt>
				for (int data = zis.read(); data != -1; data = zis.read()) {
					// Write to the {@link File} object.
					os.write(data);
				}

				// Closes this entry and readies the stream for the next!
				zis.closeEntry();
				// Flush the {@link OutputStream} to disk!
				os.flush();
				// Close the {@link OutputStream}!
				os.close();
			}

		}
		// Close the {@link ZipInputStream} after everything has completed!
		zis.close();

		// If we can put write the {@link #CURRENT_VERSION} to the {@link #versionFile} {@link File} object...
		if (putVersion(new FileOutputStream(versionFile), CURRENT_VERSION)) {
			// We will delete the {@link archive} that we were unzipping..
			if (delete(archive)) {
				// then we will have no where to propagate
			}
		}
	}

	/**
	 * Deletes a specified {@link File} object.
	 * @param file	The {@link File} object to delete/
	 * @return	true if and only if the file or directory is successfully deleted; false otherwise.
	 */
	private static boolean delete(File file) {
		// Deletes the file or directory denoted by this abstract pathname.
		return file.delete();
	}

	/**
	 * Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.
	 * @param file	The {@link File} object to 
	 * @return	true if and only if the directory was created, along with all necessary parent directories; false otherwise
	 */
	private static boolean createDirectory(File file) {
		// Is file denoted by this abstract pathname is a directory.
		if (file.isDirectory()) {
			// return <code>false</code> as the file is already a directory!
			return false;
		}
		// Creates the file or directory denoted by this abstract pathname.
		return file.mkdirs();
	}

	/**
	 * Puts the version to the specified {@link OutputStream}.
	 * @param os	The {@link OutputStream} to write the version to.
	 * @param version	The version to write to the {@link OutputStream}.
	 * @return	<code>true</code> If and only if the version was written to the specified {@link OutputStream}.
	 * @throws IOException	If some I/O exception occurs.
	 */
	private static boolean putVersion(OutputStream os, int version) throws IOException {
		// An instance of {@link DataOutputStream} used to write the version.
		DataOutputStream dos = new DataOutputStream(os);
		// Write <code>byte</code> representation of the version.
		dos.write(version);
		// Flush the {@link DataOutputStream} to disk!
		dos.flush();
		// Close the {@link DataOutputStream}.
		dos.close();
		// return <code>true</code> If and only if the version was written to the specified {@link OutputStream}.
		return true;
	}

	/**
	 * Gets the version from an {@link InputStream}.
	 * @param is	The specified {@link InputStream}.
	 * @return	The integer representation of the version.
	 * @throws IOException	If some I/O exception occurs.
	 */
	private static int getVersion(InputStream is) throws IOException {
		// An instance of {@link DataInputStream} used to parse the data from the {@link InputStream}.
		DataInputStream dis = new DataInputStream(is);
		// Reads the version from the {@link InputStream}.
		int version = dis.read();
		// Close the {@link DataInputStream}.
		dis.close();
		// Return the integer representation of the version.
		return version;
	}

}