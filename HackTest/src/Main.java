import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Main {
	private static boolean
	download(String url, String fname) {
		try (FileOutputStream fos = new FileOutputStream(fname)) {
			URL target = new URL(url);
			ReadableByteChannel rbc = Channels.newChannel(target.openStream());
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			return true;
		} catch (IOException e) {
			new File(fname).delete();
			return false;
		}
	}

	public static void
	main(String[] args) {
		YTHackTest ht = new YTHackTest("uJ374_1FAfM");
		if (YTHackTest.Err.NO_ERR != ht.startHack())
			System.out.println("ERROR : HACK");
		else {
			for (YTHackTest.YtVideoElem ve : ht.getVideoElems()) {
				if (!download(ve.url, ve.type.type.name()))
					System.out.println("Fail : downlaod: " + ve.url);
			}
		}
	}
}
