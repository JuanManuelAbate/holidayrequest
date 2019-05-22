package holidayrequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

public class HolidayRequest {

	public static void main(String[] args) {
		
		//configuration.
		ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
			      .setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1")
			      .setJdbcUsername("sa")
			      .setJdbcPassword("")
			      .setJdbcDriver("org.h2.Driver")
			      .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
		
		//starts the engine. Tables are created here.
		ProcessEngine processEngine = cfg.buildProcessEngine();
		
		//The engine has various repositories to do different things.
		RepositoryService repositoryService = processEngine.getRepositoryService();
		
		//deploy a process definition based on our xml to the process engine.
		//process definition are used as blueprints to create process instances later.
		Deployment deployment = repositoryService.createDeployment()
		  .addClasspathResource("holiday-request.bpmn20.xml")
		  .deploy();
		
		//query the process definition to check if the process where succesfully created.
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				  .deploymentId(deployment.getId())
				  .singleResult();

		//show the process definiton name.
		System.out.println("Found process definition : " + processDefinition.getName());
		
		//runtime service is used to create process instances.
		RuntimeService runtimeService = processEngine.getRuntimeService();
		
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("employee", "juanma");
		variables.put("nrOfHolidays", 10);
		variables.put("description", "New York travel");
		
		//create a concrete instance of the project, holidayRequest is the key of the 
		//process that is defined in the xml file.
		ProcessInstance processInstance =
		  runtimeService.startProcessInstanceByKey("holidayRequest", variables);
		
		//taskService is used to query tasks.
		TaskService taskService = processEngine.getTaskService();
		
		//List the task list available for the managers group, groups are defined in the xml.
		List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
		System.out.println("You have " + tasks.size() + " tasks:");
		for (int i=0; i<tasks.size(); i++) {
		  System.out.println((i+1) + ") " + tasks.get(i).getName());
		}
		
		Task task = tasks.get(0);
		//Using the task identifier, we can now get the specific process instance variables.
		Map<String, Object> processVariables = taskService.getVariables(task.getId());
		System.out.println(processVariables.get("employee") + " wants " +
		    processVariables.get("nrOfHolidays") + " of holidays.");
		
		//here we complete the task so the process can go to the next task, note that
		//the name of the new variable that is in the map is the same used in the xml
		//definition of the process in the exclusive gateway.
		boolean approved = true;
		variables = new HashMap<String, Object>();
		variables.put("approved", approved);
		taskService.complete(task.getId(), variables);
		
		//HistoryServices is used to query audit data or historical data.
		HistoryService historyService = processEngine.getHistoryService();
		//get the list of activities finished for a process instance.
		List<HistoricActivityInstance> activities =
		  historyService.createHistoricActivityInstanceQuery()
		   .processInstanceId(processInstance.getId())
		   .finished()
		   .orderByHistoricActivityInstanceEndTime().asc()
		   .list();

		for (HistoricActivityInstance activity : activities) {
		  System.out.println(activity.getActivityId() + " took "
		    + activity.getDurationInMillis() + " milliseconds");
		}
		
	}

}
