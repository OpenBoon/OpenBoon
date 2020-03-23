import Head from 'next/head'

import PageTitle from '../PageTitle'
import SuspenseBoundary from '../SuspenseBoundary'

import OverviewCards from './Cards'

const Overview = () => {
  return (
    <>
      <Head>
        <title>Account Overview</title>
      </Head>

      <PageTitle>Account Overview</PageTitle>

      <SuspenseBoundary>
        <OverviewCards />
      </SuspenseBoundary>
    </>
  )
}

export default Overview
