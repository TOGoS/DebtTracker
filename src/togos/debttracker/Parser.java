package togos.debttracker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser
{
	public static class Entity {
		public final String id;
		public String name = "unnamed entity";
		public String emailAddress = null;
		public BigDecimal balance = BigDecimal.valueOf(0, 2);
		
		public Entity( String id ) {
			this.id = id;
		}
		
		public void addBalance( BigDecimal delta ) {
			balance = balance.add(delta);
		}
	}
	
	public Map<String,Entity> entities = new LinkedHashMap<String,Entity>();
	protected static final BigDecimal ZERO = new BigDecimal("0.00");
	public int transactionCount;
	
	// TODO: Eventually put a dece tokenizer up in here
	static final Pattern ENTITY_PATTERN = Pattern.compile(
		"entity \\s+ ([^\\s]+) " +
		"(?: \\s+ : \\s+ name \\s* @ \\s* \"([^\"]*)\" )?" +
		"(?: \\s+ : \\s+ email \\s* @ \\s* \"([^\"]*)\" )?",
		Pattern.COMMENTS);
	static final Pattern ALIAS_PATTERN = Pattern.compile("alias \\s+ ([^\\s]+) \\s+ = \\s+ ([^\\s]+)", Pattern.COMMENTS);
	static final Pattern TRANSACTION_PATTERN = Pattern.compile(
		"(\\d\\d\\d\\d-\\d\\d-\\d\\d)" +
		"\\s+ : \\s+ (\\d+) " +
		"\\s+ @ \\s+ ((?!->).+?) " +
		"\\s+ -> \\s+ ([^;]+?) " +
		"(?: \\s+ ; (.*))?",
		Pattern.COMMENTS);
	
	protected static Entity getEntity( Map<String,Entity> aliases, String name ) {
		Entity e = aliases.get(name);
		if( e == null ) throw new RuntimeException("Reference to undefined entity '"+name+"'");
		return e;
	}
	
	protected static Entity[] getEntities( Map<String,Entity> aliases, String list ) {
		String[] names = list.split(",");
		Entity[] entities = new Entity[names.length];
		for( int i=0; i<names.length; ++i ) {
			entities[i] = getEntity(aliases, names[i].trim());
		}
		return entities;
	}
	
	protected static BigDecimal[] divide( BigDecimal amt, int parts ) {
		BigDecimal d = amt.divide(new BigDecimal(parts), RoundingMode.HALF_EVEN);
		BigDecimal remainder = amt.subtract(d.multiply(new BigDecimal(parts)));
		// For now just lump all remainder on one dude
		// would be better to split remainder as even as possible
		BigDecimal[] dex = new BigDecimal[parts];
		dex[0] = d.add(remainder);
		for( int i=1; i<parts; ++i ) {
			dex[i] = d;
		}
		return dex;
	}
	
	public void readFile( BufferedReader br ) throws IOException {
		Map<String, Entity> aliases = new HashMap<String, Entity>(entities);
		String line;
		Matcher m;
		boolean readingTransfers = false;
		while( (line = br.readLine()) != null ) {
			line = line.trim();
			if( "".equals(line) || line.startsWith("#") ) continue;
			
			if( "=transfers".equals(line) ) {
				readingTransfers = true;
			} else 	if( (m = ENTITY_PATTERN.matcher(line)).matches() ) {
				String entityId = m.group(1);
				Entity e = new Entity(entityId);
				e.name = m.group(2) == null ? entityId : m.group(2);
				e.emailAddress = m.group(3);
				entities.put(entityId, e);
				aliases.put(entityId, e);
			} else if( (m = ALIAS_PATTERN.matcher(line)).matches() ) {
				String newId = m.group(1);
				String oldId = m.group(2);
				Entity e = getEntity(aliases, oldId);
				aliases.put(newId, e);
			} else if( (m = TRANSACTION_PATTERN.matcher(line)).matches() ) {
				if( !readingTransfers ) {
					throw new RuntimeException("Missing '=transfers' line?");
				}
				//String dateStr = m.group(1);
				String amtStr = m.group(2);
				String payerStr = m.group(3);
				String receiverStr = m.group(4);
				//String comment = m.group(5);
				Entity[] payers = getEntities(aliases, payerStr);
				Entity[] recipients = getEntities(aliases, receiverStr);
				BigDecimal amount = new BigDecimal(amtStr);
				BigDecimal[] paidAmounts = divide(amount, payers.length);
				for( int i=0; i<payers.length; ++i ) {
					payers[i].addBalance(paidAmounts[i]);
				}
				BigDecimal[] receivedAmounts = divide(amount, recipients.length);
				for( int i=0; i<recipients.length; ++i ) {
					recipients[i].addBalance(receivedAmounts[i].negate());
				}
				++transactionCount;
			} else {
				throw new RuntimeException("Unrecognized line: " + line);
			}
		}
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
	
	public void readString( String text ) {
		try {
			readFile(new BufferedReader(new StringReader(text)));
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void printDebts() throws IOException {
		System.out.println(transactionCount + " transactions processed.");
		boolean anyNonZeroBalances = false;
		for( Entity e : entities.values() ) {
			if( !e.balance.equals(ZERO) ) {
				if( !anyNonZeroBalances ) {
					System.err.println("Ending balances:");
				}
				System.out.println(String.format("%20s : % 10.2f", e.name, e.balance.doubleValue()));
				anyNonZeroBalances = true;
			}
		}
		if( anyNonZeroBalances ) {
			System.err.println(
				"Balance indicates how much money one is owed.\n" +
				"Those with negative balances should spend some money on (or give money to)\n" +
				"those with higher balances until their own balance is zero."
			);
		} else {
			System.err.println("All balances are zero!");
		}
	}
	
	public static void main(String[] args) throws IOException {
		List<String> inputFiles = new ArrayList<String>();
		for( int i=0; i<args.length; ++i ) {
			if( !args[i].startsWith("-") || "-".equals(args[i])) {
				inputFiles.add(args[i]);
			}
		}
		if( inputFiles.size() == 0 ) inputFiles.add("-");
		
		Parser p = new Parser();
		for( String inputFile : inputFiles ) {
			p.readFile(inputFile);
		}
		p.printDebts();
	}
}
