import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import ModelLinkForm from './Form'

const Model = () => {
  return (
    <>
      <Head>
        <title>Model Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Custom Models', href: '/[projectId]/models', isBeta: true },
          { title: 'Model Details', href: '/[projectId]/models/[modelId]' },
          { title: 'Link Dataset', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <ModelLinkForm />
      </SuspenseBoundary>
    </>
  )
}

export default Model
