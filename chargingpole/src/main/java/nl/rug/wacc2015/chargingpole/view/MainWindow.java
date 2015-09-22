package nl.rug.wacc2015.chargingpole.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import static java.awt.event.ActionEvent.ACTION_FIRST;
import static java.awt.event.ActionEvent.ACTION_LAST;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;

import nl.rug.wacc2015.chargingpole.controller.UIController;
import nl.rug.wacc2015.chargingpole.model.ChargeSession;
import nl.rug.wacc2015.chargingpole.model.SessionStorage;

public class MainWindow extends JFrame implements Observer {

	private static final long serialVersionUID = 1L;

	private static final Insets insets = new Insets(0, 0, 0, 0);

	private JTextArea scrollpanel;
	private JScrollPane scrollpanelpane;

	private JButton carconnectbtn;
	private JButton cardisconnectbtn;

	public MainWindow(UIController c) {
		initComponents(c);
	}

	private void initComponents(final UIController c) {
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent we) {
				c.actionPerformed(new ActionEvent(we.getWindow(), ACTION_FIRST, "POLE.RUNNING"));
			}

			@Override
			public void windowClosing(WindowEvent we) {
				c.actionPerformed(new ActionEvent(we.getWindow(), ACTION_LAST, "POLE.SHUTDOWN"));
			}
		});

		// Specify size
		Container windowcp = this.getContentPane();
		Container cp = new JPanel();
		cp.setPreferredSize(new Dimension(300, 250));
		cp.setLayout(new GridBagLayout());

		// Add the textfielD
		scrollpanel = new JTextArea();
		scrollpanel.setEditable(false);
		scrollpanelpane = new JScrollPane(scrollpanel);
		addComponent(cp, scrollpanelpane, 0, 0, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

		Container cp2 = new JPanel();
		cp2.setLayout(new GridBagLayout());

		carconnectbtn = new JButton("car connect");
		carconnectbtn.setActionCommand("CAR.CONNECT");
		carconnectbtn.addActionListener(c);

		addComponent(cp2, carconnectbtn, 0, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

		cardisconnectbtn = new JButton("car disconnect");
		cardisconnectbtn.setActionCommand("CAR.DISCONNECT");
		cardisconnectbtn.addActionListener(c);

		addComponent(cp2, cardisconnectbtn, 0, 2, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

		addComponent(cp, cp2, 0, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

		windowcp.add(cp);
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Set the text in the scroll panel.
	 *
	 * @param text
	 */
	private void setScrollText(SessionStorage ss) {
		ChargeSession s = ss.getCurrentSession();
		String text;
		if (s == null) {
			text = "No session running\n";
		} else {
			text = "Currently charging\n";
			text += "Started: " + s.getStarttime() + "\n";
			text += "KWh: \t" + Math.round(s.getKwh()*100.0)/100.0 + "\n";
			text += "Price: € \t" + Math.round(s.getPrice()*100.0)/100.0 + "\n";
			text += "Plate: " + s.getCard().getPlateNumber() + "\n";
		}
		text += "Sessions ready to be send: " + ss.getNumSessionsReady();
		scrollpanel.setText(text);
		JScrollBar vertical = scrollpanelpane.getVerticalScrollBar();
		vertical.validate();
		vertical.setValue(vertical.getMaximum());
	}

	private static void addComponent(Container container, Component component, int gridx, int gridy, int gridwidth,
			int gridheight, int anchor, int fill) {
		GridBagConstraints gbc = new GridBagConstraints(gridx, gridy, gridwidth, gridheight, 1.0, 1.0, anchor, fill,
				insets, 0, 0);
		container.add(component, gbc);
	}

	@Override
	public void update(Observable o, Object arg) {
		SessionStorage ss = (SessionStorage) o;
		setTitle(ss.getPoleIDString());
		setScrollText(ss);
		this.repaint();
	}
}
