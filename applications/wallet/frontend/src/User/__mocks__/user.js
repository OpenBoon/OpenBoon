import { CURRENT_POLICIES_DATE } from '../../Policies/helpers'

const user = {
  id: 1,
  username: 'jane.doe',
  email: 'jane.doe@zorroa.com',
  firstName: 'Jane',
  lastName: 'Doe',
  groups: [],
  roles: {
    '76917058-b147-4556-987a-0a0f11e46d9b': [
      'ML_Tools',
      'User_Admin',
      'API_Keys',
    ],
    'e93cbadb-e5ae-4598-8395-4cf5b30c0e94': [],
  },
  organizations: ['42869703-fb62-4988-a0d1-e59b15caff06'],
  isActive: true,
  isStaff: true,
  isSuperuser: false,
  lastLogin: '2020-03-20T18:17:02.472778Z',
  dateJoined: '2020-03-02T19:09:16Z',
  projectId: '76917058-b147-4556-987a-0a0f11e46d9b',
  agreedToPoliciesDate: CURRENT_POLICIES_DATE,
}

export default user
