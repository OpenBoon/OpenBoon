package com.zorroa.auth.client

import io.swagger.annotations.ApiModelProperty

enum class Permission {
    @ApiModelProperty("Permission which allows access to monitoring endpoints")
    PlatformMonitor,

    @ApiModelProperty("Permission which allows access to platform management endpoints")
    PlatformManage,

    @ApiModelProperty("Permission which provides ability to switch projects")
    ProjectOverride,

    @ApiModelProperty("Permission which provides ability to view encrypted project data")
    ProjectDecrypt,

    @ApiModelProperty("Permission which provides ability to manage API keys")
    ApiKeyManage,

    @ApiModelProperty("Permission which provides ability to read assets and associated files")
    AssetsRead,

    @ApiModelProperty("Permission which provides ability to import assets. (created and update)")
    AssetsImport,

    @ApiModelProperty("Permission which provides ability to remove assets")
    AssetsDelete,

    @ApiModelProperty("Permission which provides ability to manage projects.")
    ProjectManage,

    @ApiModelProperty("Permission which provides ability to create projects.")
    ProjectCreate;
}
