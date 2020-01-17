package com.zorroa.zmlp.apikey

import io.swagger.annotations.ApiModelProperty

enum class Permission() {

    @ApiModelProperty("Allows access to monitoring endpoints")
    SystemMonitor,

    @ApiModelProperty("Allows access to platform management endpoints")
    SystemManage,

    @ApiModelProperty("Provides ability to switch projects")
    SystemProjectOverride,

    @ApiModelProperty("Provides ability to view encrypted project data")
    SystemProjectDecrypt,

    @ApiModelProperty("Provides ability to manage API keys")
    ApiKeyManage,

    @ApiModelProperty("Provides ability to read assets and associated files")
    AssetsRead,

    @ApiModelProperty("Provides ability to import assets. (created and update)")
    AssetsImport,

    @ApiModelProperty("Provides ability to remove assets")
    AssetsDelete,

    @ApiModelProperty("Provides ability to manage projects.")
    ProjectManage,

    @ApiModelProperty("Provides ability to create projects.")
    ProjectCreate,

    @ApiModelProperty("Provides ability to read project files from cloud storage.")
    ProjectFilesRead,

    @ApiModelProperty("Provides ability to store project files in cloud storage.")
    ProjectFilesWrite
}
