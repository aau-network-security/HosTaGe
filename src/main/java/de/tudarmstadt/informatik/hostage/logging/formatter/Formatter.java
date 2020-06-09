package de.tudarmstadt.informatik.hostage.logging.formatter;


import de.tudarmstadt.informatik.hostage.logging.Record;

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

}
