package boonai.common.apikey

import io.swagger.annotations.ApiModelProperty

/**
 * Permissions users can assign.  Internal API keys are not
 * assignable unless via the service key.
 */
enum class Permission(val internal: Boolean) {

    @ApiModelProperty("The system service key.")
    SystemServiceKey(true),

    @ApiModelProperty("Allows access to monitoring endpoints")
    SystemMonitor(true),

    @ApiModelProperty("Allows access to platform management endpoints")
    SystemManage(true),

    @ApiModelProperty("Provides ability to switch projects")
    SystemProjectOverride(true),

    @ApiModelProperty("Provides ability to view encrypted project data")
    SystemProjectDecrypt(true),

    @ApiModelProperty("Provides ability to store project files in cloud storage.")
    ProjectFilesWrite(true),

    @ApiModelProperty("Provides ability to read project files from cloud storage.")
    ProjectFilesRead(true),

    @ApiModelProperty("Provides ability to read assets and associated files")
    AssetsRead(false),

    @ApiModelProperty("Provides ability to import assets. (created and update)")
    AssetsImport(false),

    @ApiModelProperty("Provides ability to remove assets")
    AssetsDelete(false),

    @ApiModelProperty("Provides ability to manage projects.")
    ProjectManage(false),

    @ApiModelProperty("Provides ability to manage datasources.")
    DataSourceManage(false),

    @ApiModelProperty("Provides ability to manage jobs and tasks.")
    DataQueueManage(false),

    @ApiModelProperty("Ability to train and manage models and datasets.")
    ModelTraining(false);
}
