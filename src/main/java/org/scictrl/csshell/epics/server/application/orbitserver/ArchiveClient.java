/**
 * 
 */
package org.scictrl.csshell.epics.server.application.orbitserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * <p>ArchiveClient class.</p>
 *
 * @author igor@scictrl.com
 */
public class ArchiveClient {
	
	/** Constant <code>SAMPLES_SEARCH_COUNT=50</code> */
	public static final int SAMPLES_SEARCH_COUNT = 50;

	
	/** Constant <code>log</code> */
	public static Logger log= LogManager.getLogger(ArchiveClient.class);
	
	/**
	 * Query JSON Archive Proxy server for PV data.
	 *
	 * @param pvName Name of PV we're querying the archiver for
	 * @param tsStart Start of time interval for PV data in nanosecond timestamp format
	 * @param tsEnd End of time interval for PV data in nanosecond timestamp format
	 * @param count Max. number of elements to retrieve from archive server
	 * @return InputStream with JSON data, null if an error occurred
	 * @param archive_url a {@link java.lang.String} object
	 */
	public static InputStream getArchiveDataStream(String pvName, long tsStart, long tsEnd, int count, String archive_url) {
		InputStream is;
		
		// Construct URL for JSON Archive server. URL format is the following:
		// http://HOSTNAME:PORT/archive/ARCHIVE_ID/samples/PV_NAME?start=START_TIMESTAMP&end=END_TIMESTAMP&count=SAMPLES_COUNT
		// For example:
		// http://ankasr-main.anka.kit.edu:9812/archive/1/samples/A:SR-S1:BPM:01:SA:X?start=1374321600&end=1374321600&count=10
		StringBuilder url = new StringBuilder(archive_url);
		url.append("samples/");
		url.append(pvName);
		url.append("?start=" + tsStart);
		url.append("&end=" + tsEnd);
		url.append("&count=" + count);
		
		//log.info("Archive URL: " + url);

		try {
			// Open HTTP connection to JSON Archive server
			URLConnection conn = new URL(url.toString()).openConnection();
			conn.addRequestProperty("Accept-Encoding", "gzip, deflate");
			conn.connect();
			
			// Check which encoding server used in reply
			// and convert input stream data to a String
			String content_encoding = conn.getHeaderField("Content-Encoding");
			
			log.debug("["+pvName+"] Content-Encoding: '"+content_encoding+"'");
			
			if ("gzip".equalsIgnoreCase(content_encoding)) {
				is = new GZIPInputStream(conn.getInputStream());
			} else if ("deflate".equalsIgnoreCase(content_encoding)) {
				is = new DeflaterInputStream(conn.getInputStream());
			} else {
				is = conn.getInputStream();
			}
		
		} catch (FileNotFoundException e) {
			log.error("["+pvName+"] no archive record (" + url.toString()+")");
			return null;
		} catch (IOException e) {
			log.error("["+pvName+"] Error while opening URL connection:" + e,e);
			return null;
		}
		
		return is;
	}
	
	/**
	 * Parse InputStream with PV data in JSON format.
	 *
	 * @param jsonInputStream InputStream with PV data from Archive Proxy server in JSON format.
	 * @return Vector with PV data; each element is an 2-element array where [0] is Long nanosecond
	 *             timestamp and [1] is Double PV value. Null is returned on error or if no data is found.
	 */
	public static Vector<Object[]> getJsonPVValues(InputStream jsonInputStream) {
		// Vector that will contain resulting data
		Vector <Object[]>pvData = new Vector<Object[]>();
		
		try {
			JsonParser jp = new JsonFactory().createJsonParser(jsonInputStream);
			
			if (jp.nextToken() != JsonToken.START_ARRAY) {
				log.error("Expected data to start with '" + JsonToken.START_ARRAY + "'");
				return null;
			}
			JsonToken jsonToken = jp.nextToken();
			
			Long tsNano = null;
			int tsObjectCount = 0;
			
			Double value = null;
			int valueObjectCount = 0;
			
			int objectDepth = 0;
			int objectCount = 0;
			while (true) {
				// This isn't really expected
				if (jsonToken == null) {
					log.error("Unexpected end of stream");
					break;
				// Normal termination of stream
				} else if (jsonToken == JsonToken.END_ARRAY && objectDepth == 0) {
					break;
				// Count how nested we are in the JSON tree
				} else if (jsonToken == JsonToken.START_OBJECT) {
					objectDepth++;
				} else if (jsonToken == JsonToken.END_OBJECT) {
					objectDepth--;
					
					// Count only top-level JSON objects
					if (objectDepth == 0) {
						objectCount++;
					}
				}
				
				// Check if we're in values for fields that are interesting to us
				if (objectDepth == 1 && jsonToken != JsonToken.FIELD_NAME) {
					// Timestamp is with nanosecond precision
					if (jp.getCurrentName() == "time") {
						tsNano = Long.valueOf(jp.getText());
						tsObjectCount = objectCount;
						
					} else if (jp.getCurrentName() == "value") {
						// PV values are stored as arrays
						if (jsonToken == JsonToken.START_ARRAY) {
							// Move token to array value
							jsonToken = jp.nextToken();
							
							value = Double.valueOf(jp.getText());
							valueObjectCount = objectCount;
						}
					}
				}
				
				// Check if we have a timestamp/value pair and put it to a collection
				if (tsNano != null && value != null) {
					if (tsObjectCount == valueObjectCount) {
						Object[] arr = {tsNano, value};
						pvData.add(arr);
					} else {
						log.warn("PV timestamp and PV value belong to different JSON objects.");
					}
					
					tsNano = null;
					value = null;
				}
				
				// This shouldn't really happen
				if (objectDepth < 0) {
					log.error("JSON top object depth is less than zero!");
					break;
				}
				
				// Move to next JSON token
				jsonToken = jp.nextToken();
			}
		} catch (Exception e) {
			log.error("Error parsing input stream with JSON data: " + e.getMessage());
			return null;
		}
		
		// Return null if there's no PV data
		if (pvData.size() == 0) {
			return null;
		}
		
		return pvData;
	}
	
	/**
	 * Parse InputStream with PV data in JSON format.
	 *
	 * @param jsonInputStream InputStream with PV data from Archive Proxy server in JSON format.
	 * @return Vector with PV data; each element is an 2-element array where [0] is Long nanosecond
	 *             timestamp and [1] is Double PV value. Null is returned on error or if no data is found.
	 * @param targetTime a long
	 */
	public static Object[] getClosestTimeValuePoint(InputStream jsonInputStream, long targetTime) {
		
		Long bestNanoTime=null;
		Double bestValue=null;
		
		try {
			JsonParser jp = new JsonFactory().createJsonParser(jsonInputStream);
			
			if (jp.nextToken() != JsonToken.START_ARRAY) {
				log.error("Expected data to start with '" + JsonToken.START_ARRAY + "'");
				return null;
			}
			JsonToken jsonToken = jp.nextToken();
			
			Long tsNano = null;
			int tsObjectCount = 0;
			
			Double value = null;
			int valueObjectCount = 0;
			
			int objectDepth = 0;
			int objectCount = 0;
			while (true) {
				
				//System.out.print("> ");
				//for (int i = 0; i < objectDepth; i++) {
				//	System.out.print("> ");
				//}
				//System.out.println(objectCount+" "+jsonToken.toString()+" "+jp.getCurrentName());

				// This isn't really expected
				if (jsonToken == null) {
					log.error("Unexpected end of stream");
					break;
				// Normal termination of stream
				} else if (jsonToken == JsonToken.END_ARRAY && objectDepth == 0) {
					break;
				// Count how nested we are in the JSON tree
				} else if (jsonToken == JsonToken.START_OBJECT) {
					objectDepth++;
				} else if (jsonToken == JsonToken.END_OBJECT) {
					objectDepth--;
					
					// Count only top-level JSON objects
					if (objectDepth == 0) {
						objectCount++;
					}
				}
				
				// Check if we're in values for fields that are interesting to us
				if (objectDepth == 1 && jsonToken != JsonToken.FIELD_NAME) {
					// Timestamp is with nanosecond precision
					if (jp.getCurrentName() == "time") {
						tsNano = Long.valueOf(jp.getText());
						tsObjectCount = objectCount;
						
					} else if (jp.getCurrentName() == "value") {
						// PV values are stored as arrays
						if (jsonToken == JsonToken.START_ARRAY) {
							// Move token to array value
							jsonToken = jp.nextToken();
							
							value = Double.valueOf(jp.getText());
							valueObjectCount = objectCount;
							
							//System.out.println("# "+value+" "+tsNano/1000000+" "+new Date(tsNano/1000000));
						}
					}
				}
				
				// Check if we have a timestamp/value pair and put it to a collection
				if (tsNano != null && value != null) {
					
					if (tsObjectCount == valueObjectCount) {
						
						if (bestNanoTime==null || bestValue==null || Math.abs(targetTime*1000000-tsNano)<Math.abs(targetTime*1000000-bestNanoTime)) {
							bestNanoTime=tsNano;
							bestValue=value;
						}
						
					} else {
						log.warn("PV timestamp and PV value belong to different JSON objects.");
					}
					
					tsNano = null;
					value = null;
				}
				
				// This shouldn't really happen
				if (objectDepth < 0) {
					log.error("JSON top object depth is less than zero!");
					break;
				}
				
				// Move to next JSON token
				jsonToken = jp.nextToken();
			}
		} catch (Exception e) {
			log.error("Error parsing input stream with JSON data: " + e.getMessage());
			return null;
		}
		
		if (bestNanoTime==null || bestValue==null) {
			return null;
		}
		
		return new Object[]{bestNanoTime,bestValue};
	}

	/**
	 * Find PV value in pvData whose timestamp is closest to targetTimestamp.
	 *
	 * @param pvData Vector containing arrays where [0] is Long nanosecond timestamp and
	 *               [1] is Double PV value
	 * @param targetTimestamp target nanosecond timestamp
	 * @return Array element from pvData that is closest to targetTimestamp
	 */
	public static Object[] getClosestPVValue(Vector<Object[]> pvData, long targetTimestamp) {
		if (pvData == null || pvData.size() == 0) {
			return null;
		}
		
		// PV data with timestamp that is closest to target timestamp
		Object[] closest = pvData.elementAt(0);
		
		// Timestamps from JSON archive are with nanosecond precision,
		// that's why we make our target timestamp also with fake
		// nanosecond precision
		targetTimestamp *= 1000000;
		
		// Find PV data closest to target timestamp
		Iterator<Object[]> iter = pvData.iterator();
		while (iter.hasNext()) {
			Object[] arr = iter.next();
			Long timestamp = (Long)arr[0];
			Long closestTimestamp = (Long)closest[0];
			
			if (Math.abs(timestamp.longValue() - targetTimestamp) < Math.abs(closestTimestamp.longValue() - targetTimestamp)) {
				closest = arr;
			}
		}
		
		return closest;
	}
	
	/**
	 * Retrieve PV values from JSON Archive Proxy server for PV names specified in pvs.
	 *
	 * @param pvs String array of PV names that will be queried from archiver
	 * @param targetTime in milliseconds
	 * @param timeWindow in milliseconds
	 * @return Double array with PV values from archiver
	 * @param archiver_url a {@link java.lang.String} object
	 */
	public static double[] getTimeInstant(String[] pvs, long targetTime, long timeWindow , String archiver_url) {
		
		// Get target date and add 10^9 to make it a fake nanosecond
		// precision timestamp, which the JSON Archive Proxy server
		// expects as a URL parameter

		// Make sure to convert offset from seconds to nanoseconds
		long tsStart = (targetTime - timeWindow) * 1000000L;
		long tsEnd = (targetTime + timeWindow) * 1000000L;
		
		// Fetch values from JSON Archive proxy for specified PVs
		double values[] = new double[pvs.length];
		for (int i = 0; i < pvs.length; i++) {
			InputStream jsonData = getArchiveDataStream(pvs[i], tsStart, tsEnd, SAMPLES_SEARCH_COUNT, archiver_url);
			
			if (jsonData==null) {
				log.warn("No reference orbit data for '"+pvs[i]+"', 0 used instead.");
				values[i]=0.0;
			} else {
			
				Object[] pair= getClosestTimeValuePoint(jsonData, targetTime);
	
				if (pair!=null && pair[0]!=null && pair[1]!=null) {
					values[i]=(double) pair[1];
				} else {
					log.warn("No reference orbit data for '"+pvs[i]+"', 0 used instead.");
					values[i]=0.0;
				}
			}
		}
		
		return values;
	}
	
	
	private ArchiveClient() {
	}

}
