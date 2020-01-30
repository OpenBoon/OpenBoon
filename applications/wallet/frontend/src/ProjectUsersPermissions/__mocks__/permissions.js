const permissions = {
  results: [
    {
      name: 'ManageApiKeys',
      description: 'Ability to manage API keys',
    },
    {
      name: 'Read',
      description: 'Ability to read assets and associated files',
    },
    {
      name: 'Import',
      description: 'Ability to import assets (create and update)',
    },
    {
      name: 'Delete',
      description: 'Ability to remove assets',
    },
    {
      name: 'ManageProjects',
      description: 'Ability to manage projects',
    },
    {
      name: 'CreateProjects',
      description: 'Ability to create projects',
    },
    {
      name: 'ReadProjects',
      description: 'Ability to read project files from cloud storage',
    },
  ],
}

export default permissions
