<?php
$bssid = $_POST ["bssid"];

$username = "hostage";
$password = "hostageDB";
$hostname = "localhost";

$dbhandle = mysql_connect ( $hostname, $username, $password ) or die ( "Unable to connect to MySQL" );

$query = "SELECT * FROM `hostage`.`sync` WHERE `bssid` = '" . $bssid . "'";

$result = mysql_query ( $query );

if (! $result) {
	die ( 'Could not select record: ' . mysql_error () );
}

while ( $row = mysql_fetch_array ( $result ) ) {
	echo json_encode ( $row );
}
?>
