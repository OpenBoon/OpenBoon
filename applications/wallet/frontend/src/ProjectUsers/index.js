import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import FormSuccess from '../FormSuccess'
import Tabs from '../Tabs'
import Table from '../Table'

import ProjectUsersRow from './Row'

const ProjectUsers = () => {
  const {
    query: { projectId, action },
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
          { title: 'Invite User(s)', href: '/[projectId]/users/add' },
        ]}
      />

      {action === 'edit-user-success' && (
        <FormSuccess>User Permissions Saved</FormSuccess>
      )}

      <div>&nbsp;</div>

      <Table
        url={`/api/v1/projects/${projectId}/users/`}
        columns={['Email', 'Permissions', '#Actions#']}
        expandColumn={2}
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
