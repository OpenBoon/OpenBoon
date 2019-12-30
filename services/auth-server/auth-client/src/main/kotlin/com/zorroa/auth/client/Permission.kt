package com.zorroa.auth.client

import io.swagger.annotations.ApiModelProperty

enum class Permission(val description: String) {

    @ApiModelProperty("Allows access to monitoring endpoints")
    SystemMonitor("Allows access to monitoring endpoints"),

    @ApiModelProperty("Allows access to platform management endpoints")
    SystemManage("Allows access to platform management endpoints"),

    @ApiModelProperty("Provides ability to switch projects")
    SystemProjectOverride("Provides ability to switch projects"),

    @ApiModelProperty("Provides ability to view encrypted project data")
    SystemProjectDecrypt("Provides ability to view encrypted project data"),

    @ApiModelProperty("Provides ability to manage API keys")
    ApiKeyManage("Provides ability to manage API keys"),

    @ApiModelProperty("Provides ability to read assets and associated files")
    AssetsRead("Provides ability to read assets and associated files"),

    @ApiModelProperty("Provides ability to import assets. (created and update)")
    AssetsImport("Provides ability to import assets. (created and update)"),

    @ApiModelProperty("Provides ability to remove assets")
    AssetsDelete("Provides ability to remove assets"),

    @ApiModelProperty("Povides ability to manage projects.")
    ProjectManage("Povides ability to manage projects."),

    @ApiModelProperty("Provides ability to create projects.")
    ProjectCreate("Provides ability to create projects.")
}
