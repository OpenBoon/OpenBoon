import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

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

      <SuspenseBoundary role={ROLES.User_Admin}>
        <ProjectUsersEditForm
          projectId={projectId}
          userId={parseInt(userId, 10)}
        />
      </SuspenseBoundary>
    </>
  )
}

export default ProjectUsersEdit
