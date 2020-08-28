package dk.aau.netsec.hostage.logging.formatter;


import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.logging.RecordAll;

public abstract class Formatter {

	/**
	 * @return Instance of DefaultFormatter.
	 */
	public static Formatter getDefault() {
		return DefaultFormatter.getInstance();
	}

	/**
	 * Formats a record.
	 * 
	 * @param record
	 *            Record to format.
	 * @return Formatted human-readable String.
	 */
	abstract public String format(Record record);

	abstract public String format(RecordAll record);


}
