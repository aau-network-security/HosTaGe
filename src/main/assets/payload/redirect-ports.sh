#!/bin/sh

# redirects ports below 1024 to a higher range using iptables, so they can be used without elevated rights
# MySQL SIP (3306 and 5060) are left out because they are >= 1024 anyways

#These are bash script arrays we can't use them in a shell script!

#             ECHO  FTP   HTTP  HTTPS S7COMM SNMP SMB (NETBIOS UDP & TCP) SSH   TELNET MODBUS SMTP
#protocol=(    "tcp" "tcp" "tcp" "tcp" "tcp" "udp" "udp" "udp"  "tcp" "tcp" "tcp" "tcp" "tcp" "tcp")
#origin=(       7     21    80    443   102	 161   137   138    139   22    23    445   25   502)
#destination=( 28144 28169 28217 28580 28239 28298 28274 28275 28276 28159 28160 28582 28162 28639)

#Shell scripts don't support arrays for old shells :(

#Delete previous rules to avoid duplicates
  iptables -t nat -D PREROUTING -p tcp --dport 7 -j DNAT \
      --to 0.0.0.0:28144
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 7 -j DNAT \
      --to 0.0.0.0:28144

  iptables -t nat -D PREROUTING -p tcp --dport 21 -j DNAT \
      --to 0.0.0.0:28169
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 21 -j DNAT \
      --to 0.0.0.0:28169

  iptables -t nat -D PREROUTING -p tcp --dport 80 -j DNAT \
      --to 0.0.0.0:28217
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 80 -j DNAT \
      --to 0.0.0.0:28217

  iptables -t nat -D PREROUTING -p tcp --dport 443 -j DNAT \
      --to 0.0.0.0:28580
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 443 -j DNAT \
      --to 0.0.0.0:28580

  iptables -t nat -D PREROUTING -p tcp --dport 102 -j DNAT \
      --to 0.0.0.0:28239
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 102 -j DNAT \
      --to 0.0.0.0:28239

  iptables -t nat -D PREROUTING -p udp --dport 161 -j DNAT \
      --to 0.0.0.0:28298
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p udp --dport 161 -j DNAT \
      --to 0.0.0.0:28298

  iptables -t nat -D PREROUTING -p udp --dport 137 -j DNAT \
      --to 0.0.0.0:28274
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p udp --dport 137 -j DNAT \
      --to 0.0.0.0:28274

  iptables -t nat -D PREROUTING -p udp --dport 138 -j DNAT \
      --to 0.0.0.0:28275
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p udp --dport 138 -j DNAT \
      --to 0.0.0.0:28275

  iptables -t nat -D PREROUTING -p tcp --dport 139 -j DNAT \
      --to 0.0.0.0:28276
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 139 -j DNAT \
      --to 0.0.0.0:28276

  iptables -t nat -D PREROUTING -p tcp --dport 22 -j DNAT \
      --to 0.0.0.0:28159
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 22 -j DNAT \
      --to 0.0.0.0:28159

  iptables -t nat -D PREROUTING -p tcp --dport 23 -j DNAT \
      --to 0.0.0.0:28160
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 23 -j DNAT \
      --to 0.0.0.0:28160

  iptables -t nat -D PREROUTING -p tcp --dport 445 -j DNAT \
      --to 0.0.0.0:28582
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 445 -j DNAT \
      --to 0.0.0.0:28582

  iptables -t nat -D PREROUTING -p tcp --dport 25 -j DNAT \
      --to 0.0.0.0:28162
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 25 -j DNAT \
      --to 0.0.0.0:28162

  iptables -t nat -D PREROUTING -p tcp --dport 502 -j DNAT \
      --to 0.0.0.0:28639
  iptables -t nat -D OUTPUT -d 127.0.0.1 -p tcp --dport 502 -j DNAT \
      --to 0.0.0.0:28639

#Create new iptables rules.

  iptables -t nat -A PREROUTING -p tcp --dport 7 -j DNAT \
      --to 0.0.0.0:28144
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 7 -j DNAT \
      --to 0.0.0.0:28144

  iptables -t nat -A PREROUTING -p tcp --dport 21 -j DNAT \
      --to 0.0.0.0:28169
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 21 -j DNAT \
      --to 0.0.0.0:28169

  iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT \
      --to 0.0.0.0:28217
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 80 -j DNAT \
      --to 0.0.0.0:28217

  iptables -t nat -A PREROUTING -p tcp --dport 443 -j DNAT \
      --to 0.0.0.0:28580
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 443 -j DNAT \
      --to 0.0.0.0:28580

  iptables -t nat -A PREROUTING -p tcp --dport 102 -j DNAT \
      --to 0.0.0.0:28239
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 102 -j DNAT \
      --to 0.0.0.0:28239

  iptables -t nat -A PREROUTING -p udp --dport 161 -j DNAT \
      --to 0.0.0.0:28298
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p udp --dport 161 -j DNAT \
      --to 0.0.0.0:28298

  iptables -t nat -A PREROUTING -p udp --dport 137 -j DNAT \
      --to 0.0.0.0:28274
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p udp --dport 137 -j DNAT \
      --to 0.0.0.0:28274

  iptables -t nat -A PREROUTING -p udp --dport 138 -j DNAT \
      --to 0.0.0.0:28275
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p udp --dport 138 -j DNAT \
      --to 0.0.0.0:28275

  iptables -t nat -A PREROUTING -p tcp --dport 139 -j DNAT \
      --to 0.0.0.0:28276
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 139 -j DNAT \
      --to 0.0.0.0:28276

  iptables -t nat -A PREROUTING -p tcp --dport 22 -j DNAT \
      --to 0.0.0.0:28159
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 22 -j DNAT \
      --to 0.0.0.0:28159

  iptables -t nat -A PREROUTING -p tcp --dport 23 -j DNAT \
      --to 0.0.0.0:28160
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 23 -j DNAT \
      --to 0.0.0.0:28160

  iptables -t nat -A PREROUTING -p tcp --dport 445 -j DNAT \
      --to 0.0.0.0:28582
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 445 -j DNAT \
      --to 0.0.0.0:28582

  iptables -t nat -A PREROUTING -p tcp --dport 25 -j DNAT \
      --to 0.0.0.0:28162
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 25 -j DNAT \
      --to 0.0.0.0:28162

  iptables -t nat -A PREROUTING -p tcp --dport 502 -j DNAT \
      --to 0.0.0.0:28639
  iptables -t nat -A OUTPUT -d 127.0.0.1 -p tcp --dport 502 -j DNAT \
      --to 0.0.0.0:28639