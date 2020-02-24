package de.tudarmstadt.informatik.hostage.logging.formatter;

import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.formatter.protocol.ProtocolFormatter;

public class DefaultFormatter extends Formatter {

	private static Formatter INSTANCE = new DefaultFormatter();

	public static Formatter getInstance() {
		return INSTANCE;
	}

	private DefaultFormatter() {
	}

	@Override
	public synchronized String format(Record record) {
		ProtocolFormatter formatter = ProtocolFormatter.getFormatter(record
				.getProtocol());
		return String.format("%d %s [%d,%s:%d,%s:%d,%s]",
				record.getAttack_id(), record.getType().name(),
				record.getTimestamp(), record.getLocalIP(),
				record.getLocalPort(), record.getRemoteIP(),
				record.getRemotePort(), (record.getPacket() == null ? "" : formatter.format(record.getPacket())));
	}

}
