package togos.debttracker;

import java.math.BigDecimal;

import junit.framework.TestCase;
import togos.debttracker.Parser.Entity;

public class ParserTest extends TestCase
{
	protected static final String TED_EID = "ccd4d1b4-1a3d-460c-902f-681dcc0194fd";
	protected static final String BOB_EID = "1be5ab07-7829-4643-899b-bf5e4bdb12b8";
	
	protected static final String introText =
		"# A comment line!\n" +
		"entity "+TED_EID+" : name @ \"Ted\" : email @ \"ted@example.com\"\n" +
		"entity "+BOB_EID+" : name @ \"Bob Q\"\n" +
		"\n" + // A blank line!
		"alias Ted = "+TED_EID+"\n" +
		"alias BobQ = "+BOB_EID+"\n" +
		"\n";
	
	protected static final String transactionText =
		introText +
		"2014-01-03 : 600 @ BobQ -> BobQ, Ted ; rent\n";
	
	public void testParseIntro() {
		Parser p = new Parser();
		p.readString(introText);
		assertEquals(2, p.entities.size());
		{
			Entity ted = p.entities.get("ccd4d1b4-1a3d-460c-902f-681dcc0194fd");
			assertEquals("ccd4d1b4-1a3d-460c-902f-681dcc0194fd", ted.id);
			assertEquals("Ted", ted.name);
		}
		{
			Entity bob = p.entities.get("1be5ab07-7829-4643-899b-bf5e4bdb12b8");
			assertEquals("1be5ab07-7829-4643-899b-bf5e4bdb12b8", bob.id);
			assertEquals("Bob Q", bob.name);
		}
	}
	
	public void testTransactions() {
		Parser p = new Parser();
		p.readString(transactionText);
		assertEquals(new BigDecimal("-300.00"), p.entities.get(TED_EID).balance);
		assertEquals(new BigDecimal("+300.00"), p.entities.get(BOB_EID).balance);
	}
}
