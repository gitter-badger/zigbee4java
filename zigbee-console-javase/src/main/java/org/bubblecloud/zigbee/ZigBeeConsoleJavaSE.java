package org.bubblecloud.zigbee;

import org.apache.log4j.xml.DOMConfigurator;
import org.bubblecloud.zigbee.network.SerialPort;
import org.bubblecloud.zigbee.network.port.SerialPortImpl;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Example runtime arguments on Mac OS-X: /dev/cu.usbmodem1411 4951 22 false
 * @author <a href="mailto:christopherhattonuk@gmail.com">Chris Hatton</a>
 */
public class ZigBeeConsoleJavaSE {
    /**
     * The {@link org.slf4j.Logger}.
     */
    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ZigBeeConsoleJavaSE.class);
    /**
     * The default baud rate.
     */
	private static final int DefaultBaudRate = 38400;
    /**
     * The usage.
     */
	public static final String USAGE = "Syntax: java -jar zigbee4java-serialPort.jar SERIALPORT CHANNEL PAN RESET" +
			" [HTTP(S) PORT] [AUTHORIZATION TOKEN] [KEYSTORE] [SSLPROTOCOL1,SSLPROTOCOL2...]";

    /**
     * Private constructor to disable constructing main class.
     */
	private ZigBeeConsoleJavaSE() {
    }

    /**
     * The main method.
     * @param args the command arguments
     */
	public static void main(final String[] args) {
		DOMConfigurator.configure("./log4j.xml");

		final String serialPortName;
		final int channel;
		final int pan;
		final boolean resetNetwork;
        final int port;
        final String apiAccessToken;
        final String keystorePath;
        final char[] password;
        final String[] sslProtocols;
		try {
			serialPortName = args[0];
			channel        = Integer.parseInt(args[1]);
			pan            = parseDecimalOrHexInt(args[2]);			
			resetNetwork   = args[3].equals("true");

            if (args.length > 4) {
                port = Integer.parseInt(args[4]);
            } else {
                port = -1;
            }
            if (args.length > 5) {
                apiAccessToken = args[5];
            } else {
                apiAccessToken = null;
            }
            if (args.length > 6) {
                keystorePath = args[6];
            } else {
                keystorePath = null;
            }
            if (args.length > 7) {
                sslProtocols = args[7].split(",");
            } else {
                sslProtocols = null;
            }
            if (args.length > 6) {
                System.out.printf("Please enter your password: ");
                char[] passwordInput = System.console().readPassword();
                if (passwordInput.length > 6) {
                    password = passwordInput;
                } else {
                    password = null;
                }
            } else {
                password = null;
            }

		} catch (final Throwable t) {
            t.printStackTrace();
			System.out.println(USAGE);
			return;
		}

		final SerialPort serialPort = new SerialPortImpl(serialPortName, DefaultBaudRate);
		final ZigBeeConsole console = new ZigBeeConsole(serialPort,pan,channel,resetNetwork);

        final AuthorizationProvider authorizationProvider = new AuthorizationProvider() {
            @Override
            public AccessLevel getAccessLevel(final String accessToken) {
                return apiAccessToken != null &&
                        apiAccessToken.equals(accessToken) ? AccessLevel.ADMINISTRATION : AccessLevel.NONE;
            }
        };

        final ZigBeeConsoleHttpServer zigBeeConsoleServer;
        if (args.length > 5) {
            try {
                zigBeeConsoleServer = new ZigBeeConsoleHttpServer(console, port, keystorePath, password, sslProtocols,
                        authorizationProvider);
                zigBeeConsoleServer.start();
            } catch (final IOException e) {
                LOGGER.error("Error starting ZigBeeConsole HTTP server in port: " + port, e);
                return;
            }
        } else {
            zigBeeConsoleServer = null;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (zigBeeConsoleServer != null) {
                    zigBeeConsoleServer.stop();
                }
            }
        }));

        console.start();

	}

    /**
     * Parse decimal or hexadecimal integer.
     * @param s the string
     * @return the parsed integer value
     */
	private static int parseDecimalOrHexInt(String s) {
		int radix = 10;
		String number = s;
		if (number.startsWith("0x")) {
			number = number.substring(2);
			radix = 16;
		}
		return Integer.parseInt(number, radix);
	}
}
