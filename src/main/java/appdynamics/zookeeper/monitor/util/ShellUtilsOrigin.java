package appdynamics.zookeeper.monitor.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ShellUtilsOrigin {
	public static void runShell(String directory, String command, String[] args, Map<String, String> environment)
	{
	    try
	    {
	        if(directory.trim().equals(""))
	            directory = "/";

	        String[] cmd = new String[args.length + 1];
	        cmd[0] = command;

	        int count = 1;

	        for(String s : args)
	        {
	            cmd[count] = s;
	            count++;
	        }
	        
	        ProcessBuilder pb = new ProcessBuilder(cmd);

	        Map<String, String> env = pb.environment();

	        for(String s : environment.keySet())
	            env.put(s, environment.get(s));

	        pb.directory(new File(directory));

	        Process process = pb.start();

	        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        BufferedWriter outputReader = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
	        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	        int exitValue = process.waitFor();

	        if(exitValue != 0) // has errors
	        {
	            while(errReader.ready())
	            {
	            	log.error("runShell/ErrShell: " + errReader.readLine());
	            }
	        }
	        else
	        {
	            while(inputReader.ready())
	            {
	            	log.info("runShell/Shell Result : " + inputReader.readLine());
	            }
	        }
	    }
	    catch(Exception e)
	    {
	    	log.error("runShell/Err: RunShell, " + e.toString());
	    }
	}

	public static void runShell(String path, String command, String[] args)
	{
	    try
	    {
	        String[] cmd = new String[args.length + 1];

	        if(!path.trim().isEmpty())
	            cmd[0] = path + "/" + command;
	        else
	            cmd[0] = command;

	        int count = 1;

	        for(String s : args)
	        {
	            cmd[count] = s;
	            count++;
	        }

	        Process process = Runtime.getRuntime().exec(cmd);

	        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        BufferedWriter outputReader = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
	        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	        int exitValue = process.waitFor();

	        if(exitValue != 0) // has errors
	        {
	            while(errReader.ready())
	            {
	            	log.error("runShell/ErrShell: " + errReader.readLine());
	            }
	        }
	        else
	        {
	            while(inputReader.ready())
	            {
	            	log.info("runShell/Shell Result: " + inputReader.readLine());
	            }
	        }
	    }
	    catch(Exception e)
	    {
	    	log.error("runShell/Err: RunShell, " + e.toString());
	    }
	}
}
