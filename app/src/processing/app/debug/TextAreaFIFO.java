// adapted from https://community.oracle.com/thread/1479784

package processing.app.debug;

import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class TextAreaFIFO extends JTextArea implements DocumentListener
{
	private int maxChars;
	private int updateCount;  // limit how often we trim the document
	private boolean doTrim;

	public TextAreaFIFO(int max) {
		maxChars = max;
		updateCount = 0;
		doTrim = true;
		getDocument().addDocumentListener( this );
	}

	public void allowTrim(boolean trim) {
		doTrim = trim;
	}

	public void insertUpdate(DocumentEvent e) {
		if (++updateCount > 150 && doTrim) {
			updateCount = 0;
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					trimDocument();
				}
			});
		}
	}

	public void removeUpdate(DocumentEvent e) {}
	public void changedUpdate(DocumentEvent e) {}

	public void trimDocument() {
		int len = 0;
		len = getDocument().getLength();
		if (len > maxChars) {
			int n = len - maxChars;
			//System.out.println("trimDocument: remove " + n + " chars");
			try {
				getDocument().remove(0, n);
			} catch(BadLocationException ble) {
			}
		}
	}
}
