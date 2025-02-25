package discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class Discovery {

	private static Logger Log = Logger.getLogger(Discovery.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	static final InetSocketAddress DISCOVERY_ADDR = new InetSocketAddress("226.226.226.226", 2266);
	static final int DISCOVERY_PERIOD = 1000;
	static final int DISCOVERY_TIMEOUT = 30000;
	
	static final int MAX_DATAGRAM_SIZE = 65536;


	private static final String DELIMITER = "\t";

	/**
	 * 
	 * Announces periodically a service in a separate thread .
	 * 
	 * @param serviceName the name of the service being announced.
	 * @param serviceURI  the location of the service
	 */
	public static void announce(String serviceName, String serviceURI) {
		Log.info(String.format("Starting Discovery announcements on: %s for: %s -> %s", DISCOVERY_ADDR, serviceName,
				serviceURI));

		byte[] pktBytes = String.format("%s%s%s", serviceName, DELIMITER, serviceURI).getBytes();

		DatagramPacket pkt = new DatagramPacket(pktBytes, pktBytes.length, DISCOVERY_ADDR);
		new Thread(() -> {
			try (DatagramSocket ms = new DatagramSocket()) {
				for (;;) {
					ms.send(pkt);
					Thread.sleep(DISCOVERY_PERIOD);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Performs discovery of instances of the service with the given name.
	 * 
	 * @param serviceName      the name of the service being discovered
	 * @param minRepliesNeeded the required number of service replicas to find.
	 * @return an array of URI with the service instances discovered. Returns an
	 *         empty, 0-length, array if the service is not found within the alloted
	 *         time.
	 * @throws IOException 
	 * 
	 */
	public static URI[] findUrisOf(String serviceName, int minRepliesNeeded){
		// TODO: treat exception?
		try (MulticastSocket socket = new MulticastSocket(DISCOVERY_ADDR.getPort())) {
			socket.joinGroup(DISCOVERY_ADDR.getAddress());

			Set<URI> result = new HashSet<URI>(minRepliesNeeded);
			long timeToDie = System.currentTimeMillis() + DISCOVERY_TIMEOUT;

			while (true) {
				byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.setSoTimeout(DISCOVERY_TIMEOUT);
				socket.receive(request);

				String message = new String(request.getData(), 0, request.getLength());
				System.out.println("Received " + message);
				URI serviceURI = URI.create(message.split(DELIMITER)[1]);
				String name = message.split(DELIMITER)[0];

				if (result.contains(serviceURI)) {
					int temp = (int) (timeToDie - System.currentTimeMillis());
					if (temp <= 0)
						break;
					socket.setSoTimeout(temp);
				} else {
					if (serviceName.equals(name)) {
						result.add(serviceURI);
						timeToDie = System.currentTimeMillis() + DISCOVERY_TIMEOUT;
					}
				}

				if (result.size() >= minRepliesNeeded) {
					break;
				}
			}

			return result.toArray(new URI[result.size()]);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}
}
