const permissions = {
  results: [
    {
      name: 'SystemMonitor',
      description: 'Allows access to monitoring endpoints',
    },
    {
      name: 'SystemManage',
      description: 'Allows access to platform management endpoints',
    },
    {
      name: 'SystemProjectOverride',
      description: 'Provides ability to switch projects',
    },
    {
      name: 'SystemProjectDecrypt',
      description: 'Provides ability to view encrypted project data',
    },
    {
      name: 'ApiKeyManage',
      description: 'Provides ability to manage API keys',
    },
    {
      name: 'AssetsRead',
      description: 'Provides ability to read assets and associated files',
    },
    {
      name: 'AssetsImport',
      description: 'Provides ability to import assets. (created and update)',
    },
    {
      name: 'AssetsDelete',
      description: 'Provides ability to remove assets',
    },
    {
      name: 'ProjectManage',
      description: 'Provides ability to manage projects.',
    },
    {
      name: 'ProjectCreate',
      description: 'Provides ability to create projects.',
    },
    {
      name: 'ProjectFilesRead',
      description: 'Provides ability to read project files from cloud storage.',
    },
    {
      name: 'ProjectFilesWrite',
      description: 'Provides ability to store project files in cloud storage.',
    },
  ],
}

export default permissions
