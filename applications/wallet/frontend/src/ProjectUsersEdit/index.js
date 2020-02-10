import Head from 'next/head'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import Loading from '../Loading'

import ProjectUsersEditForm from './Form'

const ProjectUsersEdit = () => {
  const {
    query: { projectId, userId },
  } = useRouter()

  const { data: user = {} } = useSWR(
    `/api/v1/projects/${projectId}/users/${userId}`,
  )
  const { data: { results: permissions } = {} } = useSWR(
    `/api/v1/projects/${projectId}/permissions/`,
  )

  return (
    <>
      <Head>
        <title>User Admin</title>
      </Head>

      <PageTitle>Project User Admin</PageTitle>

      <Tabs
        tabs={[
          { title: 'View All', href: '/[projectId]/users' },
          { title: 'Invite User(s)', href: '/[projectId]/users/add' },
          { title: 'Edit User', href: '/[projectId]/users/[userId]/edit' },
        ]}
      />

      {!user.id || !Array.isArray(permissions) ? (
        <Loading />
      ) : (
        <ProjectUsersEditForm
          projectId={projectId}
          user={user}
          permissions={permissions}
        />
      )}
    </>
  )
}

export default ProjectUsersEdit
