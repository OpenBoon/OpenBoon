import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ProjectUsersAddForm from './Form'

const ProjectUsersAdd = () => {
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
        ]}
      />

      <SuspenseBoundary role={ROLES.User_Admin}>
        <ProjectUsersAddForm />
      </SuspenseBoundary>
    </>
  )
}

export default ProjectUsersAdd
