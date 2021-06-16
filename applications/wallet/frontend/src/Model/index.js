import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import ModelContent from './Content'
import ModelDataset from './Dataset'

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

        <Tabs
          tabs={[{ title: 'Dataset', href: '/[projectId]/models/[modelId]' }]}
        />

        <ModelDataset />
      </SuspenseBoundary>
    </>
  )
}

export default Model
