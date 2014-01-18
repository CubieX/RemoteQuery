package com.github.CubieX.RemoteQuery;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.web.util.HtmlUtils;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RQHTTPHandler
{
   RQSchedulerHandler schedHandler = null;
   RemoteQuery plugin = null;
   final private static char[] hexArray = "0123456789ABCDEF".toCharArray(); // possible HEX values   
   String iv = "d0ec238b641fa21d";  // initialization vector (16 bytes) -> should be randomly generated! Only used for CBC or  similar cipher mode.
   Cipher cipher = null;

   public RQHTTPHandler(RemoteQuery plugin, RQSchedulerHandler schedHandler)
   {
      this.plugin = plugin;
      this.schedHandler = schedHandler;

      try
      {
         cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // must match with remote PHP scripts encryption scheme
      }
      catch (NoSuchAlgorithmException e)
      {        
         e.printStackTrace();
      }
      catch (NoSuchPaddingException e)
      {         
         e.printStackTrace();
      }
   }

   public void executeQueryHTTPasync(final CommandSender sender, String playerName)
   {
      final String queryParam = playerName;

      if((null != queryParam) && !queryParam.isEmpty())
      {
         queryParam.trim();
      }
      else
      {         
         return;
      }

      plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable()
      {
         @Override
         public void run()
         {
            String targetURL = RemoteQuery.scriptURL;
            String urlParameters = "";
            String encryptedData = "";
            URL url;
            HttpURLConnection connection = null;

            try
            {
               encryptedData = encrypt(queryParam);               
               if(RemoteQuery.debug){sender.sendMessage("PLUGIN: Plain: " + queryParam + " -> Encrypted HEX (padded): " + encryptedData + " -> Decrypted again (padded): " + decrypt(encryptedData));}
            }
            catch (Exception e1)
            {
               e1.printStackTrace();
            }

            if(null != encryptedData)
            {
               urlParameters = "?u=" + encryptedData; // AES-128 encrypted player name as HEX string. No URLEncoding, because HEX is web-save.

               if(RemoteQuery.debug){sender.sendMessage("PLUGIN Request: " + RemoteQuery.scriptURL + urlParameters);}
            }

            try
            {
               //Create connection               
               url = new URL(targetURL + urlParameters);
               connection = (HttpURLConnection)url.openConnection(); // will execute the request
               connection.setRequestMethod("GET");

               connection.setUseCaches (false);
               connection.setDoInput(true);
               connection.setDoOutput(true);

               //Get Response 
               InputStream is = connection.getInputStream();
               BufferedReader rd = new BufferedReader(new InputStreamReader(is));
               String line;
               StringBuffer response = new StringBuffer(); 

               while((line = rd.readLine()) != null)
               {
                  response.append(line);
                  response.append('\r');
               }

               rd.close();

               if(RemoteQuery.debug){sender.sendMessage("PHP Response: " + response.toString());}

               // construct hash of given PW and salt
               //String hash = constructPWhashForIPboard(escapeHTML("myPW"), "|AmJu"); // TODO get IPboard hash method running!

               //if(RemoteQuery.debug){sender.sendMessage("PLUGIN: Hash: " + hash);}              
            }
            catch (Exception e)
            {
               schedHandler.sendSyncMessage(sender, ChatColor.RED + "Fehler beim Senden der HTTP request!");
               e.printStackTrace();               
            }
            finally
            {
               if(connection != null)
               {
                  connection.disconnect();
               }
            }
         }
      });
   }

   private String encrypt(String message) throws Exception
   {
      /*byte[] salt = {
            (byte)0xd3, (byte)0xa3, (byte)0x61, (byte)0x8c,
            (byte)0x16, (byte)0xc1, (byte)0xfe, (byte)0x22
        };

      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 16384, 128);
      SecretKey tmp = factory.generateSecret(spec);
      SecretKey keyspec = new SecretKeySpec(tmp.getEncoded(), "AES");*/

      SecretKeySpec keyspec = new SecretKeySpec(RemoteQuery.secretKey.getBytes("UTF-8"), "AES"); // fixed key alone is not so secure
      //IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes("UTF-8"));      
      //cipher.init(Cipher.ENCRYPT_MODE,keyspec,ivspec); // CBC uses iv
      cipher.init(Cipher.ENCRYPT_MODE, keyspec); // ECB uses no iv
      byte[] plainBytes = message.getBytes("UTF-8");
      byte[] encrypted = cipher.doFinal(plainBytes);

      return bytesToHexString(encrypted);
   }

   // Input must be HEX string (32 digits (16 HEX values))
   private String decrypt(String encryptedMessage)
   {
      byte[] encMsgAsBytes = null;
      byte[] decrypted = null;

      try
      {
         encMsgAsBytes = hexStringToByteArray(encryptedMessage);
         SecretKeySpec keyspec = new SecretKeySpec(RemoteQuery.secretKey.getBytes("UTF-8"), "AES");
         //IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes("UTF-8"));         
         //cipher.init(Cipher.ENCRYPT_MODE,keyspec,ivspec); // CBC uses iv
         cipher.init(Cipher.DECRYPT_MODE, keyspec); // ECB uses no iv
         decrypted = cipher.doFinal(encMsgAsBytes);         
      }
      catch (InvalidKeyException e1)
      {         
         e1.printStackTrace();
      }
      /*catch (InvalidAlgorithmParameterException e1)
      {         
         e1.printStackTrace();
      }*/
      catch (UnsupportedEncodingException e)
      {
         e.printStackTrace();
      }
      catch (IllegalBlockSizeException e)
      {        
         e.printStackTrace();
      }
      catch (BadPaddingException e)
      {         
         e.printStackTrace();
      }      

      String plainText = hexStringToCleanUTF8string(bytesToHexString(decrypted));

      return plainText;
   }

   private String constructPWhashForIPboard(String pw, String salt)
   {
      String hash = "";

      //$hash = md5( md5( $salt ) . md5( $password ) );
      /* this is how IPboard constructs the hash
         $hash is the value stored in the database column members_pass_hash.
         $salt is the value stored in the database column members_pass_salt.
         $password is the plain text password.  (but some chars are HTML-escaped decimally!!
         see: http://www.invisionpower.com/support/guides/_/advanced-and-developers/integration/login-modules-r42)
       */

      byte[] hashedPW = null;
      byte[] hashedSalt = null;

      try
      {
         hashedPW = getMD5(pw.getBytes("UTF-8")); // beware of special chars! IPboard chnages some chars to other format. e.g.: ! -> &#33;
         if(RemoteQuery.debug){RemoteQuery.log.info("Hashed PW: " + bytesToHexString(hashedPW));}
         hashedSalt = getMD5(salt.getBytes("UTF-8"));
         if(RemoteQuery.debug){RemoteQuery.log.info("Hashed Salt: " + bytesToHexString(hashedSalt));}
      }
      catch (UnsupportedEncodingException e)
      {       
         e.printStackTrace();
      }

      byte[] concatValue = concatByteArrays(hashedPW, hashedSalt);
      hash = bytesToHexString(getMD5(concatValue));

      return hash;
   }

   private byte[] getMD5(byte[] value)
   {      
      byte[] theDigest = null;      
      MessageDigest md = null;

      try
      {  
         md = MessageDigest.getInstance("MD5");
      }      
      catch (NoSuchAlgorithmException e)
      {         
         e.printStackTrace();
      }

      if(null != md)
      {
         theDigest = md.digest(value);
      }

      return theDigest;
   }

   private String bytesToHexString(byte[] bytes)
   {
      char[] hexChars = new char[bytes.length * 2];

      for ( int j = 0; j < bytes.length; j++ ) {
         int v = bytes[j] & 0xFF;
         hexChars[j * 2] = hexArray[v >>> 4];
         hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }

      return new String(hexChars);
   }

   private byte[] hexStringToByteArray(String s)
   {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
         data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
               + Character.digit(s.charAt(i+1), 16));
      }
      return data;
   }

   private String hexStringToCleanUTF8string(String hexString)
   {
      ByteBuffer buff = ByteBuffer.allocate(hexString.length()/2);
      for (int i = 0; i < hexString.length(); i+=2) {
         buff.put((byte)Integer.parseInt(hexString.substring(i, i+2), 16));
      }
      buff.rewind();
      Charset cs = Charset.forName("UTF-8");
      CharBuffer cb = cs.decode(buff);

      String cleanedText = cb.toString().replaceAll("\\s+", ""); // delete spaces, newlines, tabs, line feeds

      return cleanedText;
   }

   private byte[] concatByteArrays(byte[] a, byte[] b)
   {
      byte[] c = new byte[a.length + b.length];
      System.arraycopy(a, 0, c, 0, a.length);
      System.arraycopy(b, 0, c, a.length, b.length);

      return c;
   }

   private String escapeHTML(String s)
   {
      String escaped = HtmlUtils.htmlEscapeDecimal(s); // suitable for IPboard password field
      return (escaped);
   }
}
