import java.io.*;
import java.net.*;

public class receiver{
	/* variables & constants */
	private static final int data_type_packet = 1;
	private static final int EOT_type_packet = 2;
	private static final int max_byte_size = 65535;
	private static BufferedWriter arrival_writer = null;
	private static BufferedWriter output_writer = null;
	private static int expected_seqnum = 0;
	private static DatagramSocket receiver_socket = null;
	private static boolean is_first_time_receiving = true;
	private static packet receive_packet;
	private static byte[] receive_data = new byte[max_byte_size];
	private static DatagramPacket receive_data_packet;
	private static packet send_packet;
	private static byte[] send_data = new byte[max_byte_size];
	private static DatagramPacket send_data_packet;
	
	/* input value */
	private static InetAddress emulator_address = null;
	private static int emulator_port_no = 0;
	private static int receiver_port_no = 0;
	private static String output_file_name;
	
	/* initiation */
	private static void init(String input[]){
		// check if input correctly typed
		if(input.length != 4){
			System.err.println("USAGE:");
			System.err.println("java receiver");
			System.err.println("	<hostname for the network emulator>,");
			System.err.println("	<UDP port number used by the link emulator to receive ACKs from the receiver>,");
			System.err.println("	<UDP port number used by the receiver to receive data from the emulator>,");
			System.err.println("	<name of the file into which the received data is written>");
			System.exit(1);
		}
		// read & save addresses, port numbers, output file name  
		try{
			emulator_address = InetAddress.getByName(input[0]);
		}catch (UnknownHostException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		try{
			emulator_port_no = Integer.parseInt(input[1]);
			receiver_port_no = Integer.parseInt(input[2]);
		}catch (NumberFormatException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		output_file_name = input[3];
		// create writers for arrival.log and output_file
		try{
			arrival_writer = new BufferedWriter(new FileWriter("arrival.log"));
			output_writer = new BufferedWriter(new FileWriter(output_file_name));
		}catch (IOException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
	}
	
	/* transaction */
	private static void transaction(){
		// create UDP socket
		try{
			receiver_socket = new DatagramSocket(receiver_port_no);
		}catch (SocketException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		// actual data & ACKs transactions
		try{
			while(true){
				receive_data_packet = new DatagramPacket(receive_data, receive_data.length);
				receiver_socket.receive(receive_data_packet);
				receive_packet = packet.parseUDPdata(receive_data);
				// 1. receiving data packet
				// check if received packet is data type
				if(receive_packet.getType() == data_type_packet){
					// received packet is data. record it in arrival.log
					arrival_writer.write(Integer.toString(receive_packet.getSeqNum()) + '\n');
					//System.out.println("receiver: receiving packet(data) "+ receive_packet.getSeqNum());
					// received packet's seqnum is in-order
					if(receive_packet.getSeqNum() == expected_seqnum % 32){
						// since received packet is data, and in correct order, store the data in output
						output_writer.write(new String(receive_packet.getData()));
						// we should now send ACK with proper seqnum (same seqnum as received packet)
						send_packet = packet.createACK(expected_seqnum % 32);
						send_data = send_packet.getUDPdata();
						send_data_packet = new DatagramPacket(send_data, send_data.length, emulator_address, emulator_port_no);
						receiver_socket.send(send_data_packet);
						//System.out.println("receiver: sending packet(ACK) "+ (expected_seqnum % 32));
						// first received packet is correctly handled
						is_first_time_receiving = false;
						expected_seqnum++;
					} 
					// first received packet's seqnum should be 0. skip and do nothing until receives packet 0
					else if(is_first_time_receiving && receive_packet.getSeqNum() != expected_seqnum % 32){
						continue;
					}
					// received packet's seqnum is not in-order
					else{
						// send ACK with highest in-order seqnum (should be 1 less than the seqnum we were expecting)
						send_packet = packet.createACK(expected_seqnum % 32 - 1);
						send_data = send_packet.getUDPdata();
						send_data_packet = new DatagramPacket(send_data, send_data.length, emulator_address, emulator_port_no);
						receiver_socket.send(send_data_packet);
						//System.out.println("receiver: sending packet(ACK) "+ (expected_seqnum % 32 - 1));
					}
				}
				// check if received packet is EOT type
				else if(receive_packet.getType() == EOT_type_packet){
					//System.out.println("receiver: receiving packet(EOT) "+ receive_packet.getSeqNum());
					// send EOT packet with same seqnum of received EOT packet
					send_packet = packet.createEOT(receive_packet.getSeqNum());
					send_data = send_packet.getUDPdata();
					send_data_packet = new DatagramPacket(send_data, send_data.length, emulator_address, emulator_port_no);
					receiver_socket.send(send_data_packet);
					//System.out.println("receiver: sending packet(data) "+receive_packet.getSeqNum());
					// since assignment states that the EOT packet never gets lost, close everything
					output_writer.close();
					arrival_writer.close();
					receiver_socket.close();
					break;
				} 
			}
		} catch(Exception e){
			System.err.println("ERROR: " + e.getStackTrace());
			System.exit(1);
		} 
	}
	
	/* main */
	public static void main(String argv[]){
		init(argv);
		transaction();
	}
}
