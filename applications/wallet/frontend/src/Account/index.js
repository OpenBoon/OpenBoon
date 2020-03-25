import Head from 'next/head'

import PageTitle from '../PageTitle'
import SuspenseBoundary from '../SuspenseBoundary'

import AccountContent from './Content'

const Account = () => {
  return (
    <>
      <Head>
        <title>Account Overview</title>
      </Head>

      <PageTitle>Account Overview</PageTitle>

      <SuspenseBoundary>
        <AccountContent />
      </SuspenseBoundary>
    </>
  )
}

export default Account
