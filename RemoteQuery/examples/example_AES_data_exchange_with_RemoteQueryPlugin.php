<?php
$encryptedUserName = $_GET['u'];
$plainUserName = "";

$secret_key = "0123456789abcdef"; 	// SECRET key, same as in RemoteQuery plugin.
                                    // 16 Bytes means AES-128, 32 Bytes means AES 256. Best given in HEX.
$cipher     = "rijndael-128";		// the cipher to use. See list below. Must match with plugins cipher scheme.

function pkcs5_pad ($text, $blocksize)
{
	$pad = $blocksize - (strlen($text) % $blocksize);
    return $text . str_repeat(chr($pad), $pad);
}

function pkcs5_unpad($text)
{
    $pad = ord($text{strlen($text)-1});
    if ($pad > strlen($text)) return false;
    if (strspn($text, chr($pad), strlen($text) - $pad) != $pad) return false;
    return substr($text, 0, -1 * $pad);
}

function hex2bin($hexdata)
{
	$bindata="";

	for ($i=0;$i<strlen($hexdata);$i+=2)
	{
		$bindata.=chr(hexdec(substr($hexdata,$i,2)));
	}
	
	return $bindata;
}

// Parameters for cryptare:
//$text // The text that you want to encrypt or decrypt. Encrypt: Text as plain UTF-8. Decrypt: Data as BINARY string.
//$key  // The key you're using to encrypt.
//$cipher  // The algorithm.
//$encrypt = 1 if you want to crypt, or 0 if you want to decrypt. CAUTION: When decrypting, feed with binary data! Not HEX!
// RETURN: If Encrypting -> BINARY string. If Decrypting: Plain UTF-8 string.
function cryptare($text, $key, $cipher, $encrypt)
{
    $encrypted_data="";
    switch($cipher)
    {
        case "3des":
            $td = mcrypt_module_open('tripledes', '', 'ecb', '');
            break;
        case "rijndael-128":
            $td = mcrypt_module_open('rijndael-128', '', 'ecb', ''); // ecb is not really secure for hiding patterns. (e.g. encrypting images)
            break;
        case "twofish":
            $td = mcrypt_module_open('twofish', '', 'ecb', '');
            break;     
        case "rijndael-192":
            $td = mcrypt_module_open('rijndael-192', '', 'ecb', '');
            break;
        case "blowfish-compat":
            $td = mcrypt_module_open('blowfish-compat', '', 'ecb', '');
            break;
        case "des":
            $td = mcrypt_module_open('des', '', 'ecb', '');
            break;
        case "rijndael-256":
            $td = mcrypt_module_open('rijndael-256', '', 'ecb', '');
            break;
        default:
            $td = mcrypt_module_open('blowfish', '', 'ecb', '');
            break;                                           
    }
   
    $iv = mcrypt_create_iv(mcrypt_enc_get_iv_size($td), MCRYPT_RAND);
    $key = substr($key, 0, mcrypt_enc_get_key_size($td));
    mcrypt_generic_init($td, $key, $iv);
   
    if($encrypt)
    {
		// add PKCS5Padding to prevent mcrypt to add ZeroBytePadding wich Java has no native support for
		// CAUTION: When padding text with < (n * blocksize), n*blocksize bytes are needed to store this text
		//          When padding text with >= (n * blocksize) + 1 < ((n+1) * blocksize), (n+1)*blocksize bytes are needed to store this text
		// Example: 01234567890ABCDE (16 Bytes) -> Padded: 01234567890ABCDExxxxxxxxxxxxxxxx (32 Bytes)
		//          01234567890ABCD  (15 Bytes) -> Padded: 01234567890ABCDx (16 Bytes)
		$paddedText = pkcs5_pad($text, mcrypt_get_block_size(MCRYPT_RIJNDAEL_128, 'ecb')); // mcrypt_enc_get_block_size(td) does not work??
	
		// ENCRYPT
        $resulting_data = mcrypt_generic($td, $paddedText);
    }
    else
    {
		// DECRYPT
        $resulting_data = mdecrypt_generic($td, $text);
		
		// remove PKCS5Padding to get plain text back
		$resulting_data = pkcs5_unpad($resulting_data);
    }
   
    mcrypt_generic_deinit($td);
    mcrypt_module_close($td);
   
    return $resulting_data;
}

// Use mysqli API to connect to DB
function sqlGetPlayerData($userName)
{
	$select = "SELECT member_id, name, title FROM bb_members WHERE name = '" . $userName . "';";
	$mysqli = new mysqli("localhost", "dbuser", "dbpass", "database");	

	$result = $mysqli->query($select);

	if ($mysqli->error) // check for errors
	{
		try
		{   
			throw new Exception("MySQL error $mysqli->error <br> Query:<br> $query", $msqli->errno);   
		}
		catch(Exception $e )
		{
			echo "Error No: ".$e->getCode(). " - ". $e->getMessage() . "<br>";
			echo nl2br($e->getTraceAsString());
		}
	}

	// OUTPUT results
	$row = $result->fetch_assoc(); // warning: will move the cursor to next row! Only use for single result.
	return $row['member_id'] . " " . $row['name'] . " " . $row['title']; // print one line of result directly for console usage

	/* close connection */
	$mysqli->close();
}

// BEGIN ACTIONS ================================================================

// Get User data from DB
//echo " DEBUG Decrypting user name: " . $encryptedUserName . " ...";
$plainUserName = cryptare(hex2bin($encryptedUserName), $secret_key, $cipher, 0);
//echo " DEBUG Decrypted user name to : " . $plainUserName;
//echo " DEBUG DB UserData: " . sqlGetPlayerData($plainUserName);

//echo htmlentities(" DEBUG_Echo u=" . $encryptedUserName); // for well formed HTML output

// DEBUG
//echo " Calling cryptare in ENcrypt mode...";
//$encryptedTextBIN = cryptare($plainUserName, $secret_key, $cipher, 1); // ENCRYPT
//$encryptedTextHEX = bin2hex($encryptedTextBIN); // convert to HEX to safely send via HTML
//echo " Encrypted Text Original: " . $encryptedTextBIN;
//echo " Encrypted Text HEX (padded): " . $encryptedTextHEX;
//echo " Calling cryptare in DEcrypt mode...";
//$encryptedTextBIN = hex2bin($encryptedTextHEX); // convert back to BIN to input into decryptor
//echo " Encrypted Text converted back from HEX to BIN: " . $encryptedTextBIN;
//$plainUserName = cryptare($encryptedTextBIN, $secret_key, $cipher, 0); // DECRYPT
//echo " Decrypted Text Original: " . $plainUserName;
?>