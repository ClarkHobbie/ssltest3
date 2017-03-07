import org.apache.log4j.xml.DOMConfigurator;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.SecureRandom;


public class SSLTest {
    public static class ServerHandler extends IoHandlerAdapter {
        @Override
        public void sessionCreated(IoSession session) throws Exception {
            System.out.println ("Got connection");
        }

        @Override
        public void messageReceived(IoSession session, Object message ) throws Exception {
            String s = message.toString();
            s.trim();
            System.out.println("Got " + s);
            session.write(s);
        }
    }


    public static class ClientHandler extends IoHandlerAdapter {
        private String prompt;

        public ClientHandler (String prompt) {
            this.prompt = prompt;
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            System.out.println ("\nGot " + message.toString());
            System.out.print (prompt);
        }
    }


    public static class CommandLine {
        private String[] argv;
        private int argIndex = 0;
        private String mode = "server";
        private boolean useTls = true;
        private String host = "localhost";
        private int port = 6789;
        private String log4jFilename = "log4j.xml";

        public String getLog4jFilename() {
            return log4jFilename;
        }

        public void setLog4jFilename(String log4jFilename) {
            this.log4jFilename = log4jFilename;
        }

        public String[] getArgv () {
            return argv;
        }

        public String getArg () {
            if (argIndex >= argv.length)
                return null;

            return argv[argIndex];
        }

        public void advance () {
            argIndex++;
        }

        public String getMode () {
            return mode;
        }

        public void setMode (String mode) {
            this.mode = mode;
        }

        public boolean useTls () {
            return useTls;
        }

        public void setUseTls (boolean useTls) {
            this.useTls = useTls;
        }

        public String getHost () {
            return host;
        }

        public void setHost (String host) {
            this.host = host;
        }

        public int getPort () {
            return port;
        }

        public void setPort (int port) {
            this.port = port;
        }

        public CommandLine (String[] argv) {
            this.argv = argv;
            parse();
        }

        public void parse () {
            if (argv.length < 1)
                return;

            if (null != getArg() && getArg().equalsIgnoreCase("nossl")) {
                setUseTls(false);
                advance();
            }

            //
            // client/server mode
            //
            if (null != getArg()) {
                setMode(getArg());
                advance();
            }

            //
            // host
            //
            if (null != getArg()) {
                setHost(getArg());
                advance();
            }

            //
            // port
            //
            if (null != getArg()) {
                int temp = Integer.parseInt(getArg());
                setPort(temp);
                advance();
            }

            while (null != getArg())
            {
                if (getArg().equalsIgnoreCase("-l"))
                {
                    advance();
                    setLog4jFilename(getArg());
                    advance();
                }
            }
        }
    }

    private boolean useTls = true;

    public boolean useTls () {
        return useTls;
    }

    public SSLTest(boolean useTls) {
        this.useTls = useTls;
    }

    public static void closeIgnoreExceptions (InputStream inputStream) {
        if (null != inputStream) {
            try {
                inputStream.close();
            } catch (IOException e) {}
        }
    }

    public KeyStore getKeyStore (String filename, String password) {
        KeyStore keyStore = null;
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(filename);
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load (fileInputStream, password.toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            closeIgnoreExceptions(fileInputStream);
        }

        return keyStore;
    }


    public void server (int port) {
        try {
            String trustStoreFilename = "truststore";
            String trustStorePassword = "whatever";

            String keyStoreFilename = "serverkeystore";
            String keyStorePassword = "whatever";
            KeyStore keyStore = getKeyStore(keyStoreFilename, keyStorePassword);

            KeyStore trustStore = getKeyStore(trustStoreFilename, trustStorePassword);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            IoAcceptor acceptor = new NioSocketAcceptor();

            if (useTls()) {
                SslFilter sslFilter = new SslFilter(sslContext);
                acceptor.getFilterChain().addLast("tls", sslFilter);
            }

            acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));

            IoHandler serverHandler = new ServerHandler();
            acceptor.setHandler(serverHandler);

            InetSocketAddress socketAddress = new InetSocketAddress(port);

            System.out.println ("listening on port " + port);

            acceptor.bind(socketAddress);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void client (String host, int port) {
        try {
            String trustStoreFilename = "truststore";
            String trustStorePassword = "whatever";

            KeyStore keyStore = getKeyStore(trustStoreFilename, trustStorePassword);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init (keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init (null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            NioSocketConnector connector = new NioSocketConnector();

            if (useTls()) {
                SslFilter sslFilter = new SslFilter(sslContext);
                sslFilter.setUseClientMode(true);
                connector.getFilterChain().addLast("tls", sslFilter);
            }

            connector.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory( Charset.forName( "UTF-8" ))));

            String prompt = host + ":" + port +"> ";

            ClientHandler clientHandler = new ClientHandler(prompt);
            connector.setHandler(clientHandler);

            System.out.println ("connecting to " + host + ":" + port);


            InetSocketAddress address = new InetSocketAddress(host, port);
            ConnectFuture connectFuture = connector.connect(address);
            connectFuture.awaitUninterruptibly();

            IoSession session = connectFuture.getSession();

            System.out.print(prompt);

            InputStreamReader inputStreamReader = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(inputStreamReader);

            String s = in.readLine();
            s.trim();

            while (!s.equalsIgnoreCase("quit")) {
                session.write(s);
                System.out.print(prompt);
                s = in.readLine();
                s.trim();
            }

            in.close();
            CloseFuture closeFuture = session.closeNow();
            closeFuture.awaitUninterruptibly();
            connector.dispose();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void configureLg4j (String filename) {
        DOMConfigurator.configure(filename);
    }


    public static void main (String[] argv) {
        CommandLine commandLine = new CommandLine(argv);

        configureLg4j(commandLine.getLog4jFilename());

        SSLTest sslTest = new SSLTest(commandLine.useTls());

        if (commandLine.getMode().equalsIgnoreCase("server"))
            sslTest.server(commandLine.getPort());
        else if (commandLine.getMode().equalsIgnoreCase("client"))
            sslTest.client(commandLine.getHost(), commandLine.getPort());
        else {
            System.err.println ("unknown mode: " + commandLine.getMode());
        }
    }
}
