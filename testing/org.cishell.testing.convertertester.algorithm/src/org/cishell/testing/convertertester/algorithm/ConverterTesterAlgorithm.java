package org.cishell.testing.convertertester.algorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;

import org.cishell.framework.CIShellContext;
import org.cishell.framework.algorithm.Algorithm;
import org.cishell.framework.algorithm.AlgorithmFactory;
import org.cishell.framework.algorithm.AlgorithmProperty;
import org.cishell.framework.data.BasicData;
import org.cishell.framework.data.Data;
import org.cishell.framework.data.DataProperty;
import org.cishell.testing.convertertester.core.converter.graph.ConverterGraph;
import org.cishell.testing.convertertester.core.tester2.ConverterTester2;
import org.cishell.testing.convertertester.core.tester2.reportgen.ReportGenerator;
import org.cishell.testing.convertertester.core.tester2.reportgen.allconvs.AllConvsReportGenerator;
import org.cishell.testing.convertertester.core.tester2.reportgen.alltests.AllTestsReportGenerator;
import org.cishell.testing.convertertester.core.tester2.reportgen.convgraph.GraphReportGenerator;
import org.cishell.testing.convertertester.core.tester2.reportgen.readme.ReadMeReportGenerator;
import org.cishell.testing.convertertester.core.tester2.reportgen.reports.AllConvsReport;
import org.cishell.testing.convertertester.core.tester2.reportgen.reports.AllTestsReport;
import org.cishell.testing.convertertester.core.tester2.reportgen.reports.ConvReport;
import org.cishell.testing.convertertester.core.tester2.reportgen.reports.FilePassReport;
import org.cishell.testing.convertertester.core.tester2.reportgen.reports.ReadMeReport;
import org.cishell.testing.convertertester.core.tester2.reportgen.reports.TestReport;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

//TODO: Maybe let the user specify which converters he/she wants to test, or other things
//TODO: Make it progress-trackable

public class ConverterTesterAlgorithm implements Algorithm, AlgorithmProperty {

    private CIShellContext cContext;
    private BundleContext bContext;
    private LogService log;
    
    public ConverterTesterAlgorithm(Data[] data, Dictionary parameters,
    		CIShellContext cContext, BundleContext bContext ) {
        this.cContext = cContext;
        this.bContext = bContext;
        
        this.log = (LogService) cContext.getService(
				LogService.class.getName());
        
    }

    public Data[] execute() {
    	
    	this.log.log(LogService.LOG_WARNING, 
    			"-------NOTICE-------" + "\n" + 	
    			"The Converter Tester will take " +
    			"approximately 30 seconds to run all the tests, and around " +
    			"20 seconds to display all the results. Thank you for " +
    			"waiting :)" + "\n" +
    			"-----END NOTICE-----");
    	
    	Data[] returnDM;

    	final IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    	if (windows.length == 0) {
    		return null;
    	}
    
    	Display display = PlatformUI.getWorkbench().getDisplay();
    	DataUpdater dataUpdater = new DataUpdater (windows[0], this.log);
    	
    	if (Thread.currentThread() != display.getThread()) {
    		display.syncExec(dataUpdater);
		} else {
			dataUpdater.run();
		}
    	
    	if (!dataUpdater.returnList.isEmpty()){
    		int size = dataUpdater.returnList.size();
    		returnDM = new Data[size];
    		for(int index=0; index<size; index++){
    			returnDM[index]=(Data)dataUpdater.returnList.get(index);
    		}
    		return returnDM;
    	}
    	else {    		
    		return null;
    	}
    	
    }
    
    final class DataUpdater implements Runnable {
    	boolean loadFileSuccess = false;
    	IWorkbenchWindow window;
    	ArrayList returnList = new ArrayList();
    	LogService log;
    	
    	DataUpdater (IWorkbenchWindow window, LogService log){
    		this.log = log;
    		this.window = window;    		
    	}    	
    	
    	public void run () {
    		
    		try {
    				//get all the converters
		   			ServiceReference[] convRefs = getConverterReferences();
		   			
		   			//generate converter paths inside converter graph, for use in executing the test
		   			ConverterGraph converterGraph = new ConverterGraph(convRefs, bContext, log);
		   			
		   			//extract converter graph in nwb file format.
		   			File nwbGraph = converterGraph.asNWB();
		   			
		   			//initialize all the report generators
		   			
		   			AllTestsReportGenerator allGen     = new AllTestsReportGenerator(this.log);
		   			AllConvsReportGenerator allConvGen = new AllConvsReportGenerator(this.log);
		   			GraphReportGenerator    graphGen   = new GraphReportGenerator(nwbGraph, this.log);
		   			ReadMeReportGenerator   readmeGen  = new ReadMeReportGenerator();
		   			
		   			//execute the tests, and provide the results to the report generators
		   			ConverterTester2 ct = new ConverterTester2(log);
		   			ct.execute(converterGraph,
		   					new ReportGenerator[] 
		   					   {allGen, allConvGen, graphGen, readmeGen},
		   					cContext, bContext);
		   			/*
		   			 * report generators have now been supplied with the test
		   			 * results, and their reports can now be extracted.
		   			 */
		   			
		   			//return readme report
		   			
		   			ReadMeReport readmeReport = readmeGen.getReadMe();
		   			File readmeFile = readmeReport.getReportFile();
		   			Data readMeData = createReportData(readmeFile,
		   					readmeReport.getName(), null);
		   			addReturn(readMeData);
		   			
		   			//return all tests report
		   			
		   			AllTestsReport allReport = allGen.getAllTestsReport();
		   			File allReportFile = allReport.getAllTestsReport();
		   			Data allReportData = createReportData(allReportFile,
		   					allReport.getName() , null);
		   			addReturn(allReportData);
		   			
		   			TestReport[] sTestReports = allReport.getSuccessfulTestReports();
		   			addFilePasses(sTestReports, allReportData);
		   			
		   			TestReport[] ppTestReports = allReport.getPartialSuccessTestReports();
		   			addFilePasses(ppTestReports, allReportData);
		   			
		   			TestReport[] fTestReports = allReport.getFailedTestReports();
		   			addFilePasses(fTestReports, allReportData);
		   			
		   			//return all converters report
		   			
		   			AllConvsReport allConvReport = allConvGen.getAllConvsReport();
		   			File allConvReportFile = allConvReport.getReport();
		   			Data allConvReportData = createReportData(allConvReportFile, allConvReport.getName(),
		   					null);
		   			addReturn(allConvReportData);
		   			
		   				//return each converter report
		   			ConvReport[] convReports = allConvReport.getConverterReports();
		   			for (int ii = 0; ii < convReports.length; ii++) {
		   				ConvReport convReport = convReports[ii];
		   				File convReportFile = convReport.getReport();
		   				Data convReportData = createReportData(convReportFile, convReport.getName(), allConvReportData);
		   				addReturn(convReportData);
		   				
		   				TestReport[] trs = convReport.getTestReports();
		   				addFilePasses(trs, convReportData);
		   			}
		   			
		   			//return annotated graph report
		   			
		   			File graphReportFile = graphGen.getGraphReport();
		   			Data graphReport = createReportData(graphReportFile, "Annotated Graph Report", null,
		   					"file:text/nwb", DataProperty.NETWORK_TYPE);
		   			addReturn(graphReport);
    		} catch (Exception e) {
    			this.log.log(LogService.LOG_ERROR, "Converter Tester Failed.", e);
    		}
    }
    	
    	/**
    	 * Add a report to a list of reports that are later returned.
    	 * @param report the report to be returned from this algorithm
    	 */
        private void addReturn(Data report) {
        	this.returnList.add(report);
        }
        
        
        /**
         * Returns file pass reports associated with tests or converters.
         * @param testReports reports to be returned as children or test or converter
         * @param parent the parent of the file pass
         */
        private void addFilePasses(TestReport[] testReports, Data parent) {
    			for (int ii = 0; ii < testReports.length; ii++) {
    				TestReport tr = testReports[ii];
    				File testReportFile = tr.getTestReport();
//    				System.out.println("In algorithm, file pass name is : " + tr.getName());
//    				System.out.println("In algorithm FILE name is : " + testReportFile.getName());
    				Data testReportData = createReportData(testReportFile,
    						tr.getName(), parent);
    				addReturn(testReportData);
    				
    				FilePassReport[] sFilePassReports = tr.getSuccessfulFilePassReports();
    				for (int kk = 0; kk < sFilePassReports.length; kk++) {
    					FilePassReport fp = sFilePassReports[kk];
    					File fpFile = fp.getFilePassReport();
    					Data fpData = createReportData(fpFile, fp.getName(),
    							testReportData);
    					addReturn(fpData);
    				}
    				
    				FilePassReport[] fFilePassReports = tr.getFailedFilePassReports();	
    				for (int kk = 0; kk < fFilePassReports.length; kk++) {
    					FilePassReport fp = fFilePassReports[kk];
    					File fpFile = fp.getFilePassReport();
    					Data fpData = createReportData(fpFile, fp.getName(),
    							testReportData);
    					addReturn(fpData);
    				}
    			}
        }
        
        /**
         * Wraps the report with metadata in a form that is ready to be 
         * returned from the algorithm.
         * 
         * @param report the report to be turned into data
         * @param label how the report will be labeled in the data manager window
         * @param parent which report this report will hang from 
         * (null if it is not a child of any report)
         * @param format The file format or class name of the report
         * @param type whether the report is a network or text file
         * @return the report encapsulated in data, ready to be returned.
         */
        private Data createReportData(Object report, String label, Data parent, String format, String type) {
        	Data reportData = new BasicData(report, format);
			Dictionary metadata = reportData.getMetaData();
			metadata.put(DataProperty.LABEL, label);
			metadata.put(DataProperty.TYPE, type);
			if (parent != null) {
				metadata.put(DataProperty.PARENT, parent);
			}
			return reportData;
        }
        
        /**
         * Alternate version of createReportData that assumes the report is a plain text file
         * @param report the report to be turned into data
         * @param label how the report will be labeled in the data manager window
         * @param parent which report this report will hang from 
         * (null if it is not a child of any report)
         * @return the report encapsulated in data, ready to be returned.
         */
        private Data createReportData(Object report, String label, Data parent) {
        	return createReportData(report, label, parent, "file:text/plain", DataProperty.TEXT_TYPE);
        }
   }
    
  
    
    private ServiceReference[] getConverterReferences() {
		  String filter = "(&("+ALGORITHM_TYPE+"="+TYPE_CONVERTER+"))";// +

		  try {
		  ServiceReference[] refs = bContext.getServiceReferences(
				  AlgorithmFactory.class.getName(), filter);
		  
		  return refs;
		  } catch (InvalidSyntaxException e) {
			  System.out.println("OOPS!");
			  System.out.println(e);
			  return null;
		  }
	}
 
    

    
}