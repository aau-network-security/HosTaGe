package dk.aau.netsec.hostage.logging.formatter;

import dk.aau.netsec.hostage.logging.Record;
import dk.aau.netsec.hostage.logging.RecordAll;
import dk.aau.netsec.hostage.logging.formatter.protocol.ProtocolFormatter;

public class DefaultFormatter extends Formatter {

	private static final Formatter INSTANCE = new DefaultFormatter();

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

	@Override
	public String format(RecordAll record) {
		ProtocolFormatter formatter = ProtocolFormatter.getFormatter(record
				.getProtocol());
		return String.format("%d %s [%d,%s:%d,%s:%d,%s]",
				record.getAttack_id(), record.getType().name(),
				record.getTimestamp(), record.getLocalIP(),
				record.getLocalPort(), record.getRemoteIP(),
				record.getRemotePort(), (record.getPacket() == null ? "" : formatter.format(record.getPacket())));
	}

}
