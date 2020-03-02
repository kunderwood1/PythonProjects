#!/usr/bin/env python
import socket
import struct




def main():

    con = socket.socket(socket.AF_PACKET, socket.SOCK_RAW, socket.ntohs(3))
    while True:
        raw_data, addr = con.recvfrom(65536)
        dest_mac,src_mac, eth_proto, data = ethernet_frame(raw_data)
        print('\nEthernet Frame:')
        print('destination: {},source: {}, protol: {}'.format(dest_mac, src_mac, eth_proto))

        if eth_proto == 8:
            (version, header_len, ttl, proto, src, dest, data) = IPV4_packet(data)
            print('IPV4 Packet')
            print('Version: {}, Header_len: {},TTL: {}, Protocol: {}, Source: {}, Destination: {} '.format(version, header_len, ttl, proto, src, dest))
                    
            if proto == 1:
                (icmp_type, code, checksum, data) =ICMP_packet(data)
                print('Type: {}, Code: {}, Checksum: {}'.format(icmp_type, code, checksum))
                print('Data: {}'.format(data))
            elif proto == 6:
                (src_port, dest_port, seq, ack, flag_urg, flag_ack, flag_psh, flag_rst, flag_syn,flag_fin, data) =TCP_packet(data)
                print('TCP:')
                print('Source Port: {}, Destination Port: {}, Sequence: {},Acknowledgment: {}'.format(src_port, dest_port, seq, ack))
                print('Flags:')
                print('urg: {}, ack: {}, psh: {}, rst: {}, syn,fin: {}'.format(flag_urg, flag_ack, flag_psh, flag_rst, flag_syn,flag_fin))
                print('Data: {}'.format(data))
            elif proto == 17:
                (src_port, dest_port, size, data) = UDP_packet(data)
                print('UDP:')
                print('Source Port: {}, Destination Port: {}, Size: {}'.format(src_port, dest_port, size))
                print('Data: {}'.format(data))
            else:
                print('Data: {}'.format(data))

#unpack frame XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
def ethernet_frame(data):
	dest_mac, src_mac, proto = struct.unpack('! 6s 6s H', data[:14])
	return get_mac_addr(dest_mac), get_mac_addr(src_mac), socket.htons(proto), data[14:]


# return mac XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
def get_mac_addr(bytes_addr):
	bytes_str = map('{:02x}'.format, bytes_addr)
	mac_addr = ':'.join(bytes_str).upper()
	return mac_addr


#IPV4 stuff XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
def IPV4_packet(data):
	version_header_len = data[0]
	version = version_header_len >> 4
	header_len = (version_header_len& 15) * 4
	ttl, proto, src, dest = struct.unpack('! 8x B B 2x 4s 4s',data[:20])
	return version, header_len, ttl, proto, IPV4(src), IPV4(dest), data[header_len:]


def IPV4(addr):
	return '.'.join(map(str,addr))
	
def ICMP_packet(data):
	icmp_type, code, checksum = struct.unpac('! B B H', data[:4])
	return icmp_type, code, checksum, data[4:] 

def TCP_packet(data):
	(src_port, dest_port, seq, ack, offset_reserved_flags) = struct.unpack('! H H L L H', data[:14])
	offset = (offset_reserved_flags >> 12) * 4
	flag_urg = (offset_reserved_flags & 32) >> 5
	flag_ack = (offset_reserved_flags & 16) >> 4
	flag_psh = (offset_reserved_flags & 8) >> 3
	flag_rst = (offset_reserved_flags & 4) >> 2
	flag_syn = (offset_reserved_flags & 2) >> 1
	flag_fin = offset_reserved_flags & 1 
	return src_port, dest_port, seq, ack, flag_urg, flag_ack, flag_psh, flag_rst, flag_syn,flag_fin, data[offset:]

def UDP_packet(data):
	src_port, dest_port, size = struct.unpack('! H H 2x H', data[:8])
	return src_port, dest_port, size, data[8:]
	

main()



