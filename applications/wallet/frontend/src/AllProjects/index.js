import Head from 'next/head'

import PageTitle from '../PageTitle'
import SuspenseBoundary from '../SuspenseBoundary'

import AllProjectsContent from './Content'

const AllProjects = () => {
  return (
    <>
      <Head>
        <title>All Projects</title>
      </Head>

      <PageTitle>All Projects</PageTitle>

      <SuspenseBoundary>
        <AllProjectsContent />
      </SuspenseBoundary>
    </>
  )
}

export default AllProjects
