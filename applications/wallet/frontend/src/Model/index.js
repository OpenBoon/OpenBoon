import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelContent from './Content'

const Model = () => {
  return (
    <>
      <Head>
        <title>Model Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Custom Models', href: '/[projectId]/models', isBeta: true },
          { title: 'Model Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelContent />
      </SuspenseBoundary>
    </>
  )
}

export default Model
