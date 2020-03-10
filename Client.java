import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Client{
    private final JLabel iconLabel = new JLabel();
    private DatagramSocket RTPsocket;
    private static final int RTP_RCV_PORT = 25000;
    private final Timer timer;
    private final byte[] buffer;
    private final static int INIT = 0;
    private final static int READY = 1;
    private  final static int PLAYING = 2;
    private static int state;
    private Socket RTSPsocket;
    private  static BufferedReader RTSPBufferedReader;
    private static BufferedWriter RTSPBufferedWriter;
    private static String VideoFileName;
    private int RTSPSeqNb = 0;
    private int RTSPid = 0;
    private final static String CRLF = "\r\n";
    private Client(){
        JFrame f = new JFrame("Client");
        f.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                System.exit(0);
            }
        });
        JButton setupButton = new JButton("Setup");
        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton tearButton = new JButton("Exit");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        iconLabel.setIcon(null);

        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390,370));
        f.setVisible(true);

        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);


        buffer = new byte[15000];
    }
    public static void main(String[] args) throws Exception{
        Client theClient = new Client();
        String ServerHost = args[0];
        int RTSP_server_port = Integer.parseInt(args[1]);
        InetAddress ServerIP = InetAddress.getByName(ServerHost);

        VideoFileName = args[2];
        theClient.RTSPsocket = new Socket(ServerIP, RTSP_server_port);
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(theClient.RTSPsocket.getInputStream()) );
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsocket.getOutputStream()) );
        state = INIT;
    }


    class setupButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            if (state == INIT){
                try{

                    RTPsocket = new DatagramSocket(RTP_RCV_PORT);
                    RTPsocket.setSoTimeout(5);
                }
                catch (SocketException se){
                    System.out.println("Socket exception: "+se);
                    System.exit(0);
                }
                RTSPSeqNb = 1;
                send_RTSP_request("SETUP");
                if (parse_server_response() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    state = READY;
                    System.out.println("New RTSP state: READY");
                }
            }
        }
    }


    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            if (state == READY) {
                RTSPSeqNb++;
                send_RTSP_request("PLAY");
                if (parse_server_response() != 200)
                    System.out.println("Invalid Server Response");
                else {

                    state = PLAYING;
                    System.out.println("New RTSP state: PLAYING");
                    timer.start();
                }
            }
        }
    }

    class pauseButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            if (state == PLAYING){
                RTSPSeqNb++;
                send_RTSP_request("PAUSE");
                if (parse_server_response() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    state = READY;
                    System.out.println("New RTSP state: READY");
                    timer.stop();
                }
            }
        }
    }


    class tearButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            RTSPSeqNb++;
            send_RTSP_request("TEARDOWN");
            if (parse_server_response() != 200)
                System.out.println("Invalid Server Response");
            else {
                state = INIT;
                System.out.println("New RTSP state: INIT");
                timer.stop();
                System.exit(0);
            }
        }
    }



    class timerListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            DatagramPacket rcvdp = new DatagramPacket(buffer, buffer.length);
            try{
                RTPsocket.receive(rcvdp);
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());
                rtp_packet.printheader();
                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                ImageIcon icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe){
                System.out.println("Nothing to read");
            }
            catch (IOException ioe) {
                System.out.println("Exception caught: "+ioe);
            }
        }
    }

    private int parse_server_response(){
        int reply_code = 0;
        try{
            String StatusLine = RTSPBufferedReader.readLine();
            System.out.println(StatusLine);
            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken();
            reply_code = Integer.parseInt(tokens.nextToken());


            if (reply_code == 200){
                String SeqNumLine = RTSPBufferedReader.readLine();
                System.out.println(SeqNumLine);

                String SessionLine = RTSPBufferedReader.readLine();
                System.out.println(SessionLine);
                tokens = new StringTokenizer(SessionLine);
                tokens.nextToken();
                RTSPid = Integer.parseInt(tokens.nextToken());
            }
        }
        catch(Exception ex) {
            System.out.println("Exception caught : "+ex);
            System.exit(0);
        }
        return(reply_code);
    }

    private void send_RTSP_request(String request_type){
        try{
            RTSPBufferedWriter.write(request_type+" "+VideoFileName+" RTSP/1.0"+CRLF);
            RTSPBufferedWriter.write("CSeq: "+RTSPSeqNb+CRLF);
            if ((new String(request_type)).compareTo("SETUP") == 0)
                RTSPBufferedWriter.write("Transport: RTP/UDP; client_port= "+RTP_RCV_PORT+CRLF);
            else
                RTSPBufferedWriter.write("Session: "+RTSPid+"\n");
            RTSPBufferedWriter.flush();
        }
        catch(Exception ex){
            System.out.println("Exception caught : "+ex);
            System.exit(0);
        }
    }
}