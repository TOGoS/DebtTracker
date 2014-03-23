package togos.debttracker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Parser
{
	class Entity {
		public String name = "unnamed entity";
		public BigDecimal balance = BigDecimal.valueOf(0, 2);
		
		public void addBalance( BigDecimal delta ) {
			balance = balance.add(delta);
		}
	}
	
	Map<String,Entity> entities = new LinkedHashMap<String,Entity>(); 
	
	// TODO: Eventually put a dece tokenizer up in here
	static final Pattern ENTITY_PATTERN = Pattern.compile("entity \\s+ ([^\\s+]) \\s+ : \\s+ name \\s* @ \\s* \"([^\"]*)\"", Pattern.COMMENTS);
	
	public void readFile( BufferedReader br ) throws IOException {
		// TODO
	}
	
	public void readFile( InputStream is ) throws IOException {
		readFile( new BufferedReader(new InputStreamReader(is)));
	}
	
	public void readFile( String filename ) throws IOException {
		if( "-".equals(filename) ) {
			readFile( System.in );
		} else {
			FileInputStream fr = new FileInputStream(filename);
			try {
				readFile(fr);
			} finally {
				fr.close();
			}
		}
	}
	
	public void printDebts() throws IOException {
		// TODO
	}
	
	public static void main(String[] args) throws IOException {
		List<String> inputFiles = new ArrayList<String>();
		for( int i=0; i<args.length; ++i ) {
			if( !args[i].startsWith("-") || "-".equals(args[i])) {
				inputFiles.add(args[i]);
			}
		}
		
		Parser p = new Parser();
		for( String inputFile : inputFiles ) {
			p.readFile(inputFile);
		}
		p.printDebts();
	}
}
