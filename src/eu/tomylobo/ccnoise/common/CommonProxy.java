package eu.tomylobo.ccnoise.common;

public class CommonProxy {
	public static final String TEX_BLOCKS = "/eu/tomylobo/ccnoise/ccnoise_blocks.png";

	public void init() {
	}

	public void debugPrint(String format, Object... args) {
		System.out.println(String.format(format, args));
	}
}
