import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A self re-writing class that morphs based on principles of DNA mutation and copy errors.
 * 
 * @author jzheaux
 *
 */
public class SelfContainedMissionStatement {
	private final String strand = "GG";
	private final StrandSplicer ss = new StrandSplicer();
	
	public void copy(StrandHolder sh, String name, OutputStream os) throws IOException {
		String newStrand = ss.splice(new StrandHolder(this), sh);
		
		File src = new File(this.getClass().getName() + ".java");
		try ( InputStream in = new FileInputStream(src);
				OutputStream out = new StrandHolderOutputStream(os, name, newStrand) ) {
			IOUtils.copy(in, out);
		}
	}
	
	public void render(OutputStream os) throws IOException {
		try ( InputStream in = new ByteArrayInputStream(strand.getBytes());
				OutputStream out = new DataOutputStream(os) ) {
			IOUtils.copy(in, out);
		}
	}
	
	public String getStrand() {
		return strand;
	}
	
	/**
	 * A reflective wrapper to keep each individual mission statement class from having
	 * to inherit from its predecessor.
	 * 
	 * @author jzheaux
	 *
	 */
	private static class StrandHolder {
		private Object strand;
		
		public StrandHolder(Object strand) {
			this.strand = strand;
		}
		
		public void copy(StrandHolder sh, String name, OutputStream os) throws IOException {
			try {
				Method m = strand.getClass().getMethod("copy", StrandHolder.class, String.class, OutputStream.class);
				m.invoke(strand, sh, name, os);
			} catch ( Throwable t ) {
				throw new IOException(t);
			}
		}
		
		public void render(OutputStream os) throws IOException {
			try {
				Method m = strand.getClass().getMethod("render", OutputStream.class);
				m.invoke(strand, os);
			} catch ( Throwable t ) {
				throw new IOException(t);
			}
		}
		
		public String getStrand() {
			try {
				Method m = strand.getClass().getMethod("getStrand");
				return (String)m.invoke(strand);
			} catch ( Throwable t ) {
				throw new IllegalStateException(t);
			}
		}
	}
	
	
	public static class StrandSplicer {
		private double replacementProbability = 0.1;
		private double deletionProbability = 0.01;
		private double insertionProbability = 0.25;
		
		private final Random random = new Random();
		private final char[] codes = { 'G', 'A', 'T', 'C' };
		
		public StrandSplicer() {}
		public StrandSplicer(double r, double d, double i) {
			replacementProbability = r;
			deletionProbability = d;
			insertionProbability = i;
		}
		
		public String splice(StrandHolder left, StrandHolder right) {
			String lStrand = left.getStrand();
			String rStrand = right.getStrand();
			StringBuilder newStrand = new StringBuilder();
			for ( int i = 0; i < Math.max(rStrand.length(), lStrand.length()); i++ ) {
				double rand = random.nextDouble();
				double normalizer = ( 1 + i / (double)Math.max(rStrand.length(), lStrand.length()));
				if ( rand <= deletionProbability ) { // deletion
					// don't copy at all
				} else if ( rand <= replacementProbability * normalizer ) { // replacement
					int newChar = random.nextInt(4);
					newStrand.append(codes[newChar]);
				} else { // insertion
					char toCopy;
					if ( i < rStrand.length() && i < lStrand.length() ) {
						toCopy = random.nextBoolean() ? rStrand.charAt(i) : lStrand.charAt(i); 
					} else {
						toCopy = rStrand.length() > lStrand.length() ? rStrand.charAt(i) : lStrand.charAt(i);
					}
					
					newStrand.append(toCopy);
					if ( rand <= insertionProbability * normalizer ) {
						int newChar = random.nextInt(4);
						newStrand.append(codes[newChar]);
					}
				}
			}
			
			return newStrand.toString();
		}
	}

	/**
	 * Switches key references from current class to new class in raw Java file.
	 * 
	 * @author jzheaux
	 *
	 */
	public static class StrandHolderOutputStream extends OutputStream {
		private OutputStream os;
		private StringBuilder sb = new StringBuilder();
		private String name;
		private String strand;

		public StrandHolderOutputStream(OutputStream os, String name, String strand) {
			this.os = os;
			this.name = name;
			this.strand = strand;
		}
		
		@Override
		public void write(int b) throws IOException {
			if ( b == 10 ) {
				String line = sb.toString();
				os.write(line.replaceAll(SelfContainedMissionStatement.class.getSimpleName(), name)
							.replaceAll("private final String strand = \".*\";", "private final String strand = \"" + strand + "\";").getBytes());
				os.write((char)b);
				sb = new StringBuilder();
			} else {
				sb.append((char)b);
			}
		}

		public void flush() throws IOException {
			os.write(sb.toString().getBytes());
		}
		
		public void close() throws IOException {
			flush();
		}
	}

	
	/**
	 * Takes in a DNA strand (a sequence of As, Cs, Gs, and Ts) and formulates
	 * it into a mission statement.
	 * 
	 * @author jzheaux
	 *
	 */
	public static class MissionStatementOutputStream extends OutputStream {
		private OutputStream os;
		
		/**
		 * The strand-to-english lookup table for converting, say AC into 
		 */
		private static final Map<String, InstructionSet> instruction = new HashMap<String, InstructionSet>();
		
		/**
		 * The different patterns that will manifest, depenending on the length of the string
		 */
		private static final String[] patterns = new String[] {
			"",
			"verb",
			"preamble verb",
			"preamble verb conclusion",
			"preamble verb conjunction verb",
			"preamble verb conjunction verb noun",
			"preamble verb noun conjunction verb noun",
			"preamble verb adjective noun conjunction verb noun",
			"preamble verb adjective noun conjunction verb adjective noun",
			"preamble adverb verb adjective noun conjunction verb adjective noun",
			"preamble adverb verb adjective noun conjunction adverb verb adjective noun",
			"preamble adverb verb adjective noun conjunction adverb verb adjective noun conclusion"
		};
		
		static {
			try {
				addToInstructions("preamble", "11Our mission is to\n20We\n21We will commit to\n22We commit to\n23We strive to\n24We will work to\n300Out job is to\n310It is our job to\n311It is our mission to\n312It it our challenge to\n313It is our objective to\n314It is our responsibility to\n320Our challenge is to continue to\n321Our job is to continue to\n322Our objective is to continue to\n323Our mission is to continue to\n324We strive to continue to\n330We commit to continue to\n331We will commit to continue to\n332We will work to continue to\n333We will strive to continue to\n4000It is our inescapable duty to continue to\n4100It is our certain duty to continue to\n4110It is our solemn and incontrovertible duty to continue to\n4111Our solumn and incontrovertible duty is to continue to\n");
				addToInstructions("adverb", "1assertively\n20authoritatively\n21collaboratively\n22competently\n23completely\n24continually\n300conveniently\n310dramatically\n311efficiently\n312enthusiastically\n313globally\n314interactively\n320proactively\n321professionally\n322quickly\n323seamlessly\n324synergistically\n");
				addToInstructions("verb", "1evolve\n20administrate\n21build\n22coordinate\n23create\n24customize\n300disseminate\n310engineer\n311enhance\n312embrace\n313facilitate\n314fashion\n320foster\n321initiate\n322integrate\n323leverage\n324leverage others'\n330leverage existing\n331maintain\n332monetize\n333morph\n334negotiate\n340network\n341operationalize\n342optimize\n343orchestrate\n344productize\n4000promote\n4100provide access to\n4110pursue\n4111recontextualise\n4112restore\n4113revolutionize\n4114simplify\n4120synergize\n4121synthesize\n4122supply\n4123utilize");
				addToInstructions("adjective", "1best-of-breed\n20best practice\n21business\n22competitive\n23corporate\n24cost effective\n300cutting-edge\n310diverse\n311e-business\n312e-commerce\n313e-markets\n314e-services\n320e-tailers\n321economically sound\n322effective\n323emerging\n324enterprise\n330enterprise-wide\n331error-free\n332ethical\n333excellent\n334extensible\n340high standards in\n341high-payoff\n342high-quality\n343holistic\n344inexpensive\n4000innovative\n4100interdependent\n4110leading-edge\n4111long-term high-impact\n4112low-risk high-yield\n4113market-driven\n4114mission-critical\n4120multimedia based\n4121next-generation\n4122outcome-driven\n4123parallel\n4124performance based\n4130paradigm-shift\n4131principle-centered\n4132professional\n4133progressive\n4134prospective\n4140quality\n4141resource-leveling\n4142revolutionary\n4143scalable\n4144seven-habits-conforming\n4200timely\n4210unique\n4211user-centric\n4212value-added\n4213viral\n4214virtual\n4220web 2.0\n4221web 3.0\n4222world-class");
				addToInstructions("noun", "1benefits\n20catalysts for change\n21content\n22data\n23deliverables\n24information\n300infrastructures\n310intellectual capital\n311leadership skills\n312materials\n313meta-services\n314methods\n320methods of empowerment\n321mindshare\n322opportunities\n323paradigms\n324products\n330resources\n331services\n332solutions\n333sources\n334technology");
				addToInstructions("conjunction", "1and\n20as well as\n21to allow us to continue to\n22as well as continue to\n23in order to\n24in order to continue to\n300while endeavoring to\n310while endeavoring to continue to\n311while continuing to\n312as well as endeavor to\n313as well as endeavor to continue to\n");
				addToInstructions("conclusion", "1to solve business problems\n20to stay relevant in tomorrow's world\n21to stay pertinent in tomorrow's world\n22while encouraging personal employee growth\n23to set us apart from the competition\n24to meet our customer's needs\n");
			} catch ( IOException e ) {
				throw new IllegalArgumentException(e);
			}
		}
		
		private final Map<Character, Integer> chars = new HashMap<Character, Integer>() {{
			put('G', 0);
			put('A', 1);
			put('T', 2);
			put('C', 3);
		}};
		
		private static void addToInstructions(String name, String set) throws IOException {
			instruction.put(name, new InstructionSet(new ByteArrayInputStream(set.getBytes())));
		}
		
		private List<Integer> codes = new ArrayList<Integer>();
		private int charsLeftInCode = 0;
		
		/**
		 * 
		 * @param os - To where the mission statement should be written
		 */
		public MissionStatementOutputStream(OutputStream os) {
			this.os = os;
		}

		@Override
		public void write(int b) throws IOException {
			if ( b != 10 ) {
				int length = chars.get((char)b) + 1;
				if ( charsLeftInCode <= 0 ) {
					charsLeftInCode = length;
					
					// sets code immediately to 1, 20, 300, or 4000.
					// this way, codes sequences starting with the same 
					// letter will always have the same length
					codes.add((int)(length * Math.pow(10, length - 1)));
				} else {
					// fills in the corresponding zeros in the code according to the
					// numeric value of the protein, e.g.
					// A = 20
					// C = 4000
					// AC = 24
					// TAT = 323
					int current = codes.get(codes.size()-1);
					int newValue = (int)(current + length * Math.pow(10, charsLeftInCode - 1));
					codes.set(codes.size() - 1, newValue);
				}
				charsLeftInCode--;
			}
		}
		
		public void flush() throws IOException {		
			int whichPattern = Math.min(patterns.length - 1, codes.size());
			String pattern = patterns[whichPattern];
			try ( Scanner scanner = new Scanner(pattern) ) {
				int index = 0;
				while ( scanner.hasNext() ) {
					InstructionSet dna = instruction.get(scanner.next());
					Integer code = codes.get(index++);
					String instruction = dna.get(code);
					os.write(instruction.getBytes());
					if ( index != whichPattern ) {
						os.write(' ');
					} else {
						os.write('.');
						os.write('\n');
					}
				}
			}
			
			os.flush();
			codes = new ArrayList<Integer>();
			charsLeftInCode = 0;
		}

		public void close() throws IOException {
			flush();
		}
	}

	/**
	 * A utility class for copying an input stream to an output stream.
	 * 
	 * @author jzheaux
	 *
	 */
	public static class IOUtils {
		private IOUtils() {}
		
		public static void copy(InputStream is, OutputStream os) throws IOException {
			byte[] b = new byte[1024];
			int read;
			while ( ( read = is.read(b, 0, b.length) ) != -1 ) {
				os.write(b, 0, read);
			}
		}
	}

	/**
	 * Represents interpretations of DNA codes to English elements of mission statements.
	 * @author jzheaux
	 *
	 */
	public static class InstructionSet {
		private static final Pattern DNA_FILE_LINE_PATTERN = Pattern.compile("(\\d+)(.*)");
		
		private final Map<Integer, String> instructions;
		private final List<Integer> keys;
		
		public InstructionSet(InputStream src) throws IOException {
			Map<Integer, String> instructions = new LinkedHashMap<Integer, String>();
			List<Integer> keys = new ArrayList<Integer>();
			
			try ( BufferedReader br = new BufferedReader(new InputStreamReader(src)) ) {
				String line;
				while ( ( line = br.readLine() ) != null ) {
					Matcher m = DNA_FILE_LINE_PATTERN.matcher(line);
					if ( m.find() ) {
						instructions.put(Integer.parseInt(m.group(1)), m.group(2));
						keys.add(Integer.parseInt(m.group(1)));
					}
				}
				this.instructions = Collections.unmodifiableMap(instructions);
				this.keys = Collections.unmodifiableList(keys);
			}
		}
		
		public String get(Integer i) {
			return instructions.get(findBiggestKeySmallerThanOrEqualTo(i, 0, keys.size()));
		}
		
		public Integer findBiggestKeySmallerThanOrEqualTo(Integer value, int min, int max) {
			int index = ( min + max ) / 2;
			if ( value.equals(keys.get(index)) ) {
				return keys.get(index);
			}
				
			if ( max == min + 1 ) return keys.get(min);
			
			if ( keys.get(index) > value ) {
				return findBiggestKeySmallerThanOrEqualTo(value, min, index);
			} else {
				return findBiggestKeySmallerThanOrEqualTo(value, index, max);
			}
		}
	}
	
	public static StrandHolder bootstrap(String name) throws Exception {
		return new StrandHolder(Class.forName(name).newInstance());
	}
	
	private static StrandHolder compileAndLoad(String name) throws Exception {
		String javaHome = System.getProperty("java.home");
		File file = new File(javaHome);
		File javac = new File(new File(file.getParent(), "bin"), "javac");
		File toCompile = new File(name + ".java");
		
		ProcessBuilder pb = new ProcessBuilder(javac.getAbsolutePath(), "-cp", ".", toCompile.getAbsolutePath()).inheritIO();
		Process pe = pb.start();
		int code = pe.waitFor();
		SelfContainedMissionStatement.class.getClassLoader().loadClass(name);
		
		return bootstrap(name);
	}
	
	public static StrandHolder bootstrap(StrandHolder left, StrandHolder right, String name) throws Exception {
		left.copy(right, name, new FileOutputStream(name + ".java"));
		return compileAndLoad(name);
	}
	
	public static StrandHolder bootstrap(StrandHolder left, String rightStrandName, String name) throws Exception {
		StrandHolder right = compileAndLoad(rightStrandName);
		return bootstrap(left, right, name);
	}
	
	public static StrandHolder bootstrap(String leftStrandName, String rightStrandName, String name) throws Exception {
		StrandHolder left = compileAndLoad(leftStrandName);
		return bootstrap(left, rightStrandName, name);
	}
	
	public static void main(String[] args) throws Exception {
		StrandHolder ms = new StrandHolder(new SelfContainedMissionStatement());
		if ( args.length == 0 ) {

		} else if ( args.length == 1 ) {
			ms = bootstrap(ms, ms, args[0]);
		} else {
			ms = bootstrap(ms, args[0], args[1]);
		}
		ms.render(new MissionStatementOutputStream(System.out));
	}
}
