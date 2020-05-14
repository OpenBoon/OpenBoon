import Head from 'next/head'

import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import AssetContent from './Content'

const Asset = () => {
  return (
    <>
      <Head>
        <title>Asset</title>
      </Head>

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <AssetContent />
      </SuspenseBoundary>
    </>
  )
}

export default Asset
