namespace java com.zorroa.common.cluster.thrift

typedef map<string,string> Attrs;

exception CusterExceptionT {
    1: i32 what,
    2: string why
}

struct StackElementT {
    1:string file,
    2:string className,
    3:string method,
    4:i32 lineNumber
}

struct TaskStartT {
    1:i32 id,
    2:i32 jobId,
    3:i32 parent,
    4:string name,
    5:Attrs env,
    6:binary argMap,
    7:string workDir;
    8:string sharedDir;
    9:string scriptPath,
    10:string logPath,
    11:binary script,
    12:string masterHost,
    13:i32 order
}

struct ExpandT {
    1:string name,
    2:binary script
}

struct TaskStopT {
    1:i32 exitStatus
}

struct TaskErrorT {
    1:string id
    2:string path,
    3:string originPath,
    4:string originService,
    5:string message,
    6:string processor,
    7:string phase,
    8:bool skipped,
    9:list<StackElementT> stack,
    13:i64 timestamp
}

struct TaskResultT {
    1:binary result,
    2:list<TaskErrorT> errors = []
}

struct TaskKillT {
    1:i32 id,
    2:i32 jobId,
    3:string user,
    4:string reason
}

struct TaskStatsT {
    1:i32 warningCount=0,
    2:i32 errorCount=0,
    3:i32 successCount=0
}

struct AnalystT {
    1:string id,
    2:string arch,
    3:string os,
    4:string url,
    5:i32 port,
    6:i32 threadCount,
    7:bool data,
    8:bool master,
    9:i32 state,
    10:list<i32> taskIds,
    11:i64 updatedTime,
    12:i32 threadsUsed,
    13:i32 queueSize,
    14:double loadAvg,
    15:binary metrics
}

service MasterServerService {
    oneway void ping(1:AnalystT node),
    void reportTaskStarted(1:i32 id) throws (1:CusterExceptionT e),
    void reportTaskStopped(1:i32 id, 2:TaskStopT result) throws (1:CusterExceptionT e),
    void reportTaskRejected(1:i32 id, 2:string reason) throws (1:CusterExceptionT e),
    void reportTaskStats(1:i32 id, 2:TaskStatsT stats) throws (1:CusterExceptionT e),
    void reportTaskErrors(1:i32 id, 2:list<TaskErrorT> errors) throws (1:CusterExceptionT e),

    list<TaskStartT> queuePendingTasks(1:string url, 2:i32 count) throws (1:CusterExceptionT e),
    void expand(1:i32 parent, 2:ExpandT expand) throws (1:CusterExceptionT e)
}

service WorkerNodeService {
    TaskResultT executeTask(1:TaskStartT task) throws (1:CusterExceptionT e),
    void killTask(1:TaskKillT task) throws (1:CusterExceptionT e),
    void killAll() throws (1:CusterExceptionT e)
}
