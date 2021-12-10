#!/bin/sh

# redirects ports below 1024 to a higher range using iptables, so they can be used without elevated rights
# MySQL SIP (3306 and 5060) are left out because they are >= 1024 anyways

#These are bash script arrays we can't use them in a shell script!

#             ECHO  FTP   HTTP  HTTPS S7COMM SNMP  (NETBIOS UDP & TCP) SSH   TELNET MODBUS SMTP
#protocol=(    "tcp" "tcp" "tcp" "tcp" "tcp" "udp"  "udp"  "tcp" "tcp" "tcp" "tcp" "tcp" "tcp")
#origin=(       7     21    80    443   102	 161      138    139   22    23    445   25   502)
#destination=( 28144 28158 28217 28580 28239 28298  28275 28276 28159 28160 28582 28162 28639)

#Shell scripts don't support arrays for old shells :(

#Delete previous rules to avoid duplicates
iptables -t nat -D PREROUTING -p tcp --dport 7 -j REDIRECT --to-ports 28144
iptables -t nat -D OUTPUT -p tcp --dport 28144 -j REDIRECT --to-ports 7

iptables -t nat -D PREROUTING -p tcp --dport 21 -j REDIRECT --to-ports 28158
iptables -t nat -D OUTPUT -p tcp --dport 28158 -j REDIRECT --to-ports 21

iptables -t nat -D PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 28217
iptables -t nat -D OUTPUT -p tcp --dport 28217 -j REDIRECT --to-ports 80

iptables -t nat -D PREROUTING -p tcp --dport 443 -j REDIRECT --to-ports 28580
iptables -t nat -D OUTPUT -p tcp --dport 28580 -j REDIRECT --to-ports 443

iptables -t nat -D PREROUTING -p tcp --dport 102 -j REDIRECT --to-ports 28239
iptables -t nat -D OUTPUT -p tcp --dport 28239 -j REDIRECT --to-ports 102

#iptables -t nat -D PREROUTING -p udp --dport 161 -j REDIRECT --to-ports 28298
#iptables -t nat -D OUTPUT -p udp --dport 28298 -j REDIRECT --to-ports 161

iptables -t nat -D PREROUTING -p udp --dport 138 -j REDIRECT --to-ports 28275
iptables -t nat -D OUTPUT -p udp --dport 28275 -j REDIRECT --to-ports 138

iptables -t nat -D PREROUTING -p tcp --dport 139 -j REDIRECT --to-ports 28276
iptables -t nat -D OUTPUT -p tcp --dport 28276 -j REDIRECT --to-ports 139

iptables -t nat -D PREROUTING -p tcp --dport 22 -j REDIRECT --to-ports 28159
iptables -t nat -D OUTPUT -p tcp --dport 28159 -j REDIRECT --to-ports 22

iptables -t nat -D PREROUTING -p tcp --dport 23 -j REDIRECT --to-ports 28160
iptables -t nat -D OUTPUT -p tcp --dport 28160 -j REDIRECT --to-ports 23

iptables -t nat -D PREROUTING -p tcp --dport 445 -j REDIRECT --to-ports 28582
iptables -t nat -D OUTPUT -p tcp --dport 28582 -j REDIRECT --to-ports 445

iptables -t nat -D PREROUTING -p tcp --dport 25 -j REDIRECT --to-ports 28162
iptables -t nat -D OUTPUT -p tcp --dport 28162 -j REDIRECT --to-ports 25

iptables -t nat -D PREROUTING -p tcp --dport 502 -j REDIRECT --to-ports 28639
iptables -t nat -D OUTPUT -p tcp --dport 28639 -j REDIRECT --to-ports 502

#Create new iptables rules.
iptables -t nat -A PREROUTING -p tcp --dport 7 -j REDIRECT --to-ports 28144
iptables -t nat -A OUTPUT -p tcp --dport 28144 -j REDIRECT --to-ports 7

iptables -t nat -A PREROUTING -p tcp --dport 21 -j REDIRECT --to-ports 28158
iptables -t nat -A OUTPUT -p tcp --dport 28158 -j REDIRECT --to-ports 21

iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 28217
iptables -t nat -A OUTPUT -p tcp --dport 28217 -j REDIRECT --to-ports 80

iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-ports 28580
iptables -t nat -A OUTPUT -p tcp --dport 28580 -j REDIRECT --to-ports 443

iptables -t nat -A PREROUTING -p tcp --dport 102 -j REDIRECT --to-ports 28239
iptables -t nat -A OUTPUT -p tcp --dport 28239 -j REDIRECT --to-ports 102

#iptables -t nat -A PREROUTING -p udp --dport 161 -j REDIRECT --to-ports 28298
#iptables -t nat -A OUTPUT -p udp --dport 28298 -j REDIRECT --to-ports 161

iptables -t nat -A PREROUTING -p udp --dport 138 -j REDIRECT --to-ports 28275
iptables -t nat -A OUTPUT -p udp --dport 28275 -j REDIRECT --to-ports 138

iptables -t nat -A PREROUTING -p tcp --dport 139 -j REDIRECT --to-ports 28276
iptables -t nat -A OUTPUT -p tcp --dport 28276 -j REDIRECT --to-ports 139

iptables -t nat -A PREROUTING -p tcp --dport 22 -j REDIRECT --to-ports 28159
iptables -t nat -A OUTPUT -p tcp --dport 28159 -j REDIRECT --to-ports 22

iptables -t nat -A PREROUTING -p tcp --dport 23 -j REDIRECT --to-ports 28160
iptables -t nat -A OUTPUT -p tcp --dport 28160 -j REDIRECT --to-ports 23

iptables -t nat -A PREROUTING -p tcp --dport 445 -j REDIRECT --to-ports 28582
iptables -t nat -A OUTPUT -p tcp --dport 28582 -j REDIRECT --to-ports 445

iptables -t nat -A PREROUTING -p tcp --dport 25 -j REDIRECT --to-ports 28162
iptables -t nat -A OUTPUT -p tcp --dport 28162 -j REDIRECT --to-ports 25

iptables -t nat -A PREROUTING -p tcp --dport 502 -j REDIRECT --to-ports 28639
iptables -t nat -A OUTPUT -p tcp --dport 28639 -j REDIRECT --to-ports 502

iptables -t nat -A PREROUTING -p tcp --dport 389 -j REDIRECT --to-ports 10389
iptables -t nat -A OUTPUT -p tcp --dport 10389 -j REDIRECT --to-ports 389