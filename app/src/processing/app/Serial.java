/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PSerial - class for serial port goodness
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry & Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.app;
//import processing.core.*;

import processing.app.debug.MessageConsumerBytes;
import static processing.app.I18n._;

import gnu.io.*;

import java.io.*;
import java.util.*;


public class Serial implements SerialPortEventListener {

  //PApplet parent;

  // properties can be passed in for default values
  // otherwise defaults to 9600 N81

  // these could be made static, which might be a solution
  // for the classloading problem.. because if code ran again,
  // the static class would have an object that could be closed

  SerialPort port;

  int rate;
  int parity;
  int databits;
  int stopbits;
  boolean monitor = false;

  // read buffer and streams 

  InputStream input;
  OutputStream output;

  MessageConsumerBytes consumer;

  public Serial(boolean monitor) throws SerialException {
    this(Preferences.get("serial.port"),
      Preferences.getInteger("serial.debug_rate"),
      Preferences.get("serial.parity").charAt(0),
      Preferences.getInteger("serial.databits"),
      new Float(Preferences.get("serial.stopbits")).floatValue());
    this.monitor = monitor;
  }
    
  public Serial() throws SerialException {
    this(Preferences.get("serial.port"),
      Preferences.getInteger("serial.debug_rate"),
      Preferences.get("serial.parity").charAt(0),
      Preferences.getInteger("serial.databits"), 
      new Float(Preferences.get("serial.stopbits")).floatValue());
  }

  public Serial(int irate) throws SerialException {
    this(Preferences.get("serial.port"), irate,
      Preferences.get("serial.parity").charAt(0),
      Preferences.getInteger("serial.databits"), 
      new Float(Preferences.get("serial.stopbits")).floatValue());
  }

  public Serial(String iname, int irate) throws SerialException {
    this(iname, irate, Preferences.get("serial.parity").charAt(0),
    Preferences.getInteger("serial.databits"), 
    new Float(Preferences.get("serial.stopbits")).floatValue());
  }

  public Serial(String iname) throws SerialException {
    this(iname, Preferences.getInteger("serial.debug_rate"),
      Preferences.get("serial.parity").charAt(0),
      Preferences.getInteger("serial.databits"),
      new Float(Preferences.get("serial.stopbits")).floatValue());
  }

  public static boolean touchPort(String iname, int irate) throws SerialException {
    SerialPort port;
    boolean result = false;
    try {
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
        if ((CommPortIdentifier.PORT_SERIAL == portId.getPortType()) && (portId.getName().equals(iname))) {
          port = (SerialPort) portId.open("tap", 2000);
          port.setSerialPortParams(irate, 8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
          port.close();				
          result = true;
        }
      }
    } catch (PortInUseException e) {
      throw new SerialException(
        I18n.format(_("Serial port ''{0}'' already in use. Try quitting any programs that may be using it."), iname)
      );
    } catch (Exception e) {
      throw new SerialException(
        I18n.format(_("Error touching serial port ''{0}''."), iname), e
      );
    }
	return result;
  }

  public Serial(String iname, int irate,
                 char iparity, int idatabits, float istopbits)
  throws SerialException {
    //if (port != null) port.close();
    //this.parent = parent;
    //parent.attach(this);

    this.rate = irate;

    parity = SerialPort.PARITY_NONE;
    if (iparity == 'E') parity = SerialPort.PARITY_EVEN;
    if (iparity == 'O') parity = SerialPort.PARITY_ODD;

    this.databits = idatabits;

    stopbits = SerialPort.STOPBITS_1;
    if (istopbits == 1.5f) stopbits = SerialPort.STOPBITS_1_5;
    if (istopbits == 2) stopbits = SerialPort.STOPBITS_2;

    if (iname == "fake serial") return;

    try {
      port = null;
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId =
          (CommPortIdentifier) portList.nextElement();

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          //System.out.println("found " + portId.getName());
          if (portId.getName().equals(iname)) {
            //System.out.println("looking for "+iname);
            port = (SerialPort)portId.open("serial madness", 2000);
            input = port.getInputStream();
            output = port.getOutputStream();
            port.setSerialPortParams(rate, databits, stopbits, parity);
            port.addEventListener(this);
            port.notifyOnDataAvailable(true);
            //System.out.println("opening, ready to roll");
          }
        }
      }
    } catch (PortInUseException e) {
      throw new SerialException(
        I18n.format(
          _("Serial port ''{0}'' already in use. Try quiting any programs that may be using it."),
          iname
        )
      );
    } catch (Exception e) {
      throw new SerialException(
        I18n.format(
          _("Error opening serial port ''{0}''."),
          iname
        ),
        e
      );
//      //errorMessage("<init>", e);
//      //exception = e;
//      //e.printStackTrace();
    }
    
    if (port == null) {
      throw new SerialNotFoundException(
        I18n.format(
          _("Serial port ''{0}'' not found. Did you select the right one from the Tools > Serial Port menu?"),
          iname
        )
      );
    }
  }

  public void setBaud(int irate) {
    if (port != null) {
      try {
        port.setSerialPortParams(irate, 8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
      } catch (Exception e) { }
    }
  }

  public void setup() {
    //parent.registerCall(this, DISPOSE);
  }


  //public void size(int w, int h) { }

  //public void pre() { }

  //public void draw() { }

  //public void post() { }

  //public void mouse(java.awt.event.MouseEvent event) { }

  //public void key(java.awt.event.KeyEvent e) { }


  public void dispose() {
    try {
      // do io streams need to be closed first?
      if (input != null) input.close();
      if (output != null) output.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    input = null;
    output = null;

    try {
      if (port != null) port.close();  // close the port

    } catch (Exception e) {
      e.printStackTrace();
    }
    port = null;
  }
  
  
  public void addListener(MessageConsumerBytes consumer) {
    this.consumer = consumer;
  }


  synchronized public void serialEvent(SerialPortEvent serialEvent) {
    //System.out.println("serial port event"); // " + serialEvent);
    //System.out.flush();
    //System.out.println("into");
    //System.out.flush();
    //System.err.println("type " + serialEvent.getEventType());
    if (serialEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
      try {
        while (input.available() > 0) {
          byte[] tmpbuf = new byte[4096];
          int tmpcount = input.read(tmpbuf);
          if (this.consumer != null && tmpcount > 0) {
            this.consumer.message(new String(tmpbuf, 0, tmpcount));
            //this.consumer.messageBytes(tmpbuf, tmpcount);
          }
        }
        //System.out.println("no more");

      } catch (IOException e) {
        errorMessage("serialEvent", e);
        //e.printStackTrace();
        //System.out.println("angry");
      }
      catch (Exception e) {
      }
    }
    //System.out.println("out of");
    //System.err.println("out of event " + serialEvent.getEventType());
  }


  /**
   * This will handle both ints, bytes and chars transparently.
   */
  public void write(int what) {  // will also cover char
    try {
      output.write(what & 0xff);  // for good measure do the &
      output.flush();   // hmm, not sure if a good idea

    } catch (Exception e) { // null pointer or serial port dead
      errorMessage("write", e);
    }
  }


  public void write(byte bytes[]) {
    try {
      output.write(bytes);
      output.flush();   // hmm, not sure if a good idea

    } catch (Exception e) { // null pointer or serial port dead
      //errorMessage("write", e);
      e.printStackTrace();
    }
  }


  /**
   * Write a String to the output. Note that this doesn't account
   * for Unicode (two bytes per char), nor will it send UTF8 
   * characters.. It assumes that you mean to send a byte buffer 
   * (most often the case for networking and serial i/o) and
   * will only use the bottom 8 bits of each char in the string.
   * (Meaning that internally it uses String.getBytes)
   *
   * If you want to move Unicode data, you can first convert the
   * String to a byte stream in the representation of your choice
   * (i.e. UTF8 or two-byte Unicode data), and send it as a byte array.
   */
  public void write(String what) {
    write(what.getBytes());
  }

  public void setDTR(boolean state) {
    port.setDTR(state);
  }

  public void setRTS(boolean state) {
    port.setRTS(state);
  }

  /**
   * If this just hangs and never completes on Windows, 
   * it may be because the DLL doesn't have its exec bit set.
   * Why the hell that'd be the case, who knows.
   */
  static public List<String> list() {
    List<String> list = new ArrayList<String>();
    try {
      //System.err.println("trying");
      @SuppressWarnings("unchecked")
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      //System.err.println("got port list");
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId = 
          (CommPortIdentifier) portList.nextElement();
        //System.out.println(portId);

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          String name = portId.getName();
          list.add(name);
        }
      }

    } catch (UnsatisfiedLinkError e) {
      //System.err.println("1");
      errorMessage("ports", e);

    } catch (Exception e) {
      //System.err.println("2");
      errorMessage("ports", e);
    }
    //System.err.println("move out");
    return list;
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  static public void errorMessage(String where, Throwable e) {
    System.err.println(I18n.format(_("Error inside Serial.{0}()"), where));
    e.printStackTrace();
  }
}


  /*
  class SerialMenuListener implements ItemListener {
    //public SerialMenuListener() { }

    public void itemStateChanged(ItemEvent e) {
      int count = serialMenu.getItemCount();
      for (int i = 0; i < count; i++) {
        ((CheckboxMenuItem)serialMenu.getItem(i)).setState(false);
      }
      CheckboxMenuItem item = (CheckboxMenuItem)e.getSource();
      item.setState(true);
      String name = item.getLabel();
      //System.out.println(item.getLabel());
      PdeBase.properties.put("serial.port", name);
      //System.out.println("set to " + get("serial.port"));
    }
  }
  */


  /*
  protected Vector buildPortList() {
    // get list of names for serial ports
    // have the default port checked (if present)
    Vector list = new Vector();

    //SerialMenuListener listener = new SerialMenuListener();
    boolean problem = false;

    // if this is failing, it may be because
    // lib/javax.comm.properties is missing.
    // java is weird about how it searches for java.comm.properties
    // so it tends to be very fragile. i.e. quotes in the CLASSPATH
    // environment variable will hose things.
    try {
      //System.out.println("building port list");
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId = 
          (CommPortIdentifier) portList.nextElement();
        //System.out.println(portId);

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          //if (portId.getName().equals(port)) {
          String name = portId.getName();
          //CheckboxMenuItem mi = 
          //new CheckboxMenuItem(name, name.equals(defaultName));

          //mi.addItemListener(listener);
          //serialMenu.add(mi);
          list.addElement(name);
        }
      }
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      problem = true;

    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }

    //if (serialMenu.getItemCount() == 0) {
      //System.out.println("dimming serial menu");
    //serialMenu.setEnabled(false);
    //}

    // only warn them if this is the first time
    if (problem && PdeBase.firstTime) {
      JOptionPane.showMessageDialog(this, //frame,
                                    "Serial port support not installed.\n" +
                                    "Check the readme for instructions\n" +
                                    "if you need to use the serial port.    ",
                                    "Serial Port Warning",
                                    JOptionPane.WARNING_MESSAGE);
    }
    return list;
  }
  */


 
