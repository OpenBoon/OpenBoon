import Head from 'next/head'

import SuspenseBoundary from '../SuspenseBoundary'

import VisualizerContent from './Content'

const Visualizer = () => {
  return (
    <>
      <Head>
        <title>Visualizer</title>
      </Head>

      <SuspenseBoundary>
        <VisualizerContent />
      </SuspenseBoundary>
    </>
  )
}

export default Visualizer
