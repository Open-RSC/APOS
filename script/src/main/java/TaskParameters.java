public class TaskParameters
{
	public static TaskParameters buildFromString(String fromString)
	{
		//Si le header est présent on l'enleve
		if(fromString.startsWith(TASKING_HEADER))
			fromString = fromString.substring(TASKING_HEADER.length());
		
		TaskParameters taskParams = new TaskParameters();
		
		String[] in = fromString.split(";");
		
		for(int i = 0; i < in.length; i++)
		{
			if(in[i].startsWith("MainScriptName="))
			{
				taskParams.MainScriptName = in[i].substring("MainScriptName=".length());
				
				continue;
			}
			if(in[i].startsWith("MainScriptParams="))
			{
				taskParams.MainScriptParams = in[i].substring("MainScriptParams=".length());
				continue;
			}
			if(in[i].startsWith("TaskInputParams="))
			{
				taskParams.TaskInputParams = in[i].substring("TaskInputParams=".length());
				continue;
			}
			if(in[i].startsWith("TaskExtraInputParams="))
			{
				taskParams.TaskExtraInputParams = in[i].substring("TaskExtraInputParams=".length());
				continue;
			}
			
		}
		
		return taskParams;
	}
	
	@Override
	public String toString()
	{
		String result = "";
		result+="MainScriptName=" + MainScriptName;
		result+=";MainScriptParams=" + MainScriptParams;
		result+=";TaskInputParams=" + TaskInputParams;
		result+=";TaskExtraInputParams=" + TaskExtraInputParams;
		
		return result;
	}
	
	public String MainScriptName = "";
	public String MainScriptParams = "";
	public String TaskInputParams = "";
	public String TaskExtraInputParams = "";
	
	public static String TASKING_HEADER = "TASKING:";
}