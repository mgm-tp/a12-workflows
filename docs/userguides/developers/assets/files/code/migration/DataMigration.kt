import com.mgmtp.a12.dataservices.migration.MigrationStep
import com.mgmtp.a12.dataservices.migration.MigrationTask
import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.RuntimeService
...

@MigrationStep(name = "DataMigration", version = "1.0.1")
class SameVersion @Autowired constructor(val runtimeService: RuntimeService, val processEngine: ProcessEngine) {

    private final val PROCESS_DEFINITION = "ParallelGatewayProcess"

    @MigrationTask(name = "data-migration")
    fun migration() {
        val tasks = processEngine.taskService.createTaskQuery().processDefinitionKey(PROCESS_DEFINITION).list()
        for (task in tasks) {
            if (task.taskDefinitionKey == "TaskA") {
                processEngine.taskService.setVariableLocal(task.id, "StringA", "This is an updated TaskA")
            } else {
                processEngine.taskService.setVariableLocal(task.id, "StringB", "This is an updated TaskB")
            }
        }
    }
}
