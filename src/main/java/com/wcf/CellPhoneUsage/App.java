package com.wcf.CellPhoneUsage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;

class PrintJobWatcher {
	boolean done = false;

	PrintJobWatcher(DocPrintJob job) {
		job.addPrintJobListener(new PrintJobAdapter() {
			public void printJobCanceled(PrintJobEvent pje) {
				allDone();
			}

			public void printJobCompleted(PrintJobEvent pje) {
				allDone();
			}

			public void printJobFailed(PrintJobEvent pje) {
				allDone();
			}

			public void printJobNoMoreEvents(PrintJobEvent pje) {
				allDone();
			}

			void allDone() {
				synchronized (PrintJobWatcher.this) {
					done = true;
					System.out.println("Printing done ...");
					PrintJobWatcher.this.notify();
				}
			}
		});
	}

	public synchronized void waitForDone() {
		try {
			while (!done) {
				wait();
			}
		} catch (InterruptedException e) {
		}
	}
}

class Usage {
	public Usage(Date usageDate, int totalMinutes, float totalData) {
		super();
		this.usageDate = usageDate;
		this.totalMinutes = totalMinutes;
		this.totalData = totalData;
	}

	Date usageDate;
	int totalMinutes;
	float totalData;
}

class Employee {
	public Employee(int employeeId, String employeeName, String cellPhoneModel, Date purchaseDate) {
		super();
		this.employeeId = employeeId;
		this.employeeName = employeeName;
		this.cellPhoneModel = cellPhoneModel;
		this.purchaseDate = purchaseDate;
	}

	int employeeId;
	String employeeName;
	String cellPhoneModel;
	Date purchaseDate;
	HashMap<String, Usage> usageList = new HashMap<String, Usage>();
}

public class App {

	private Connection getDatabaseConnection() {

		try {
			Class.forName("org.postgresql.Driver");

			return DriverManager.getConnection("jdbc:postgresql://localhost:5432/cellphoneusage", "postgres", "password");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	private int getNumberOfPhones(Connection connection) {

		try {
			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT COUNT(employeeId) AS totalPhones \r\n" + "FROM cellphone");

			if (rs.next()) {
				return rs.getInt("totalPhones");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	private void createHeaderSection(Connection connection, StringBuilder sb) {
		LocalDate localDate = LocalDate.now();

		int totalPhones = getNumberOfPhones(connection);

		try {
			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery(
					"SELECT SUM(totalminutes) AS totalMinutes, SUM(totaldata) AS totalData, TRUNC(avg(totalminutes), 3) AS avgMinutes, TRUNC(avg(totaldata), 3) AS avgData \r\n"
							+ "FROM cellphoneusage");

			if (rs.next()) {
				sb.append("Current Date, Number of Phones, Total Minutes, Total Data, Average Minutes, Average Data\n");
				sb.append(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDate) + ", " + totalPhones
						+ ", " + rs.getInt("totalMinutes") + ", " + " " + rs.getFloat("totalData") + ", "
						+ rs.getFloat("avgMinutes") + ", " + rs.getFloat("avgData") + "\n\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void createDetailSection(Connection connection, StringBuilder sb) {
		HashMap<String, Employee> employees = new HashMap<String, Employee>();

		try {

			Statement stmt = connection.createStatement();

			ResultSet rs = stmt.executeQuery(
					"SELECT cp.employeeId, cp.employeeName, cp.model, cp.purchaseDate, cpu.date, cpu.totalminutes, cpu.totaldata\r\n"
							+ "FROM cellphone cp\r\n"
							+ "LEFT JOIN cellphoneusage cpu ON cpu.employeeId = cp.employeeId\r\n"
							+ "GROUP BY cp.employeeId, cp.employeeName, cp.model, cp.purchaseDate, cpu.date, cpu.totalminutes, cpu.totaldata\r\n"
							+ "ORDER BY cp.employeeId, cpu.date");

			while (rs.next()) {
				String employeeId = rs.getString("employeeId");
				Employee employee = employees.get(employeeId);
				if (employee == null) {
					employee = new Employee(rs.getInt("employeeId"), rs.getString("employeeName"),
							rs.getString("model"), rs.getDate("purchaseDate"));
					employees.put(employeeId, employee);
				}
				
				// Using a HashMap to ensure the usage date is in the right year-month. If records have the same usage date then just add the total minutes and data to the Usage object.	
				Date usageDate = rs.getDate("date");
				SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM"); 
				String yearmonth = formatter.format(usageDate);
				Usage usage = employee.usageList.get(yearmonth);
				if (usage == null) {
					usage = new Usage(usageDate, rs.getInt("totalMinutes"), rs.getFloat("totalData"));
					employee.usageList.put(yearmonth, usage);
				} else {
					usage.totalMinutes += rs.getInt("totalMinutes");
					usage.totalData += rs.getFloat("totalData");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		sb.append("Employee Id, Employee Name, Model, Purchase Date, Minutes Usage, Data Usage, Usage Date\n");
				
		for (Employee employee : employees.values()) {
			sb.append(employee.employeeId + ", " + employee.employeeName + ", " + employee.cellPhoneModel + ", "
					+ employee.purchaseDate);

			List<String> list = new ArrayList<String>(employee.usageList.keySet());
			
			Collections.sort(list, new Comparator<String>() {
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}				
			});

			for (String key : list) {
				Usage usage = employee.usageList.get(key);
				sb.append(", " + usage.totalMinutes + ", " + String.format("%.3f", usage.totalData) + ", " + key);
			}
			
			sb.append("\n");
		}
	}

	private void createReport(Connection connection) {
		
		StringBuilder sb = new StringBuilder();
		
		createHeaderSection(connection, sb);
		createDetailSection(connection, sb);

		System.out.println(sb.toString());
		
		sb.append("\f");
		
		String defaultPrinter = PrintServiceLookup.lookupDefaultPrintService().getName();
		System.out.println("Default printer: " + defaultPrinter);
		PrintService service = PrintServiceLookup.lookupDefaultPrintService();

		try {
			InputStream is = new ByteArrayInputStream(sb.toString().getBytes("UTF8"));
			PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
			pras.add(new Copies(1));

			DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
			Doc doc = new SimpleDoc(is, flavor, null);
			DocPrintJob job = service.createPrintJob();

			PrintJobWatcher pjw = new PrintJobWatcher(job);
			job.print(doc, pras);
			pjw.waitForDone();
			is.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (PrintException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		App app = new App();

		Connection connection = app.getDatabaseConnection();

		if (connection == null) {
			System.out.println("Error: Could not connect to the database");
			return;
		}

		app.createReport(connection);

		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
