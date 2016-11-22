package visualvm;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;

import com.google.common.base.MoreObjects;
import com.google.common.io.ByteStreams;

// ref: org.netbeans.modules.profiler.LoadedSnapshot
public class Snapshot {

	private static final String PROFILER_FILE_MAGIC_STRING = "nBpRoFiLeR";
	private static final byte SNAPSHOT_FILE_VERSION_MAJOR = 1;
	private static final int SNAPSHOT_TYPE_CPU = 1;

	private int type;
	private Properties settings;
	private String comments;
	private CPUResultsSnapshot cpuResults;
	
	public static Snapshot load(File file) throws FileNotFoundException, IOException {
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			return load(in);
		}
	}

	public static Snapshot load(DataInputStream in) throws IOException {

		//
		// binary file format:
		// 1. magic number: "nbprofiler"
		// 2. int type
		// 3. int length of snapshot data size
		// 4. snapshot data bytes
		// 5. int length of settings data size
		// 6. settings data bytes (.properties plain text file format)
		// 7. String (UTF) custom comments

		String magicString = new String(readData(in, PROFILER_FILE_MAGIC_STRING.length()), UTF_8);
		if (!PROFILER_FILE_MAGIC_STRING.equals(magicString)) {
			throw new IllegalArgumentException("Illegal magic string");
		}

		Snapshot s = new Snapshot();

		// undocumented
		byte majorVersion = in.readByte();
		byte minorVersion = in.readByte();
		if (majorVersion != SNAPSHOT_FILE_VERSION_MAJOR) {
			throw new IllegalArgumentException("Unsupported snapshot version: " + majorVersion);
		}
		s.setVersion(majorVersion, minorVersion);

		int type = in.readInt();
		if (type != SNAPSHOT_TYPE_CPU) {
			throw new IllegalArgumentException("Unsupported snapshot type: " + type);
		}
		s.setType(type);

		int compressedDataSize = in.readInt();

		// undocumented
		int uncompressedDataSize = in.readInt();

		InputStream data = ByteStreams.limit(in, compressedDataSize);
		CPUResultsSnapshot cpuResults = new CPUResultsSnapshot();
		cpuResults.readFromStream(new DataInputStream(new InflaterInputStream(data)));
		s.setCpuResults(cpuResults);

		int settingsSize = in.readInt();
		byte[] settingsBytes = readData(in, settingsSize);
		Properties settings = new Properties();
		settings.load(new ByteArrayInputStream(settingsBytes));
		s.setSettings(settings);

		s.setComments(in.readUTF());

		return s;
	}

	private void setCpuResults(CPUResultsSnapshot cpuResults) {
		this.cpuResults = cpuResults;

	}

	private void setVersion(byte majorVersion, byte minorVersion) {
		// TODO Auto-generated method stub

	}

	private void setComments(String comments) {
		this.comments = comments;
	}

	private void setSettings(Properties settings) {
		this.settings = settings;
	}

	private void setType(int type) {
		this.type = type;
	}
	
	
	
	public int getType() {
		return type;
	}

	public Properties getSettings() {
		return settings;
	}

	public String getComments() {
		return comments;
	}

	public CPUResultsSnapshot getCpuResults() {
		return cpuResults;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				// TODO add version
				.add("type", type)
				.add("cpuResults", cpuResults)
				.add("settings", settings)
				.add("comments", comments)
				.toString();
	}

	private static byte[] readData(DataInputStream in, int size) throws IOException {
		byte[] data = new byte[size];
		int dataLen = in.read(data);
		if (dataLen != size) {
			throw new IllegalArgumentException("Unable to read data (" + size + " bytes)");
		}
		return data;
	}
}
