#!/bin/bash

# redirects ports below 1024 to a higher range using iptables, so they can be used without elevated rights
# MySQL SIP (3306 and 5060) are left out because they are >= 1024 anyways

#             ECHO  FTP   HTTP  HTTPS S7COMM SNMP SMB (NETBIOS UDP & TCP) SSH   TELNET MODBUS SMTP  BACnet
protocol=(    "tcp" "tcp" "tcp" "tcp" "tcp" "udp" "udp" "udp"  "tcp" "tcp" "tcp" "tcp" "tcp" "tcp"   "udp" )
origin=(       7     21    80    443   102	 161   137   138    139   22    23    445   25   502      124  )
destination=( 28144 28158 28217 28580 28239 28298 28274 28275 28276 28159 28160 28582 28162 28639    28261 ) # simply offset by 1024 + 27113
length=${#protocol[@]} # count protocol elements

# for (( i=0; i<$length; i++ ))
#for i in `seq 0 9` # fix for android's annoyingly limited bash

for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 # another fix for devices missing the seq command

do
	# echo ${protocol[$i]} ${origin[$i]} ${destination[$i]} # debug

	# delete previous rules to avoid duplicates
	iptables -t nat -D PREROUTING -p ${protocol[$i]} --dport ${origin[$i]} -j REDIRECT --to-ports ${destination[$i]}
	iptables -t nat -D OUTPUT -p ${protocol[$i]} --dport ${destination[$i]} -j REDIRECT --to-ports ${origin[$i]}

	# add new rules
	iptables -t nat -A PREROUTING -p ${protocol[$i]} --dport ${origin[$i]} -j REDIRECT --to-ports ${destination[$i]}
	iptables -t nat -A OUTPUT -p ${protocol[$i]} --dport ${destination[$i]} -j REDIRECT --to-ports ${origin[$i]}
done
