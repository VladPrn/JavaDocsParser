package edu.veterans.java;

import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.JLabel;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;

//Пользовательский интерфейс
public class App {
	private JFrame frmJavadocsparser;
	private JTextPane textConsole;
	private JButton buttonStop;
	private JLabel lblClassCount;
	private JLabel lblInterfaceCount;

	private OWLCreator cl;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					App window = new App();
					window.frmJavadocsparser.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public App() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmJavadocsparser = new JFrame();
		frmJavadocsparser.setTitle("JavaDocsParser");
		frmJavadocsparser.setBounds(100, 100, 448, 348);
		frmJavadocsparser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmJavadocsparser.getContentPane().setLayout(null);
		
		JButton btnStart = new JButton("Start");
		btnStart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				parse();
			}
		});
		btnStart.setBounds(10, 11, 89, 23);
		frmJavadocsparser.getContentPane().add(btnStart);
		
		textConsole = new JTextPane();
		
		JScrollPane scroll = new JScrollPane(textConsole);
		scroll.setBounds(10, 95, 414, 206);
		frmJavadocsparser.getContentPane().add(scroll);
		
		buttonStop = new JButton("Stop");
		buttonStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		buttonStop.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				cl.stopCreating();
			}
		});
		buttonStop.setBounds(104, 11, 89, 23);
		frmJavadocsparser.getContentPane().add(buttonStop);
		
		JLabel lbl1 = new JLabel("Parsed class count:");
		lbl1.setBounds(10, 45, 137, 14);
		frmJavadocsparser.getContentPane().add(lbl1);
		
		JLabel lbl2 = new JLabel("Parsed interface count:");
		lbl2.setBounds(10, 70, 137, 14);
		frmJavadocsparser.getContentPane().add(lbl2);
		
		lblClassCount = new JLabel("0");
		lblClassCount.setBounds(163, 45, 46, 14);
		frmJavadocsparser.getContentPane().add(lblClassCount);
		
		lblInterfaceCount = new JLabel("0");
		lblInterfaceCount.setBounds(163, 70, 46, 14);
		frmJavadocsparser.getContentPane().add(lblInterfaceCount);
	}
	
	//Создание онтологии
	private void parse() {
		try {
			cl = new OWLCreator();
			
			cl.addMessager(new MessageSend() {
				@Override
				public void send(String message) {					
					addTextToConsole(message);
				}
			});
			
			cl.addProgressClasses(new Progress() {
				@Override
				public void alreadyDid(int count) {
					lblClassCount.setText("" + count);
				}		
			});
			
			cl.addProgressInterfaces(new Progress() {
				@Override
				public void alreadyDid(int count) {
					lblInterfaceCount.setText("" + count);
				}
				
			});
			
			cl.start();
			} catch(OWLOntologyCreationException ex) {
				ex.printStackTrace();
			}
	}
	
	//Добавить текст в консоль и прокрутить её вниз 
	private void addTextToConsole(String message) {
		try {
			textConsole.getDocument().insertString(textConsole.getDocument().getLength(), message + System.lineSeparator(), null);
			textConsole.setCaretPosition(textConsole.getDocument().getLength());
		} catch (BadLocationException ex) {
			//Ошибка, которая должна никогда не случится
		}
		
	}
}