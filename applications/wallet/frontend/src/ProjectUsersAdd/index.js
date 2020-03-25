import { useRouter } from 'next/router'
import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import SuspenseBoundary from '../SuspenseBoundary'

import ProjectUsersAddForm from './Form'

const ProjectUsersAdd = () => {
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
          { title: 'View All', href: '/[projectId]/users' },
          { title: 'Add User(s)', href: '/[projectId]/users/add' },
        ]}
      />

      <SuspenseBoundary>
        <ProjectUsersAddForm key={projectId} />
      </SuspenseBoundary>
    </>
  )
}

export default ProjectUsersAdd
