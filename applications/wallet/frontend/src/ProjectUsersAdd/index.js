import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

const ProjectUsersAdd = () => {
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
    </>
  )
}

export default ProjectUsersAdd
