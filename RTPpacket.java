public class RTPpacket{
    static final int HEADER_SIZE = 12;
    public final int Version;
    public final int Padding;
    public final int Extension;
    public final int CC;
    public final int Marker;
    public int PayloadType;
    public int SequenceNumber;
    public int TimeStamp;
    public final int Ssrc;
    public byte[] header;
    public int payload_size;
    public byte[] payload;

    public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length){
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;
        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;
        header = new byte[HEADER_SIZE];
        payload_size = data_length;
        payload = new byte[data_length];

    }


    public RTPpacket(byte[] packet, int packet_size)
    {
        Version = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        Ssrc = 0;


        if (packet_size >= HEADER_SIZE)
        {

            header = new byte[HEADER_SIZE];
            System.arraycopy(packet, 0, header, 0, HEADER_SIZE);


            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];
            if (packet_size - HEADER_SIZE >= 0)
                System.arraycopy(packet, HEADER_SIZE, payload, 0, packet_size - HEADER_SIZE);


            PayloadType = header[1] & 127;
            SequenceNumber = unsigned_int(header[3]) + 256*unsigned_int(header[2]);
            TimeStamp = unsigned_int(header[7]) + 256*unsigned_int(header[6]) + 65536*unsigned_int(header[5]) + 16777216*unsigned_int(header[4]);
        }
    }


    public int getpayload(byte[] data) {

        if (payload_size >= 0)
            System.arraycopy(payload, 0, data, 0, payload_size);

        return(payload_size);
    }


    public int getpayload_length() {
        return(payload_size);
    }

    public int getlength() {
        return(payload_size + HEADER_SIZE);
    }

    public int getpacket(byte[] packet)
    {
        if (HEADER_SIZE >= 0)
            System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        if (payload_size >= 0)
            System.arraycopy(payload, 0, packet, HEADER_SIZE, payload_size);

        return(payload_size + HEADER_SIZE);
    }

    public int gettimestamp() {
        return(TimeStamp);
    }

    public int getsequencenumber() {
        return(SequenceNumber);
    }

    public int getpayloadtype() {
        return(PayloadType);
    }



    public void printheader()
    {

    for (int i=0; i < (HEADER_SIZE-4); i++)
      {
	for (int j = 7; j>=0 ; j--)
	  if (((1<<j) & header[i] ) != 0)
	    System.out.print("1");
	else
	  System.out.print("0");
	System.out.print(" ");
      }

    System.out.println();

    }
    static int unsigned_int(int nb) {
        if (nb >= 0)
            return(nb);
        else
            return(256+nb);
    }

}