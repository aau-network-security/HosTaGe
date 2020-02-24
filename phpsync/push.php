<?php
$json = json_decode ( $_POST ["record"], true );

$values = array ();
$i = 0;
$line = "(";
foreach ( $json as $key => $value ) {
	$line = $line . "'" . $value . "',";
}
$line = substr ( $line, 0, strlen ( $line ) - 1 ) . ")";
$values [$i] = $line;
++ $i;

$values = implode ( ",", $values );

$username = "hostage";
$password = "hostageDB";
$hostname = "localhost";

$dbhandle = mysql_connect ( $hostname, $username, $password ) or die ( "Unable to connect to MySQL" );

$result = mysql_query ( "INSERT INTO `hostage`.`sync` (`bssid`, `ssid`, `latitude`, `longitude`, `timestamp`, `attacks`, `portscans`) VALUES " . $values );

if (! $result) {
	die ( 'Could not insert record: ' . mysql_error () );
}
?>
