import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'

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

      {action === 'edit-user-success' && (
        <div
          css={{
            display: 'flex',
            paddingTop: spacing.base,
          }}
        >
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            User roles saved.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View All', href: '/[projectId]/users' },
          { title: 'Add User(s)', href: '/[projectId]/users/add' },
        ]}
      />

      <Table
        role={ROLES.User_Admin}
        legend="Users"
        url={`/api/v1/projects/${projectId}/users/`}
        refreshKeys={[]}
        refreshButton={false}
        columns={['Email', 'Roles', '#Actions#']}
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
