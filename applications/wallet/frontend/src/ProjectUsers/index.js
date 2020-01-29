import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import Table from '../Table'

import ProjectUsersRow from './Row'

const ProjectUsers = () => {
  const {
    query: { projectId },
  } = useRouter()

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
        ]}
      />

      <div>&nbsp;</div>

      <Table
        url={`/api/v1/projects/${projectId}/users/`}
        columns={['User Name', 'Email', 'Permissions', '#Actions#']}
        expandColumn={3}
        renderEmpty="No users"
        renderRow={({ result, revalidate }) => (
          <ProjectUsersRow
            key={result.id}
            projectId={projectId}
            user={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default ProjectUsers
