import Head from 'next/head'

import PageTitle from '../PageTitle'
import SuspenseBoundary from '../SuspenseBoundary'

import ProjectCards from './Cards'

const Project = () => {
  return (
    <div>
      <Head>
        <title>Project Dashboard</title>
      </Head>

      <PageTitle>Project Dashboard</PageTitle>

      <SuspenseBoundary>
        <ProjectCards />
      </SuspenseBoundary>
    </div>
  )
}

export default Project
