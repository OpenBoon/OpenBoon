import Head from 'next/head'

import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelMatrixContent from './Content'

const ModelMatrix = () => {
  return (
    <>
      <Head>
        <title>Confusion Matrix</title>
      </Head>

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelMatrixContent />
      </SuspenseBoundary>
    </>
  )
}

export default ModelMatrix
