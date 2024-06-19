/**
 * 
 */
package org.scictrl.csshell.epics.server.application.orbitserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.scictrl.csshell.Tools;
import org.scictrl.csshell.epics.server.Record;
import org.scictrl.csshell.epics.server.ValueLinks;
import org.scictrl.csshell.epics.server.ValueLinks.ValueHolder;
import org.scictrl.csshell.epics.server.application.AbstractApplication;
import org.scictrl.csshell.epics.server.application.orbitserver.Orbit.O;
import org.scictrl.csshell.epics.server.application.orbitserver.Orbit.Stat;
import org.scictrl.csshell.epics.server.processor.MemoryValueProcessor;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import tools.BootstrapLoader;

/**
 * <p>OrbitServerApplication class.</p>
 *
 * @author igor@scictrl.com
 */
public class OrbitServerApplication extends AbstractApplication {

	private static final String ORBIT_SERVER = "OrbitServer";
	/** Constant <code>PV_Y="PvY"</code> */
	public static final String PV_Y = "PvY";
	/** Constant <code>PV_X="PvX"</code> */
	public static final String PV_X = "PvX";
	/** Constant <code>PV_BBA_Y="PvBbaY"</code> */
	public static final String PV_BBA_Y = "PvBbaY";
	/** Constant <code>PV_BBA_X="PvBbaX"</code> */
	public static final String PV_BBA_X = "PvBbaX";
	
	/** Constant <code>SFX_STAT_X=":Stat:X"</code> */
	public static final String SFX_STAT_X = 		":Stat:X";
	/** Constant <code>SFX_STAT_X_AVG=":Stat:X:AVG"</code> */
	public static final String SFX_STAT_X_AVG = 	":Stat:X:AVG";
	/** Constant <code>SFX_STAT_X_RMS=":Stat:X:RMS"</code> */
	public static final String SFX_STAT_X_RMS = 	":Stat:X:RMS";
	/** Constant <code>SFX_STAT_X_STD=":Stat:X:STD"</code> */
	public static final String SFX_STAT_X_STD = 	":Stat:X:STD";
	/** Constant <code>SFX_STAT_X_MAX=":Stat:X:MAX"</code> */
	public static final String SFX_STAT_X_MAX = 	":Stat:X:MAX";
	/** Constant <code>SFX_STAT_Y=":Stat:Y"</code> */
	public static final String SFX_STAT_Y = 		":Stat:Y";
	/** Constant <code>SFX_STAT_Y_AVG=":Stat:Y:AVG"</code> */
	public static final String SFX_STAT_Y_AVG = 	":Stat:Y:AVG";
	/** Constant <code>SFX_STAT_Y_RMS=":Stat:Y:RMS"</code> */
	public static final String SFX_STAT_Y_RMS = 	":Stat:Y:RMS";
	/** Constant <code>SFX_STAT_Y_STD=":Stat:Y:STD"</code> */
	public static final String SFX_STAT_Y_STD = 	":Stat:Y:STD";
	/** Constant <code>SFX_STAT_Y_MAX=":Stat:Y:MAX"</code> */
	public static final String SFX_STAT_Y_MAX = 	":Stat:Y:MAX";
	/** Constant <code>SFX_STAT_STRING=":Stat:String"</code> */
	public static final String SFX_STAT_STRING = 	":Stat:String";

	/** Constant <code>REFERENCE_X="Reference:X"</code> */
	public static final String REFERENCE_X = 				"Reference:X";
	/** Constant <code>REFERENCE_Y="Reference:Y"</code> */
	public static final String REFERENCE_Y = 				"Reference:Y";
	/** Constant <code>REFERENCE_SAVE_CSV="Reference:SaveCSV"</code> */
	public static final String REFERENCE_SAVE_CSV = 		"Reference:SaveCSV";
	/** Constant <code>REFERENCE_COMMENT="Reference:Comment"</code> */
	public static final String REFERENCE_COMMENT = 			"Reference:Comment";
	/** Constant <code>REFERENCE_DIFF_BBAREF="Reference:Diff:BBARef"</code> */
	public static final String REFERENCE_DIFF_BBAREF = 		"Reference:Diff:BBARef";
	/** Constant <code>REFERENCE_DIFF_INJREF="Reference:Diff:InjRef"</code> */
	public static final String REFERENCE_DIFF_INJREF = 		"Reference:Diff:InjRef";
	/** Constant <code>REFERENCE_DIFF_INSREF="Reference:Diff:InsRef"</code> */
	public static final String REFERENCE_DIFF_INSREF = 		"Reference:Diff:InsRef";
	/** Constant <code>REFERENCE_AS_BBAREF="Reference:AsBBARef"</code> */
	public static final String REFERENCE_AS_BBAREF= 		"Reference:AsBBARef";
	/** Constant <code>REFERENCE_AS_INJREF="Reference:AsInjRef"</code> */
	public static final String REFERENCE_AS_INJREF= 		"Reference:AsInjRef";
	/** Constant <code>REFERENCE_AS_INSREF="Reference:AsInsRef"</code> */
	public static final String REFERENCE_AS_INSREF= 		"Reference:AsInsRef";

	/** Constant <code>BBAREF_X="BBARef:X"</code> */
	public static final String BBAREF_X = 					"BBARef:X";
	/** Constant <code>BBAREF_Y="BBARef:Y"</code> */
	public static final String BBAREF_Y = 					"BBARef:Y";
	/** Constant <code>BBAREF_AS_REFERENCE="BBARef:AsReference"</code> */
	public static final String BBAREF_AS_REFERENCE= 		"BBARef:AsReference";
	/** Constant <code>BBAREF_SAVE_CSV="BBARef:SaveCSV"</code> */
	public static final String BBAREF_SAVE_CSV = 			"BBARef:SaveCSV";
	/** Constant <code>BBAREF_COMMENT="BBARef:Comment"</code> */
	public static final String BBAREF_COMMENT = 			"BBARef:Comment";

	/** Constant <code>INSREF_X="InsRef:X"</code> */
	public static final String INSREF_X = 					"InsRef:X";
	/** Constant <code>INSREF_Y="InsRef:Y"</code> */
	public static final String INSREF_Y = 					"InsRef:Y";
	/** Constant <code>INSREF_AS_REFERENCE="InsRef:AsReference"</code> */
	public static final String INSREF_AS_REFERENCE= 		"InsRef:AsReference";
	/** Constant <code>INSREF_SAVE_CSV="InsRef:SaveCSV"</code> */
	public static final String INSREF_SAVE_CSV = 			"InsRef:SaveCSV";
	/** Constant <code>INSREF_COMMENT="InsRef:Comment"</code> */
	public static final String INSREF_COMMENT = 			"InsRef:Comment";

	/** Constant <code>INJREF_X="InjRef:X"</code> */
	public static final String INJREF_X = 					"InjRef:X";
	/** Constant <code>INJREF_Y="InjRef:Y"</code> */
	public static final String INJREF_Y = 					"InjRef:Y";
	/** Constant <code>INJREF_AS_REFERENCE="InjRef:AsReference"</code> */
	public static final String INJREF_AS_REFERENCE= 		"InjRef:AsReference";
	/** Constant <code>INJREF_SAVE_CSV="InjRef:SaveCSV"</code> */
	public static final String INJREF_SAVE_CSV = 			"InjRef:SaveCSV";
	/** Constant <code>INJREF_COMMENT="InjRef:Comment"</code> */
	public static final String INJREF_COMMENT = 			"InjRef:Comment";

	/** Constant <code>LIVE_X="Live:X"</code> */
	public static final String LIVE_X = 					"Live:X";
	/** Constant <code>LIVE_Y="Live:Y"</code> */
	public static final String LIVE_Y = 					"Live:Y";
	/** Constant <code>LIVE_COMMENT="Live:Comment"</code> */
	public static final String LIVE_COMMENT = 				"Live:Comment";
	
	/** Constant <code>LIVE_RAW_X="Live:Raw:X"</code> */
	public static final String LIVE_RAW_X = 				"Live:Raw:X";
	/** Constant <code>LIVE_RAW_Y="Live:Raw:Y"</code> */
	public static final String LIVE_RAW_Y = 				"Live:Raw:Y";
	/** Constant <code>LIVE_RAW_SAVE="Live:Raw:SaveMem"</code> */
	public static final String LIVE_RAW_SAVE = 				"Live:Raw:SaveMem";
	/** Constant <code>LIVE_RAW_COMMENT="Live:Raw:Comment"</code> */
	public static final String LIVE_RAW_COMMENT = 			"Live:Raw:Comment";
	
	private static final String[] expand(String str) {
		String[] r= new String[3];
		r[0]=str.replace('1', '1');
		r[1]=str.replace('1', '2');
		r[2]=str.replace('1', '3');
		return r;
	}

	/** Constant <code>STATUS_LOADING_ARCHIVE</code> */
	public static final String[] STATUS_LOADING_ARCHIVE = expand("Status:LoadingArchive:1");

	/** Constant <code>ARCHIVE_X</code> */
	public static final String[] ARCHIVE_X = 					expand("Archive:1:X");
	/** Constant <code>ARCHIVE_Y</code> */
	public static final String[] ARCHIVE_Y = 					expand("Archive:1:Y");
	/** Constant <code>ARCHIVE_SAVE_CSV</code> */
	public static final String[] ARCHIVE_SAVE_CSV = 			expand("Archive:1:SaveCSV");
	/** Constant <code>ARCHIVE_RAW_X</code> */
	public static final String[] ARCHIVE_RAW_X = 				expand("Archive:1:Raw:X");
	/** Constant <code>ARCHIVE_RAW_Y</code> */
	public static final String[] ARCHIVE_RAW_Y = 				expand("Archive:1:Raw:Y");
	/** Constant <code>ARCHIVE_RAW_SAVE</code> */
	public static final String[] ARCHIVE_RAW_SAVE = 			expand("Archive:1:Raw:SaveMem");
	/** Constant <code>ARCHIVE_RAW_SAVE_CSV</code> */
	public static final String[] ARCHIVE_RAW_SAVE_CSV = 		expand("Archive:1:Raw:SaveCSV");
	/** Constant <code>ARCHIVE_REFERENCE_X</code> */
	public static final String[] ARCHIVE_REFERENCE_X = 			expand("Archive:1:Reference:X");
	/** Constant <code>ARCHIVE_REFERENCE_Y</code> */
	public static final String[] ARCHIVE_REFERENCE_Y = 			expand("Archive:1:Reference:Y");
	/** Constant <code>ARCHIVE_REFERENCE_SAVE</code> */
	public static final String[] ARCHIVE_REFERENCE_SAVE = 		expand("Archive:1:Reference:SaveMem");
	/** Constant <code>ARCHIVE_REFERENCE_SAVE_CSV</code> */
	public static final String[] ARCHIVE_REFERENCE_SAVE_CSV = 	expand("Archive:1:Reference:SaveCSV");
	/** Constant <code>ARCHIVE_TIME</code> */
	public static final String[] ARCHIVE_TIME = 				expand("Archive:1:Time");
	/** Constant <code>ARCHIVE_TIME_USE_PICKER</code> */
	public static final String[] ARCHIVE_TIME_USE_PICKER = 		expand("Archive:1:Time:UsePicker");
	/** Constant <code>ARCHIVE_TIME_STRING</code> */
	public static final String[] ARCHIVE_TIME_STRING = 			expand("Archive:1:Time:String");
	/** Constant <code>ARCHIVE_TIME_PICKER</code> */
	public static final String[] ARCHIVE_TIME_PICKER = 			expand("Archive:1:Time:Picker");
	/** Constant <code>ARCHIVE_TIME_PICKER_STRING</code> */
	public static final String[] ARCHIVE_TIME_PICKER_STRING = 	expand("Archive:1:Time:Picker:String");
	/** Constant <code>ARCHIVE_TIME_PICKER_y</code> */
	public static final String[] ARCHIVE_TIME_PICKER_y = 		expand("Archive:1:Time:Picker:y");
	/** Constant <code>ARCHIVE_TIME_PICKER_M</code> */
	public static final String[] ARCHIVE_TIME_PICKER_M = 		expand("Archive:1:Time:Picker:M");
	/** Constant <code>ARCHIVE_TIME_PICKER_d</code> */
	public static final String[] ARCHIVE_TIME_PICKER_d = 		expand("Archive:1:Time:Picker:d");
	/** Constant <code>ARCHIVE_TIME_PICKER_H</code> */
	public static final String[] ARCHIVE_TIME_PICKER_H = 		expand("Archive:1:Time:Picker:H");
	/** Constant <code>ARCHIVE_TIME_PICKER_m</code> */
	public static final String[] ARCHIVE_TIME_PICKER_m = 		expand("Archive:1:Time:Picker:m");
	/** Constant <code>ARCHIVE_TIME_PICKER_s</code> */
	public static final String[] ARCHIVE_TIME_PICKER_s = 		expand("Archive:1:Time:Picker:s");

	/** Constant <code>MEM_X="Mem:X"</code> */
	public static final String MEM_X = 						"Mem:X";
	/** Constant <code>MEM_Y="Mem:Y"</code> */
	public static final String MEM_Y = 						"Mem:Y";
	/** Constant <code>MEM_COMMENT="Mem:Comment"</code> */
	public static final String MEM_COMMENT = 				"Mem:Comment";
	/** Constant <code>MEM_SAVE_CSV="Mem:SaveCSV"</code> */
	public static final String MEM_SAVE_CSV = 				"Mem:SaveCSV";
	
	/** Constant <code>MEM_RAW_X="Mem:Raw:X"</code> */
	public static final String MEM_RAW_X = 					"Mem:Raw:X";
	/** Constant <code>MEM_RAW_Y="Mem:Raw:Y"</code> */
	public static final String MEM_RAW_Y = 					"Mem:Raw:Y";
	/** Constant <code>MEM_RAW_SAVE_CSV="Mem:Raw:SaveCSV"</code> */
	public static final String MEM_RAW_SAVE_CSV = 			"Mem:Raw:SaveCSV";
	/** Constant <code>MEM_RAW_AS_REFERENCE="Mem:Raw:AsReference"</code> */
	public static final String MEM_RAW_AS_REFERENCE= 		"Mem:Raw:AsReference";
	/** Constant <code>MEM_RAW_COMMENT="Mem:Raw:Comment"</code> */
	public static final String MEM_RAW_COMMENT = 			"Mem:Raw:Comment";
	/** Constant <code>MEM_RAW_CLEAR="Mem:Raw:Clear"</code> */
	public static final String MEM_RAW_CLEAR = 				"Mem:Raw:Clear";
	//public static final String MEM_RAW_TIME = 			"Mem:Raw:Time";
	//public static final String MEM_RAW_TIME_STRING = 		"Mem:Raw:Time:String";
	
	/** Constant <code>LOAD_X="Load:X"</code> */
	public static final String LOAD_X = 					"Load:X";
	/** Constant <code>LOAD_Y="Load:Y"</code> */
	public static final String LOAD_Y = 					"Load:Y";
	/** Constant <code>LOAD_LOAD_CSV="Load:LoadCSV"</code> */
	public static final String LOAD_LOAD_CSV = 				"Load:LoadCSV";
	/** Constant <code>LOAD_SAVE_CSV="Load:SaveCSV"</code> */
	public static final String LOAD_SAVE_CSV = 				"Load:SaveCSV";
	/** Constant <code>LOAD_SAVE="Load:SaveMem"</code> */
	public static final String LOAD_SAVE = 					"Load:SaveMem";
	/** Constant <code>LOAD_AS_REFERENCE="Load:AsReference"</code> */
	public static final String LOAD_AS_REFERENCE= 			"Load:AsReference";
	/** Constant <code>LOAD_AS_BBAREF="Load:AsBBARef"</code> */
	public static final String LOAD_AS_BBAREF= 				"Load:AsBBARef";
	/** Constant <code>LOAD_AS_INSREF="Load:AsInsRef"</code> */
	public static final String LOAD_AS_INSREF= 				"Load:AsInsRef";
	/** Constant <code>LOAD_AS_INJREF="Load:AsInjRef"</code> */
	public static final String LOAD_AS_INJREF= 				"Load:AsInjRef";
	/** Constant <code>LOAD_COMMENT="Load:Comment"</code> */
	public static final String LOAD_COMMENT = 				"Load:Comment";
	/** Constant <code>LOAD_FILE="Load:File"</code> */
	public static final String LOAD_FILE = 					"Load:File";

	/** Constant <code>BPM_POSITIONS="BPM:Positions"</code> */
	public static final String BPM_POSITIONS = 				"BPM:Positions";
	/** Constant <code>BPM_NAMES="BPM:Names"</code> */
	public static final String BPM_NAMES = 					"BPM:Names";

	/** Constant <code>STATUS_IS_REFERENCE_BBA="Status:IsReferenceBBA"</code> */
	public static final String STATUS_IS_REFERENCE_BBA = 	"Status:IsReferenceBBA";
	/** Constant <code>STATUS_IS_REFERENCE_INJ="Status:IsReferenceInj"</code> */
	public static final String STATUS_IS_REFERENCE_INJ = 	"Status:IsReferenceInj";
	/** Constant <code>STATUS_IS_REFERENCE_INS="Status:IsReferenceIns"</code> */
	public static final String STATUS_IS_REFERENCE_INS = 	"Status:IsReferenceIns";
	/** Constant <code>STATUS_IS_REFERENCE_BAD="Status:IsReferenceBad"</code> */
	public static final String STATUS_IS_REFERENCE_BAD = 	"Status:IsReferenceBad";

	/**
	 * Time window dT in milliseconds within which (from -dT to + dT ) search for samples will be performed.
	 */
	public static final long SAMPLES_SEARCH_TIME_WINDOW = 5;

	class FileSaver implements Runnable {
		
		String name;
		File file;
		Orbit orbit;
		String comment;
		boolean scheduled=false;
		Throwable t;
		
		public FileSaver(File file, String name) {
			this.file=file;
			this.name=name;
		}
		
		@Override
		public void run() {
			Orbit o=null;
			String c=null;
			
			synchronized (this) {
				o=orbit;
				c=comment;
				scheduled=false;
			}
			
			if (file.exists()) {
				try {
					File file1= new File(file.getAbsolutePath()+".1");
					if (file1.exists()) {
						File file2= new File(file.getAbsolutePath()+".2");
						if (file2.exists()) {
							File file3= new File(file.getAbsolutePath()+".3");
							if (file3.exists()) {
								file3.delete();
							}
							file2.renameTo(file3);
							file2.delete();
						}
						file1.renameTo(file2);
						file1.delete();
					}
					file.renameTo(file1);
					file.delete();
				} catch (Exception e) {
					log4error("File '"+file.getAbsolutePath()+"' rotation failed!", e);
				}
			}
			
			try {
				saveOrbitAsCSV(file, o, name, c);
			} catch (Exception e) {
				if (!e.getClass().equals(t.getClass())) {
					log4error("Failed to save to '"+file+"'", e);
					t=e;
				}
			}
		}
		
		synchronized void commitSave(Orbit orbit, String comment) {
			
			this.orbit= orbit;
			this.comment= comment;

			if (!scheduled) {
				scheduled=true;
				database.schedule(this, 5000);
			}
		}
		
	}
	
	
	private String[] pvX;
	private String[] pvY;
	
	private String archive_url;
	
	private ValueHolder[] updateX; 
	private ValueHolder[] updateY; 
	private ValueHolder[] lastX; 
	private ValueHolder[] lastY;
	private double[] positions;
	private File referenceFile;
	private Orbit reference;
	private boolean hasReference=false;
	private String[] names;
	private Orbit orbit; 
	private Orbit orbitRaw;
	private File memFile;
	private Orbit memRaw;
	private Orbit mem;
	private FileSaver memSaveTask;
	private FileSaver referenceSaveTask; 
	private Set<String> setting= Collections.synchronizedSet(new HashSet<String>(10));
	private Orbit bbaRef;
	private String[] pvBbaX;
	private String[] pvBbaY;
	//private ValueHolder[] lastBbaX;
	//private ValueHolder[] lastBbaY;
	private Orbit insRef;
	private Orbit injRef;
	private Orbit load;

	private Orbit[] archive;
	private Orbit[] archiveRaw;
	private Orbit[] archiveReference;
	private GregorianCalendar[] timePicker;
	private boolean[] archiveDefined;
	private File bbaRefFile;
	private FileSaver bbaRefSaveTask;
//	private int count;
	private File injRefFile;
	private FileSaver injRefSaveTask;
	private File insRefFile;
	private FileSaver insRefSaveTask;
	
	/**
	 * <p>Constructor for OrbitServerApplication.</p>
	 */
	public OrbitServerApplication() {
		
	}
	
	private void configArchive(int i) {
		addRecordOfMemoryValueProcessor(ARCHIVE_X[i], "Archive orbit for selected time, X axis", 0.0, 10.0, "mm", (short) 4, archive[i].getPosH());
		addRecordOfMemoryValueProcessor(ARCHIVE_Y[i], "Archive orbit for selected time, Y axis", 0.0, 10.0, "mm", (short) 4, archive[i].getPosV());
		configStat(ARCHIVE_X[i], "Archive orbit", archive[i]);
		addRecordOfOnDemandProcessor(ARCHIVE_SAVE_CSV[i], "Save archive orbit as CSV file", DBRType.BYTE, 4096);
		addRecordOfMemoryValueProcessor(ARCHIVE_REFERENCE_X[i], "Archive reference orbit for selected time, X axis", 0.0, 10.0, "mm", (short) 4, archiveReference[i].getPosH());
		addRecordOfMemoryValueProcessor(ARCHIVE_REFERENCE_Y[i], "Archive reference orbit for selected time, Y axis", 0.0, 10.0, "mm", (short) 4, archiveReference[i].getPosV());
		configStat(ARCHIVE_REFERENCE_X[i], "Archive reference orbit", archiveReference[i]);
		addRecordOfOnDemandProcessor(ARCHIVE_REFERENCE_SAVE_CSV[i], "Save archive reference orbit as CSV file", DBRType.BYTE, 4096);
		addRecordOfCommandProcessor(ARCHIVE_REFERENCE_SAVE[i], "Save archive reference orbit", 1000);
		addRecordOfMemoryValueProcessor(ARCHIVE_RAW_X[i], "Archive raw orbit for selected time, X axis", 0.0, 10.0, "mm", (short) 4, archiveRaw[i].getPosH());
		addRecordOfMemoryValueProcessor(ARCHIVE_RAW_Y[i], "Archive raw orbit for selected time, Y axis", 0.0, 10.0, "mm", (short) 4, archiveRaw[i].getPosV());
		configStat(ARCHIVE_RAW_X[i], "Archive raw orbit", archiveRaw[i]);
		addRecordOfCommandProcessor(ARCHIVE_RAW_SAVE[i], "Save archive raw orbit", 1000);
		addRecordOfOnDemandProcessor(ARCHIVE_RAW_SAVE_CSV[i], "Save archive raw orbit as CSV file", DBRType.BYTE, 4096);
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME[i], "Date selection", 0, Integer.MAX_VALUE, "s", 0);
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_STRING[i], "Date selection", DBRType.STRING, "");
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER[i], "Date selection widget", 0, Integer.MAX_VALUE, "s", (int)(timePicker[i].getTimeInMillis()/1000));
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_y[i], "Date selection widget", 2012, Integer.MAX_VALUE, "year", timePicker[i].get(Calendar.YEAR));
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_M[i], "Date selection widget", 0, 13, "month", timePicker[i].get(Calendar.MONTH)+1);
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_d[i], "Date selection widget", 0, 32, "day", timePicker[i].get(Calendar.DAY_OF_MONTH));
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_H[i], "Date selection widget", -1, 24, "hour", timePicker[i].get(Calendar.HOUR_OF_DAY));
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_m[i], "Date selection widget", -1, 60, "minute", timePicker[i].get(Calendar.MINUTE));
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_s[i], "Date selection widget", -1, 60, "second", timePicker[i].get(Calendar.SECOND));
		addRecordOfMemoryValueProcessor(ARCHIVE_TIME_PICKER_STRING[i], "Date selection widget", DBRType.STRING, Tools.FORMAT_ISO_DATE_NO_T_TIME.format(timePicker[i]));
		addRecordOfCommandProcessor(ARCHIVE_TIME_USE_PICKER[i], "Fetch from archive beam on picker time", 1000);

		addRecordOfMemoryValueProcessor(STATUS_LOADING_ARCHIVE[i], "Archive orbit load in progress", DBRType.BYTE, 0);
	}
	
	private void configStat(final String record, final String name, final Orbit o) {
		
		if (record.endsWith(":X") || record.endsWith(":Y")) {
			
			String prefix= record.substring(0, record.length()-2);
			
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_X, name+" stats for X axis", -1000.0, 1000.0, "", (short) 3, o.getStatH());
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_X_AVG, name+" AVG stat for X axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatH()[Orbit.Stat._AVG]);
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_X_MAX, name+" MAX stat for X axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatH()[Orbit.Stat._MAX]);
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_X_RMS, name+" RMS stat for X axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatH()[Orbit.Stat._RMS]);
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_X_STD, name+" STD stat for X axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatH()[Orbit.Stat._STD]);

			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_Y, name+" stats for Y axis", -1000.0, 1000.0, "", (short) 3, o.getStatH());
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_Y_AVG, name+" AVG stat for Y axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatV()[Orbit.Stat._AVG]);
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_Y_MAX, name+" MAX stat for Y axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatV()[Orbit.Stat._MAX]);
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_Y_RMS, name+" RMS stat for Y axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatV()[Orbit.Stat._RMS]);
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_Y_STD, name+" STD stat for Y axis", -1000.0, 1000.0, "mm", (short) 3, o.getStatV()[Orbit.Stat._STD]);
		
			addRecordOfMemoryValueProcessor(prefix+SFX_STAT_STRING, name+" stats", DBRType.STRING, o.toStringStatFancy());
		}
	}	
	
	/** {@inheritDoc} */
	@Override
	public void configure(String name, HierarchicalConfiguration config) {
		super.configure(name, config);

		/* URL to JSON Archive Reader server, which fetches archive data
		 * from Cassandra database */
		if (config.getString("archive_url") == null) {
			log4error("'archive_url' tag is missing!");
			throw new IllegalStateException("'archive_url' is missing!");
		}
		archive_url = config.getString("archive_url");
		if (archive_url.endsWith("/") == false) {
			archive_url = archive_url.concat("/");
		}
		log4info("JSON archive URL set to: '" + archive_url + "'");
		
		names= config.getStringArray("bpms");
		
		if (names==null) {
			log4error("'bpms' tag is missing!");
			throw new IllegalStateException("'bpms' tag is missing!");
		}
		
		if (names.length==0) {
			log4error("'bpms' tag is having no BPM names!");
			throw new IllegalStateException("'bpms' tag is having no BPM names!");
		}
		
		//count=names.length;

		String posFile= config.getString("bpm_positions");
		
		positions= loadBPMPositions(posFile);
		
		referenceFile = initPersistancyFile(config.getString("reference_orbit","reference_orbit.csv"));
		referenceSaveTask= new FileSaver(referenceFile,"The Reference Orbit Autosave");
		reference = initPersistantOrbit(referenceFile,"reference orbit");
		hasReference=reference!=null;
		if (reference==null) {
			reference= initDummy();
		}
		
		memFile = initPersistancyFile(config.getString("mem_orbit","mem_orbit.csv"));
		memSaveTask= new FileSaver(memFile,"Mem Orbit Autosave");
		memRaw = initPersistantOrbit(memFile,"mem orbit");
		if (memRaw==null) {
			memRaw= initDummy();
			mem= initDummy();
		} else {
			mem= new Orbit(memRaw,reference);
		}

		bbaRefFile = initPersistancyFile(config.getString("bbaref_orbit","bbaref_orbit.csv"));
		bbaRefSaveTask= new FileSaver(bbaRefFile,"BBA-Ref Orbit Autosave");
		bbaRef = initPersistantOrbit(bbaRefFile,"BBA Ref orbit");
		if (bbaRef==null) {
			bbaRef= initDummy();
		}

		injRefFile = initPersistancyFile(config.getString("injref_orbit","injref_orbit.csv"));
		injRefSaveTask= new FileSaver(injRefFile,"Inj-Ref Orbit Autosave");
		injRef = initPersistantOrbit(injRefFile,"Inj Ref orbit");
		if (injRef==null) {
			injRef= initDummy();
		}

		insRefFile = initPersistancyFile(config.getString("insref_orbit","insref_orbit.csv"));
		insRefSaveTask= new FileSaver(insRefFile,"Ins-Ref Orbit Autosave");
		insRef = initPersistantOrbit(insRefFile,"Ins Ref orbit");
		if (insRef==null) {
			insRef= initDummy();
		}

		addRecordOfMemoryValueProcessor(BPM_NAMES, "List of BPMs in proper order", DBRType.STRING, (Object[])names);
		addRecordOfMemoryValueProcessor(BPM_POSITIONS, "BPMs positions on orbit", 0.0, 110.7, "m", (short)2, positions);
		
		addRecordOfMemoryValueProcessor(REFERENCE_X, "Orbit reference for X axis", 0.0, 10.0, "mm", (short) 4, reference.getPosH());
		addRecordOfMemoryValueProcessor(REFERENCE_Y, "Orbit reference for Y axis", 0.0, 10.0, "mm", (short) 4, reference.getPosV());
		configStat(REFERENCE_X, "Orbit reference", reference);
		addRecordOfMemoryValueProcessor(REFERENCE_COMMENT, "Reference orbit comments", DBRType.STRING, "");
		addRecordOfOnDemandProcessor(REFERENCE_SAVE_CSV, "Save reference as CSV file", DBRType.BYTE, 4096);
		addRecordOfCommandProcessor(REFERENCE_AS_BBAREF, "Use refrence orbit as BBA offset", 1000);
		addRecordOfCommandProcessor(REFERENCE_AS_INJREF, "Use refrence orbit as Injection offset", 1000);
		addRecordOfCommandProcessor(REFERENCE_AS_INSREF, "Use refrence orbit as Insertion offset", 1000);

		getRecord(REFERENCE_COMMENT).setPersistent(true);
		
		addRecordOfMemoryValueProcessor(MEM_X, "Mem orbit on X axis", 0.0, 10.0, "mm", (short) 4, mem.getPositions(O.H));
		addRecordOfMemoryValueProcessor(MEM_Y, "Mem orbit on Y axis", 0.0, 10.0, "mm", (short) 4, mem.getPositions(O.V));
		configStat(MEM_X, "Mem orbit", mem);
		addRecordOfMemoryValueProcessor(MEM_COMMENT, "Mem orbit comments", DBRType.STRING, "");
		addRecordOfOnDemandProcessor(MEM_SAVE_CSV, "Save Mem as CSV file", DBRType.BYTE, 4096);

		getRecord(MEM_COMMENT).setPersistent(true);
		
		addRecordOfMemoryValueProcessor(MEM_RAW_X, "Mem raw on X axis", 0.0, 10.0, "mm", (short) 4, memRaw.getPositions(O.H));
		addRecordOfMemoryValueProcessor(MEM_RAW_Y, "Mem raw on Y axis", 0.0, 10.0, "mm", (short) 4, memRaw.getPositions(O.V));
		configStat(MEM_RAW_X, "Mem raw orbit", memRaw);
		addRecordOfMemoryValueProcessor(MEM_RAW_COMMENT, "Mem raw orbit comments", DBRType.STRING, "");
		addRecordOfCommandProcessor(MEM_RAW_AS_REFERENCE, "Use raw Mem orbit as reference", 1000);
		addRecordOfCommandProcessor(MEM_RAW_CLEAR, "Clears raw Mem orbit.", 1000);
		addRecordOfOnDemandProcessor(MEM_RAW_SAVE_CSV, "Mem raw as CSV file", DBRType.BYTE, 4096);

		getRecord(MEM_RAW_COMMENT).setPersistent(true);
		load= initDummy();

		addRecordOfMemoryValueProcessor(LOAD_X, "Load orbit on X axis", 0.0, 10.0, "mm", (short) 4, load.getPositions(O.H));
		addRecordOfMemoryValueProcessor(LOAD_Y, "Load orbit on Y axis", 0.0, 10.0, "mm", (short) 4, load.getPositions(O.V));
		configStat(LOAD_X, "Load orbit", load);
		addRecordOfMemoryValueProcessor(LOAD_COMMENT, "Load orbit comments", DBRType.STRING, "");
		addRecordOfMemoryValueProcessor(LOAD_FILE, "Load orbit file", DBRType.STRING, "<none>");
		addRecordOfMemoryValueProcessor(LOAD_LOAD_CSV, "Load from CSV file",  new byte[4096]);
		addRecordOfOnDemandProcessor(LOAD_SAVE_CSV, "Save as CSV file", DBRType.BYTE, 4096);
		addRecordOfCommandProcessor(LOAD_AS_REFERENCE, "Use Load orbit as reference", 1000);
		addRecordOfCommandProcessor(LOAD_AS_BBAREF, "Use Load orbit as BBA offset", 1000);
		addRecordOfCommandProcessor(LOAD_AS_INJREF, "Use Load orbit as Injection offset", 1000);
		addRecordOfCommandProcessor(LOAD_AS_INSREF, "Use Load orbit as Insertion offset", 1000);
		addRecordOfCommandProcessor(LOAD_SAVE, "Save Load orbit", 1000);

		//bbaRef= initDummy();
		
		addRecordOfMemoryValueProcessor(BBAREF_X, "BBA offset for X axis", 0.0, 10.0, "mm", (short) 4, bbaRef.getPosH());
		addRecordOfMemoryValueProcessor(BBAREF_Y, "BBA offset for Y axis", 0.0, 10.0, "mm", (short) 4, bbaRef.getPosV());
		configStat(BBAREF_X, "BBA offset", bbaRef);
		addRecordOfMemoryValueProcessor(BBAREF_COMMENT, "BBA offset comments", DBRType.STRING, "");
		addRecordOfCommandProcessor(BBAREF_AS_REFERENCE, "Use BBA offset as reference", 1000);
		addRecordOfOnDemandProcessor(BBAREF_SAVE_CSV, "Save BBA offset as CSV file", DBRType.BYTE, 4096);
		getRecord(BBAREF_COMMENT).setPersistent(true);

		addRecordOfMemoryValueProcessor(INJREF_X, "Injection orbit offset for X axis", 0.0, 10.0, "mm", (short) 4, injRef.getPosH());
		addRecordOfMemoryValueProcessor(INJREF_Y, "Injection orbit offset for Y axis", 0.0, 10.0, "mm", (short) 4, injRef.getPosV());
		configStat(INJREF_X, "Injection orbit reference", injRef);
		addRecordOfMemoryValueProcessor(INJREF_COMMENT, "Injection offset orbit comments", DBRType.STRING, "");
		addRecordOfCommandProcessor(INJREF_AS_REFERENCE, "Use Injection offset orbit as reference", 1000);
		addRecordOfOnDemandProcessor(INJREF_SAVE_CSV, "Save Injection orbit as CSV file", DBRType.BYTE, 4096);
		getRecord(INJREF_COMMENT).setPersistent(true);

		addRecordOfMemoryValueProcessor(INSREF_X, "Inserction orbit offset for X axis", 0.0, 10.0, "mm", (short) 4, insRef.getPosH());
		addRecordOfMemoryValueProcessor(INSREF_Y, "Inserction orbit offset for Y axis", 0.0, 10.0, "mm", (short) 4, insRef.getPosV());
		configStat(INSREF_X, "Inserction orbit offset", insRef);
		addRecordOfMemoryValueProcessor(INSREF_COMMENT, "Inserction offset orbit comments", DBRType.STRING, "");
		addRecordOfCommandProcessor(INSREF_AS_REFERENCE, "Use Inserction offset orbit as reference", 1000);
		addRecordOfOnDemandProcessor(INSREF_SAVE_CSV, "Save Inserction orbit as CSV file", DBRType.BYTE, 4096);
		getRecord(INSREF_COMMENT).setPersistent(true);

		addRecordOfMemoryValueProcessor(REFERENCE_DIFF_BBAREF, "Diff RMS between Orbit reference and BBA offset", 0.0, 100.0, "mm", (short) 3, 0.0);
		addRecordOfMemoryValueProcessor(REFERENCE_DIFF_INJREF, "Diff RMS between Orbit reference and Injection offset", 0.0, 100.0, "mm", (short) 3, 0.0);
		addRecordOfMemoryValueProcessor(REFERENCE_DIFF_INSREF, "Diff RMS between Orbit reference and Insertion offset", 0.0, 100.0, "mm", (short) 3, 0.0);
		addRecordOfMemoryValueProcessor(STATUS_IS_REFERENCE_BAD, "Reference is not same as one of the offsets", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS_IS_REFERENCE_BBA, "Reference is same as BBA offset", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS_IS_REFERENCE_INJ, "Reference is same as Injection offset", DBRType.BYTE, 0);
		addRecordOfMemoryValueProcessor(STATUS_IS_REFERENCE_INS, "Reference is same as Insertion offset", DBRType.BYTE, 0);

		updateDiffs();

		orbit= initDummy();
		
		addRecordOfMemoryValueProcessor(LIVE_X, "Actual orbit on X axis", 0.0, 10.0, "mm", (short) 4, orbit.getPositions(O.H));
		addRecordOfMemoryValueProcessor(LIVE_Y, "Actual orbit on Y axis", 0.0, 10.0, "mm", (short) 4, orbit.getPositions(O.V));
		configStat(LIVE_X, "Actual orbit", orbit);
		addRecordOfMemoryValueProcessor(LIVE_COMMENT, "Actualorbit comments", DBRType.STRING, "");
		
		orbitRaw= initDummy();
		
		addRecordOfMemoryValueProcessor(LIVE_RAW_X, "Actual raw orbit on X axis", 0.0, 10.0, "mm", (short) 4, orbitRaw.getPositions(O.H));
		addRecordOfMemoryValueProcessor(LIVE_RAW_Y, "Actual raw orbit on Y axis", 0.0, 10.0, "mm", (short) 4, orbitRaw.getPositions(O.V));
		configStat(LIVE_RAW_X, "Actual raw orbit", orbitRaw);
		addRecordOfMemoryValueProcessor(LIVE_RAW_COMMENT, "Actualorbit comments", DBRType.STRING, "");
		addRecordOfCommandProcessor(LIVE_RAW_SAVE, "Save actual raw orbit", 1000);
		
		// ARCHIVE
		
		archiveDefined= new boolean[]{false,false,false};
		archive= new Orbit[] {initDummy(),initDummy(),initDummy()};
		archiveReference= new Orbit[] {initDummy(),initDummy(),initDummy()};
		archiveRaw= new Orbit[] {initDummy(),initDummy(),initDummy()};
		timePicker= new GregorianCalendar[]{new GregorianCalendar(), new GregorianCalendar(), new GregorianCalendar()};
		for (GregorianCalendar g : timePicker) {
			g.setLenient(false);
		}

		configArchive(0);
		configArchive(1);
		configArchive(2);

		
		pvX= new String[names.length];
		pvY= new String[names.length];
		pvBbaX= new String[names.length];
		pvBbaY= new String[names.length];
		
		for (int i = 0; i < names.length; i++) {
			pvX[i]= names[i]+":SA:X";
			pvY[i]= names[i]+":SA:Y";
			pvBbaX[i]= names[i].replace("BPM", "OrbitCheckBPM") + ":RefOrbit:X";
			pvBbaY[i]= names[i].replace("BPM", "OrbitCheckBPM") + ":RefOrbit:Y";
			
			addRecord(pvBbaX[i], MemoryValueProcessor.newDoubleProcessor(pvBbaX[i], "BBA offset", 0.0, false).getRecord());
			addRecord(pvBbaY[i], MemoryValueProcessor.newDoubleProcessor(pvBbaY[i], "BBA offset", 0.0, false).getRecord());
		}
		
		lastX= new ValueHolder[names.length];
		lastY= new ValueHolder[names.length];
		//lastBbaX= new ValueHolder[names.length];
		//lastBbaY= new ValueHolder[names.length];
		
		connectLinks(PV_X, pvX);
		connectLinks(PV_Y, pvY);
		//connectLinks(PV_BBA_X, pvBbaX);
		//connectLinks(PV_BBA_Y, pvBbaY);

	}

	private void updateDiffs() {
		double diffbba= diffRMS(reference, bbaRef);
		getRecord(REFERENCE_DIFF_BBAREF).setValue(diffbba);

		double diffinj= diffRMS(reference, injRef);
		getRecord(REFERENCE_DIFF_INJREF).setValue(diffinj);
		
		double diffins= diffRMS(reference, insRef);
		getRecord(REFERENCE_DIFF_INSREF).setValue(diffins);
		
		
		boolean isbba = diffbba<0.0001;
		boolean isinj = diffinj<0.0001;
		boolean isins = diffins<0.0001;
		
		getRecord(STATUS_IS_REFERENCE_BBA).setValue(isbba);
		getRecord(STATUS_IS_REFERENCE_INJ).setValue(isinj);
		getRecord(STATUS_IS_REFERENCE_INS).setValue(isins);

		getRecord(STATUS_IS_REFERENCE_BAD).setValue(!isbba && !isinj && !isins);
	}

	private File initPersistancyFile(String file) {
		
		File f=null;
				
		if (file != null) {
			f= new File(file);
			
			if (f==null || !f.exists() || !f.isFile()) {
				f= BootstrapLoader.getInstance().getApplicationConfigFile(ORBIT_SERVER, file);
			}
		}
		
		if (file != null && !f.exists()) {
			
			File p= f.getParentFile();
			
			if (!p.exists()) {
				try {
					p.mkdirs();
				} catch (Exception e) {
					log4error("Failed to create path '"+p+"'", e);
				}
			}
		}

		return f;
	}

	private Orbit initDummy() {
		double[] dd= new double[names.length];
		return new Orbit(dd, dd);
	}
	
	private Orbit initPersistantOrbit(File file, String name) {
		
		if (file!=null && file.exists() && file.isFile()) {
			
			try {
				Orbit o= loadOrbitAsCSV(file);
				log4info("The "+name+" loaded from '"+file+"'");
				return o;
			} catch (IOException e) {
				log4error("Failed to load "+name+" from '"+file+"'",e);
			}
			
		} else {
			log4error("The "+name+" file does not exist: '"+file+"'");
		}
		
		return null;
	}

	private double[] loadBPMPositions(String posFile) {
		
		
		Properties pos= null;

		if (posFile!=null) {
			File f= new File(posFile);
			if (f.exists() && f.isFile()) {
				pos= new Properties();
				InputStream is=null;
				try {
					is = new BufferedInputStream(new FileInputStream(f));
					pos.load(is);
				} catch (Exception e) {
					log4error("Failed to load BPM positions from '"+posFile+"'",e);
				} finally {
					if (is!=null) {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				try {
					pos= BootstrapLoader.getInstance().getApplicationConfigProperties(ORBIT_SERVER, posFile);
				} catch (Exception e) {
					log4error("Failed to load BPM positions from '"+posFile+"'",e);
				}
			}
		}
		
		double[] dd= new double[names.length];

		if (pos!=null && pos.keySet().containsAll(Arrays.asList(names))) {
			for (int i = 0; i < names.length; i++) {
				dd[i] = Double.parseDouble(pos.getProperty(names[i], String.valueOf(i+1)));
			}
			log4info("BPM positions loaded from '"+posFile+"'.");
		} else {
			for (int i = 0; i < names.length; i++) {
				dd[i] = i+1;
			}
			log4info("BPM positions not found, using index instead.");
		}
		
		return dd;
	}

	/**
	 * <p>loadOrbitAsCSV.</p>
	 *
	 * @param csv a {@link java.lang.String} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @throws java.io.IOException if any.
	 */
	protected Orbit loadOrbitAsCSV(String csv) throws IOException {
		Reader r=null;
		try {
			r = new StringReader(csv); 
			
			return loadOrbitAsCSV(r);
			
		} finally {
			if (r!=null) {
				try {
					r.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * <p>loadOrbitAsCSV.</p>
	 *
	 * @param orbitFile a {@link java.io.File} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @throws java.io.IOException if any.
	 */
	protected Orbit loadOrbitAsCSV(File orbitFile) throws IOException {
		Reader r=null;
		try {
			r = new BufferedReader(new FileReader(orbitFile)); 
			
			return loadOrbitAsCSV(r);
			
		} finally {
			if (r!=null) {
				try {
					r.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * <p>loadOrbitAsCSV.</p>
	 *
	 * @param csv a {@link java.io.Reader} object
	 * @return a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @throws java.io.IOException if any.
	 */
	protected Orbit loadOrbitAsCSV(Reader csv) throws IOException {
		Reader r=null;
		try {
			r = new Reader(csv){
				@Override
				public void close() throws IOException {
					((Reader)lock).close();
				}
				@Override
				public int read(char[] cbuf, int off, int len)
						throws IOException {
					int r= ((Reader)lock).read(cbuf, off, len);
					for (int i = 0; i < cbuf.length; i++) {
						if (cbuf[i]=='/') {
							cbuf[i]='#';
						}
					}
					return r;
				}
			};
			
			CSVFormat form = CSVFormat.TDF.builder()
					.setCommentMarker('#')
					.setIgnoreEmptyLines(true)
					.setIgnoreSurroundingSpaces(true)
					.setSkipHeaderRecord(true).build();
			
			CSVParser par= form.parse(r);
			
			Map<String, CSVRecord> ref= new HashMap<String, CSVRecord>(names.length);
			
			Iterator<CSVRecord> it= par.iterator();
			
			while(it.hasNext()) {
				CSVRecord rec= it.next();
				String s= rec.get(0);
				if (s.endsWith(":SA")) {
					s=s.substring(0, s.length()-3);
				}
				ref.put(s, rec);
			}
			
			double[] refX= new double[names.length];
			double[] refY= new double[names.length];
			
			for (int i = 0; i < names.length; i++) {
				CSVRecord rec= ref.get(names[i]);
				if (rec==null) {
					log4error("Reference orbit is missing for BPM '"+names[i]+"'.");
				} else {
					refX[i]= Double.parseDouble(rec.get(1));
					refY[i]= Double.parseDouble(rec.get(2));
				}
			}
			
			return new Orbit(refX,refY);
			
		} finally {
			if (r!=null) {
				try {
					r.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * <p>saveOrbitAsCSV.</p>
	 *
	 * @param orbitFile a {@link java.io.File} object
	 * @param orbit a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @param name a {@link java.lang.String} object
	 * @param comments a {@link java.lang.String} object
	 * @throws java.io.IOException if any.
	 */
	protected void saveOrbitAsCSV(File orbitFile, Orbit orbit, String name, String... comments) throws IOException {
		Writer r=null;
		try {
			r = new BufferedWriter(new FileWriter(orbitFile));
			
			saveOrbitAsCSV(r, orbit, name, comments);
			
		} finally {
			if (r!=null) {
				try {
					r.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * <p>saveOrbitAsCSV.</p>
	 *
	 * @param print a {@link java.lang.Appendable} object
	 * @param orbit a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @param name a {@link java.lang.String} object
	 * @param comments a {@link java.lang.String} object
	 * @throws java.io.IOException if any.
	 */
	protected void saveOrbitAsCSV(Appendable print, Orbit orbit, String name, String... comments) throws IOException {
		
		int lines= comments!=null ? comments.length+1 : 1; 
		
		Object[] com= new Object[lines];
		
		com[0] = name+", created by OrbitServer on "+new Date().toString();
		for (int i = 1; i < com.length; i++) {
			com[i]=comments[i-1];
		}
		
		CSVFormat form = CSVFormat.TDF.builder()
				.setCommentMarker('#')
				.setIgnoreEmptyLines(true)
				.setIgnoreSurroundingSpaces(true)
				.setSkipHeaderRecord(true)
				.setHeader("Name","Horizontal","Vertical")
				.setHeaderComments(com)
				.setRecordSeparator("\r\n")
				.build();
		
		CSVPrinter p= form.print(print);
		
		String[] n= names;
		double[] h= orbit.getPosH();
		double[] v= orbit.getPosV();
		
		for (int i = 0; i < n.length; i++) {
			p.printRecord(n[i],h[i],v[i]);
		}
		
		p.close();
	}
	
	/**
	 * <p>saveOrbitAsCSV.</p>
	 *
	 * @param orbit a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @param name a {@link java.lang.String} object
	 * @param comments a {@link java.lang.String} object
	 * @return a {@link java.lang.String} object
	 */
	protected String saveOrbitAsCSV(Orbit orbit, String name, String... comments) {
		StringWriter sw= new StringWriter();
		try {
			saveOrbitAsCSV(sw, orbit, name, comments);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}
	
	/**
	 * <p>diffRMS.</p>
	 *
	 * @param a a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @param b a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @return a double
	 */
	protected double diffRMS(Orbit a, Orbit b) {
		double[] ax= a.getPosH();
		double[] ay= a.getPosV();
		double[] bx= b.getPosH();
		double[] by= b.getPosV();
		
		double rms= 0.0;
		
		for (int i = 0; i < by.length; i++) {
			double d= ax[i]-bx[i];
			rms+= (d*d);
			d= ay[i]-by[i];
			rms+= (d*d);
		}
		
		return Math.sqrt(rms/2.0/ax.length);
	}

	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyLinkChange(String name) {
		super.notifyLinkChange(name);
		
		if (name == PV_X) {
			ValueLinks vl= getLinks(PV_X);
			if (vl.isInvalid()) {
				getRecord(LIVE_X).updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM, true);
				getRecord(LIVE_RAW_X).updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM, true);
			} else if (vl.isReady()) {
				if (vl.getLastSeverity().isGreaterThan(Severity.NO_ALARM)) {
					getRecord(LIVE_X).updateAlarm(vl.getLastSeverity(), vl.getLastStatus());
					getRecord(LIVE_RAW_X).updateAlarm(vl.getLastSeverity(), vl.getLastStatus());
				} else {
					boolean change= true;
					updateX= vl.consume();
					for (int i = 0; i < lastX.length; i++) {
						if (lastX[i]==updateX[i]) {
							change=false;
							break;
						}
					}
					if (change) {
						lastX=updateX;
						double[] a= new double[lastX.length];
						for (int i = 0; i < a.length; i++) {
							a[i]=lastX[i].doubleValue();
						}
						//System.out.println("UPDATE "+System.currentTimeMillis());
						orbitRaw= updateOrbit(a, orbitRaw.getPosV(),null,LIVE_RAW_X,null,false); 
						//System.out.println("A "+orbitRaw.getPosH()[0]+" "+orbitRaw.getPosV()[0]);
						orbit= updateOrbit(orbitRaw.getPosH(),orbitRaw.getPosV(),reference,LIVE_X,null,hasReference);
						getRecord(LIVE_X).updateAlarm(Severity.NO_ALARM,Status.NO_ALARM);
						getRecord(LIVE_RAW_X).updateAlarm(Severity.NO_ALARM,Status.NO_ALARM);
					} else {
						//System.out.println("CHANGE "+System.currentTimeMillis());
					}
				}
			}
		} else if (name == PV_Y) {
			ValueLinks vl= getLinks(PV_Y);
			if (vl.isInvalid()) {
				getRecord(LIVE_Y).updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM, true);
				getRecord(LIVE_RAW_Y).updateAlarm(Severity.INVALID_ALARM, Status.LINK_ALARM, true);
			} else if (vl.isReady()) {
				if (vl.getLastSeverity().isGreaterThan(Severity.NO_ALARM)) {
					getRecord(LIVE_Y).updateAlarm(vl.getLastSeverity(), vl.getLastStatus());
					getRecord(LIVE_RAW_Y).updateAlarm(vl.getLastSeverity(), vl.getLastStatus());
				} else {
					boolean change= true;
					updateY= vl.consume();
					for (int i = 0; i < lastY.length; i++) {
						if (lastY[i]==updateY[i]) {
							change=false;
							break;
						}
					}
					if (change) {
						lastY=updateY;
						double[] a= new double[lastY.length];
						for (int i = 0; i < a.length; i++) {
							a[i]=lastY[i].doubleValue();
						}
						//System.out.println("UPDATE "+System.currentTimeMillis());
						orbitRaw= updateOrbit(orbitRaw.getPosH(),a,null,null,LIVE_RAW_Y,false); 
						//System.out.println("B "+orbitRaw.getPosH()[0]+" "+orbitRaw.getPosV()[0]);
						orbit= updateOrbit(orbitRaw.getPosH(),orbitRaw.getPosV(),reference,null,LIVE_Y,hasReference);
						getRecord(LIVE_Y).updateAlarm(Severity.NO_ALARM,Status.NO_ALARM);
						getRecord(LIVE_RAW_Y).updateAlarm(Severity.NO_ALARM,Status.NO_ALARM);
					} else {
						//System.out.println("CHANGE "+System.currentTimeMillis());
					}
				}
			} else {
				//String [] s= vl.getNotConnected();
				//System.out.println("Not connected: "+Arrays.toString(s));
			}
		/*} else if (name == PV_BBA_X) {
			ValueLinks vl= getLinks(PV_BBA_X);
			if (vl.isReady()) {
				if (vl.getLastSeverity().isGreaterThan(Severity.NO_ALARM)) {
					getRecord(BBAREF_X).updateAlarm(vl.getLastSeverity(), vl.getLastStatus());
				} else {
					lastBbaX= vl.consume();
					double[] a= new double[lastBbaX.length];
					for (int i = 0; i < a.length; i++) {
						a[i]=lastBbaX[i].doubleValue();
					}
					bbaRef= updateOrbit(a,bbaRef.getPosV(),null,BBAREF_X,null,BBAREF_STAT_X,null,BBAREF_STAT_STRING,false);
					double diff= diffRMS(reference, bbaRef);
					getRecord(REFERENCE_DIFF_BBAREF).setValue(diff);
					getRecord(STATUS_IS_REFERENCE_BAD).setValue(diff!=0.0);
					getRecord(BBAREF_X).updateAlarm(Severity.NO_ALARM,Status.NO_ALARM);
				}
			}
		} else if (name == PV_BBA_Y) {
			ValueLinks vl= getLinks(PV_BBA_Y);
			if (vl.isReady()) {
				if (vl.getLastSeverity().isGreaterThan(Severity.NO_ALARM)) {
					getRecord(BBAREF_Y).updateAlarm(vl.getLastSeverity(), vl.getLastStatus());
				} else {
					lastBbaY= vl.consume();
					double[] a= new double[lastBbaY.length];
					for (int i = 0; i < a.length; i++) {
						a[i]=lastBbaY[i].doubleValue();
					}
					bbaRef= updateOrbit(bbaRef.getPosH(),a,null,null,BBAREF_Y,null,BBAREF_STAT_Y,BBAREF_STAT_STRING,false);
					double diff= diffRMS(reference, bbaRef);
					getRecord(REFERENCE_DIFF_BBAREF).setValue(diff);
					getRecord(STATUS_IS_REFERENCE_BAD).setValue(diff!=0.0);
					getRecord(BBAREF_Y).updateAlarm(Severity.NO_ALARM,Status.NO_ALARM);
				}
			}*/
		}

	}
	
	
	private void notifyArchiveChange(final String name, final int i) {
		if (name == ARCHIVE_TIME_PICKER[i]) {
			long time= getRecord(ARCHIVE_TIME_PICKER[i]).getValueAsInt()*1000L;
			if (timePicker[i].getTimeInMillis() != time) {
				timePicker[i].setTimeInMillis(time);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME_PICKER_y[i]) {
			int val = getRecord(name).getValueAsInt();
			int old= timePicker[i].get(Calendar.YEAR);
			if (old != val) {
				timePicker[i].add(Calendar.YEAR, val-old);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME_PICKER_M[i]) {
			int val = getRecord(name).getValueAsInt()-1;
			int old= timePicker[i].get(Calendar.MONTH);
			if (old != val) {
				timePicker[i].add(Calendar.MONTH, val-old);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME_PICKER_d[i]) {
			int val = getRecord(name).getValueAsInt();
			int old= timePicker[i].get(Calendar.DAY_OF_MONTH);
			if (old != val) {
				timePicker[i].add(Calendar.DAY_OF_MONTH, val-old);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME_PICKER_H[i]) {
			int val = getRecord(name).getValueAsInt();
			int old= timePicker[i].get(Calendar.HOUR_OF_DAY);
			if (old != val) {
				timePicker[i].add(Calendar.HOUR_OF_DAY, val-old);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME_PICKER_m[i]) {
			int val = getRecord(name).getValueAsInt();
			int old= timePicker[i].get(Calendar.MINUTE);
			if (old != val) {
				timePicker[i].add(Calendar.MINUTE, val-old);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME_PICKER_s[i]) {
			int val = getRecord(name).getValueAsInt();
			int old= timePicker[i].get(Calendar.SECOND);
			if (old != val) {
				timePicker[i].add(Calendar.SECOND, val-old);
				propagatePicker(i);
			}
		} else if (name == ARCHIVE_TIME[i]) {
			long time= getRecord(name).getValueAsInt()*1000L;
			getRecord(ARCHIVE_TIME_STRING[i]).setValue(Tools.FORMAT_ISO_DATE_NO_T_TIME.format(time));
			
			getRecord(STATUS_LOADING_ARCHIVE[i]).setValue(1);
			database.getExecutor().execute(new Runnable() {
				

				@Override
				public void run() {
					// make query to Casandra with this date and fill orbit
					
					try {
						// Get X and Y orbit data
						Date targetDate = new Date(getRecord(name).getValueAsInt()*1000L);
						log4info("Start fetching orbit data for timestamp: " + getRecord(name).getValueAsInt()+" ("+targetDate+").");
						
						double[] orbitx = ArchiveClient.getTimeInstant(pvX,targetDate.getTime(),SAMPLES_SEARCH_TIME_WINDOW,archive_url);
						double[] orbity = ArchiveClient.getTimeInstant(pvY,targetDate.getTime(),SAMPLES_SEARCH_TIME_WINDOW,archive_url);
	
						double[] refx = ArchiveClient.getTimeInstant(pvBbaX,targetDate.getTime(),SAMPLES_SEARCH_TIME_WINDOW,archive_url);
						double[] refy = ArchiveClient.getTimeInstant(pvBbaY,targetDate.getTime(),SAMPLES_SEARCH_TIME_WINDOW,archive_url);

						getRecord(STATUS_LOADING_ARCHIVE[i]).setValue(0);
	
						if (orbitx!=null && orbity!=null) {
							log4info("Done fetching orbit data");
							
							archiveDefined[i]=true;
							
							if (refx!=null && refy!=null) {
								archiveReference[i]= updateOrbit(refx, refy, null, ARCHIVE_REFERENCE_X[i], ARCHIVE_REFERENCE_Y[i], false);
							} else {
								log4error("Failed fetching archive reference orbit data, dummy data used!");
								archiveReference[i]= initDummy();
							}
							archiveRaw[i]= updateOrbit(orbitx, orbity, null, ARCHIVE_RAW_X[i], ARCHIVE_RAW_Y[i], false);
							archive[i]= updateOrbit(orbitx, orbity, archiveReference[i], ARCHIVE_X[i], ARCHIVE_Y[i], true);
							resetOnDemandProcessor(ARCHIVE_RAW_SAVE_CSV[i]);
							resetOnDemandProcessor(ARCHIVE_REFERENCE_SAVE_CSV[i]);
							resetOnDemandProcessor(ARCHIVE_SAVE_CSV[i]);
						} else {
							log4error("Failed fetching orbit data from '"+archive_url+"'");
						}
					} catch (Throwable t) {
						log4error("Failed fetching orbit data from '"+archive_url+"'",t);
					}
				}
			});
		}
	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordChange(String name, boolean alarmOnly) {
		super.notifyRecordChange(name, alarmOnly);

		if (name == REFERENCE_X || name == REFERENCE_Y) {
			if (!setting.contains(REFERENCE_X) && !setting.contains(REFERENCE_Y) 
					&& (getRecord(REFERENCE_X).getValue()!=reference.getPosH() 
					|| getRecord(REFERENCE_Y).getValue()!=reference.getPosV())) {
				reference= updateOrbit(getRecord(REFERENCE_X).getValueAsDoubleArray(), getRecord(REFERENCE_Y).getValueAsDoubleArray(), null, REFERENCE_X, REFERENCE_Y, false);
				referenceSaveTask.commitSave(reference,getRecord(REFERENCE_COMMENT).getValueAsString());
				updateDiffs();
				orbit= updateOrbit(orbitRaw.getPosH(),orbitRaw.getPosV(),reference,LIVE_X,LIVE_Y,hasReference);
				mem= updateOrbit(memRaw.getPosH(), memRaw.getPosV(), reference, MEM_X, MEM_Y, hasReference);
				//if (archiveDefined) archive= updateOrbit(archiveRaw.getPosH(), archiveRaw.getPosV(), reference, ARCHIVE_X, ARCHIVE_Y, ARCHIVE_STAT_X, ARCHIVE_STAT_Y, ARCHIVE_STAT_STRING, hasReference);
				resetOnDemandProcessor(REFERENCE_SAVE_CSV);
			}
		} else if (name == BBAREF_X || name == BBAREF_Y) {
			if (!setting.contains(BBAREF_X) && !setting.contains(BBAREF_Y) 
					&& (getRecord(BBAREF_X).getValue()!=bbaRef.getPosH() 
					|| getRecord(BBAREF_Y).getValue()!=bbaRef.getPosV())) {
				bbaRef= updateOrbit(getRecord(BBAREF_X).getValueAsDoubleArray(), getRecord(BBAREF_Y).getValueAsDoubleArray(), null, BBAREF_X, BBAREF_Y, false);
				bbaRefSaveTask.commitSave(bbaRef,getRecord(BBAREF_COMMENT).getValueAsString());
				updateDiffs();
				resetOnDemandProcessor(BBAREF_SAVE_CSV);
				for (int i = 0; i < pvBbaX.length; i++) {
					Record r= getRecord(pvBbaX[i]);
					r.setValue(bbaRef.getPosH()[i]);
					r= getRecord(pvBbaY[i]);
					r.setValue(bbaRef.getPosV()[i]);
				}
			}
		} else if (name == INJREF_X || name == INJREF_Y) {
			if (!setting.contains(INJREF_X) && !setting.contains(INJREF_Y) 
					&& (getRecord(INJREF_X).getValue()!=injRef.getPosH() 
					|| getRecord(INJREF_Y).getValue()!=injRef.getPosV())) {
				injRef= updateOrbit(getRecord(INJREF_X).getValueAsDoubleArray(), getRecord(INJREF_Y).getValueAsDoubleArray(), null, INJREF_X, INJREF_Y, false);
				injRefSaveTask.commitSave(injRef,getRecord(INJREF_COMMENT).getValueAsString());
				updateDiffs();
				resetOnDemandProcessor(INJREF_SAVE_CSV);
			}
		} else if (name == INSREF_X || name == INSREF_Y) {
			if (!setting.contains(INSREF_X) && !setting.contains(INSREF_Y) 
					&& (getRecord(INSREF_X).getValue()!=insRef.getPosH() 
					|| getRecord(INSREF_Y).getValue()!=insRef.getPosV())) {
				insRef= updateOrbit(getRecord(INSREF_X).getValueAsDoubleArray(), getRecord(INSREF_Y).getValueAsDoubleArray(), null, INSREF_X, INSREF_Y, false);
				insRefSaveTask.commitSave(insRef,getRecord(INSREF_COMMENT).getValueAsString());
				updateDiffs();
				resetOnDemandProcessor(INSREF_SAVE_CSV);
			}
		} else if (name == MEM_RAW_X || name == MEM_RAW_Y) {
			if (!setting.contains(MEM_RAW_X) && !setting.contains(MEM_RAW_Y) 
					&& (getRecord(MEM_RAW_X).getValue()!=memRaw.getPosH() 
					|| getRecord(MEM_RAW_Y).getValue()!=memRaw.getPosV())) {
				memRaw= updateOrbit(getRecord(MEM_RAW_X).getValueAsDoubleArray(), getRecord(MEM_RAW_Y).getValueAsDoubleArray(), null, MEM_RAW_X, MEM_RAW_Y, false);
				mem= updateOrbit(memRaw.getPosH(), memRaw.getPosV(), reference, MEM_X, MEM_Y, hasReference);
				memSaveTask.commitSave(memRaw,getRecord(MEM_RAW_COMMENT).getValueAsString());
				resetOnDemandProcessor(MEM_RAW_SAVE_CSV);
				resetOnDemandProcessor(MEM_SAVE_CSV);
			}
		} else {
			notifyArchiveChange(name, 0);
			notifyArchiveChange(name, 1);
			notifyArchiveChange(name, 2);
		}
	}
	
	private void propagatePicker(int i) {
		if (timePicker[i].getTimeInMillis() != getRecord(ARCHIVE_TIME_PICKER[i]).getValueAsInt()*1000L) {
			getRecord(ARCHIVE_TIME_PICKER[i]).setValue((int)(timePicker[i].getTimeInMillis()/1000));
		}
			
		getRecord(ARCHIVE_TIME_PICKER_STRING[i]).setValue(Tools.FORMAT_ISO_DATE_NO_T_TIME.format(timePicker[i].getTimeInMillis()));

		updateRecord(ARCHIVE_TIME_PICKER_y[i],Calendar.YEAR,0,i);
		updateRecord(ARCHIVE_TIME_PICKER_M[i],Calendar.MONTH,-1,i);
		updateRecord(ARCHIVE_TIME_PICKER_d[i],Calendar.DAY_OF_MONTH,0,i);
		updateRecord(ARCHIVE_TIME_PICKER_H[i],Calendar.HOUR_OF_DAY,0,i);
		updateRecord(ARCHIVE_TIME_PICKER_m[i],Calendar.MINUTE,0,i);
		updateRecord(ARCHIVE_TIME_PICKER_s[i],Calendar.SECOND,0,i);
	}
		
	private void updateRecord(String name, int field, int offset, int i) {
		Record r= getRecord(name);
		int old= r.getValueAsInt()+offset;
		int val= timePicker[i].get(field);
		if (old!=val) {
			r.setValue(val-offset);
		}
	}
	
	private boolean notifyArchiveWrite(final String name, final int i) {
		if (name == ARCHIVE_TIME_USE_PICKER[i]) {
			getRecord(ARCHIVE_TIME[i]).setValue(getRecord(ARCHIVE_TIME_PICKER[i]).getValue());
			return true;
		} else if (name == ARCHIVE_TIME_PICKER_STRING[i]) {
			String time= getRecord(ARCHIVE_TIME_PICKER_STRING[i]).getValueAsString();
					
			try {
				Object o= Tools.PARSE_ISO_DATE_NO_T_TIME.parseObject(time);
				
				if (o!=null && o instanceof Date) {
					getRecord(ARCHIVE_TIME_PICKER[i]).setValue(((Date)o).getTime()/1000);
				} else {
					getRecord(ARCHIVE_TIME_PICKER_STRING[i]).setValue(Tools.FORMAT_ISO_DATE_NO_T_TIME.format(timePicker[i].getTimeInMillis()));
				}
				
			} catch (Exception e) {
				log4error("Parsing timestamp failed.", e);
				getRecord(ARCHIVE_TIME_PICKER_STRING[i]).setValue(Tools.FORMAT_ISO_DATE_NO_T_TIME.format(timePicker[i].getTimeInMillis()));
			}
			return true;
		} else if (name == ARCHIVE_RAW_SAVE[i]) {
			saveOrbit(archiveRaw[i],"archive orbit saved");
			return true;
		} else if (name == ARCHIVE_REFERENCE_SAVE[i]) {
			saveOrbit(archiveReference[i],"archive reference orbit saved");
			return true;
		}
		return false;

	}
	
	/** {@inheritDoc} */
	@Override
	protected synchronized void notifyRecordWrite(String name) {
		super.notifyRecordWrite(name);
		
		if (name == LIVE_RAW_SAVE) {
			saveOrbit(orbitRaw,"live orbit saved");
		} else if (name == LOAD_SAVE) {
			saveOrbit(load,"load orbit saved");
		} else if (name == MEM_RAW_CLEAR) {
			saveOrbit(reference,"Mem cleared");
		} else if (name == MEM_RAW_AS_REFERENCE) {
			asReferenceOrbit(memRaw,"Mem orbit set as reference");
		} else if (name == BBAREF_AS_REFERENCE) {
			asReferenceOrbit(bbaRef,"BBA offset set as reference");
		} else if (name == INJREF_AS_REFERENCE) {
			asReferenceOrbit(injRef,"Injection offset set as reference");
		} else if (name == INSREF_AS_REFERENCE) {
			asReferenceOrbit(insRef,"Insertion offset set as reference");
		} else if (name == REFERENCE_AS_BBAREF) {
			asBBARefOrbit(reference,"reference set as BBA offset");
		} else if (name == REFERENCE_AS_INJREF) {
			asInjRefOrbit(reference,"reference set as Inj offset");
		} else if (name == REFERENCE_AS_INSREF) {
			asInsRefOrbit(reference,"reference set as Ins offset");
		} else if (name == LOAD_AS_REFERENCE) {
			asReferenceOrbit(load,new StringBuilder(128).append("load orbit set as reference (").append(getRecord(LOAD_FILE).getValueAsString()).append(')').toString());
		} else if (name == LOAD_AS_BBAREF) {
			asBBARefOrbit(load,new StringBuilder(128).append("load orbit set as BBA offset (").append(getRecord(LOAD_FILE).getValueAsString()).append(')').toString());
		} else if (name == LOAD_AS_INJREF) {
			asInjRefOrbit(load,new StringBuilder(128).append("load orbit set as Inj offset (").append(getRecord(LOAD_FILE).getValueAsString()).append(')').toString());
		} else if (name == LOAD_AS_INSREF) {
			asInsRefOrbit(load,new StringBuilder(128).append("load orbit set as Ins offset (").append(getRecord(LOAD_FILE).getValueAsString()).append(')').toString());
		} else if (name == LOAD_LOAD_CSV) {
			try {
				//System.out.println(getRecord(MEM_RAW_LOAD_CSV).getValueAsString());
				load= loadOrbitAsCSV(getRecord(LOAD_LOAD_CSV).getValueAsString());
				updateOrbit(load.getPosH(),load.getPosV(),null,LOAD_X,LOAD_Y,false);
				String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] load file (").append(getRecord(LOAD_FILE).getValueAsString()).append(").").toString();
				getRecord(LOAD_COMMENT).setValue(s);
			} catch (Exception e) {
				log4error("Loading file failed", e);
				load= initDummy();
				updateOrbit(load.getPosH(),load.getPosV(),null,LOAD_X,LOAD_Y,false);
				String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] failed loading file (").append(getRecord(LOAD_FILE).getValueAsString()).append(").").toString();
				getRecord(LOAD_COMMENT).setValue(s);
			}
			resetOnDemandProcessor(LOAD_SAVE_CSV);
		} else if (name.endsWith("RefOrbit:X")) {
			int idx=-1;
			for (int i = 0; i < pvBbaX.length; i++) {
				if (pvBbaX[i]==name) {
					idx=i;
					break;
				}
			}
			if (idx>-1) {
				double[] d= Arrays.copyOf(bbaRef.getPosH(),bbaRef.getPosH().length);
				d[idx]=getRecord(name).getValueAsDouble();
				getRecord(BBAREF_X).setValue(d);
			}
		} else if (name.endsWith("RefOrbit:Y")) { 
			int idx=-1;
			for (int i = 0; i < pvBbaY.length; i++) {
				if (pvBbaY[i]==name) {
					idx=i;
					break;
				}
			}
			if (idx>-1) {
				double[] d= Arrays.copyOf(bbaRef.getPosV(),bbaRef.getPosV().length);
				d[idx]=getRecord(name).getValueAsDouble();
				getRecord(BBAREF_Y).setValue(d);
			}
		} else {
			if (notifyArchiveWrite(name, 0)) return;
			if (notifyArchiveWrite(name, 1)) return;
			if (notifyArchiveWrite(name, 2)) return;
		}
		
	}

	private void asReferenceOrbit(Orbit o, String comment) {
		String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] ").append(comment).append('.').toString();
		getRecord(REFERENCE_COMMENT).setValue(s);
		getRecord(REFERENCE_X).setValue(o.getPosH());
		getRecord(REFERENCE_Y).setValue(o.getPosV());
		resetOnDemandProcessor(REFERENCE_SAVE_CSV);
	}
	private void asBBARefOrbit(Orbit o, String comment) {
		String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] ").append(comment).append('.').toString();
		getRecord(BBAREF_COMMENT).setValue(s);
		getRecord(BBAREF_X).setValue(o.getPosH());
		getRecord(BBAREF_Y).setValue(o.getPosV());
		resetOnDemandProcessor(BBAREF_SAVE_CSV);
	}
	private void asInjRefOrbit(Orbit o, String comment) {
		String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] ").append(comment).append('.').toString();
		getRecord(INJREF_COMMENT).setValue(s);
		getRecord(INJREF_X).setValue(o.getPosH());
		getRecord(INJREF_Y).setValue(o.getPosV());
		resetOnDemandProcessor(INJREF_SAVE_CSV);
	}
	private void asInsRefOrbit(Orbit o, String comment) {
		String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] ").append(comment).append('.').toString();
		getRecord(INSREF_COMMENT).setValue(s);
		getRecord(INSREF_X).setValue(o.getPosH());
		getRecord(INSREF_Y).setValue(o.getPosV());
		resetOnDemandProcessor(INSREF_SAVE_CSV);
	}
	private void saveOrbit(Orbit o, String comment) {
		String s = new StringBuilder(128).append('[').append(Tools.nowIsoDateNoTTime()).append("] ").append(comment).append('.').toString();
		getRecord(MEM_COMMENT).setValue(s);
		getRecord(MEM_RAW_COMMENT).setValue(s);
		getRecord(MEM_RAW_X).setValue(o.getPosH());
		getRecord(MEM_RAW_Y).setValue(o.getPosV());
		resetOnDemandProcessor(MEM_RAW_SAVE_CSV);
		resetOnDemandProcessor(MEM_SAVE_CSV);
	}
	
	/** {@inheritDoc} */
	@Override
	public Object getValue(Object key) {
		
		for (int i = 0; i < 3; i++) {
			if (key==ARCHIVE_RAW_SAVE_CSV[i]) {
				String s= saveOrbitAsCSV(archiveRaw[i],"Archive Raw Orbit","At timestamp "+getRecord(ARCHIVE_TIME_STRING[i]).getValueAsString());
				return s;
			}
			if (key==ARCHIVE_REFERENCE_SAVE_CSV[i]) {
				String s= saveOrbitAsCSV(archiveReference[i],"Archive Reference Orbit","At timestamp "+getRecord(ARCHIVE_TIME_STRING[i]).getValueAsString());
				return s;
			}
			if (key==ARCHIVE_SAVE_CSV[i]) {
				String s= saveOrbitAsCSV(archive[i],"Archive Relative Orbit","At timestamp "+getRecord(ARCHIVE_TIME_STRING[i]).getValueAsString());
				return s;
			}
		}
		if (key==BBAREF_SAVE_CSV) {
			String s= saveOrbitAsCSV(bbaRef,"BBA Offset Refrence Orbit");
			return s;
		}
		if (key==INJREF_SAVE_CSV) {
			String s= saveOrbitAsCSV(injRef,"Injection Offset Refrence Orbit");
			return s;
		}
		if (key==INSREF_SAVE_CSV) {
			String s= saveOrbitAsCSV(insRef,"Insertion Offset Refrence Orbit");
			return s;
		}
		if (key==REFERENCE_SAVE_CSV) {
			String s= saveOrbitAsCSV(reference,"The Reference Orbit",getRecord(REFERENCE_COMMENT).getValueAsString());
			return s;
		}
		if (key==LOAD_SAVE_CSV) {
			String s= saveOrbitAsCSV(load,"Load Orbit",getRecord(LOAD_COMMENT).getValueAsString());
			return s;
		}
		if (key==MEM_RAW_SAVE_CSV) {
			String s= saveOrbitAsCSV(memRaw,"Mem Raw Orbit",getRecord(MEM_RAW_COMMENT).getValueAsString());
			return s;
		}
		if (key==MEM_SAVE_CSV) {
			String s= saveOrbitAsCSV(mem,"Mem Orbit",getRecord(MEM_COMMENT).getValueAsString());
			return s;
		}


		return super.getValue(key);
	}
	
	/** {@inheritDoc} */
	@Override
	public void activate() {
		super.activate();
		/*database.schedule(new Runnable() {
			@Override
			public void run() {
				Record rx= getRecord(LIVE_X);
				Record ry= getRecord(LIVE_Y);
				if (System.currentTimeMillis()-EPICSUtilities.toUTC(rx.getTimestamp())>3000 || System.currentTimeMillis()-EPICSUtilities.toUTC(ry.getTimestamp())>3000) {
					rx.updateAlarm(Severity.MAJOR_ALARM, Status.LINK_ALARM,false);
					ry.updateAlarm(Severity.MAJOR_ALARM, Status.LINK_ALARM,false);
					ValueLinks vlx= getLinks(PV_X);
					ValueLinks vly= getLinks(PV_Y);
					if (vlx.isReady() && vly.isReady()) {
						orbitRaw= updateOrbit(vlx.consumeAsDoubles(), vly.consumeAsDoubles(),null,LIVE_RAW_X,LIVE_RAW_Y,LIVE_RAW_STAT_X,LIVE_RAW_STAT_Y,LIVE_RAW_STAT_STRING,false); 
						orbit= updateOrbit(orbitRaw.getPosH(),orbitRaw.getPosV(),reference,LIVE_X,LIVE_Y,LIVE_STAT_X,LIVE_STAT_Y,LIVE_STAT_STRING,hasReference);
					}
				}
			}
		}, 10000);*/
	}

	/**
	 * <p>updateOrbit.</p>
	 *
	 * @param x an array of {@link double} objects
	 * @param y an array of {@link double} objects
	 * @param ref a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 * @param orbitX a {@link java.lang.String} object
	 * @param orbitY a {@link java.lang.String} object
	 * @param hasRef a boolean
	 * @return a {@link org.scictrl.csshell.epics.server.application.orbitserver.Orbit} object
	 */
	protected Orbit updateOrbit(final double[] x, final double[] y, final Orbit ref, final String orbitX, final String orbitY, final boolean hasRef) {

		Orbit o= hasRef ? new Orbit(x, y, ref) : new Orbit(x, y);

		setting.add(orbitX);
		setting.add(orbitY);
		if (orbitX!=null) {
			String pr= orbitX.substring(0, orbitX.length()-2);
			
			getRecord(orbitX).setValue(o.getPosH());
			double[] d= o.getStatH();
			getRecord(pr+SFX_STAT_X).setValue(d);
			getRecord(pr+SFX_STAT_X_AVG).setValue(d[Stat._AVG]);
			getRecord(pr+SFX_STAT_X_MAX).setValue(d[Stat._MAX]);
			getRecord(pr+SFX_STAT_X_RMS).setValue(d[Stat._RMS]);
			getRecord(pr+SFX_STAT_X_STD).setValue(d[Stat._STD]);

			getRecord(pr+SFX_STAT_STRING).setValue(o.toStringStatFancy());
		}
		if (orbitY!=null) {
			String pr= orbitY.substring(0, orbitY.length()-2);

			getRecord(orbitY).setValue(o.getPosV());
			double[] d= o.getStatV();
			getRecord(pr+SFX_STAT_Y).setValue(d);
			getRecord(pr+SFX_STAT_Y_AVG).setValue(d[Stat._AVG]);
			getRecord(pr+SFX_STAT_Y_MAX).setValue(d[Stat._MAX]);
			getRecord(pr+SFX_STAT_Y_RMS).setValue(d[Stat._RMS]);
			getRecord(pr+SFX_STAT_Y_STD).setValue(d[Stat._STD]);
			
			if (orbitX==null) {
				getRecord(pr+SFX_STAT_STRING).setValue(o.toStringStatFancy());
			}
		}
		
		setting.remove(orbitX);
		setting.remove(orbitY);
		
		return o;
	}

}
