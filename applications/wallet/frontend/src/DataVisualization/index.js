import Head from 'next/head'

import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import DataVisualizationContent from './Content'

const DataVisualization = () => {
  return (
    <>
      <Head>
        <title>Data Visualization</title>
      </Head>

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <DataVisualizationContent />
      </SuspenseBoundary>
    </>
  )
}

export default DataVisualization
