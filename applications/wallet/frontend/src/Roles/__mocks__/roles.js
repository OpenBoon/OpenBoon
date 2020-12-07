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
      description:
        'Allows adding and removing users as well as managing their roles. This includes adding and removing their own roles.',
      permissions: ['ProjectManage'],
    },
  ],
}

export default roles
