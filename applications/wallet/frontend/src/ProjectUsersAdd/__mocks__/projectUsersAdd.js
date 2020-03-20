const projectUsersAdd = {
  results: {
    succeeded: [
      {
        email: 'tester@fake.com',
        roles: ['ML_Tools', 'API_Keys'],
        permissions: ['AssetsRead'],
        statusCode: 201,
        body: {
          id: 6,
          url:
            'https://wallet.zmlp.zorroa.com/api/v1/projects/00000000-0000-0000-0000-000000000000/users/6/',
          username: 'tester@fake.com',
          firstName: '',
          lastName: '',
          email: '',
          isActive: true,
          isStaff: false,
          isSuperuser: false,
          lastLogin: null,
          dateJoined: '2020-02-12T18:15:50Z',
          roles: ['ML_Tools', 'API_Keys'],
          permissions: ['AssetsRead'],
        },
      },
    ],
    failed: [
      {
        email: 'danny@zorroa.com',
        roles: ['ML_Tools', 'API_Keys'],
        permissions: ['AssetsRead'],
        statusCode: 201,
        body: {
          id: 4,
          url:
            'https://wallet.zmlp.zorroa.com/api/v1/projects/00000000-0000-0000-0000-000000000000/users/4/',
          username: 'danny@zorroa.com',
          firstName: 'Danny',
          lastName: 'Tiesling',
          email: 'danny@zorroa.com',
          isActive: true,
          isStaff: false,
          isSuperuser: false,
          lastLogin: '2020-02-07T02:34:48.138187Z',
          dateJoined: '2020-01-31T17:19:46.003951Z',
          roles: ['ML_Tools', 'API_Keys'],
          permissions: ['AssetsRead'],
        },
      },
    ],
  },
}

export default projectUsersAdd
