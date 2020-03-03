import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

import ProjectUsersEditForm from './Form'

const ProjectUsersEdit = () => {
  const {
    query: { projectId, userId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>User Admin</title>
      </Head>

      <PageTitle>Project User Admin</PageTitle>

      <Tabs
        tabs={[
          { title: 'View All', href: '/[projectId]/users' },
          { title: 'Add User(s)', href: '/[projectId]/users/add' },
          { title: 'Edit User', href: '/[projectId]/users/[userId]/edit' },
        ]}
      />

      <ProjectUsersEditForm
        projectId={projectId}
        userId={parseInt(userId, 10)}
      />
    </>
  )
}

export default ProjectUsersEdit
