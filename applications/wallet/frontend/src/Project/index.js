import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary from '../SuspenseBoundary'

import ProjectCards from './Cards'

const Project = () => {
  return (
    <>
      <Head>
        <title>Project Dashboard</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Account Overview', href: '/' },
          { title: 'Project Dashboard', href: false },
        ]}
      />

      <SuspenseBoundary>
        <ProjectCards />
      </SuspenseBoundary>
    </>
  )
}

export default Project
