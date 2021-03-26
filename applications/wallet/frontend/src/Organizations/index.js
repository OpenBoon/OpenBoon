import Head from 'next/head'

import PageTitle from '../PageTitle'
import SuspenseBoundary from '../SuspenseBoundary'

import OrganizationsContent from './Content'

const Organizations = () => {
  return (
    <>
      <Head>
        <title>Organizations Admin</title>
      </Head>

      <PageTitle>Organizations Admin</PageTitle>

      <SuspenseBoundary>
        <OrganizationsContent />
      </SuspenseBoundary>
    </>
  )
}

export default Organizations
