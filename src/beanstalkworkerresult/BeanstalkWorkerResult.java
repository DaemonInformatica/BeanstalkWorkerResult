/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package beanstalkworkerresult;

import dk.safl.beanstemc.Beanstemc;
import dk.safl.beanstemc.BeanstemcException;
import dk.safl.beanstemc.Job;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
/**
 *
 * @author matrix
 */
public class BeanstalkWorkerResult 
{
    private final   String      m_tubename      = "result";
    private final   boolean     m_bRun          = true;
    private final   String      m_dbName        = "dbMD5Decode";
    private         Connection  m_conn;
    private final   String      m_dbUser        = "root";
    private final   String      m_dbPassword    = "";
    
    
    public BeanstalkWorkerResult()
    {
        setupDatabaseConn();
    }

    
    private void setupDatabaseConn()
    {
        try 
        {
            Class.forName("com.mysql.jdbc.Driver"); 
            
            m_conn = DriverManager.getConnection("jdbc:mysql://localhost/" 
                    + m_dbName + "?" 
                    + "user=" + m_dbUser 
                    + "&password=" + m_dbPassword);

        } 
        catch (SQLException ex) 
        {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        catch(ClassNotFoundException cE)
        {
            System.err.println("Class not found exception: " + cE.toString());
        }
    }
    
    private String getCurrentStatus(int dbID)
    {
        PreparedStatement   pSt;
        String              status = "";
        
        try
        {
            pSt = m_conn.prepareStatement("SELECT status FROM tblMD5Process WHERE id=?");
            pSt.setInt(1, dbID);
            ResultSet rs = pSt.executeQuery();
            
            if(rs.next())
            {
                status = rs.getString(1);
            }
        }
        catch(SQLException sqlE) {}
        
        return status;
    }
    
    private String statusEnumToString(EnumJobStatus eStatus)
    {
        switch(eStatus)
        {
            case JobStatusDone:         return "completed";
            case JobStatusProcessing:   return "processing";
        }
        
        return "";
    }
    
    private EnumJobStatus statusStringToEnum(String status)
    {
        if(status.equals("processing") == true)
            return EnumJobStatus.JobStatusProcessing;
        else if(status.equals("completed") == true)
            return EnumJobStatus.JobStatusDone;
        
        return EnumJobStatus.JobStatusUnknown;
    }
    
    private int getMD5IDByProcessID(int processID)        
    {
        int id = 0;
        
        try
        {
            PreparedStatement pSt = m_conn.prepareStatement("SELECT md5ID FROM tblMD5Process WHERE id=?");
            pSt.setInt(1, processID);
            ResultSet rs = pSt.executeQuery();
            
            if(rs.next())
                id = rs.getInt(1);
            
        }
        catch (SQLException ex) 
        {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: "     + ex.getSQLState());
            System.out.println("VendorError: "  + ex.getErrorCode());
        }
        
        return id;
    }
    
    private void updateStatus(EnumJobStatus eStatus, int databaseID, String result)
    {
        // get current status of database job. 
        String  status  = getCurrentStatus(databaseID);
        long    lNow    = System.currentTimeMillis();
        Date    date    = new Date(lNow);

        try
        {
            // if status is already 'completed' (the md5 was decoded by another job.) ignore this update. else: update the status for this job. 
            if(status.equals("completed") == false)
            {
                String strStatus        = statusEnumToString(eStatus);
                String strTimeUpdate    = eStatus == EnumJobStatus.JobStatusProcessing ? "tsStart=?" : "tsEnd=?";
                PreparedStatement pSt   = m_conn.prepareStatement("UPDATE tblMD5Process SET status=?, updated=?, " + strTimeUpdate + " WHERE id=?");
                
                // pSt.setString(1,    strStatus);
                pSt.setObject(1, strStatus);
                pSt.setDate(2, date);
                pSt.setLong(3, lNow);
                pSt.setInt(4, databaseID);
                pSt.execute();
            }   
            else
            {
                return;
            }
            
            // eStatus is completed and the result is not "": We cracked a password. update it to the database. 
            if(result.length() > 0)
            {
                int                 md5ID   = getMD5IDByProcessID(databaseID);
                PreparedStatement   pSt     = m_conn.prepareStatement("UPDATE tblMD5 SET md5decoded=?, updated=? WHERE id=?");

                pSt.setString(1,    result);
                pSt.setDate(2,      date);
                pSt.setInt(3,       md5ID);
                pSt.execute();
            }
            
        }
        catch (SQLException ex) 
        {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: "     + ex.getSQLState());
            System.out.println("VendorError: "  + ex.getErrorCode());
        }

    }
    
    private void runJob(Job j)
    {
        System.out.println("Processing job.");
        
        // Decode JSON
        try
        {            
            // Decode json string. 
            byte        baData[]    = j.getData();
            String      data        = new String(baData, "UTF-8");
            JSONObject  o           = (JSONObject)JSONValue.parse(data);
        
            // get database ID and job-status. 
            long lID            = (Long)o.get("id");
            int nID             = (int)lID;
            String strStatus    = (String)o.get("status");
            String result       = "";
            
            // if status is 'completed': get the result. 
            if(strStatus.equals("completed") == true)
                result = (String)o.get("found");
            
            System.out.println("jobID: " + j.getId() + " - databaseID: " + nID + " - status: " + strStatus + "result: " + result);

            // update status in database. 
            EnumJobStatus   eStatus = statusStringToEnum(strStatus);
            
            updateStatus(eStatus, nID, result);
            
            
        }
        catch(UnsupportedEncodingException usE) { System.err.println("Dude, you messed up the encoding..."); }

    }
    
    public void runWorker(String host, int port)
    {
        System.out.println("Running result workerthread.");
        try
        {
            // init beanstem connection. 
            Beanstemc stem = new Beanstemc(host, port);
            
            // watch correct tube. 
            stem.watch(m_tubename);
            
            // for each job: 
            while(m_bRun)
            {
                System.out.println("runWorker: waiting for job.");
                Job j = stem.reserve();
                System.out.println("job received.");
                stem.delete(j);
                runJob(j);
            }
        }
        catch(UnknownHostException uhE) { System.err.println("Unknown host!"); }
        catch(IOException ioE)          { System.err.println("Oh noes! IOException! " + ioE.toString()); }
        catch(BeanstemcException bsE)   { System.err.println("Beanstem keeled over! " + bsE.toString()); }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        // TODO code application logic here
        BeanstalkWorkerResult app = new BeanstalkWorkerResult();
        app.runWorker("localhost", 9000);
    }
}
