const roles = {
  results: [
    {
      name: 'ML_Tools',
      description:
        'Provides access to the Job Queue, Data Sources, and Visualizer.',
      permissions: ['AssetsRead', 'AssetsImport', 'AssetsDelete'],
    },
    {
      name: 'API_Keys',
      description: 'Provides access to API Key provisioning.',
      permissions: ['ProjectManage'],
    },
    {
      name: 'User_Admin',
      description: 'Provides access to User Administration for a Project.',
      permissions: ['ProjectManage'],
    },
  ],
}

export default roles
