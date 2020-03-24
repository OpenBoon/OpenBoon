import Head from 'next/head'

import PageTitle from '../PageTitle'
import SuspenseBoundary from '../SuspenseBoundary'

import AccountCards from './Cards'

const Account = () => {
  return (
    <>
      <Head>
        <title>Account Overview</title>
      </Head>

      <PageTitle>Account Overview</PageTitle>

      <SuspenseBoundary>
        <AccountCards />
      </SuspenseBoundary>
    </>
  )
}

export default Account
