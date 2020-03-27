import Head from 'next/head'

import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import VisualizerContent from './Content'

const Visualizer = () => {
  return (
    <>
      <Head>
        <title>Visualizer</title>
      </Head>

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <VisualizerContent />
      </SuspenseBoundary>
    </>
  )
}

export default Visualizer
