import com.mgmtp.a12.dataservices.migration.MigrationStep
import com.mgmtp.a12.dataservices.migration.MigrationTask
import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.RuntimeService
...

@MigrationStep(name = "ProcessMigration", version = "1.0.1")
class ProcessMigration @Autowired constructor(val runtimeService: RuntimeService, val processEngine: ProcessEngine) {

    private final val PROCESS_DEFINITION = "ParallelGatewayProcess"

    @MigrationTask(name = "process-migration")
    fun migration() {
        val processDefinitionFrom =
            processEngine.repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION)
                .processDefinitionVersion(1)
                .singleResult()

        val processDefinitionTo =
            processEngine.repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION)
                .processDefinitionVersion(2)
                .singleResult()

        val processes =
            processEngine.runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinitionFrom.id)
                .list()

        val migrationPlan = runtimeService.createMigrationPlan(processDefinitionFrom.id, processDefinitionTo.id).mapEqualActivities()
            .mapActivities("TaskB", "TaskC").build()

        runtimeService.newMigration(migrationPlan).processInstanceIds(processes.map { it.processInstanceId }).execute()

    }
}
