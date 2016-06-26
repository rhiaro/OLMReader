package com.khubla.olmreader.olm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import com.khubla.olmreader.olm.generated.Email;
import com.khubla.olmreader.olm.generated.Emails;
import com.khubla.olmreader.olm.generated.MessageAttachment;
import com.khubla.olmreader.util.GenericJAXBMarshaller;

public class OLMFile {
   private static final String XML = ".xml";
   final ZipFile zipfile;

   public OLMFile(String filename) throws IOException {
      zipfile = new ZipFile(filename);
   }

   public byte[] readAttachment(MessageAttachment messageAttachment) throws ZipException, IOException {
      final ZipArchiveEntry zipEntry = zipfile.getEntry(messageAttachment.getOPFAttachmentURL());
      if (null != zipEntry) {
         final ByteArrayOutputStream boas = new ByteArrayOutputStream();
         IOUtils.copy(zipfile.getInputStream(zipEntry), boas);
         return boas.toByteArray();
      }
      return null;
   }

   public void readOLMFile(OLMMessageCallback olmMessageCallback, OLMRawMessageCallback olmRawMessageCallback) {
      try {
         for (final Enumeration<ZipArchiveEntry> e = zipfile.getEntries(); e.hasMoreElements();) {
            final ZipArchiveEntry zipEntry = e.nextElement();
            System.out.println(zipEntry.getName());
            if (zipEntry.isDirectory() == false) {
               if (zipEntry.getName().trim().toLowerCase().endsWith(XML)) {
                  /*
                   * raw callback
                   */
                  if (null != olmRawMessageCallback) {
                     final InputStream inputStream = zipfile.getInputStream(zipEntry);
                     final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     IOUtils.copy(inputStream, baos);
                     olmRawMessageCallback.message(baos.toString());
                  }
                  /*
                   * message callback
                   */
                  if (null != olmMessageCallback) {
                     final InputStream inputStream = zipfile.getInputStream(zipEntry);
                     final GenericJAXBMarshaller<Emails> marshaller = new GenericJAXBMarshaller<Emails>(Emails.class);
                     Emails emails = null;
                     try {
                        emails = marshaller.unmarshall(inputStream);
                     } catch (Exception ex) {
                        // ex.printStackTrace();
                     }
                     if ((null != emails) && (null != emails.getEmail())) {
                        for (int i = 0; i < emails.getEmail().size(); i++) {
                           final Email email = emails.getEmail().get(i);
                           olmMessageCallback.message(email);
                        }
                     }
                  }
               }
            }
         }
         zipfile.close();
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }
}
