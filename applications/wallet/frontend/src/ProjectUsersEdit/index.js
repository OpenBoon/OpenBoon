import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

const ProjectUsersEdit = () => {
  return (
    <>
      <Head>
        <title>User Admin</title>
      </Head>

      <PageTitle>Project User Admin</PageTitle>

      <Tabs
        tabs={[
          { title: 'Users', href: '/[projectId]/users' },
          { title: 'Create User', href: '/[projectId]/users/add' },
          { title: 'Edit User', href: '/[projectId]/users/[userId]/edit' },
        ]}
      />
    </>
  )
}

export default ProjectUsersEdit
