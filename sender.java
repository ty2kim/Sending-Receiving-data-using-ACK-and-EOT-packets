import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class sender{
	/* variables & constants */
	private static final int window_size = 10;
	private static final int data_capacity = 500;
	private static final int max_byte_size = 65535;
	private static final int packet_timer_length = 2000; 
	private static LinkedList<String> data_list = new LinkedList<String>();
	private static final int ACK_type_packet = 0;
	private static final int EOT_type_packet = 2;
	private static Timer packet_timer = null;
	private static DatagramSocket sender_socket = null;
	private static int base = 0;
	private static int next_seqnum = 0;
	private static BufferedWriter seqnum_writer = null;
	private static BufferedWriter ack_writer = null;
	private static int num_rounds = 0;
	private static int stored_ack;
	private static boolean is_first_time_receiving = true;
	private static packet send_packet;
	private static byte[] send_data = new byte[max_byte_size];
	private static DatagramPacket send_data_packet;
	private static packet receive_packet;
	private static byte[] receive_data = new byte[max_byte_size];
	private static DatagramPacket receive_data_packet;
	
	/* input value */
	private static InetAddress emulator_address = null;
	private static int emulator_port_no = 0;
	private static int sender_port_no = 0;
	
	/* action listener */
	private static void set_action_listener(){
		ActionListener timeout = new ActionListener(){
			// when timeout happens,
			public void actionPerformed(ActionEvent event){  
					//System.out.println("timeout!!!");
					//packet send_packet;
					//byte[] send_data = new byte[max_byte_size];
					//DatagramPacket send_data_packet = null;
					// re-send all transmitted-but-yet-to-be-acknowledged packets
					// base, base+1, ..... , next_seqnum-1
					for(int i = base; (i < data_list.size() && i < next_seqnum); i++){
						try{
							send_packet = packet.createPacket(i % 32, data_list.get(i));
							send_data = send_packet.getUDPdata();
							send_data_packet = new DatagramPacket(send_data, send_data.length, emulator_address, emulator_port_no);
						}catch(Exception e){
							System.err.println("ERROR: " + e.getMessage());
							System.exit(1);
						}
						// if a packet is successfully re-sent, record it in seqnum.log
						try{
							sender_socket.send(send_data_packet);
							//System.out.println("sender: re-sending packet(data) "+ (i % 32));
							seqnum_writer.write(Integer.toString(i % 32) + '\n');
						}catch(IOException e){
							System.err.println("ERROR: " + e.getMessage());
							System.exit(1);
						}

					}
			}
		};
		// create timer
		packet_timer = new Timer(packet_timer_length, timeout);
	}
	
	/* reorganize input file */
	private static void reorganize_input_file(String file_name){
		BufferedReader file_reader = null;
		// store input file into file_reader
		try{
			file_reader = new BufferedReader(new FileReader(file_name));
		}catch(FileNotFoundException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		// capture 500bytes each, and push it on to data_list
		try{
			String remains = "";
			String temp = "";
			String line = "";
			while(file_reader.ready() == true){
				line = remains + file_reader.readLine();
				temp = line;
				if(line.length() > data_capacity - 1){
					while (temp.length() > data_capacity - 1){
						data_list.add(temp.substring(0, data_capacity));
						// if a line contains exactly 500bytes(without newline), temp will empty
						if(temp.length() == data_capacity){
							temp = "";
						}
						else{
							temp = temp.substring(data_capacity);
						}
					}
				}
				remains = temp;
				if(file_reader.ready() != true){	
					data_list.add(remains);
				}
				// since readLine() function ignores newline(), add '\n' to the accumulated string
				remains = remains + '\n';
			}
			// close the file_reader
			file_reader.close();
		}catch (IOException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
	}
	
	/* initiation */
	private static void init(String input[]){
		// check if input correctly typed
		if(input.length != 4){
			System.err.println("USAGE:");
			System.err.println("java sender");
			System.err.println("	<host address of the network emulator>,");
			System.err.println("	<UDP port number used by the emulator to receive data from the sender>,");
			System.err.println("	<UDP port number used by the sender to receive ACKs from the emulator>,");
			System.err.println("	<name of the file to be transferred>");
			System.exit(1);
		}
		// read & save addresses, port numbers
		try{
			emulator_address = InetAddress.getByName(input[0]);
		}catch (UnknownHostException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		try{
			emulator_port_no = Integer.parseInt(input[1]);
			sender_port_no = Integer.parseInt(input[2]);
		}catch (NumberFormatException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		// read & save input file
		reorganize_input_file(input[3]);
		// set up action listener for timer
		set_action_listener();
		// create writers for seqnum.log and ack.log
		try{
			seqnum_writer = new BufferedWriter(new FileWriter("seqnum.log"));
			ack_writer = new BufferedWriter(new FileWriter("ack.log"));
		}catch (IOException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
	}
	
	/* transaction */
	private static void transaction(){
		// create UDP socket
		try{
			sender_socket = new DatagramSocket(sender_port_no);
		}catch (SocketException e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		}
		// actual data & ACKs transactions
		try{
			while(true){
				// 1. sending data packet
				// check if window is full
				while(next_seqnum < base + window_size && next_seqnum < data_list.size()){					
					send_packet = packet.createPacket(next_seqnum % 32, data_list.get(next_seqnum));
					send_data = send_packet.getUDPdata();
					send_data_packet = new DatagramPacket(send_data, send_data.length, emulator_address, emulator_port_no);
					sender_socket.send(send_data_packet); // packet sent
					//System.out.println("sender: sending packet(data) "+ (next_seqnum % 32));
					// a packet has been sent, record it in seqnum.log
					seqnum_writer.write(Integer.toString(next_seqnum % 32) + '\n');
					// start the timer
					if(base == next_seqnum){
						packet_timer.start();
					}
					next_seqnum++;
				} 
				// 2. sending EOT packet
				if(base == data_list.size()){
					send_packet = packet.createEOT(next_seqnum % 32);
					send_data = send_packet.getUDPdata();
					send_data_packet = new DatagramPacket(send_data, send_data.length, emulator_address, emulator_port_no);
					sender_socket.send(send_data_packet);
					//System.out.println("sender: sending packet(EOT) "+ (next_seqnum % 32));
				}
				// 3. receiving ACK packet
				receive_data_packet = new DatagramPacket(receive_data, receive_data.length);
				sender_socket.receive(receive_data_packet);
				receive_packet = packet.parseUDPdata(receive_data); // packet received
				if(receive_packet.getType() == ACK_type_packet){
					// a packet has been received, record it in ack.log
					ack_writer.write(Integer.toString(receive_packet.getSeqNum()) + '\n');
					//System.out.println("sender: receiving packet(ACK) "+receive_packet.getSeqNum());
					// first received ACK packet is always seqnum=0;
					if(is_first_time_receiving){
						base = 1;
						is_first_time_receiving = false;
					}
					// if received packet is duplicate, ignore it. if not,
					else if(receive_packet.getSeqNum() != stored_ack){
						base = receive_packet.getSeqNum() + (num_rounds * 32) + 1;
						if(receive_packet.getSeqNum() == 31){
							num_rounds++;
						}
					}
					// used for checking duplicates
					stored_ack = receive_packet.getSeqNum();
					// if there are no flying transmitted-but-yet-to-be-acknowledged packets, stop the timer
					if(base == next_seqnum){
						packet_timer.stop();
					}
					// if there are more transmitted-but-yet-to-be-acknowledged packets
					else{ 
						packet_timer.start(); 
					}
				}
				// 4. receiving EOT packet
				else if(receive_packet.getType() == EOT_type_packet){
					// received packet's type is EOT. break and close 
					//System.out.println("sender: receiving packet(EOT) "+receive_packet.getSeqNum());
					sender_socket.close();
					seqnum_writer.close();
					ack_writer.close();
					break;
				}
			}
		}catch (Exception e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(1);
		} 
	}
	
	/* main */
	public static void main(String argv[]){
		init(argv);
		transaction();
	}
}
