/**
 * 
 */
package org.scictrl.csshell.epics.casperr;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import org.scictrl.csshell.ConnectorUtilities;
import org.scictrl.csshell.Poop;
import org.scictrl.csshell.RemoteException;
import org.scictrl.csshell.epics.EPICSConnector;

import gov.aps.jca.JCALibrary;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.configuration.DefaultConfiguration;
import gov.aps.jca.event.ContextExceptionEvent;
import gov.aps.jca.event.ContextExceptionListener;
import gov.aps.jca.event.ContextMessageEvent;
import gov.aps.jca.event.ContextMessageListener;
import gov.aps.jca.event.ContextVirtualCircuitExceptionEvent;
import tools.SpringUtilities;

/**
 * <p>CASperr class.</p>
 *
 * @author igor@scictrl.com
 */
public class CASperr {

	/**
	 * <p>main.</p>
	 *
	 * @param args an array of {@link java.lang.String} objects
	 */
	public static void main(String[] args) {
		try {
			
			DefaultConfiguration config = new DefaultConfiguration("EPICSPlugConfig");
		    config.setAttribute("class", JCALibrary.CHANNEL_ACCESS_SERVER_JAVA);

		    final CASperr sperr= new CASperr();
		    
		    
		    
		    
		    final List<String> pvs= new ArrayList<String>();
		    
		    Server serv= new Server();
		    
		    serv.addPropertyChangeListener(Server.PROPERTY_LAST_ADDED, new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					try {
						pvs.add((String)evt.getNewValue());
						Collections.sort(pvs);
						
						sperr.getCache().addRecord((String)evt.getNewValue());
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		    
			// create context
		    ServerContext c = JCALibrary.getInstance().createServerContext(config, serv);
		    
			// register all context listeners
			c.addContextExceptionListener(new ContextExceptionListener() {
				
				@Override
				public void contextVirtualCircuitException(
						ContextVirtualCircuitExceptionEvent ev) {
					System.out.println(ev);
				}
				
				@Override
				public void contextException(ContextExceptionEvent ev) {
					System.out.println(ev);
				}
			});
			c.addContextMessageListener(new ContextMessageListener() {
				
				@Override
				public void contextMessage(ContextMessageEvent ev) {
					System.out.println(ev);
				}
			});

			
			
			JFrame f= sperr.getFrame();
			f.setVisible(true);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private JFrame frame;
	private JTable table;
	private AbstractTableModel tableModel;
	private PVCache cache;
	private List<String> names= new ArrayList<String>();
	private JPanel topPane;
	private JTextField filterText;
	private JButton filterClearButton;
	private JPanel bottomPane;
	private JButton updateButton;
	private EPICSConnector connector;
	private ThreadPoolExecutor executor;

	/**
	 * Constructor.
	 */
	public CASperr() {
	}
	
	/**
	 * <p>Getter for the field <code>cache</code>.</p>
	 *
	 * @return a {@link org.scictrl.csshell.epics.casperr.PVCache} object
	 */
	public PVCache getCache() {
		if (cache == null) {
			cache = new PVCache(new File("pvCache.xml"));
			rebuildTable();
		}

		return cache;
	}
	
	private JFrame getFrame() {
		
		if (frame == null) {
			frame = new JFrame("CS Sperr");
			
			JPanel cont= new JPanel();
			cont.setLayout(new GridBagLayout());
			
			GridBagConstraints gbc= new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(11, 11, 4, 11),0,0);
			cont.add(getTopPane(), gbc);

			JScrollPane jsp= new JScrollPane();
			jsp.setViewportView(getTable());
			
			gbc.gridy=1;
			gbc.weighty=1;
			gbc.insets=new Insets(0, 11,0,11);
			cont.add(jsp, gbc);
			
			gbc.gridy=2;
			gbc.weighty=0;
			gbc.insets=new Insets(4, 11,11,11);
			cont.add(getBottomPane(), gbc);
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setContentPane(cont);
			frame.setSize(400, 400);
		}

		return frame;
		
	}


	private JPanel getBottomPane() {
		if (bottomPane == null) {
			bottomPane = new JPanel();
			bottomPane.setLayout(new SpringLayout());
			
			updateButton= new JButton("Update");
			updateButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					updatePVs();
				}
			});
			bottomPane.add(updateButton);
			SpringUtilities.makeCompactGrid(bottomPane, 1, 1, 0, 0, 0, 0);
		}

		return bottomPane;
	}
	
	private void updatePVs() {
		
		updateButton.setEnabled(false);
		
		if (connector==null) {
			try {
				Properties p= new Properties();
				p.setProperty(ConnectorUtilities.CONNECTION_TIMEOUT, "5000");
				connector = EPICSConnector.newInstance(p);
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		}

		new Thread() {
			@Override
			public void run() {
				
				for (int i=0; i<names.size(); i++) {
					final String s= names.get(i);
					
					getExecutor().submit(new Runnable() {
						
						@Override
						public void run() {
							try {
								Poop<?, ?> p= connector.getOneShot(s);
								if (p!=null) {
									getCache().updateRecord(s, p);
								}
							} catch (Exception e) {
								e.printStackTrace();
							} 
						}
					});
				}
				updateButton.setEnabled(true);
			}
		}.start();

	}

	private JPanel getTopPane() {
		if (topPane == null) {
			topPane = new JPanel();
			
			topPane.setLayout(new SpringLayout());
			
			topPane.add(new JLabel("Filter:"));
			
			filterText= new JTextField();
			filterText.setPreferredSize(new Dimension(70, 21));
			filterText.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent e) {
					rebuildTable();
				}
				
				@Override
				public void insertUpdate(DocumentEvent e) {
					rebuildTable();
				}
				
				@Override
				public void changedUpdate(DocumentEvent e) {
					rebuildTable();
				}
			});
			topPane.add(filterText);
			
			filterClearButton= new JButton("Clear");
			filterClearButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					filterText.setText("");
				}
			});
			topPane.add(filterClearButton);
			
			
			SpringUtilities.makeCompactGrid(topPane, 1, 3, 11, 11, 11, 11);
		}

		return topPane;
	}

	/**
	 * <p>rebuildTable.</p>
	 */
	protected void rebuildTable() {
		if (filterText==null || filterText.getText()==null || filterText.getText().length()==0) {
			names.clear();
			names.addAll(Arrays.asList(getCache().getNames()));
		} else {
			names.clear();
			String f= filterText.getText().toLowerCase();
			String[] nn= getCache().getNames();
			for (String n : nn) {
				if (n.toLowerCase().contains(f)) {
					names.add(n);
				}
			}
		}
		
		if (tableModel==null) {
			return;
		}
		
		tableModel.fireTableDataChanged();
	}

	private JTable getTable() {
		if (table == null) {
			table = new JTable();
			
			tableModel= new AbstractTableModel() {
				
				final String[] columns={"PV","Type","Value","Status"};
				
				private static final long serialVersionUID = 1L;

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					
					switch (columnIndex) {
					case 0:
						return names.get(rowIndex);
					case 1:
						Poop<?, ?> p=getCache().getRecord(names.get(rowIndex)).getPoop();
						if (p!=null) {
							return p.getMetaData().getDataType();
						} else {
							return "";
						}
					case 2:
						Poop<?, ?> p1=getCache().getRecord(names.get(rowIndex)).getPoop();
						if (p1!=null) {
							return p1.getString();
						} else {
							return "";
						}
					case 3:
						Poop<?, ?> p2=getCache().getRecord(names.get(rowIndex)).getPoop();
						if (p2!=null) {
							return p2.getStatus().toString();
						} else {
							return "";
						}

					default:
						return "";
					}
				}
				
				@Override
				public int getRowCount() {
					return names.size();
				}
				
				@Override
				public int getColumnCount() {
					return columns.length;
				}
				@Override
				public String getColumnName(int column) {
					return columns[column];
				}
			};
			
			getCache().addPropertyChangeListener(PVCache.PROPERTY_RECORD_ADDED, new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					rebuildTable();
				}
			});
			
			getCache().addPropertyChangeListener(PVCache.PROPERTY_RECORD_UPDATED, new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					tableModel.fireTableDataChanged();
				}
			});

			table.setModel(tableModel);
			
		}

		return table;
	}
	
	/**
	 * <p>Getter for the field <code>executor</code>.</p>
	 *
	 * @return a {@link java.util.concurrent.ExecutorService} object
	 */
	public ExecutorService getExecutor() {
		
		if (executor == null) {
			executor = new ThreadPoolExecutor(10, 10, 10000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			
		}

		return executor;
		
	}

}
