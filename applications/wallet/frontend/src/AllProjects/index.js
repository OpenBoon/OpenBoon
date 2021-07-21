import Head from 'next/head'

import SuspenseBoundary from '../SuspenseBoundary'

import AllProjectsContent from './Content'

const AllProjects = () => {
  return (
    <>
      <Head>
        <title>All Projects</title>
      </Head>

      <SuspenseBoundary>
        <AllProjectsContent />
      </SuspenseBoundary>
    </>
  )
}

export default AllProjects
